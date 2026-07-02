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
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceSecurityTest {

    @Mock
    private OAuthClientRepository clientRepository;
    @Mock
    private OAuthAuthorizationCodeRepository authorizationCodeRepository;
    @Mock
    private OAuthRefreshTokenRepository refreshTokenRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private OAuthService service;
    private OAuthScopeRegistry scopeRegistry;

    @BeforeEach
    void setUp() {
        scopeRegistry = new OAuthScopeRegistry();
        service = new OAuthService(
                clientRepository,
                authorizationCodeRepository,
                refreshTokenRepository,
                appUserRepository,
                jwtService,
                passwordEncoder,
                scopeRegistry,
                15);
    }

    @Test
    void createClientRejectsUnsupportedGrantTypes() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new OAuthService.CreateClientCommand(
                        "client_ok",
                        "Client",
                        serviceAccountId,
                        "https://example.com/callback",
                        "smartcbwtf.facility.read",
                        "client_credentials password"),
                        UUID.randomUUID()));

        assertEquals("Unsupported OAuth grant type: password", thrown.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientRejectsExplicitOfflineAccessWithoutRefreshGrant() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new OAuthService.CreateClientCommand(
                        "client_ok",
                        "Client",
                        serviceAccountId,
                        "https://example.com/callback",
                        "offline_access smartcbwtf.facility.read",
                        "authorization_code"),
                        UUID.randomUUID()));

        assertEquals("offline_access requires refresh_token grant", thrown.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientOmitsImplicitOfflineAccessWhenRefreshGrantIsUnavailable() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-secret");

        service.createClient(new OAuthService.CreateClientCommand(
                "client_ok",
                "Client",
                serviceAccountId,
                null,
                null,
                "client_credentials"),
                UUID.randomUUID());

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertFalse(scopeRegistry.splitScopes(captor.getValue().getAllowedScopes()).contains("offline_access"));
        assertTrue(scopeRegistry.splitScopes(captor.getValue().getAllowedScopes()).contains("smartcbwtf.facility.read"));
    }

    @Test
    void createClientRejectsInsecureNonLoopbackRedirectUri() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new OAuthService.CreateClientCommand(
                        "client_ok",
                        "Client",
                        serviceAccountId,
                        "http://evil.example/callback",
                        "smartcbwtf.facility.read",
                        "authorization_code refresh_token"),
                        UUID.randomUUID()));

        assertEquals("redirect_uris must be absolute HTTPS URLs without fragments or user info; loopback HTTP is allowed for local development",
                thrown.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientRejectsRedirectUriFragments() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new OAuthService.CreateClientCommand(
                        "client_ok",
                        "Client",
                        serviceAccountId,
                        "https://app.example/callback#token",
                        "smartcbwtf.facility.read",
                        "authorization_code"),
                        UUID.randomUUID()));

        assertEquals("redirect_uris must be absolute HTTPS URLs without fragments or user info; loopback HTTP is allowed for local development",
                thrown.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientRejectsRedirectUriUserInfo() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createClient(new OAuthService.CreateClientCommand(
                        "client_ok",
                        "Client",
                        serviceAccountId,
                        "https://user@app.example/callback",
                        "smartcbwtf.facility.read",
                        "authorization_code"),
                        UUID.randomUUID()));

        assertEquals("redirect_uris must be absolute HTTPS URLs without fragments or user info; loopback HTTP is allowed for local development",
                thrown.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientAllowsHttpsAndLoopbackHttpRedirectUris() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-secret");

        service.createClient(new OAuthService.CreateClientCommand(
                "client_ok",
                "Client",
                serviceAccountId,
                "https://app.example/callback, http://localhost:3000/oauth/callback",
                "smartcbwtf.facility.read",
                "authorization_code refresh_token"),
                UUID.randomUUID());

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertEquals("https://app.example/callback http://localhost:3000/oauth/callback",
                captor.getValue().getRedirectUris());
    }

    @Test
    void createClientNormalizesNameBeforePersisting() {
        UUID serviceAccountId = UUID.randomUUID();
        when(appUserRepository.findById(serviceAccountId)).thenReturn(Optional.of(activeUser(serviceAccountId)));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-secret");

        service.createClient(new OAuthService.CreateClientCommand(
                "client_ok",
                "  Operations\nClient\t ",
                serviceAccountId,
                null,
                "smartcbwtf.facility.read",
                "client_credentials"),
                UUID.randomUUID());

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertEquals("Operations Client", captor.getValue().getName());
    }

    @Test
    void revokeOnlyRevokesTokensOwnedByCallingClient() {
        OAuthClient caller = client("client_a");
        OAuthClient owner = client("client_b");
        OAuthRefreshToken refreshToken = new OAuthRefreshToken();
        refreshToken.setClient(owner);
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(caller));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(service.hash("refresh-token"))).thenReturn(Optional.of(refreshToken));

        service.revoke("client_a", "secret", "refresh-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void introspectReturnsInactiveForTokenIssuedToDifferentClient() {
        OAuthClient caller = client("client_a");
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(caller));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.parseClaims("access-token")).thenReturn(claims);
        when(claims.get("token_use", String.class)).thenReturn(OAuthService.ACCESS_TOKEN_TYPE);
        when(claims.get("client_id", String.class)).thenReturn("client_b");

        Map<String, Object> response = service.introspect("client_a", "secret", "access-token");

        assertEquals(false, response.get("active"));
        verify(appUserRepository, never()).findById(any());
    }

    @Test
    void introspectReturnsInactiveForNonOauthAccessToken() {
        OAuthClient caller = client("client_a");
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(caller));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.parseClaims("session-token")).thenReturn(claims);
        when(claims.get("token_use", String.class)).thenReturn(null);

        Map<String, Object> response = service.introspect("client_a", "secret", "session-token");

        assertEquals(false, response.get("active"));
        verify(appUserRepository, never()).findById(any());
    }

    @Test
    void refreshTokenRejectsInactiveBoundUser() {
        OAuthClient client = client("client_a");
        client.setAllowedGrantTypes("refresh_token");
        AppUser inactiveUser = activeUser(UUID.randomUUID());
        inactiveUser.setActive(false);
        OAuthRefreshToken refreshToken = new OAuthRefreshToken();
        refreshToken.setClient(client);
        refreshToken.setUser(inactiveUser);
        refreshToken.setScope("offline_access smartcbwtf.facility.read");
        refreshToken.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(service.hash("refresh-token"))).thenReturn(Optional.of(refreshToken));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.refreshToken("client_a", "secret", "refresh-token"));

        assertEquals("Refresh token user is disabled or missing", thrown.getMessage());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void disableClientRevokesActiveRefreshTokens() {
        OAuthClient client = client("client_a");
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));
        when(refreshTokenRepository.revokeActiveTokensForClient(eq("client_a"), any(Instant.class))).thenReturn(2);

        OAuthService.ClientDisableResult result = service.disableClient("client_a");

        assertEquals("client_a", result.clientId());
        assertFalse(result.active());
        assertEquals(2, result.revokedRefreshTokens());
        assertFalse(client.isActive());
        verify(clientRepository).save(client);
        verify(refreshTokenRepository).revokeActiveTokensForClient(eq("client_a"), any(Instant.class));
    }

    @Test
    void publicTokenOperationsRejectBlankTokenInputs() {
        assertEquals("token is required",
                assertThrows(IllegalArgumentException.class,
                        () -> service.introspect("client_a", "secret", " ")).getMessage());
        assertEquals("token is required",
                assertThrows(IllegalArgumentException.class,
                        () -> service.revoke("client_a", "secret", "")).getMessage());
        assertEquals("refresh_token is required",
                assertThrows(IllegalArgumentException.class,
                        () -> service.refreshToken("client_a", "secret", null)).getMessage());
        verify(clientRepository, never()).findById(any());
    }

    @Test
    void tokenOperationsRejectOversizedPublicInputsBeforeRepositoryLookup() {
        assertEquals("client_id must be 120 characters or less",
                assertThrows(IllegalArgumentException.class,
                        () -> service.clientCredentials("x".repeat(121), "secret", null)).getMessage());
        assertEquals("token must be 8192 characters or less",
                assertThrows(IllegalArgumentException.class,
                        () -> service.introspect("client_a", "secret", "x".repeat(8193))).getMessage());

        verify(clientRepository, never()).findById(any());
    }

    @Test
    void clientCredentialsUsesConfiguredOAuthTokenTtl() {
        AppUser serviceAccount = activeUser(UUID.randomUUID());
        OAuthClient client = client("client_a");
        client.setServiceAccountUser(serviceAccount);
        client.setAllowedGrantTypes("client_credentials");
        client.setAllowedScopes("smartcbwtf.facility.read");
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(eq(serviceAccount.getUsername()), any(Map.class), eq(15L)))
                .thenReturn("access-token");

        OAuthService.TokenResponse response = service.clientCredentials(
                "client_a",
                "secret",
                "smartcbwtf.facility.read");

        assertEquals("access-token", response.access_token());
        assertEquals(900, response.expires_in());
        verify(jwtService).generateToken(eq(serviceAccount.getUsername()), any(Map.class), eq(15L));
    }

    @Test
    void clientCredentialsOmitsImplicitOfflineAccessScope() {
        AppUser serviceAccount = activeUser(UUID.randomUUID());
        OAuthClient client = client("client_a");
        client.setServiceAccountUser(serviceAccount);
        client.setAllowedGrantTypes("client_credentials refresh_token");
        client.setAllowedScopes("offline_access smartcbwtf.facility.read");
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(eq(serviceAccount.getUsername()), any(Map.class), eq(15L)))
                .thenReturn("access-token");

        OAuthService.TokenResponse response = service.clientCredentials("client_a", "secret", null);

        assertEquals("smartcbwtf.facility.read", response.scope());
        assertEquals("access-token", response.access_token());
    }

    @Test
    void clientCredentialsRejectsExplicitOfflineAccessScope() {
        AppUser serviceAccount = activeUser(UUID.randomUUID());
        OAuthClient client = client("client_a");
        client.setServiceAccountUser(serviceAccount);
        client.setAllowedGrantTypes("client_credentials refresh_token");
        client.setAllowedScopes("offline_access smartcbwtf.facility.read");
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.clientCredentials("client_a", "secret", "offline_access smartcbwtf.facility.read"));

        assertEquals("offline_access is not valid for client_credentials grant", thrown.getMessage());
    }

    @Test
    void authorizationCodeOmitsImplicitOfflineAccessForLegacyClientWithoutRefreshGrant() {
        AppUser user = activeUser(UUID.randomUUID());
        OAuthClient client = client("client_a");
        client.setAllowedGrantTypes("authorization_code");
        client.setAllowedScopes("offline_access smartcbwtf.facility.read");
        client.setRedirectUris("https://example.com/callback");
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));

        service.createAuthorizationCode(new OAuthService.AuthorizationCodeCommand(
                "client_a",
                "https://example.com/callback",
                null,
                null,
                validPkceValue(),
                "S256"),
                user);

        ArgumentCaptor<OAuthAuthorizationCode> captor = ArgumentCaptor.forClass(OAuthAuthorizationCode.class);
        verify(authorizationCodeRepository).save(captor.capture());
        assertEquals("smartcbwtf.facility.read", captor.getValue().getScope());
    }

    @Test
    void authorizationCodeRejectsExplicitOfflineAccessForLegacyClientWithoutRefreshGrant() {
        AppUser user = activeUser(UUID.randomUUID());
        OAuthClient client = client("client_a");
        client.setAllowedGrantTypes("authorization_code");
        client.setAllowedScopes("offline_access smartcbwtf.facility.read");
        client.setRedirectUris("https://example.com/callback");
        when(clientRepository.findById("client_a")).thenReturn(Optional.of(client));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createAuthorizationCode(new OAuthService.AuthorizationCodeCommand(
                        "client_a",
                        "https://example.com/callback",
                        "offline_access smartcbwtf.facility.read",
                        null,
                        validPkceValue(),
                        "S256"),
                        user));

        assertEquals("offline_access requires refresh_token grant", thrown.getMessage());
        verify(authorizationCodeRepository, never()).save(any());
    }

    @Test
    void authorizationCodeRejectsInvalidPkceChallengeBeforePersisting() {
        AppUser user = activeUser(UUID.randomUUID());

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.createAuthorizationCode(new OAuthService.AuthorizationCodeCommand(
                        "client_a",
                        "https://example.com/callback",
                        "smartcbwtf.facility.read",
                        null,
                        "short",
                        "S256"),
                        user));

        assertEquals("code_challenge must be 43-128 characters using letters, numbers, '-', '.', '_', or '~'",
                thrown.getMessage());
        verify(clientRepository, never()).findById(any());
        verify(authorizationCodeRepository, never()).save(any());
    }

    @Test
    void exchangeAuthorizationCodeRejectsInvalidPkceVerifierBeforeClientLookup() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> service.exchangeAuthorizationCode(
                        "client_a",
                        "secret",
                        "auth-code",
                        "https://example.com/callback",
                        "short"));

        assertEquals("code_verifier must be 43-128 characters using letters, numbers, '-', '.', '_', or '~'",
                thrown.getMessage());
        verify(clientRepository, never()).findById(any());
    }

    private OAuthClient client(String clientId) {
        OAuthClient client = new OAuthClient();
        client.setClientId(clientId);
        client.setClientSecretHash("hash");
        client.setActive(true);
        client.setConfidential(true);
        return client;
    }

    private AppUser activeUser(UUID id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("svc");
        user.setRole("CBWTF_ADMIN");
        user.setActive(true);
        return user;
    }

    private String validPkceValue() {
        return "A".repeat(43);
    }
}
