package com.smartcbwtf.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class OAuthScopeRegistry {
    private static final List<String> SUPER_ADMIN_SCOPES = List.of(
            "offline_access",
            "smartcbwtf.profile.read",
            "smartcbwtf.profile.write",
            "smartcbwtf.platform.read",
            "smartcbwtf.platform.write",
            "smartcbwtf.oauth.manage",
            "smartcbwtf.users.read",
            "smartcbwtf.users.write",
            "smartcbwtf.facility.read",
            "smartcbwtf.facility.write",
            "smartcbwtf.hcf.read",
            "smartcbwtf.hcf.write",
            "smartcbwtf.contracts.read",
            "smartcbwtf.contracts.write",
            "smartcbwtf.manifests.read",
            "smartcbwtf.manifests.write",
            "smartcbwtf.collections.read",
            "smartcbwtf.collections.write",
            "smartcbwtf.routes.read",
            "smartcbwtf.routes.write",
            "smartcbwtf.vehicles.read",
            "smartcbwtf.vehicles.write",
            "smartcbwtf.weighments.read",
            "smartcbwtf.weighments.write",
            "smartcbwtf.treatment.read",
            "smartcbwtf.treatment.write",
            "smartcbwtf.disposal.read",
            "smartcbwtf.disposal.write",
            "smartcbwtf.compliance.read",
            "smartcbwtf.compliance.write",
            "smartcbwtf.billing.read",
            "smartcbwtf.billing.write",
            "smartcbwtf.inventory.read",
            "smartcbwtf.inventory.write",
            "smartcbwtf.maintenance.read",
            "smartcbwtf.maintenance.write",
            "smartcbwtf.incidents.read",
            "smartcbwtf.incidents.write",
            "smartcbwtf.documents.read",
            "smartcbwtf.documents.write",
            "smartcbwtf.audit.read",
            "smartcbwtf.webhooks.manage");

    private static final List<String> CBWTF_ADMIN_SCOPES = List.of(
            "offline_access",
            "smartcbwtf.profile.read",
            "smartcbwtf.profile.write",
            "smartcbwtf.users.read",
            "smartcbwtf.users.write",
            "smartcbwtf.facility.read",
            "smartcbwtf.facility.write",
            "smartcbwtf.hcf.read",
            "smartcbwtf.hcf.write",
            "smartcbwtf.contracts.read",
            "smartcbwtf.contracts.write",
            "smartcbwtf.manifests.read",
            "smartcbwtf.manifests.write",
            "smartcbwtf.collections.read",
            "smartcbwtf.collections.write",
            "smartcbwtf.routes.read",
            "smartcbwtf.routes.write",
            "smartcbwtf.vehicles.read",
            "smartcbwtf.vehicles.write",
            "smartcbwtf.weighments.read",
            "smartcbwtf.weighments.write",
            "smartcbwtf.treatment.read",
            "smartcbwtf.treatment.write",
            "smartcbwtf.disposal.read",
            "smartcbwtf.disposal.write",
            "smartcbwtf.compliance.read",
            "smartcbwtf.compliance.write",
            "smartcbwtf.billing.read",
            "smartcbwtf.billing.write",
            "smartcbwtf.inventory.read",
            "smartcbwtf.inventory.write",
            "smartcbwtf.maintenance.read",
            "smartcbwtf.maintenance.write",
            "smartcbwtf.incidents.read",
            "smartcbwtf.incidents.write",
            "smartcbwtf.documents.read",
            "smartcbwtf.documents.write",
            "smartcbwtf.audit.read",
            "smartcbwtf.webhooks.manage");

    private static final List<String> HCF_ADMIN_SCOPES = List.of(
            "offline_access",
            "smartcbwtf.profile.read",
            "smartcbwtf.profile.write",
            "smartcbwtf.facility.read",
            "smartcbwtf.hcf.read",
            "smartcbwtf.hcf.write",
            "smartcbwtf.contracts.read",
            "smartcbwtf.manifests.read",
            "smartcbwtf.manifests.write",
            "smartcbwtf.collections.read",
            "smartcbwtf.compliance.read",
            "smartcbwtf.billing.read",
            "smartcbwtf.inventory.read",
            "smartcbwtf.inventory.write",
            "smartcbwtf.documents.read",
            "smartcbwtf.documents.write");

    private static final List<String> TOP_MANAGEMENT_SCOPES = List.of(
            "offline_access",
            "smartcbwtf.profile.read",
            "smartcbwtf.facility.read",
            "smartcbwtf.hcf.read",
            "smartcbwtf.hcf.write",
            "smartcbwtf.contracts.read",
            "smartcbwtf.contracts.write",
            "smartcbwtf.billing.read",
            "smartcbwtf.billing.write");

    public List<String> allScopes() {
        return SUPER_ADMIN_SCOPES;
    }

    public List<String> allowedScopesForRole(String role) {
        if ("SUPER_ADMIN".equals(role)) {
            return SUPER_ADMIN_SCOPES;
        }
        if ("CBWTF_ADMIN".equals(role)) {
            return CBWTF_ADMIN_SCOPES;
        }
        if ("HCF_ADMIN".equals(role)) {
            return HCF_ADMIN_SCOPES;
        }
        if ("TOP_MANAGEMENT".equals(role)) {
            return TOP_MANAGEMENT_SCOPES;
        }
        return List.of(
                "smartcbwtf.profile.read",
                "smartcbwtf.facility.read",
                "smartcbwtf.routes.read",
                "smartcbwtf.collections.read",
                "smartcbwtf.collections.write",
                "smartcbwtf.manifests.read",
                "smartcbwtf.manifests.write",
                "smartcbwtf.weighments.write");
    }

    public List<String> normalizeAndValidate(String requestedScope, String role, String clientAllowedScopes) {
        Set<String> roleAllowed = new LinkedHashSet<>(allowedScopesForRole(role));
        Set<String> clientAllowed = splitScopes(clientAllowedScopes);
        if (clientAllowed.isEmpty()) {
            clientAllowed = roleAllowed;
        }

        Set<String> requested = splitScopes(requestedScope);
        if (requested.isEmpty()) {
            requested = new LinkedHashSet<>(clientAllowed);
        }

        List<String> granted = new ArrayList<>();
        for (String scope : requested) {
            if (!roleAllowed.contains(scope) || !clientAllowed.contains(scope)) {
                throw new IllegalArgumentException("Scope not allowed for this client or user role: " + scope);
            }
            granted.add(scope);
        }
        return granted;
    }

    public Set<String> splitScopes(String scopes) {
        Set<String> result = new LinkedHashSet<>();
        if (scopes == null || scopes.isBlank()) {
            return result;
        }
        for (String scope : scopes.trim().split("\\s+")) {
            if (!scope.isBlank()) {
                result.add(scope.trim());
            }
        }
        return result;
    }

    public String joinScopes(List<String> scopes) {
        return String.join(" ", scopes);
    }

    public String requiredScope(String method, String path) {
        boolean read = isRead(method);
        String p = path.toLowerCase(Locale.ROOT);
        if (isPublicProtocolPath(p)) {
            return null;
        }
        if (p.startsWith("/api/integration/audit-events") || p.startsWith("/api/superadmin/audit-logs")) {
            return "smartcbwtf.audit.read";
        }
        if (p.equals("/oauth/userinfo")) {
            return "smartcbwtf.profile.read";
        }
        if (p.equals("/api/users/me") || p.equals("/api/users/me/change-password")) {
            return read ? "smartcbwtf.profile.read" : "smartcbwtf.profile.write";
        }
        if (p.startsWith("/api/internal/gps/ingest/")) {
            return "smartcbwtf.webhooks.manage";
        }
        if (p.startsWith("/api/internal") || p.startsWith("/api/admin/errors")) {
            return read ? "smartcbwtf.platform.read" : "smartcbwtf.platform.write";
        }
        if (p.startsWith("/api/admin/users") || p.startsWith("/api/superadmin/users")
                || p.startsWith("/api/cbwtf/staff")
                || p.equals("/api/auth/unlock") || p.startsWith("/api/auth/unlock/")) {
            return read ? "smartcbwtf.users.read" : "smartcbwtf.users.write";
        }
        if (p.startsWith("/api/oauth") || p.startsWith("/api/admin/oauth")) {
            return "smartcbwtf.oauth.manage";
        }
        if (p.startsWith("/api/errors")) {
            return read ? "smartcbwtf.incidents.read" : "smartcbwtf.incidents.write";
        }
        if (p.startsWith("/api/management/dues-approvals") || p.startsWith("/api/top-mgmt/approvals/dues")) {
            return read ? "smartcbwtf.billing.read" : "smartcbwtf.billing.write";
        }
        if (p.startsWith("/api/top-mgmt/approvals/corrections")) {
            return read ? "smartcbwtf.contracts.read" : "smartcbwtf.contracts.write";
        }
        if (p.startsWith("/api/top-mgmt/approvals/hcfs") && p.contains("rent-agreement")) {
            return "smartcbwtf.contracts.read";
        }
        if (p.startsWith("/api/top-mgmt/approvals/hcfs")) {
            return read ? "smartcbwtf.hcf.read" : "smartcbwtf.hcf.write";
        }
        if (p.startsWith("/api/admin") || p.startsWith("/api/superadmin") || p.startsWith("/api/management")
                || p.startsWith("/api/top-mgmt")) {
            return read ? "smartcbwtf.platform.read" : "smartcbwtf.platform.write";
        }
        if (p.contains("/profile") || p.equals("/api/auth/unlock")) {
            return read ? "smartcbwtf.profile.read" : "smartcbwtf.profile.write";
        }
        if (p.contains("agreement") || p.contains("terms")) {
            return read ? "smartcbwtf.contracts.read" : "smartcbwtf.contracts.write";
        }
        if (p.startsWith("/api/cbwtf/hcfs") || p.startsWith("/api/hcf/profile")) {
            return read ? "smartcbwtf.hcf.read" : "smartcbwtf.hcf.write";
        }
        if (p.contains("qr") || p.startsWith("/api/bags") || p.startsWith("/api/labels")) {
            return read ? "smartcbwtf.manifests.read" : "smartcbwtf.manifests.write";
        }
        if (p.contains("route")) {
            return read ? "smartcbwtf.routes.read" : "smartcbwtf.routes.write";
        }
        if (p.contains("vehicle") || p.contains("gps")) {
            return read ? "smartcbwtf.vehicles.read" : "smartcbwtf.vehicles.write";
        }
        if (p.contains("attendance") || p.contains("location") || p.contains("waste") || p.startsWith("/api/mobile")) {
            return read ? "smartcbwtf.collections.read" : "smartcbwtf.collections.write";
        }
        if (p.contains("compliance") || p.contains("report")) {
            return read ? "smartcbwtf.compliance.read" : "smartcbwtf.compliance.write";
        }
        if (p.contains("billing") || p.contains("invoice") || p.contains("payment") || p.contains("bank-account")) {
            return read ? "smartcbwtf.billing.read" : "smartcbwtf.billing.write";
        }
        if (p.contains("consumable") || p.contains("inventory")) {
            return read ? "smartcbwtf.inventory.read" : "smartcbwtf.inventory.write";
        }
        if (p.startsWith("/api/facilities") || p.startsWith("/api/config") || p.contains("dashboard")
                || p.contains("analytics") || p.contains("branding") || p.contains("notification")) {
            return read ? "smartcbwtf.facility.read" : "smartcbwtf.facility.write";
        }
        return read ? "smartcbwtf.facility.read" : "smartcbwtf.facility.write";
    }

    private boolean isPublicProtocolPath(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/oauth/token")
                || path.equals("/oauth/introspect")
                || path.equals("/oauth/revoke")
                || path.equals("/.well-known/openid-configuration")
                || path.equals("/.well-known/oauth-authorization-server");
    }

    public Map<String, List<String>> roleMatrix() {
        Map<String, List<String>> matrix = new LinkedHashMap<>();
        matrix.put("SUPER_ADMIN", SUPER_ADMIN_SCOPES);
        matrix.put("CBWTF_ADMIN", CBWTF_ADMIN_SCOPES);
        matrix.put("HCF_ADMIN", HCF_ADMIN_SCOPES);
        matrix.put("TOP_MANAGEMENT", TOP_MANAGEMENT_SCOPES);
        matrix.put("DRIVER_OR_PLANT_OPERATOR", allowedScopesForRole("DRIVER"));
        return matrix;
    }

    private boolean isRead(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }
}
