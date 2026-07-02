package com.smartcbwtf.controller;

import com.smartcbwtf.service.OAuthScopeRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthDiscoveryControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void discoveryAdvertisesRegistryScopes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/openid-configuration");
        request.setScheme("https");
        request.setServerName("api.smartcbwtf.example");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            OAuthDiscoveryController controller = new OAuthDiscoveryController(
                    "smart-cbwtf-test",
                    new OAuthScopeRegistry());

            List<String> scopes = (List<String>) controller.authorizationServerMetadata().get("scopes_supported");

            assertTrue(scopes.contains("smartcbwtf.users.write"));
            assertTrue(scopes.contains("smartcbwtf.platform.write"));
            assertTrue(scopes.contains("smartcbwtf.incidents.write"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void discoveryDoesNotAdvertiseUnsupportedJwksOrIdTokens() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/.well-known/oauth-authorization-server");
        request.setScheme("https");
        request.setServerName("api.smartcbwtf.example");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            OAuthDiscoveryController controller = new OAuthDiscoveryController(
                    "smart-cbwtf-test",
                    new OAuthScopeRegistry());

            Map<String, Object> metadata = controller.authorizationServerMetadata();

            assertEquals("introspection", metadata.get("access_token_validation"));
            assertTrue(metadata.containsKey("introspection_endpoint"));
            assertFalse(metadata.containsKey("jwks_uri"));
            assertFalse(metadata.containsKey("id_token_signing_alg_values_supported"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void legacyOpenIdConfigurationReturnsSameOAuthMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/.well-known/openid-configuration");
        request.setScheme("https");
        request.setServerName("api.smartcbwtf.example");
        request.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            OAuthDiscoveryController controller = new OAuthDiscoveryController(
                    "smart-cbwtf-test",
                    new OAuthScopeRegistry());

            assertEquals(
                    controller.authorizationServerMetadata(),
                    controller.openIdConfiguration());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
