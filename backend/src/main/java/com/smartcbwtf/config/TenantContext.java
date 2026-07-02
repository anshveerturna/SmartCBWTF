package com.smartcbwtf.config;

import java.util.UUID;

/**
 * Thread-local context for multi-tenant query scoping.
 * Populated by JwtAuthFilter from the authenticated user's current database bindings.
 * All repository queries should be scoped by this context.
 */
public class TenantContext {

    private static final ThreadLocal<TenantInfo> CONTEXT = new ThreadLocal<>();

    public static void set(TenantInfo tenantInfo) {
        CONTEXT.set(tenantInfo);
    }

    public static TenantInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static UUID getTenantId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.tenantId() : null;
    }

    public static UUID getHcfId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.hcfId() : null;
    }

    public static UUID getUserId() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.userId() : null;
    }

    public static String getRole() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.role() : null;
    }

    public static String getUsername() {
        TenantInfo info = CONTEXT.get();
        return info != null ? info.username() : null;
    }

    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(getRole());
    }

    public static boolean isCbwtfAdmin() {
        return "CBWTF_ADMIN".equals(getRole());
    }

    public static boolean isHcfAdmin() {
        return "HCF_ADMIN".equals(getRole());
    }

    /**
     * Tenant information extracted from JWT.
     */
    public record TenantInfo(
            UUID userId, // user ID from JWT
            UUID tenantId, // facility_id for CBWTF
            UUID hcfId, // hcf_id for HCF users
            String role, // user role
            String username // username from JWT
    ) {
    }
}
