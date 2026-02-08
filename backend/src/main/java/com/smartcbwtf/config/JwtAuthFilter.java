package com.smartcbwtf.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final com.smartcbwtf.repository.AppUserRepository appUserRepository;

    public JwtAuthFilter(JwtService jwtService, com.smartcbwtf.repository.AppUserRepository appUserRepository) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Claims claims = jwtService.parseClaims(token);
                    String username = claims.getSubject();
                    String userIdStr = claims.get("user_id", String.class);

                    if (!StringUtils.hasText(username) || !StringUtils.hasText(userIdStr)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token claims");
                        return;
                    }

                    java.util.UUID userId = parseUuidClaim(userIdStr, "user_id");
                    var user = appUserRepository.findById(userId).orElse(null);
                    if (user == null || !user.isActive()) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account disabled or not found");
                        return;
                    }

                    if (user.isLocked()) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account locked");
                        return;
                    }

                    if (!username.equals(user.getUsername())) {
                        log.warn("JWT subject mismatch for userId {}", userId);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
                        return;
                    }

                    String dbRole = user.getRole();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_" + dbRole)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    String tenantIdStr = claims.get("tenant_id", String.class);
                    String hcfIdStr = claims.get("hcf_id", String.class);
                    java.util.UUID tenantId = parseOptionalUuidClaim(tenantIdStr, "tenant_id");
                    java.util.UUID hcfId = parseOptionalUuidClaim(hcfIdStr, "hcf_id");

                    TenantContext.set(new TenantContext.TenantInfo(userId, tenantId, hcfId, dbRole, username));
                } catch (IllegalArgumentException e) {
                    log.warn("Rejected invalid JWT claim on {}: {}", request.getRequestURI(), e.getMessage());
                } catch (Exception e) {
                    log.warn("Rejected JWT on {}: {}", request.getRequestURI(), e.getMessage());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    private java.util.UUID parseUuidClaim(String value, String claimName) {
        try {
            return java.util.UUID.fromString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + claimName + " claim");
        }
    }

    private java.util.UUID parseOptionalUuidClaim(String value, String claimName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseUuidClaim(value, claimName);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.equals("/api/health")
                || path.equals("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/uploads/")
                || path.startsWith("/files/")
                || path.startsWith("/api/terms/latest");
    }
}
