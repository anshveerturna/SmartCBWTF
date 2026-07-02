package com.smartcbwtf.service;

import com.smartcbwtf.config.JwtService;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.OAuthAuthorizationCode;
import com.smartcbwtf.domain.OAuthClient;
import com.smartcbwtf.domain.OAuthRefreshToken;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.OAuthAuthorizationCodeRepository;
import com.smartcbwtf.repository.OAuthClientRepository;
import com.smartcbwtf.repository.OAuthRefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OAuthService {
    public static final String ACCESS_TOKEN_TYPE = "oauth_access_token";
    private static final String OFFLINE_ACCESS_SCOPE = "offline_access";
    private static final int MAX_CLIENT_ID_LENGTH = 120;
    private static final int MAX_CLIENT_SECRET_LENGTH = 512;
    private static final int MAX_CLIENT_NAME_LENGTH = 120;
    private static final int MAX_REDIRECT_URIS_LENGTH = 4000;
    private static final int MAX_REDIRECT_URI_LENGTH = 2048;
    private static final int MAX_SCOPE_LENGTH = 4000;
    private static final int MAX_GRANT_TYPES_LENGTH = 200;
    private static final int MAX_STATE_LENGTH = 500;
    private static final int MAX_TOKEN_LENGTH = 8192;
    private static final int MAX_CODE_LENGTH = 512;
    private static final int MAX_PKCE_METHOD_LENGTH = 16;
    private static final java.util.regex.Pattern PKCE_VALUE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._~-]{43,128}$");
    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
            "authorization_code",
            "refresh_token",
            "client_credentials");

    private final OAuthClientRepository clientRepository;
    private final OAuthAuthorizationCodeRepository authorizationCodeRepository;
    private final OAuthRefreshTokenRepository refreshTokenRepository;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OAuthScopeRegistry scopeRegistry;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long accessTokenTtlMinutes;

    public OAuthService(
            OAuthClientRepository clientRepository,
            OAuthAuthorizationCodeRepository authorizationCodeRepository,
            OAuthRefreshTokenRepository refreshTokenRepository,
            AppUserRepository appUserRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            OAuthScopeRegistry scopeRegistry,
            @Value("${security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        this.clientRepository = clientRepository;
        this.authorizationCodeRepository = authorizationCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.scopeRegistry = scopeRegistry;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    @Transactional
    public CreatedClient createClient(CreateClientCommand command, UUID createdBy) {
        if (command.serviceAccountUserId() == null) {
            throw new IllegalArgumentException("serviceAccountUserId is required");
        }
        String clientName = cleanLineRequired(command.name(), "name", MAX_CLIENT_NAME_LENGTH);
        AppUser serviceAccount = appUserRepository.findById(command.serviceAccountUserId())
                .orElseThrow(() -> new IllegalArgumentException("Service account user not found"));
        if (!serviceAccount.isActive()) {
            throw new IllegalArgumentException("OAuth client service account must be active");
        }
        String clientId = StringUtils.hasText(command.clientId())
                ? normalizeClientId(command.clientId())
                : "smartcbwtf_" + randomToken(18);
        if (clientRepository.existsById(clientId)) {
            throw new IllegalArgumentException("OAuth client already exists");
        }

        String grantTypes = normalizeGrantTypes(command.grantTypes());
        String redirectUris = normalizeRedirectUris(command.redirectUris(),
                scopeRegistry.splitScopes(grantTypes).contains("authorization_code"));

        List<String> scopes = scopeRegistry.normalizeAndValidate(
                command.scopes(),
                serviceAccount.getRole(),
                scopeRegistry.joinScopes(scopeRegistry.allowedScopesForRole(serviceAccount.getRole())));
        scopes = omitOrRejectOfflineAccessWithoutRefreshGrant(command.scopes(), grantTypes, scopes);

        String secret = randomToken(36);
        OAuthClient client = new OAuthClient();
        client.setClientId(clientId);
        client.setClientSecretHash(passwordEncoder.encode(secret));
        client.setName(clientName);
        client.setRedirectUris(redirectUris);
        client.setAllowedScopes(scopeRegistry.joinScopes(scopes));
        client.setAllowedGrantTypes(grantTypes);
        client.setServiceAccountUser(serviceAccount);
        client.setConfidential(true);
        client.setActive(true);
        client.setCreatedBy(createdBy);
        clientRepository.save(client);
        return new CreatedClient(toClientView(client), secret);
    }

    @Transactional
    public OAuthAuthorizationResult createAuthorizationCode(AuthorizationCodeCommand command, AppUser user) {
        String redirectUri = cleanLineRequired(command.redirectUri(), "redirect_uri", MAX_REDIRECT_URI_LENGTH);
        String scope = optionalCleanLine(command.scope(), "scope", MAX_SCOPE_LENGTH);
        String state = optionalCleanLine(command.state(), "state", MAX_STATE_LENGTH);
        String codeChallenge = cleanLineRequired(command.codeChallenge(), "code_challenge", 128);
        validatePkceValue(codeChallenge, "code_challenge");
        String method = optionalCleanLine(command.codeChallengeMethod(), "code_challenge_method",
                MAX_PKCE_METHOD_LENGTH);
        if (method == null) {
            method = "S256";
        }

        OAuthClient client = getActiveClient(command.clientId());
        assertGrantType(client, "authorization_code");
        assertRedirectUri(client, redirectUri);
        if (!"S256".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Only S256 PKCE is supported");
        }

        List<String> scopes = scopeRegistry.normalizeAndValidate(scope, user.getRole(), client.getAllowedScopes());
        scopes = omitOrRejectOfflineAccessWithoutRefreshGrant(scope, client.getAllowedGrantTypes(), scopes);
        String code = randomToken(32);
        OAuthAuthorizationCode authCode = new OAuthAuthorizationCode();
        authCode.setCodeHash(hash(code));
        authCode.setClient(client);
        authCode.setUser(user);
        authCode.setRedirectUri(redirectUri);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setCodeChallengeMethod("S256");
        authCode.setScope(scopeRegistry.joinScopes(scopes));
        authCode.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        authorizationCodeRepository.save(authCode);
        return new OAuthAuthorizationResult(code, redirectUri, state);
    }

    @Transactional
    public TokenResponse exchangeAuthorizationCode(String clientId, String clientSecret, String code,
            String redirectUri, String codeVerifier) {
        String safeCode = cleanLineRequired(code, "code", MAX_CODE_LENGTH);
        String safeRedirectUri = cleanLineRequired(redirectUri, "redirect_uri", MAX_REDIRECT_URI_LENGTH);
        String safeCodeVerifier = cleanLineRequired(codeVerifier, "code_verifier", 128);
        validatePkceValue(safeCodeVerifier, "code_verifier");
        OAuthClient client = authenticateClient(clientId, clientSecret);
        assertGrantType(client, "authorization_code");
        OAuthAuthorizationCode authCode = authorizationCodeRepository.findByCodeHash(hash(safeCode))
                .orElseThrow(() -> new IllegalArgumentException("Invalid authorization code"));
        if (!authCode.getClient().getClientId().equals(client.getClientId())) {
            throw new IllegalArgumentException("Authorization code was issued to a different client");
        }
        if (authCode.getConsumedAt() != null || authCode.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Authorization code expired or already consumed");
        }
        if (!authCode.getRedirectUri().equals(safeRedirectUri)) {
            throw new IllegalArgumentException("redirect_uri does not match authorization code");
        }
        if (!verifyPkce(safeCodeVerifier, authCode.getCodeChallenge())) {
            throw new IllegalArgumentException("Invalid PKCE verifier");
        }
        assertOfflineAccessAllowedByClient(authCode.getScope(), client);
        authCode.setConsumedAt(Instant.now());
        authorizationCodeRepository.save(authCode);
        return tokenPair(client, authCode.getUser(), authCode.getScope(), true, "authorization_code");
    }

    @Transactional
    public TokenResponse clientCredentials(String clientId, String clientSecret, String scope) {
        OAuthClient client = authenticateClient(clientId, clientSecret);
        assertGrantType(client, "client_credentials");
        AppUser serviceAccount = client.getServiceAccountUser();
        if (serviceAccount == null || !serviceAccount.isActive()) {
            throw new IllegalArgumentException("OAuth client is not bound to an active service account");
        }
        String safeScope = optionalCleanLine(scope, "scope", MAX_SCOPE_LENGTH);
        List<String> scopes = scopeRegistry.normalizeAndValidate(safeScope, serviceAccount.getRole(),
                client.getAllowedScopes());
        scopes = omitOrRejectOfflineAccessForClientCredentials(safeScope, scopes);
        return accessTokenOnly(client, serviceAccount, scopeRegistry.joinScopes(scopes), "client_credentials");
    }

    @Transactional
    public TokenResponse refreshToken(String clientId, String clientSecret, String refreshToken) {
        String safeRefreshToken = cleanLineRequired(refreshToken, "refresh_token", MAX_TOKEN_LENGTH);
        OAuthClient client = authenticateClient(clientId, clientSecret);
        assertGrantType(client, "refresh_token");
        OAuthRefreshToken token = refreshTokenRepository.findByTokenHash(hash(safeRefreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!token.getClient().getClientId().equals(client.getClientId())) {
            throw new IllegalArgumentException("Refresh token was issued to a different client");
        }
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }
        if (token.getUser() == null || !token.getUser().isActive()) {
            throw new IllegalArgumentException("Refresh token user is disabled or missing");
        }
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
        return tokenPair(client, token.getUser(), token.getScope(), true, "refresh_token");
    }

    @Transactional
    public void revoke(String clientId, String clientSecret, String token) {
        String safeToken = cleanLineRequired(token, "token", MAX_TOKEN_LENGTH);
        OAuthClient client = authenticateClient(clientId, clientSecret);
        refreshTokenRepository.findByTokenHash(hash(safeToken)).ifPresent(refreshToken -> {
            if (!refreshToken.getClient().getClientId().equals(client.getClientId())) {
                return;
            }
            refreshToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
        });
    }

    public Map<String, Object> introspect(String clientId, String clientSecret, String token) {
        String safeToken = cleanLineRequired(token, "token", MAX_TOKEN_LENGTH);
        OAuthClient client = authenticateClient(clientId, clientSecret);
        try {
            var claims = jwtService.parseClaims(safeToken);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get("token_use", String.class))) {
                return Map.of("active", false);
            }
            String tokenClientId = claims.get("client_id", String.class);
            if (!client.getClientId().equals(tokenClientId)) {
                return Map.of("active", false);
            }
            String userId = claims.get("user_id", String.class);
            AppUser user = appUserRepository.findById(UUID.fromString(userId)).orElse(null);
            if (user == null || !user.isActive()) {
                return Map.of("active", false);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("active", true);
            response.put("sub", claims.getSubject());
            response.put("iss", claims.getIssuer());
            response.put("client_id", claims.get("client_id", String.class));
            response.put("scope", claims.get("scope", String.class));
            response.put("role", user.getRole());
            response.put("user_id", user.getId().toString());
            response.put("tenant_id", user.getFacility() != null ? user.getFacility().getId().toString() : null);
            response.put("hcf_id", user.getHcf() != null ? user.getHcf().getId().toString() : null);
            response.put("exp", claims.getExpiration().toInstant().getEpochSecond());
            return response;
        } catch (Exception e) {
            return Map.of("active", false);
        }
    }

    public OAuthClient getActiveClient(String clientId) {
        String safeClientId = cleanLineRequired(clientId, "client_id", MAX_CLIENT_ID_LENGTH);
        OAuthClient client = clientRepository.findById(safeClientId)
                .orElseThrow(() -> new IllegalArgumentException("OAuth client not found"));
        if (!client.isActive()) {
            throw new IllegalArgumentException("OAuth client is disabled");
        }
        return client;
    }

    public ClientView toClientView(OAuthClient client) {
        AppUser serviceAccount = client.getServiceAccountUser();
        return new ClientView(
                client.getClientId(),
                client.getName(),
                client.getRedirectUris(),
                client.getAllowedScopes(),
                client.getAllowedGrantTypes(),
                client.isActive(),
                client.isConfidential(),
                serviceAccount != null ? serviceAccount.getId() : null,
                serviceAccount != null ? serviceAccount.getUsername() : null,
                serviceAccount != null ? serviceAccount.getRole() : null,
                client.getCreatedAt(),
                client.getUpdatedAt());
    }

    @Transactional
    public ClientDisableResult disableClient(String clientId) {
        OAuthClient client = getActiveClient(clientId);
        client.setActive(false);
        clientRepository.save(client);
        int revokedRefreshTokens = refreshTokenRepository.revokeActiveTokensForClient(
                client.getClientId(), Instant.now());
        return new ClientDisableResult(client.getClientId(), false, revokedRefreshTokens);
    }

    private OAuthClient authenticateClient(String clientId, String clientSecret) {
        OAuthClient client = getActiveClient(clientId);
        String safeClientSecret = optionalCleanLine(clientSecret, "client_secret", MAX_CLIENT_SECRET_LENGTH);
        if (client.isConfidential() && !passwordEncoder.matches(safeClientSecret == null ? "" : safeClientSecret,
                client.getClientSecretHash())) {
            throw new IllegalArgumentException("Invalid OAuth client credentials");
        }
        return client;
    }

    private TokenResponse tokenPair(OAuthClient client, AppUser user, String scope, boolean includeRefreshToken,
            String grantType) {
        String accessToken = buildAccessToken(client, user, scope, grantType);
        String refreshToken = null;
        if (includeRefreshToken && scopeRegistry.splitScopes(scope).contains(OFFLINE_ACCESS_SCOPE)) {
            assertOfflineAccessAllowedByClient(scope, client);
            refreshToken = randomToken(48);
            OAuthRefreshToken entity = new OAuthRefreshToken();
            entity.setTokenHash(hash(refreshToken));
            entity.setClient(client);
            entity.setUser(user);
            entity.setScope(scope);
            entity.setExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS));
            refreshTokenRepository.save(entity);
        }
        return new TokenResponse(accessToken, "Bearer", accessTokenTtlMinutes * 60, refreshToken, scope);
    }

    private List<String> omitOrRejectOfflineAccessWithoutRefreshGrant(String requestedScope, String grantTypes,
            List<String> scopes) {
        if (!scopes.contains(OFFLINE_ACCESS_SCOPE) || clientAllowsGrant(grantTypes, "refresh_token")) {
            return scopes;
        }
        if (requestedScopes(requestedScope).contains(OFFLINE_ACCESS_SCOPE)) {
            throw offlineAccessRequiresRefreshGrant();
        }
        return scopes.stream()
                .filter(scope -> !OFFLINE_ACCESS_SCOPE.equals(scope))
                .toList();
    }

    private List<String> omitOrRejectOfflineAccessForClientCredentials(String requestedScope, List<String> scopes) {
        if (!scopes.contains(OFFLINE_ACCESS_SCOPE)) {
            return scopes;
        }
        if (requestedScopes(requestedScope).contains(OFFLINE_ACCESS_SCOPE)) {
            throw new IllegalArgumentException("offline_access is not valid for client_credentials grant");
        }
        return scopes.stream()
                .filter(scope -> !OFFLINE_ACCESS_SCOPE.equals(scope))
                .toList();
    }

    private void assertOfflineAccessAllowedByClient(String scope, OAuthClient client) {
        if (scopeRegistry.splitScopes(scope).contains(OFFLINE_ACCESS_SCOPE)
                && !clientAllowsGrant(client.getAllowedGrantTypes(), "refresh_token")) {
            throw offlineAccessRequiresRefreshGrant();
        }
    }

    private boolean clientAllowsGrant(String grantTypes, String grantType) {
        return scopeRegistry.splitScopes(grantTypes).contains(grantType);
    }

    private Set<String> requestedScopes(String requestedScope) {
        String safeScope = optionalCleanLine(requestedScope, "scope", MAX_SCOPE_LENGTH);
        return scopeRegistry.splitScopes(StringUtils.hasText(safeScope) ? safeScope.replace(",", " ") : "");
    }

    private IllegalArgumentException offlineAccessRequiresRefreshGrant() {
        return new IllegalArgumentException("offline_access requires refresh_token grant");
    }

    private TokenResponse accessTokenOnly(OAuthClient client, AppUser user, String scope, String grantType) {
        return new TokenResponse(buildAccessToken(client, user, scope, grantType), "Bearer", accessTokenTtlMinutes * 60,
                null, scope);
    }

    private String buildAccessToken(OAuthClient client, AppUser user, String scope, String grantType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId().toString());
        claims.put("role", user.getRole());
        claims.put("full_name", user.getFullName());
        claims.put("profile_photo_url", user.getProfilePhotoUrl());
        claims.put("tenant_id", user.getFacility() != null ? user.getFacility().getId().toString() : null);
        claims.put("hcf_id", user.getHcf() != null ? user.getHcf().getId().toString() : null);
        claims.put("client_id", client.getClientId());
        claims.put("scope", scope);
        claims.put("token_use", ACCESS_TOKEN_TYPE);
        claims.put("grant_type", grantType);
        return jwtService.generateToken(user.getUsername(), claims, accessTokenTtlMinutes);
    }

    private String normalizeClientId(String clientId) {
        String normalized = clientId.trim();
        if (!normalized.matches("[A-Za-z0-9._-]{6,80}")) {
            throw new IllegalArgumentException(
                    "client_id must be 6-80 characters and contain only letters, numbers, dot, underscore, or hyphen");
        }
        return normalized;
    }

    private String normalizeGrantTypes(String grantTypes) {
        String safeGrantTypes = optionalCleanLine(grantTypes, "grant_types", MAX_GRANT_TYPES_LENGTH);
        Set<String> requested = scopeRegistry.splitScopes(
                StringUtils.hasText(safeGrantTypes)
                        ? safeGrantTypes.replace(",", " ")
                        : "authorization_code refresh_token client_credentials");
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("At least one OAuth grant type is required");
        }
        for (String grantType : requested) {
            if (!SUPPORTED_GRANT_TYPES.contains(grantType)) {
                throw new IllegalArgumentException("Unsupported OAuth grant type: " + grantType);
            }
        }
        return String.join(" ", requested);
    }

    private String normalizeRedirectUris(String redirectUris, boolean required) {
        String safeRedirectUris = optionalCleanLine(redirectUris, "redirect_uris", MAX_REDIRECT_URIS_LENGTH);
        Set<String> requested = scopeRegistry.splitScopes(
                StringUtils.hasText(safeRedirectUris) ? safeRedirectUris.replace(",", " ") : "");
        if (requested.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException("redirect_uris are required for authorization_code clients");
            }
            return "";
        }
        return String.join(" ", requested.stream()
                .map(this::validateRedirectUri)
                .toList());
    }

    private String validateRedirectUri(String redirectUri) {
        String safeRedirectUri = cleanLineRequired(redirectUri, "redirect_uri", MAX_REDIRECT_URI_LENGTH);
        URI uri;
        try {
            uri = URI.create(safeRedirectUri);
        } catch (IllegalArgumentException e) {
            throw invalidRedirectUri();
        }
        if (!uri.isAbsolute() || uri.getHost() == null || uri.getFragment() != null || uri.getUserInfo() != null) {
            throw invalidRedirectUri();
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) {
            return uri.toString();
        }
        if ("http".equals(scheme) && isLoopbackHost(uri.getHost())) {
            return uri.toString();
        }
        throw invalidRedirectUri();
    }

    private IllegalArgumentException invalidRedirectUri() {
        return new IllegalArgumentException(
                "redirect_uris must be absolute HTTPS URLs without fragments or user info; loopback HTTP is allowed for local development");
    }

    private boolean isLoopbackHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.equals("localhost")
                || normalized.startsWith("127.")
                || normalized.equals("::1")
                || normalized.equals("0:0:0:0:0:0:0:1");
    }

    private void assertRedirectUri(OAuthClient client, String redirectUri) {
        if (!scopeRegistry.splitScopes(client.getRedirectUris().replace(",", " ")).contains(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri is not registered for this OAuth client");
        }
    }

    private void assertGrantType(OAuthClient client, String grantType) {
        if (!scopeRegistry.splitScopes(client.getAllowedGrantTypes()).contains(grantType)) {
            throw new IllegalArgumentException("OAuth client is not allowed to use grant_type=" + grantType);
        }
    }

    private boolean verifyPkce(String codeVerifier, String expectedChallenge) {
        if (!StringUtils.hasText(codeVerifier)) {
            return false;
        }
        return pkceS256(codeVerifier).equals(expectedChallenge);
    }

    private void validatePkceValue(String value, String fieldName) {
        if (!PKCE_VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " must be 43-128 characters using letters, numbers, '-', '.', '_', or '~'");
        }
    }

    private String pkceS256(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate PKCE challenge", e);
        }
    }

    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private String randomToken(int bytes) {
        byte[] token = new byte[bytes];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String cleanLineRequired(String value, String fieldName, int maxLength) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private String optionalCleanLine(String value, String fieldName, int maxLength) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            return null;
        }
        requireMaxLength(cleaned, fieldName, maxLength);
        return cleaned;
    }

    private String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private void requireMaxLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
    }

    public record CreateClientCommand(
            String clientId,
            String name,
            UUID serviceAccountUserId,
            String redirectUris,
            String scopes,
            String grantTypes) {
    }

    public record CreatedClient(ClientView client, String clientSecret) {
    }

    public record ClientView(
            String clientId,
            String name,
            String redirectUris,
            String allowedScopes,
            String allowedGrantTypes,
            boolean active,
            boolean confidential,
            UUID serviceAccountUserId,
            String serviceAccountUsername,
            String serviceAccountRole,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ClientDisableResult(
            String clientId,
            boolean active,
            int revokedRefreshTokens) {
    }

    public record AuthorizationCodeCommand(
            String clientId,
            String redirectUri,
            String scope,
            String state,
            String codeChallenge,
            String codeChallengeMethod) {
    }

    public record OAuthAuthorizationResult(String code, String redirectUri, String state) {
    }

    public record TokenResponse(
            String access_token,
            String token_type,
            long expires_in,
            String refresh_token,
            String scope) {
    }
}
