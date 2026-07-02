package com.smartcbwtf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.ApiIdempotencyRecord;
import com.smartcbwtf.repository.ApiIdempotencyRecordRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private ObjectProvider<ApiIdempotencyRecordRepository> idempotencyRepositoryProvider;

    @Mock
    private ApiIdempotencyRecordRepository idempotencyRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void anonymousApiMutationsBypassIdempotencyRepository() throws Exception {
        IdempotencyFilter filter = new IdempotencyFilter(idempotencyRepositoryProvider, new ObjectMapper());
        MockHttpServletRequest request = jsonRequest("POST", "/api/public/contact", "contact-key",
                "{\"message\":\"hello\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, okJsonChain(chainCalls, HttpServletResponse.SC_ACCEPTED,
                "{\"accepted\":true}"));

        assertEquals(1, chainCalls.get());
        assertEquals(HttpServletResponse.SC_ACCEPTED, response.getStatus());
        assertEquals("{\"accepted\":true}", response.getContentAsString());
        assertNull(response.getHeader("X-Idempotent-Replay"));
        assertNull(response.getHeader("X-Operation-Id"));
        verifyNoInteractions(idempotencyRepositoryProvider);
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void authenticatedApiMutationsAreCachedPerTenantPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, tenantId, null, "CBWTF_ADMIN", "admin"));
        IdempotencyFilter filter = new IdempotencyFilter(idempotencyRepositoryProvider, new ObjectMapper());
        MockHttpServletRequest request = jsonRequest("POST", "/api/cbwtf/payments", "payment-key",
                "{\"amount\":1250}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();
        String expectedPrincipal = "CBWTF_ADMIN:" + userId + ":" + tenantId + ":";
        String expectedScope = "POST:/api/cbwtf/payments";
        when(idempotencyRepositoryProvider.getIfAvailable()).thenReturn(idempotencyRepository);
        when(idempotencyRepository.findByPrincipalKeyAndIdempotencyScopeAndIdempotencyKey(
                eq(expectedPrincipal), eq(expectedScope), eq("payment-key"))).thenReturn(Optional.empty());

        filter.doFilter(request, response, okJsonChain(chainCalls, HttpServletResponse.SC_CREATED,
                "{\"ok\":true}"));

        ArgumentCaptor<ApiIdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(ApiIdempotencyRecord.class);
        verify(idempotencyRepository).save(recordCaptor.capture());
        ApiIdempotencyRecord record = recordCaptor.getValue();
        assertEquals(1, chainCalls.get());
        assertEquals(expectedPrincipal, record.getPrincipalKey());
        assertEquals(expectedScope, record.getIdempotencyScope());
        assertEquals("payment-key", record.getIdempotencyKey());
        assertEquals(HttpServletResponse.SC_CREATED, record.getResponseStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, record.getResponseContentType());
        assertEquals("{\"ok\":true}", record.getResponseBody());
        assertNotNull(record.getOperationId());
        assertEquals(record.getOperationId(), response.getHeader("X-Operation-Id"));
    }

    @Test
    void noStoreMutationResponsesAreNotPersistedForReplay() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, tenantId, null, "CBWTF_ADMIN", "admin"));
        IdempotencyFilter filter = new IdempotencyFilter(idempotencyRepositoryProvider, new ObjectMapper());
        MockHttpServletRequest request = jsonRequest("POST", "/api/cbwtf/staff", "staff-key",
                "{\"fullName\":\"Driver One\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();
        String expectedPrincipal = "CBWTF_ADMIN:" + userId + ":" + tenantId + ":";
        String expectedScope = "POST:/api/cbwtf/staff";
        when(idempotencyRepositoryProvider.getIfAvailable()).thenReturn(idempotencyRepository);
        when(idempotencyRepository.findByPrincipalKeyAndIdempotencyScopeAndIdempotencyKey(
                eq(expectedPrincipal), eq(expectedScope), eq("staff-key"))).thenReturn(Optional.empty());

        filter.doFilter(request, response, noStoreJsonChain(chainCalls, HttpServletResponse.SC_CREATED,
                "{\"tempPassword\":\"Tmp@123456\"}"));

        assertEquals(1, chainCalls.get());
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        assertEquals("no-store", response.getHeader(HttpHeaders.CACHE_CONTROL));
        assertEquals("{\"tempPassword\":\"Tmp@123456\"}", response.getContentAsString());
        assertNull(response.getHeader("X-Operation-Id"));
        verify(idempotencyRepository, never()).save(any(ApiIdempotencyRecord.class));
    }

    @Test
    void idempotencyKeyLongerThanStorageColumnIsRejected() throws Exception {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), UUID.randomUUID(), null, "CBWTF_ADMIN",
                "admin"));
        IdempotencyFilter filter = new IdempotencyFilter(idempotencyRepositoryProvider, new ObjectMapper());
        MockHttpServletRequest request = jsonRequest("POST", "/api/cbwtf/payments", "k".repeat(201),
                "{\"amount\":1250}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, okJsonChain(chainCalls, HttpServletResponse.SC_CREATED,
                "{\"ok\":true}"));

        assertEquals(0, chainCalls.get());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        verifyNoInteractions(idempotencyRepositoryProvider);
        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    void idempotencyPayloadOverOneMbIsRejectedBeforeBodyCaching() throws Exception {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), UUID.randomUUID(), null, "CBWTF_ADMIN",
                "admin"));
        IdempotencyFilter filter = new IdempotencyFilter(idempotencyRepositoryProvider, new ObjectMapper());
        MockHttpServletRequest request = jsonRequest("POST", "/api/mobile/gps/ping", "gps-key",
                "x".repeat((1024 * 1024) + 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(request, response, okJsonChain(chainCalls, HttpServletResponse.SC_CREATED,
                "{\"ok\":true}"));

        assertEquals(0, chainCalls.get());
        assertEquals(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        verifyNoInteractions(idempotencyRepositoryProvider);
        verifyNoInteractions(idempotencyRepository);
    }

    private MockHttpServletRequest jsonRequest(String method, String path, String idempotencyKey, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Idempotency-Key", idempotencyKey);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private FilterChain okJsonChain(AtomicInteger chainCalls, int status, String body) {
        return (request, response) -> {
            chainCalls.incrementAndGet();
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(status);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.getWriter().write(body);
        };
    }

    private FilterChain noStoreJsonChain(AtomicInteger chainCalls, int status, String body) {
        return (request, response) -> {
            chainCalls.incrementAndGet();
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(status);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            httpResponse.getWriter().write(body);
        };
    }
}
