package com.smartcbwtf.controller;

import com.smartcbwtf.config.JwtAuthFilter;
import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.OAuthScopeRegistry;
import com.smartcbwtf.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class OAuthController {
    private static final int MAX_CLIENT_ID_LENGTH = 120;
    private static final int MAX_CLIENT_SECRET_LENGTH = 512;
    private static final int MAX_CODE_LENGTH = 512;
    private static final int MAX_REDIRECT_URI_LENGTH = 2048;
    private static final int MAX_SCOPE_LENGTH = 4000;
    private static final int MAX_STATE_LENGTH = 500;
    private static final int MAX_PKCE_LENGTH = 128;

    private final OAuthService oauthService;
    private final AppUserRepository appUserRepository;
    private final OAuthScopeRegistry scopeRegistry;

    public OAuthController(OAuthService oauthService,
            AppUserRepository appUserRepository, OAuthScopeRegistry scopeRegistry) {
        this.oauthService = oauthService;
        this.appUserRepository = appUserRepository;
        this.scopeRegistry = scopeRegistry;
    }

    @GetMapping("/oauth/authorize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "code_challenge_method", defaultValue = "S256") String codeChallengeMethod) {
        requireResponseType(responseType);
        String safeClientId = requireValue(clientId, "client_id", MAX_CLIENT_ID_LENGTH);
        String safeRedirectUri = requireValue(redirectUri, "redirect_uri", MAX_REDIRECT_URI_LENGTH);
        String safeScope = optionalValue(scope, "scope", MAX_SCOPE_LENGTH);
        String safeState = optionalValue(state, "state", MAX_STATE_LENGTH);
        String safeCodeChallenge = requireValue(codeChallenge, "code_challenge", MAX_PKCE_LENGTH);
        String safeCodeChallengeMethod = requireValue(codeChallengeMethod, "code_challenge_method", 16);
        AppUser user = currentUser();
        OAuthService.OAuthAuthorizationResult result = oauthService.createAuthorizationCode(
                new OAuthService.AuthorizationCodeCommand(safeClientId, safeRedirectUri, safeScope, safeState,
                        safeCodeChallenge, safeCodeChallengeMethod),
                user);

        UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(result.redirectUri())
                .queryParam("code", result.code());
        if (StringUtils.hasText(result.state())) {
            redirect.queryParam("state", result.state());
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect.toUriString()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> token(@ModelAttribute OAuthTokenRequest request, HttpServletRequest httpRequest) {
        String grantType = requireValue(request.grant_type(), "grant_type", 64);
        ClientAuth clientAuth = clientAuth(httpRequest, request.client_id(), request.client_secret());
        OAuthService.TokenResponse response = switch (grantType) {
            case "authorization_code" -> oauthService.exchangeAuthorizationCode(
                    clientAuth.clientId(),
                    clientAuth.clientSecret(),
                    requireValue(request.code(), "code", MAX_CODE_LENGTH),
                    requireValue(request.redirect_uri(), "redirect_uri", MAX_REDIRECT_URI_LENGTH),
                    requireValue(request.code_verifier(), "code_verifier", MAX_PKCE_LENGTH));
            case "client_credentials" -> oauthService.clientCredentials(
                    clientAuth.clientId(),
                    clientAuth.clientSecret(),
                    optionalValue(request.scope(), "scope", MAX_SCOPE_LENGTH));
            case "refresh_token" -> oauthService.refreshToken(
                    clientAuth.clientId(),
                    clientAuth.clientSecret(),
                    requireValue(request.refresh_token(), "refresh_token", MAX_CODE_LENGTH));
            default -> throw new IllegalArgumentException("Unsupported grant_type: " + grantType);
        };
        return privateOAuthResponse(response);
    }

    @PostMapping(value = "/oauth/introspect", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> introspect(@ModelAttribute OAuthIntrospectionRequest request, HttpServletRequest httpRequest) {
        String token = requireValue(request.token(), "token", 8192);
        ClientAuth clientAuth = clientAuth(httpRequest, request.client_id(), request.client_secret());
        return privateOAuthResponse(oauthService.introspect(clientAuth.clientId(), clientAuth.clientSecret(), token));
    }

    @PostMapping(value = "/oauth/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> revoke(@ModelAttribute OAuthRevocationRequest request, HttpServletRequest httpRequest) {
        String token = requireValue(request.token(), "token", 8192);
        ClientAuth clientAuth = clientAuth(httpRequest, request.client_id(), request.client_secret());
        oauthService.revoke(clientAuth.clientId(), clientAuth.clientSecret(), token);
        return ResponseEntity.ok(Map.of("revoked", true));
    }

    @GetMapping("/oauth/userinfo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> userinfo(HttpServletRequest request) {
        AppUser user = currentUser();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", nullToEmpty(user.getUsername()));
        claims.put("user_id", user.getId() != null ? user.getId().toString() : "");
        claims.put("name", StringUtils.hasText(user.getFullName()) ? user.getFullName() : nullToEmpty(user.getUsername()));
        claims.put("email", nullToEmpty(user.getEmail()));
        claims.put("role", nullToEmpty(user.getRole()));
        claims.put("tenant_id", user.getFacility() != null ? user.getFacility().getId().toString() : "");
        claims.put("hcf_id", user.getHcf() != null ? user.getHcf().getId().toString() : "");
        claims.put("client_id", stringAttribute(request, JwtAuthFilter.ATTR_CLIENT_ID));
        claims.put("scope", stringAttribute(request, JwtAuthFilter.ATTR_SCOPES));
        return privateOAuthResponse(claims);
    }

    private String stringAttribute(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String requireValue(String value, String fieldName, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String cleaned = value.trim();
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private String optionalValue(String value, String fieldName, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String cleaned = value.trim();
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private void requireMaxLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
    }

    private void requireResponseType(String responseType) {
        String safeResponseType = requireValue(responseType, "response_type", 32);
        if (!"code".equals(safeResponseType)) {
            throw new IllegalArgumentException("Unsupported response_type: " + safeResponseType);
        }
    }

    @GetMapping("/api/integration/scopes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN','HCF_ADMIN','TOP_MANAGEMENT')")
    public Map<String, Object> scopes() {
        String currentRole = StringUtils.hasText(TenantContext.getRole()) ? TenantContext.getRole() : "UNKNOWN";
        var allowedScopes = scopeRegistry.allowedScopesForRole(currentRole);
        boolean superAdmin = "SUPER_ADMIN".equals(currentRole);
        return Map.of(
                "currentRole", currentRole,
                "allowedScopes", allowedScopes,
                "allScopes", superAdmin ? scopeRegistry.allScopes() : allowedScopes,
                "roleMatrix", superAdmin ? scopeRegistry.roleMatrix() : Map.of(currentRole, allowedScopes));
    }

    private AppUser currentUser() {
        String username = TenantContext.getUsername();
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Authenticated user not available");
        }
        return appUserRepository.findByUsername(username).orElseThrow();
    }

    private ClientAuth clientAuth(HttpServletRequest request, String clientId, String clientSecret) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header)) {
            if (!header.regionMatches(true, 0, "Basic ", 0, 6)) {
                throw new IllegalArgumentException("Invalid OAuth client Authorization header");
            }
            if (StringUtils.hasText(clientId) || StringUtils.hasText(clientSecret)) {
                throw new IllegalArgumentException(
                        "OAuth client credentials must use either Authorization header or request body, not both");
            }
            String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid OAuth client Authorization header");
            }
            int separator = decoded.indexOf(':');
            if (separator > 0) {
                return new ClientAuth(
                        requireValue(decoded.substring(0, separator), "client_id", MAX_CLIENT_ID_LENGTH),
                        optionalValue(decoded.substring(separator + 1), "client_secret", MAX_CLIENT_SECRET_LENGTH));
            }
            throw new IllegalArgumentException("Invalid OAuth client Authorization header");
        }
        return new ClientAuth(
                requireValue(clientId, "client_id", MAX_CLIENT_ID_LENGTH),
                optionalValue(clientSecret, "client_secret", MAX_CLIENT_SECRET_LENGTH));
    }

    private ResponseEntity<?> privateOAuthResponse(Object body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    public record OAuthTokenRequest(
            @NotBlank String grant_type,
            String client_id,
            String client_secret,
            String code,
            String redirect_uri,
            String code_verifier,
            String refresh_token,
            String scope) {
    }

    public record OAuthIntrospectionRequest(String client_id, String client_secret, @NotBlank String token) {
    }

    public record OAuthRevocationRequest(String client_id, String client_secret, @NotBlank String token) {
    }

    private record ClientAuth(String clientId, String clientSecret) {
    }
}
