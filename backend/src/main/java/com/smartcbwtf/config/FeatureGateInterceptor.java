package com.smartcbwtf.config;

import com.smartcbwtf.service.FeatureGuardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global interceptor that acts as the LAST LINE OF DEFENSE for feature flag
 * enforcement.
 * 
 * Maps URL patterns to feature keys and blocks access if the feature is
 * disabled.
 * This catches any enforcement missed at controller or service level.
 * 
 * SuperAdmin users bypass all feature gates.
 */
@Component
public class FeatureGateInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeatureGateInterceptor.class);

    private final FeatureGuardService featureGuardService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Endpoint pattern → Feature key mapping
    // Using LinkedHashMap to preserve order (more specific patterns first)
    private static final Map<String, String> ENDPOINT_FEATURES = new LinkedHashMap<>();

    static {
        // Analytics endpoints
        ENDPOINT_FEATURES.put("/api/analytics/insights/**", FeatureGuardService.AI_INSIGHTS);
        ENDPOINT_FEATURES.put("/api/analytics/**", FeatureGuardService.ADVANCED_ANALYTICS);

        // Route optimization
        ENDPOINT_FEATURES.put("/api/routes/**", FeatureGuardService.ROUTE_OPTIMIZATION);

        // Compliance/CPCB reporting
        ENDPOINT_FEATURES.put("/api/compliance/**", FeatureGuardService.CPCB_REPORTING);

        // Attendance (only sync endpoint, not the core attendance tracking)
        // Note: Attendance sync uses ATTENDANCE_ENFORCEMENT if we want stricter rules
        // ENDPOINT_FEATURES.put("/api/attendance/sync",
        // FeatureGuardService.ATTENDANCE_ENFORCEMENT);

        // Vehicle tracking
        ENDPOINT_FEATURES.put("/api/tracking/**", FeatureGuardService.VEHICLE_TRACKING);

        // AI/ML insights
        ENDPOINT_FEATURES.put("/api/insights/**", FeatureGuardService.AI_INSIGHTS);
    }

    public FeatureGateInterceptor(FeatureGuardService featureGuardService) {
        this.featureGuardService = featureGuardService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Only check feature-gated endpoints
        String featureKey = matchFeature(uri);
        if (featureKey == null) {
            return true; // Not a feature-gated endpoint
        }

        log.debug("Feature gate check: {} {} → feature {}", method, uri, featureKey);

        try {
            featureGuardService.assertEnabledOrSuperAdmin(featureKey, uri);
            return true;
        } catch (Exception e) {
            // Exception will be handled by global exception handler
            throw e;
        }
    }

    /**
     * Match request URI to a feature key.
     * Returns null if no feature gate applies.
     */
    private String matchFeature(String uri) {
        for (Map.Entry<String, String> entry : ENDPOINT_FEATURES.entrySet()) {
            if (pathMatcher.match(entry.getKey(), uri)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
