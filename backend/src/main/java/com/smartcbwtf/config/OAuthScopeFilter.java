package com.smartcbwtf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.service.OAuthScopeRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
public class OAuthScopeFilter extends OncePerRequestFilter {
    private final ObjectProvider<OAuthScopeRegistry> scopeRegistryProvider;
    private final ObjectMapper objectMapper;
    private final boolean exposeApiDocs;

    public OAuthScopeFilter(ObjectProvider<OAuthScopeRegistry> scopeRegistryProvider, ObjectMapper objectMapper,
            Environment environment) {
        this.scopeRegistryProvider = scopeRegistryProvider;
        this.objectMapper = objectMapper;
        this.exposeApiDocs = Binder.get(environment)
                .bind("app.security.expose-api-docs", Boolean.class)
                .orElse(false);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tokenUse = (String) request.getAttribute(JwtAuthFilter.ATTR_TOKEN_USE);
        String rawScopes = (String) request.getAttribute(JwtAuthFilter.ATTR_SCOPES);
        OAuthScopeRegistry scopeRegistry = scopeRegistryProvider.getIfAvailable();

        if (scopeRegistry == null || !JwtAuthFilter.TOKEN_USE_OAUTH_ACCESS.equals(tokenUse)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requiredScope = scopeRegistry.requiredScope(request.getMethod(), applicationPath(request));
        Set<String> grantedScopes = scopeRegistry.splitScopes(rawScopes);
        if (!StringUtils.hasText(requiredScope) || grantedScopes.contains(requiredScope)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String requestId = response.getHeader("X-Request-Id");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", Map.of(
                        "code", "MISSING_SCOPE",
                        "category", "missing_scope",
                        "message", "OAuth token is missing required scope: " + requiredScope,
                        "retryable", false,
                        "permanent", true,
                        "details", Map.of(
                                "required_scope", requiredScope,
                                "granted_scope", rawScopes == null ? "" : rawScopes),
                        "request_id", requestId == null ? "" : requestId,
                        "timestamp", Instant.now().toString())));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = applicationPath(request);
        return PublicEndpoints.isPublicPath(path, exposeApiDocs)
                || path.equals("/oauth/authorize");
    }

    private static String applicationPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && StringUtils.hasText(path) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return StringUtils.hasText(path) ? path : "/";
    }
}
