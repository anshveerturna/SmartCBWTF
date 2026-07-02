package com.smartcbwtf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationApiSecurityTest {

    private static final String INTEGRATION_SCOPE_ROLES =
            "hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN','HCF_ADMIN','TOP_MANAGEMENT')";

    @Test
    void integrationCatalogAllowsApiCapablePortalRoles() {
        PreAuthorize annotation = IntegrationController.class.getAnnotation(PreAuthorize.class);

        assertEquals(INTEGRATION_SCOPE_ROLES, annotation.value());
    }

    @Test
    void integrationScopeMatrixAllowsApiCapablePortalRoles() throws NoSuchMethodException {
        PreAuthorize annotation = OAuthController.class.getDeclaredMethod("scopes").getAnnotation(PreAuthorize.class);

        assertEquals(INTEGRATION_SCOPE_ROLES, annotation.value());
    }

    @Test
    void hcfCatalogHidesAdminInternalAndCbwtfRoutes() {
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/admin/users", "HCF_ADMIN"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/internal/gps/vendors", "HCF_ADMIN"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/cbwtf/routes", "HCF_ADMIN"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/auth/unlock/locked-user", "HCF_ADMIN"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/hcf/compliance/daily", "HCF_ADMIN"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/integration/probe", "HCF_ADMIN"));
    }

    @Test
    void cbwtfCatalogKeepsOperationalRoutesButHidesSuperAdminRoutes() {
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/cbwtf/routes", "CBWTF_ADMIN"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/hcfs", "CBWTF_ADMIN"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/facilities/active", "CBWTF_ADMIN"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/admin/master-data/hcfs", "CBWTF_ADMIN"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/superadmin/users", "CBWTF_ADMIN"));
    }

    @Test
    void catalogEndpointVisibilityAlsoHonorsResolvedMethodAuthorization() {
        assertTrue(IntegrationController.isCatalogEndpointVisibleForRole(
                "/api/facilities/active",
                "hasAnyRole('DRIVER', 'PLANT_OPERATOR', 'CBWTF_ADMIN')",
                "CBWTF_ADMIN"));
        assertTrue(IntegrationController.isCatalogEndpointVisibleForRole(
                "/api/hcfs",
                "hasAnyRole('DRIVER', 'PLANT_OPERATOR', 'CBWTF_ADMIN')",
                "CBWTF_ADMIN"));
        assertFalse(IntegrationController.isCatalogEndpointVisibleForRole(
                "/api/bags/verify",
                "hasAnyRole('DRIVER', 'PLANT_OPERATOR')",
                "CBWTF_ADMIN"));
        assertFalse(IntegrationController.isCatalogEndpointVisibleForRole(
                "/api/location/update",
                "hasAnyRole('DRIVER', 'PLANT_OPERATOR')",
                "CBWTF_ADMIN"));
        assertFalse(IntegrationController.isCatalogEndpointVisibleForRole(
                "/api/cbwtf/routes",
                "hasRole('CBWTF_ADMIN')",
                "HCF_ADMIN"));
    }

    @Test
    void topManagementCatalogOnlyShowsApprovalWorkflowAndSharedIntegrationRoutes() {
        assertTrue(IntegrationController.isCatalogPathVisibleForRole(
                "/api/top-mgmt/approvals/dues", "TOP_MANAGEMENT"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole(
                "/api/management/dues-approvals", "TOP_MANAGEMENT"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole(
                "/api/integration/catalog/endpoints", "TOP_MANAGEMENT"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/users/me", "TOP_MANAGEMENT"));
        assertTrue(IntegrationController.isCatalogPathVisibleForRole("/api/config/mobile", "TOP_MANAGEMENT"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/cbwtf/hcfs", "TOP_MANAGEMENT"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole("/api/admin/cbwtfs", "TOP_MANAGEMENT"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole(
                "/api/auth/unlock/locked-user", "TOP_MANAGEMENT"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole(
                "/api/auth/password-reset/debug", "TOP_MANAGEMENT"));
        assertFalse(IntegrationController.isCatalogPathVisibleForRole(
                "/api/config/internal-flags", "TOP_MANAGEMENT"));
    }
}
