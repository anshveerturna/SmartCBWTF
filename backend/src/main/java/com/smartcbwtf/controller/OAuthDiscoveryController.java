package com.smartcbwtf.controller;

import com.smartcbwtf.service.OAuthScopeRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
public class OAuthDiscoveryController {
    private final String issuer;
    private final OAuthScopeRegistry scopeRegistry;

    public OAuthDiscoveryController(@Value("${security.jwt.issuer}") String issuer,
            OAuthScopeRegistry scopeRegistry) {
        this.issuer = issuer;
        this.scopeRegistry = scopeRegistry;
    }

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openIdConfiguration() {
        return authorizationServerMetadata();
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> authorizationServerMetadata() {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", baseUrl + "/oauth/authorize"),
                Map.entry("token_endpoint", baseUrl + "/oauth/token"),
                Map.entry("userinfo_endpoint", baseUrl + "/oauth/userinfo"),
                Map.entry("introspection_endpoint", baseUrl + "/oauth/introspect"),
                Map.entry("revocation_endpoint", baseUrl + "/oauth/revoke"),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code", "refresh_token", "client_credentials")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post")),
                Map.entry("introspection_endpoint_auth_methods_supported",
                        List.of("client_secret_basic", "client_secret_post")),
                Map.entry("revocation_endpoint_auth_methods_supported",
                        List.of("client_secret_basic", "client_secret_post")),
                Map.entry("code_challenge_methods_supported", List.of("S256")),
                Map.entry("access_token_validation", "introspection"),
                Map.entry("scopes_supported", scopeRegistry.allScopes()));
    }
}
