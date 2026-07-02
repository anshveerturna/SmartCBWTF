package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.OAuthScopeRegistry;
import com.smartcbwtf.service.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OAuthControllerTest {

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private OAuthService oauthService;
    private OAuthController controller;

    @BeforeEach
    void setUp() {
        oauthService = mock(OAuthService.class);
        controller = new OAuthController(oauthService, appUserRepository, new OAuthScopeRegistry());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void userinfoToleratesNullableProfileFields() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("driver1");
        user.setRole("DRIVER");

        TenantContext.set(new TenantContext.TenantInfo(userId, UUID.randomUUID(), null, "DRIVER", "driver1"));
        when(appUserRepository.findByUsername("driver1")).thenReturn(Optional.of(user));

        var response = controller.userinfo(mock(HttpServletRequest.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) response.getBody();

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        assertEquals("driver1", claims.get("sub"));
        assertEquals(userId.toString(), claims.get("user_id"));
        assertEquals("driver1", claims.get("name"));
        assertEquals("", claims.get("email"));
        assertEquals("", claims.get("tenant_id"));
        assertEquals("", claims.get("hcf_id"));
        assertEquals("", claims.get("client_id"));
        assertEquals("", claims.get("scope"));
    }

    @Test
    void tokenRejectsMissingGrantTypeBeforeServiceCall() {
        OAuthController.OAuthTokenRequest request = new OAuthController.OAuthTokenRequest(
                null, "client", "secret", null, null, null, null, null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.token(request, mock(HttpServletRequest.class)));

        assertEquals("grant_type is required", thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void tokenRejectsMalformedBasicAuthBeforeServiceCall() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("Authorization")).thenReturn("Basic not-base64");
        OAuthController.OAuthTokenRequest request = new OAuthController.OAuthTokenRequest(
                "client_credentials", null, null, null, null, null, null, "smartcbwtf.facility.read");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.token(request, httpRequest));

        assertEquals("Invalid OAuth client Authorization header", thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void tokenRejectsUnsupportedAuthorizationSchemeBeforeServiceCall() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer session-token");
        OAuthController.OAuthTokenRequest request = new OAuthController.OAuthTokenRequest(
                "client_credentials", "client_a", "secret", null, null, null, null,
                "smartcbwtf.facility.read");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.token(request, httpRequest));

        assertEquals("Invalid OAuth client Authorization header", thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void tokenRejectsMixedHeaderAndBodyClientCredentialsBeforeServiceCall() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("Authorization")).thenReturn("Basic Y2xpZW50X2E6c2VjcmV0");
        OAuthController.OAuthTokenRequest request = new OAuthController.OAuthTokenRequest(
                "client_credentials", "client_a", null, null, null, null, null,
                "smartcbwtf.facility.read");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.token(request, httpRequest));

        assertEquals("OAuth client credentials must use either Authorization header or request body, not both",
                thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void tokenRejectsOversizedClientSecretBeforeServiceCall() {
        OAuthController.OAuthTokenRequest request = new OAuthController.OAuthTokenRequest(
                "client_credentials",
                "client_a",
                "x".repeat(513),
                null,
                null,
                null,
                null,
                "smartcbwtf.facility.read");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.token(request, mock(HttpServletRequest.class)));

        assertEquals("client_secret must be 512 characters or less", thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void tokenSuccessUsesNoStoreNoCacheHeaders() {
        OAuthController.OAuthTokenRequest request = new OAuthController.OAuthTokenRequest(
                "client_credentials",
                "client_a",
                "secret",
                null,
                null,
                null,
                null,
                "smartcbwtf.facility.read");
        OAuthService.TokenResponse tokenResponse = new OAuthService.TokenResponse(
                "access-token", "Bearer", 900, null, "smartcbwtf.facility.read");
        when(oauthService.clientCredentials("client_a", "secret", "smartcbwtf.facility.read"))
                .thenReturn(tokenResponse);

        var response = controller.token(request, mock(HttpServletRequest.class));

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        assertEquals(tokenResponse, response.getBody());
    }

    @Test
    void authorizeSuccessUsesNoStoreNoCacheHeaders() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("admin");
        user.setRole("CBWTF_ADMIN");
        TenantContext.set(new TenantContext.TenantInfo(userId, UUID.randomUUID(), null, "CBWTF_ADMIN", "admin"));
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(oauthService.createAuthorizationCode(any(OAuthService.AuthorizationCodeCommand.class), any(AppUser.class)))
                .thenReturn(new OAuthService.OAuthAuthorizationResult(
                        "auth-code",
                        "https://example.com/callback",
                        "opaque-state"));

        var response = controller.authorize(
                "code",
                "client_a",
                "https://example.com/callback",
                "smartcbwtf.facility.read",
                "opaque-state",
                "A".repeat(43),
                "S256");

        assertEquals(302, response.getStatusCode().value());
        assertEquals("https://example.com/callback?code=auth-code&state=opaque-state",
                response.getHeaders().getLocation().toString());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
    }

    @Test
    void authorizeRejectsOversizedStateBeforeUserLookupOrServiceCall() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.authorize(
                        "code",
                        "client_a",
                        "https://example.com/callback",
                        "smartcbwtf.facility.read",
                        "x".repeat(501),
                        "A".repeat(43),
                        "S256"));

        assertEquals("state must be 500 characters or less", thrown.getMessage());
        verify(appUserRepository, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(oauthService);
    }

    @Test
    void authorizeRejectsUnsupportedResponseTypeBeforeUserLookupOrServiceCall() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.authorize(
                        "token",
                        "client_a",
                        "https://example.com/callback",
                        "smartcbwtf.facility.read",
                        null,
                        "A".repeat(43),
                        "S256"));

        assertEquals("Unsupported response_type: token", thrown.getMessage());
        verify(appUserRepository, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(oauthService);
    }

    @Test
    void introspectRejectsMissingTokenBeforeServiceCall() {
        OAuthController.OAuthIntrospectionRequest request = new OAuthController.OAuthIntrospectionRequest(
                "client", "secret", " ");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.introspect(request, mock(HttpServletRequest.class)));

        assertEquals("token is required", thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void introspectRejectsOversizedTokenBeforeServiceCall() {
        OAuthController.OAuthIntrospectionRequest request = new OAuthController.OAuthIntrospectionRequest(
                "client", "secret", "x".repeat(8193));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> controller.introspect(request, mock(HttpServletRequest.class)));

        assertEquals("token must be 8192 characters or less", thrown.getMessage());
        verifyNoInteractions(oauthService);
    }

    @Test
    void introspectSuccessUsesNoStoreNoCacheHeaders() {
        OAuthController.OAuthIntrospectionRequest request = new OAuthController.OAuthIntrospectionRequest(
                "client_a", "secret", "access-token");
        Map<String, Object> introspection = Map.of("active", true, "client_id", "client_a");
        when(oauthService.introspect("client_a", "secret", "access-token")).thenReturn(introspection);

        var response = controller.introspect(request, mock(HttpServletRequest.class));

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        assertEquals(introspection, response.getBody());
    }

    @Test
    @SuppressWarnings("unchecked")
    void scopesForHcfAdminExposeOnlyRoleAllowedScopes() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, UUID.randomUUID(),
                "HCF_ADMIN", "hcf"));

        Map<String, Object> response = controller.scopes();

        assertEquals("HCF_ADMIN", response.get("currentRole"));
        var allScopes = (java.util.List<String>) response.get("allScopes");
        var allowedScopes = (java.util.List<String>) response.get("allowedScopes");
        var roleMatrix = (Map<String, java.util.List<String>>) response.get("roleMatrix");
        assertEquals(allowedScopes, allScopes);
        assertTrue(allScopes.contains("smartcbwtf.hcf.read"));
        assertFalse(allScopes.contains("smartcbwtf.platform.write"));
        assertFalse(allScopes.contains("smartcbwtf.oauth.manage"));
        assertEquals(java.util.Set.of("HCF_ADMIN"), roleMatrix.keySet());
    }

    @Test
    @SuppressWarnings("unchecked")
    void scopesForSuperAdminExposeFullRoleMatrix() {
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, null,
                "SUPER_ADMIN", "root"));

        Map<String, Object> response = controller.scopes();

        var allScopes = (java.util.List<String>) response.get("allScopes");
        var roleMatrix = (Map<String, java.util.List<String>>) response.get("roleMatrix");
        assertTrue(allScopes.contains("smartcbwtf.platform.write"));
        assertTrue(roleMatrix.containsKey("SUPER_ADMIN"));
        assertTrue(roleMatrix.containsKey("CBWTF_ADMIN"));
        assertTrue(roleMatrix.containsKey("HCF_ADMIN"));
        assertTrue(roleMatrix.containsKey("TOP_MANAGEMENT"));
    }
}
