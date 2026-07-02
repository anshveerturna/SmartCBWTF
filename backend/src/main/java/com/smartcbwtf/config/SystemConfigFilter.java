package com.smartcbwtf.config;

import com.smartcbwtf.service.SystemConfigService;
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

/**
 * Filter to enforce system-wide settings like maintenance mode and login
 * disable.
 * Runs early in the filter chain to block requests before authentication.
 */
@Component
@Order(1)
public class SystemConfigFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigFilter.class);

    private final SystemConfigService configService;

    // Cache values to avoid DB calls on every request
    private volatile boolean maintenanceMode = false;
    private volatile String maintenanceMessage = "System is under maintenance";
    private volatile boolean disableAllLogins = false;
    private volatile boolean readonlyMode = false;
    private volatile long lastRefresh = 0;
    private static final long REFRESH_INTERVAL_MS = 30_000; // 30 seconds

    public SystemConfigFilter(SystemConfigService configService) {
        this.configService = configService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Refresh config values periodically
        refreshConfigIfNeeded();

        String path = request.getRequestURI();

        // Always allow health checks so probes keep working during maintenance.
        if (isHealthCheckPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow SuperAdmin to bypass maintenance mode
        boolean isSuperAdminRequest = isSuperAdminRequest(request);

        // Check maintenance mode
        if (maintenanceMode && !isSuperAdminRequest) {
            log.warn("Blocking request due to maintenance mode: {}", path);
            sendMaintenanceResponse(response);
            return;
        }

        // Check if logins are disabled (except for admin endpoints)
        if (disableAllLogins && path.equals("/api/auth/login")) {
            // Allow SuperAdmin login by checking username in request body
            // For GET requests, just block
            log.warn("Blocking login due to disable_all_logins: {}", path);
            sendLoginDisabledResponse(response);
            return;
        }

        // Check readonly mode for mutating requests
        if (readonlyMode && !isSuperAdminRequest && isMutatingRequest(request)) {
            log.warn("Blocking mutating request due to readonly mode: {} {}", request.getMethod(), path);
            sendReadonlyResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void refreshConfigIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh > REFRESH_INTERVAL_MS) {
            maintenanceMode = configService.getBoolean("platform.maintenance_mode", false);
            maintenanceMessage = configService.getString("platform.maintenance_message",
                    "System is undergoing scheduled maintenance. Please try again later.");
            disableAllLogins = configService.getBoolean("safety.disable_all_logins", false);
            readonlyMode = configService.getBoolean("safety.readonly_mode", false);
            lastRefresh = now;
        }
    }

    private boolean isSuperAdminRequest(HttpServletRequest request) {
        // Check if the request is authenticated as SuperAdmin
        // This is a simplified check - the actual auth check happens in security config
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null && "SUPER_ADMIN".equals(info.role());
    }

    private boolean isHealthCheckPath(String path) {
        return "/api/health".equals(path) || "/actuator/health".equals(path) || "/health".equals(path);
    }

    private boolean isMutatingRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (!("POST".equals(method) || "PUT".equals(method) ||
                "DELETE".equals(method) || "PATCH".equals(method))) {
            return false;
        }

        // Allow only essential auth operations even in readonly mode
        String path = request.getRequestURI();
        if ("/api/auth/login".equals(path)) {
            return false;
        }
        if (path.contains("/change-password") || path.contains("/password")) {
            return false;
        }

        return true; // Block all other mutations
    }

    private void sendMaintenanceResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
                "{\"code\":503,\"message\":\"%s\",\"maintenance\":true}",
                maintenanceMessage.replace("\"", "\\\"")));
    }

    private void sendLoginDisabledResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":503,\"message\":\"Login is temporarily disabled by administrator.\",\"loginDisabled\":true}");
    }

    private void sendReadonlyResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":503,\"message\":\"System is in read-only mode. Write operations are temporarily disabled.\",\"readonly\":true}");
    }
}
