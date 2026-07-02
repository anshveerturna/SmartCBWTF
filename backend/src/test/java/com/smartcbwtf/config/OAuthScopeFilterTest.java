package com.smartcbwtf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.service.OAuthScopeRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthScopeFilterTest {

    private final ObjectProvider<OAuthScopeRegistry> registryProvider = mock();
    private final OAuthScopeFilter filter = new OAuthScopeFilter(registryProvider, new ObjectMapper(),
            new MockEnvironment());

    @Test
    void contextPathIsRemovedBeforeScopeLookup() throws Exception {
        when(registryProvider.getIfAvailable()).thenReturn(new OAuthScopeRegistry());
        MockHttpServletRequest request = oauthRequest("GET", "/smartcbwtf/api/admin/users",
                "smartcbwtf.facility.read");
        request.setContextPath("/smartcbwtf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean(false);

        filter.doFilterInternal(request, response, chain(chainReached));

        assertEquals(403, response.getStatus());
        assertFalse(chainReached.get());
        assertTrue(response.getContentAsString().contains("smartcbwtf.users.read"));
    }

    @Test
    void contextPathScopeLookupAllowsCorrectScope() throws Exception {
        when(registryProvider.getIfAvailable()).thenReturn(new OAuthScopeRegistry());
        MockHttpServletRequest request = oauthRequest("GET", "/smartcbwtf/api/admin/users",
                "smartcbwtf.users.read");
        request.setContextPath("/smartcbwtf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean(false);

        filter.doFilterInternal(request, response, chain(chainReached));

        assertTrue(chainReached.get());
    }

    @Test
    void contextPathIsRemovedBeforePublicEndpointSkipCheck() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/smartcbwtf/api/auth/login");
        request.setContextPath("/smartcbwtf");

        assertTrue(filter.shouldNotFilter(request));
    }

    private static MockHttpServletRequest oauthRequest(String method, String path, String scopes) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setAttribute(JwtAuthFilter.ATTR_TOKEN_USE, JwtAuthFilter.TOKEN_USE_OAUTH_ACCESS);
        request.setAttribute(JwtAuthFilter.ATTR_SCOPES, scopes);
        return request;
    }

    private static FilterChain chain(AtomicBoolean chainReached) {
        return (request, response) -> chainReached.set(true);
    }
}
