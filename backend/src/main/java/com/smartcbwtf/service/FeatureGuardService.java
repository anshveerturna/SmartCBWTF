package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.TenantFeatureFlag;
import com.smartcbwtf.exception.FeatureDisabledException;
import com.smartcbwtf.repository.TenantFeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Central service for feature flag enforcement.
 * 
 * This service provides:
 * - isEnabled(): Check if a feature is enabled (non-throwing)
 * - assertEnabled(): Enforce that a feature is enabled (throws 403)
 * - assertEnabledOrSuperAdmin(): Skip enforcement for SuperAdmin
 * 
 * Uses LIVE DB queries (not cached JWT) for real-time enforcement.
 * Logs all blocked access attempts to audit log with traceId correlation.
 */
@Service
public class FeatureGuardService {

    private static final Logger log = LoggerFactory.getLogger(FeatureGuardService.class);

    private final TenantFeatureFlagRepository featureFlagRepository;
    private final AuditLogService auditLogService;

    public FeatureGuardService(TenantFeatureFlagRepository featureFlagRepository,
            AuditLogService auditLogService) {
        this.featureFlagRepository = featureFlagRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Check if a feature is enabled for a facility.
     * Non-throwing - returns false if disabled or not found.
     */
    public boolean isEnabled(UUID facilityId, String featureKey) {
        if (facilityId == null) {
            return false;
        }
        return featureFlagRepository.isFeatureEnabled(facilityId, featureKey);
    }

    /**
     * Assert that a feature is enabled for a facility.
     * Throws FeatureDisabledException (403) if not enabled.
     * Logs blocked access attempts to audit.
     */
    public void assertEnabled(UUID facilityId, String featureKey) {
        assertEnabled(facilityId, featureKey, null);
    }

    /**
     * Assert that a feature is enabled for a facility, with endpoint context.
     * Throws FeatureDisabledException (403) if not enabled.
     * Logs blocked access attempts to audit with endpoint and traceId.
     */
    public void assertEnabled(UUID facilityId, String featureKey, String endpoint) {
        if (facilityId == null) {
            log.warn("Feature check for {} failed: no tenant context", featureKey);
            throw new FeatureDisabledException(featureKey, null, endpoint);
        }

        boolean enabled = featureFlagRepository.isFeatureEnabled(facilityId, featureKey);

        if (!enabled) {
            logBlockedAccess(facilityId, featureKey, endpoint);
            log.warn("Feature {} is disabled for facility {}, endpoint: {}",
                    featureKey, facilityId, endpoint);
            throw new FeatureDisabledException(featureKey, facilityId, endpoint);
        }
    }

    /**
     * Assert that a feature is enabled, using TenantContext.
     * SuperAdmin users bypass this check.
     * 
     * Use this in interceptors/aspects where facilityId comes from context.
     */
    public void assertEnabledOrSuperAdmin(String featureKey) {
        assertEnabledOrSuperAdmin(featureKey, null);
    }

    /**
     * Assert that a feature is enabled, using TenantContext, with endpoint context.
     * SuperAdmin users bypass this check.
     */
    public void assertEnabledOrSuperAdmin(String featureKey, String endpoint) {
        // SuperAdmin bypasses all feature gates
        if (TenantContext.isSuperAdmin()) {
            log.debug("SuperAdmin bypassed feature check for {}", featureKey);
            return;
        }

        UUID facilityId = TenantContext.getTenantId();
        assertEnabled(facilityId, featureKey, endpoint);
    }

    /**
     * Log blocked access attempt to audit log with correlation ID.
     */
    private void logBlockedAccess(UUID facilityId, String featureKey, String endpoint) {
        UUID userId = TenantContext.getUserId();
        String role = TenantContext.getRole();
        String traceId = MDC.get("traceId");

        // Build JSON data for audit
        String dataJson = String.format(
                "{\"featureKey\":\"%s\",\"endpoint\":\"%s\",\"role\":\"%s\",\"traceId\":\"%s\"}",
                featureKey,
                endpoint != null ? endpoint : "unknown",
                role != null ? role : "unknown",
                traceId != null ? traceId : "none");

        auditLogService.log(
                "FEATURE", // entityType
                facilityId, // entityId (facilityId)
                "FEATURE_ACCESS_BLOCKED", // action
                userId, // actorUserId
                dataJson // data with traceId
        );

        log.info("Audit: FEATURE_ACCESS_BLOCKED - feature={}, facility={}, user={}, traceId={}",
                featureKey, facilityId, userId, traceId);
    }

    // ========== Well-known feature keys for convenience ==========

    public static final String ADVANCED_ANALYTICS = TenantFeatureFlag.ADVANCED_ANALYTICS;
    public static final String ROUTE_OPTIMIZATION = TenantFeatureFlag.ROUTE_OPTIMIZATION;
    public static final String CPCB_REPORTING = TenantFeatureFlag.CPCB_REPORTING;
    public static final String INVOICE_AUTO_SEND = TenantFeatureFlag.INVOICE_AUTO_SEND;
    public static final String PAYMENT_GATEWAY = TenantFeatureFlag.PAYMENT_GATEWAY;
    public static final String ATTENDANCE_ENFORCEMENT = TenantFeatureFlag.ATTENDANCE_ENFORCEMENT;
    public static final String VEHICLE_TRACKING = TenantFeatureFlag.VEHICLE_TRACKING;
    public static final String AI_INSIGHTS = TenantFeatureFlag.AI_INSIGHTS;
}
