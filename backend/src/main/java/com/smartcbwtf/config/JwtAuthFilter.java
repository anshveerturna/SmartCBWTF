package com.smartcbwtf.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
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
    public static final String ATTR_SCOPES = "smartcbwtf.oauth.scopes";
    public static final String ATTR_CLIENT_ID = "smartcbwtf.oauth.client_id";
    public static final String ATTR_TOKEN_USE = "smartcbwtf.oauth.token_use";
    public static final String TOKEN_USE_OAUTH_ACCESS = "oauth_access_token";

    private final JwtService jwtService;
    private final com.smartcbwtf.repository.AppUserRepository appUserRepository;
    private final ObjectProvider<com.smartcbwtf.repository.OAuthClientRepository> oAuthClientRepositoryProvider;
    private final boolean exposeApiDocs;

    public JwtAuthFilter(JwtService jwtService, com.smartcbwtf.repository.AppUserRepository appUserRepository,
            ObjectProvider<com.smartcbwtf.repository.OAuthClientRepository> oAuthClientRepositoryProvider,
            Environment environment) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.oAuthClientRepositoryProvider = oAuthClientRepositoryProvider;
        this.exposeApiDocs = Binder.get(environment)
                .bind("app.security.expose-api-docs", Boolean.class)
                .orElse(false);
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

                    String tokenUse = claims.get("token_use", String.class);
                    String clientId = claims.get("client_id", String.class);
                    if (TOKEN_USE_OAUTH_ACCESS.equals(tokenUse) && !isActiveOAuthClient(clientId)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth client disabled or not found");
                        return;
                    }

                    String dbRole = user.getRole();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_" + dbRole)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute(ATTR_SCOPES, claims.get("scope", String.class));
                    request.setAttribute(ATTR_CLIENT_ID, clientId);
                    request.setAttribute(ATTR_TOKEN_USE, tokenUse);

                    java.util.UUID tenantId = user.getFacility() != null ? user.getFacility().getId() : null;
                    java.util.UUID hcfId = user.getHcf() != null ? user.getHcf().getId() : null;

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

    private boolean isActiveOAuthClient(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return false;
        }
        var oAuthClientRepository = oAuthClientRepositoryProvider.getIfAvailable();
        if (oAuthClientRepository == null) {
            log.warn("OAuth access token rejected because OAuthClientRepository is not available");
            return false;
        }
        return oAuthClientRepository.findById(clientId)
                .map(com.smartcbwtf.domain.OAuthClient::isActive)
                .orElse(false);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicEndpoints.isPublicPath(request.getRequestURI(), exposeApiDocs);
    }
}
