package com.smartcbwtf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.ApiIdempotencyRecord;
import com.smartcbwtf.repository.ApiIdempotencyRecordRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 200;
    private static final int MAX_CACHED_REQUEST_BYTES = 1024 * 1024;
    private static final int MAX_CACHED_RESPONSE_BYTES = 1024 * 1024;

    private final ObjectProvider<ApiIdempotencyRecordRepository> idempotencyRepositoryProvider;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(ObjectProvider<ApiIdempotencyRecordRepository> idempotencyRepositoryProvider,
            ObjectMapper objectMapper) {
        this.idempotencyRepositoryProvider = idempotencyRepositoryProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (!shouldHandle(request, idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            idempotencyError(response, HttpServletResponse.SC_BAD_REQUEST, "IDEMPOTENCY_KEY_INVALID",
                    "idempotency_key_invalid", "Idempotency-Key must be 200 characters or fewer.", false, true,
                    Map.of("max_length", MAX_IDEMPOTENCY_KEY_LENGTH));
            return;
        }
        if (request.getContentLengthLong() > MAX_CACHED_REQUEST_BYTES) {
            idempotencyError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "IDEMPOTENCY_PAYLOAD_TOO_LARGE", "payload_too_large",
                    "Requests using Idempotency-Key must be 1 MB or smaller.", false, true,
                    Map.of("max_bytes", MAX_CACHED_REQUEST_BYTES));
            return;
        }
        ApiIdempotencyRecordRepository idempotencyRepository = idempotencyRepositoryProvider.getIfAvailable();
        if (idempotencyRepository == null) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyRequest cachedRequest;
        try {
            cachedRequest = new CachedBodyRequest(request, MAX_CACHED_REQUEST_BYTES);
        } catch (PayloadTooLargeException e) {
            idempotencyError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "IDEMPOTENCY_PAYLOAD_TOO_LARGE", "payload_too_large",
                    "Requests using Idempotency-Key must be 1 MB or smaller.", false, true,
                    Map.of("max_bytes", MAX_CACHED_REQUEST_BYTES));
            return;
        }
        String principalKey = principalKey();
        String scope = request.getMethod() + ":" + request.getRequestURI();
        String requestHash = hashRequest(request.getMethod(), request.getRequestURI(), request.getQueryString(),
                cachedRequest.body());

        var existing = idempotencyRepository.findByPrincipalKeyAndIdempotencyScopeAndIdempotencyKey(
                principalKey, scope, idempotencyKey);
        if (existing.isPresent()) {
            ApiIdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                conflict(response, idempotencyKey);
                return;
            }
            record.setReplayedAt(Instant.now());
            idempotencyRepository.save(record);
            response.setStatus(record.getResponseStatus());
            response.setHeader("X-Idempotent-Replay", "true");
            response.setHeader("X-Operation-Id", record.getOperationId());
            if (record.getResponseContentType() != null) {
                response.setContentType(record.getResponseContentType());
            }
            response.getWriter().write(record.getResponseBody() == null ? "" : record.getResponseBody());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(cachedRequest, wrappedResponse);
        cacheResponse(idempotencyRepository, principalKey, scope, idempotencyKey, requestHash, wrappedResponse);
        wrappedResponse.copyBodyToResponse();
    }

    private void cacheResponse(ApiIdempotencyRecordRepository idempotencyRepository, String principalKey, String scope,
            String idempotencyKey, String requestHash, ContentCachingResponseWrapper response) {
        byte[] body = response.getContentAsByteArray();
        String contentType = response.getContentType();
        if (response.getStatus() >= 500
                || body.length > MAX_CACHED_RESPONSE_BYTES
                || !isCacheableContent(contentType)
                || isNoStore(response.getHeader(HttpHeaders.CACHE_CONTROL))) {
            return;
        }

        ApiIdempotencyRecord record = new ApiIdempotencyRecord();
        record.setPrincipalKey(principalKey);
        record.setIdempotencyScope(scope);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setResponseStatus(response.getStatus());
        record.setResponseContentType(contentType);
        record.setResponseBody(new String(body, StandardCharsets.UTF_8));
        record.setOperationId("op_" + UUID.randomUUID());
        record.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        idempotencyRepository.save(record);
        response.setHeader("X-Operation-Id", record.getOperationId());
    }

    private void conflict(HttpServletResponse response, String idempotencyKey) throws IOException {
        idempotencyError(response, HttpServletResponse.SC_CONFLICT, "IDEMPOTENCY_CONFLICT",
                "idempotency_conflict", "Idempotency-Key was already used with a different request body.", false, true,
                Map.of("idempotency_key", idempotencyKey));
    }

    private boolean shouldHandle(HttpServletRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || !request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        if (!isMutation(request.getMethod())) {
            return false;
        }
        if (TenantContext.get() == null) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType == null
                || !contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private boolean isMutation(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean isCacheableContent(String contentType) {
        return contentType == null
                || contentType.contains(MediaType.APPLICATION_JSON_VALUE)
                || contentType.startsWith("text/");
    }

    private boolean isNoStore(String cacheControl) {
        return cacheControl != null && cacheControl.toLowerCase(Locale.ROOT).contains("no-store");
    }

    private String principalKey() {
        TenantContext.TenantInfo info = TenantContext.get();
        if (info == null) {
            throw new IllegalStateException("Idempotency requires authenticated tenant context");
        }
        return nullToEmpty(info.role()) + ":" + nullToEmpty(info.userId()) + ":" + nullToEmpty(info.tenantId()) + ":"
                + nullToEmpty(info.hcfId());
    }

    private void idempotencyError(HttpServletResponse response, int status, String code, String category,
            String message, boolean retryable, boolean permanent, Map<String, Object> details) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("category", category);
        error.put("message", message);
        error.put("retryable", retryable);
        error.put("permanent", permanent);
        error.put("details", details);
        error.put("request_id", response.getHeader("X-Request-Id"));
        error.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getWriter(), Map.of("error", error));
    }

    private String hashRequest(String method, String requestUri, String queryString, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(nullToEmpty(method).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(nullToEmpty(requestUri).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(nullToEmpty(queryString).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(body);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash idempotency request", e);
        }
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, int maxBodyBytes) throws IOException {
            super(request);
            this.body = readBody(request, maxBodyBytes);
        }

        byte[] body() {
            return body;
        }

        private static byte[] readBody(HttpServletRequest request, int maxBodyBytes) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            ServletInputStream inputStream = request.getInputStream();
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > maxBodyBytes) {
                    throw new PayloadTooLargeException();
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return inputStream.read();
                }

                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static class PayloadTooLargeException extends IOException {
    }
}
