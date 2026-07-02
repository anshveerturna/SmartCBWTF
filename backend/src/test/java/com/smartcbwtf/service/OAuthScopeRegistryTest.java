package com.smartcbwtf.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthScopeRegistryTest {
    private final OAuthScopeRegistry registry = new OAuthScopeRegistry();

    @Test
    void hcfAdminCannotRequestPlatformOrOauthScopes() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.normalizeAndValidate("smartcbwtf.oauth.manage", "HCF_ADMIN",
                        "smartcbwtf.oauth.manage"));
        assertTrue(ex.getMessage().contains("Scope not allowed"));
    }

    @Test
    void cbwtfAdminCanUseOperationalScopes() {
        var scopes = registry.normalizeAndValidate(
                "smartcbwtf.hcf.read smartcbwtf.routes.write smartcbwtf.billing.read smartcbwtf.users.write",
                "CBWTF_ADMIN",
                "smartcbwtf.hcf.read smartcbwtf.routes.write smartcbwtf.billing.read smartcbwtf.users.write");

        assertEquals(4, scopes.size());
        assertTrue(scopes.contains("smartcbwtf.routes.write"));
        assertTrue(scopes.contains("smartcbwtf.users.write"));
    }

    @Test
    void requiredScopeMapsRouteMutationToRoutesWrite() {
        assertEquals("smartcbwtf.routes.write",
                registry.requiredScope("POST", "/api/cbwtf/routes"));
    }

    @Test
    void requiredScopeMapsAgreementPdfToContractsRead() {
        assertEquals("smartcbwtf.contracts.read",
                registry.requiredScope("GET", "/api/cbwtf/hcfs/123/agreement/pdf"));
    }

    @Test
    void requiredScopeMapsOauthUserinfoToProfileRead() {
        assertEquals("smartcbwtf.profile.read",
                registry.requiredScope("GET", "/oauth/userinfo"));
    }

    @Test
    void requiredScopeLeavesPublicProtocolEndpointsUnscoped() {
        assertNull(registry.requiredScope("POST", "/api/auth/login"));
        assertNull(registry.requiredScope("POST", "/oauth/token"));
        assertNull(registry.requiredScope("POST", "/oauth/introspect"));
        assertNull(registry.requiredScope("POST", "/oauth/revoke"));
        assertNull(registry.requiredScope("GET", "/.well-known/openid-configuration"));
        assertNull(registry.requiredScope("GET", "/.well-known/oauth-authorization-server"));
    }

    @Test
    void requiredScopeMapsCurrentUserRoutesToProfileScopes() {
        assertEquals("smartcbwtf.profile.read",
                registry.requiredScope("GET", "/api/users/me"));
        assertEquals("smartcbwtf.profile.write",
                registry.requiredScope("POST", "/api/users/me/change-password"));
    }

    @Test
    void topManagementCanRequestOnlyApprovalWorkflowScopes() {
        var scopes = registry.normalizeAndValidate(
                "smartcbwtf.billing.write smartcbwtf.hcf.write smartcbwtf.contracts.read",
                "TOP_MANAGEMENT",
                "smartcbwtf.billing.write smartcbwtf.hcf.write smartcbwtf.contracts.read");

        assertEquals(3, scopes.size());
        assertTrue(scopes.contains("smartcbwtf.hcf.write"));
        assertThrows(IllegalArgumentException.class,
                () -> registry.normalizeAndValidate("smartcbwtf.platform.write", "TOP_MANAGEMENT",
                        "smartcbwtf.platform.write"));
    }

    @Test
    void requiredScopeMapsTopManagementApprovalsToOperationalScopes() {
        assertEquals("smartcbwtf.billing.read",
                registry.requiredScope("GET", "/api/top-mgmt/approvals/dues"));
        assertEquals("smartcbwtf.hcf.write",
                registry.requiredScope("POST", "/api/top-mgmt/approvals/hcfs/123/approve"));
        assertEquals("smartcbwtf.contracts.read",
                registry.requiredScope("GET", "/api/top-mgmt/approvals/hcfs/123/rent-agreement"));
        assertEquals("smartcbwtf.contracts.write",
                registry.requiredScope("POST", "/api/top-mgmt/approvals/corrections/123/reject"));
        assertEquals("smartcbwtf.billing.write",
                registry.requiredScope("POST", "/api/management/dues-approvals/123/approve"));
    }

    @Test
    void requiredScopeMapsUserAdministrationToUsersScopes() {
        assertEquals("smartcbwtf.users.read",
                registry.requiredScope("GET", "/api/admin/users"));
        assertEquals("smartcbwtf.users.write",
                registry.requiredScope("POST", "/api/superadmin/users/123/reset-password"));
        assertEquals("smartcbwtf.users.read",
                registry.requiredScope("GET", "/api/cbwtf/staff"));
        assertEquals("smartcbwtf.users.write",
                registry.requiredScope("POST", "/api/cbwtf/staff/123/request-gps-refresh"));
        assertEquals("smartcbwtf.users.write",
                registry.requiredScope("POST", "/api/auth/unlock/locked-user"));
    }

    @Test
    void requiredScopeMapsVendorIngestionToWebhookManageScope() {
        assertEquals("smartcbwtf.webhooks.manage",
                registry.requiredScope("POST", "/api/internal/gps/ingest/WHEELSEYE"));
    }

    @Test
    void requiredScopeMapsOtherInternalAndSystemAdminRoutesToPlatformScopes() {
        assertEquals("smartcbwtf.platform.write",
                registry.requiredScope("POST", "/api/internal/gps/update-offline"));
        assertEquals("smartcbwtf.platform.read",
                registry.requiredScope("GET", "/api/internal/gps/health"));
        assertEquals("smartcbwtf.platform.read",
                registry.requiredScope("GET", "/api/admin/errors"));
    }

    @Test
    void requiredScopeMapsAuthenticatedErrorReportsToIncidentScopes() {
        assertEquals("smartcbwtf.incidents.write",
                registry.requiredScope("POST", "/api/errors/report"));
    }
}
