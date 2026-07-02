package com.smartcbwtf.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicEndpointsTest {

    @Test
    void onlyExplicitUploadFoldersArePublic() {
        assertTrue(PublicEndpoints.isPublicPath("/uploads/profiles/avatar.png", false));
        assertTrue(PublicEndpoints.isPublicPath("/uploads/branding/facility/logo.png", false));
        assertTrue(PublicEndpoints.isPublicPath("/uploads/payment-qr/facility/qr.jpg", false));

        assertFalse(PublicEndpoints.isPublicPath("/uploads/rent-agreements/agreement.pdf", false));
        assertFalse(PublicEndpoints.isPublicPath("/uploads/consumables/image.png", false));
        assertFalse(PublicEndpoints.isPublicPath("/uploads/", false));
    }

    @Test
    void apiDocsRespectExposureFlag() {
        assertTrue(PublicEndpoints.isPublicPath("/v3/api-docs", true));
        assertTrue(PublicEndpoints.isPublicPath("/swagger-ui/index.html", true));

        assertFalse(PublicEndpoints.isPublicPath("/v3/api-docs", false));
        assertFalse(PublicEndpoints.isPublicPath("/swagger-ui/index.html", false));
    }

    @Test
    void onlyNamedWellKnownOauthMetadataIsPublic() {
        assertTrue(PublicEndpoints.isPublicPath("/.well-known/openid-configuration", false));
        assertTrue(PublicEndpoints.isPublicPath("/.well-known/oauth-authorization-server", false));

        assertFalse(PublicEndpoints.isPublicPath("/.well-known/jwks.json", false));
        assertFalse(PublicEndpoints.isPublicPath("/.well-known/acme-challenge/token", false));
    }

    @Test
    void publicApiSurfaceRemainsIntentional() {
        assertTrue(PublicEndpoints.isPublicPath("/api/public/contact", false));
        assertTrue(PublicEndpoints.isPublicPath("/api/public/agreement/verify/123", false));
        assertTrue(PublicEndpoints.isPublicPath("/api/public/agreements/123/verify", false));
        assertTrue(PublicEndpoints.isPublicPath("/api/cbwtf/consumables/123/image/view", false));

        assertFalse(PublicEndpoints.isPublicPath("/api/public/admin/debug", false));
        assertFalse(PublicEndpoints.isPublicPath("/api/public/agreement/verify/123/extra", false));
        assertFalse(PublicEndpoints.isPublicPath("/api/hcfs/register", false));
        assertFalse(PublicEndpoints.isPublicPath("/api/cbwtf/consumables/123/image/edit", false));
        assertFalse(PublicEndpoints.isPublicPath("/api/config/mobile", false));
        assertFalse(PublicEndpoints.isPublicPath("/oauth/authorize", false));
    }
}
