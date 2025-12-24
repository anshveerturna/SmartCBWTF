package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Tenant assertion service for enforcing multi-tenant data access rules.
 * All CBWTF operations should use this to verify data belongs to current
 * tenant.
 */
@Service
public class TenantAssertionService {

    private static final Logger log = LoggerFactory.getLogger(TenantAssertionService.class);

    /**
     * Assert that the current user has access to the specified facility.
     * SuperAdmin can access any facility.
     * CBWTF_ADMIN can only access their own facility.
     *
     * @param facilityId The facility to check access for
     * @throws TenantAccessDeniedException if access is denied
     */
    public void assertCanAccessFacility(UUID facilityId) {
        if (facilityId == null) {
            throw new TenantAccessDeniedException("Facility ID is required");
        }

        // SuperAdmin can access everything
        if (TenantContext.isSuperAdmin()) {
            return;
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("No tenant context - denying access to facility {}", facilityId);
            throw new TenantAccessDeniedException("No tenant context available");
        }

        if (!tenantId.equals(facilityId)) {
            log.warn("Tenant {} attempted to access facility {} - DENIED", tenantId, facilityId);
            throw new TenantAccessDeniedException(
                    String.format("Tenant %s cannot access facility %s", tenantId, facilityId));
        }
    }

    /**
     * Assert that the current tenant context is present.
     * Throws if no tenant context is set (for non-authenticated or SuperAdmin
     * requests).
     */
    public void requireTenantContext() {
        if (TenantContext.isSuperAdmin()) {
            return; // SuperAdmin doesn't need tenant context
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new TenantAccessDeniedException("Tenant context is required for this operation");
        }
    }

    /**
     * Get the current tenant ID, throwing if not present.
     * For CBWTF_ADMIN, this is their facility_id.
     */
    public UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null && !TenantContext.isSuperAdmin()) {
            throw new TenantAccessDeniedException("Tenant context is required");
        }
        return tenantId;
    }

    /**
     * Check if current user is a tenant admin (not SuperAdmin).
     */
    public boolean isTenantAdmin() {
        return TenantContext.isCbwtfAdmin() && TenantContext.getTenantId() != null;
    }

    /**
     * Exception thrown when tenant access is denied.
     */
    public static class TenantAccessDeniedException extends RuntimeException {
        public TenantAccessDeniedException(String message) {
            super(message);
        }
    }
}
