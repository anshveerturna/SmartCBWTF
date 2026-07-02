package com.smartcbwtf.config;

final class PublicEndpoints {
    private static final String[] CORE_PATTERNS = {
            "/actuator/health",
            "/.well-known/openid-configuration",
            "/.well-known/oauth-authorization-server",
            "/oauth/token",
            "/oauth/introspect",
            "/oauth/revoke",
            "/api/auth/login",
            "/api/health",
            "/api/terms/latest",
            "/uploads/profiles/**",
            "/uploads/branding/**",
            "/uploads/payment-qr/**",
            "/api/cbwtf/consumables/*/image/view",
            "/api/public/contact",
            "/api/public/agreement/verify/*",
            "/api/public/agreements/*/verify",
            "/error"
    };

    private PublicEndpoints() {
    }

    static String[] permitAllPatterns(boolean exposeApiDocs) {
        if (!exposeApiDocs) {
            return CORE_PATTERNS;
        }
        String[] patterns = java.util.Arrays.copyOf(CORE_PATTERNS, CORE_PATTERNS.length + 2);
        patterns[CORE_PATTERNS.length] = "/v3/api-docs/**";
        patterns[CORE_PATTERNS.length + 1] = "/swagger-ui/**";
        return patterns;
    }

    static boolean isPublicPath(String path, boolean exposeApiDocs) {
        if (path == null) {
            return false;
        }
        return isCorePublicPath(path) || exposeApiDocs && isApiDocsPath(path);
    }

    static boolean isApiDocsPath(String path) {
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }

    private static boolean isCorePublicPath(String path) {
        return path.equals("/actuator/health")
                || path.equals("/.well-known/openid-configuration")
                || path.equals("/.well-known/oauth-authorization-server")
                || path.equals("/oauth/token")
                || path.equals("/oauth/introspect")
                || path.equals("/oauth/revoke")
                || path.equals("/api/auth/login")
                || path.equals("/api/health")
                || path.equals("/api/terms/latest")
                || path.startsWith("/uploads/profiles/")
                || path.startsWith("/uploads/branding/")
                || path.startsWith("/uploads/payment-qr/")
                || path.matches("^/api/cbwtf/consumables/[^/]+/image/view$")
                || path.equals("/api/public/contact")
                || path.matches("^/api/public/agreement/verify/[^/]+$")
                || path.matches("^/api/public/agreements/[^/]+/verify$")
                || path.equals("/error");
    }
}
