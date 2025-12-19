package com.smartcbwtf.config;

import com.smartcbwtf.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that blocks requests from expired/suspended tenants.
 * Runs AFTER JwtAuthFilter to enforce subscription status.
 */
@Component
@Order(2) // After JwtAuthFilter
public class SubscriptionGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionGuardFilter.class);

    // Paths exempt from subscription check
    private static final Set<String> EXEMPT_PATH_PREFIXES = Set.of(
            "/api/auth/", // Login/logout always allowed
            "/api/admin/", // SuperAdmin can always access
            "/api/payment/", // Payment endpoints for reactivation
            "/api/config/mobile", // Mobile app config check
            "/actuator/", // Health checks
            "/swagger", // API docs
            "/v3/api-docs" // OpenAPI
    );

    private final SubscriptionService subscriptionService;

    public SubscriptionGuardFilter(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip exempt paths
        if (isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if no tenant context (unauthenticated or SuperAdmin)
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // No tenant = either unauthenticated (will fail later) or SuperAdmin
            filterChain.doFilter(request, response);
            return;
        }

        // Check if SuperAdmin (they bypass subscription checks)
        if (TenantContext.isSuperAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check subscription status
        if (!subscriptionService.isActive(tenantId)) {
            log.warn("Blocked request from expired/suspended tenant: {}", tenantId);
            sendSubscriptionInactiveResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        for (String prefix : EXEMPT_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void sendSubscriptionInactiveResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write("""
                        {
                            "code": "SUBSCRIPTION_INACTIVE",
                            "message": "Your subscription has expired or been suspended. Please contact your administrator or renew your subscription.",
                            "timestamp": "%s"
                        }
                        """
                        .formatted(java.time.Instant.now().toString()));
    }
}
