package com.smartcbwtf.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                    String role = claims.get("role", String.class);

                    if (StringUtils.hasText(username) && StringUtils.hasText(role)) {
                        // Set up Spring Security authentication
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Populate TenantContext for query scoping
                        String tenantIdStr = claims.get("tenant_id", String.class);
                        String hcfIdStr = claims.get("hcf_id", String.class);

                        java.util.UUID tenantId = tenantIdStr != null ? java.util.UUID.fromString(tenantIdStr) : null;
                        java.util.UUID hcfId = hcfIdStr != null ? java.util.UUID.fromString(hcfIdStr) : null;

                        TenantContext.set(new TenantContext.TenantInfo(tenantId, hcfId, role, username));
                    }
                } catch (Exception ignored) {
                    // On parse failure, fall through to reject by security chain
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // Always clear TenantContext after request
            TenantContext.clear();
        }
    }
}
