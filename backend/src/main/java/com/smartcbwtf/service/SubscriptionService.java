package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for subscription lifecycle management.
 * All state-changing operations are audited.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final FacilityRepository facilityRepository;
    private final SubscriptionAuditRepository auditRepository;
    private final TenantFeatureFlagRepository featureFlagRepository;
    private final SystemConfigService systemConfigService;

    public SubscriptionService(
            FacilityRepository facilityRepository,
            SubscriptionAuditRepository auditRepository,
            TenantFeatureFlagRepository featureFlagRepository,
            SystemConfigService systemConfigService) {
        this.facilityRepository = facilityRepository;
        this.auditRepository = auditRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.systemConfigService = systemConfigService;
    }

    // ========== CONFIG-BASED DEFAULTS ==========

    /**
     * Get default subscription duration in months from system config.
     */
    public int getDefaultDurationMonths() {
        return systemConfigService.getInt("subscription.default_duration_months", 12);
    }

    /**
     * Check if trial is enabled from system config.
     */
    public boolean isTrialEnabled() {
        return systemConfigService.getBoolean("subscription.trial_enabled", true);
    }

    /**
     * Get trial duration in days from system config.
     */
    public int getTrialDurationDays() {
        return systemConfigService.getInt("subscription.trial_duration_days", 14);
    }

    /**
     * Get maximum temporary access days from system config.
     */
    public int getMaxTempAccessDays() {
        return systemConfigService.getInt("subscription.temp_access_max_days", 30);
    }

    /**
     * Get invoice due days from system config.
     */
    public int getInvoiceDueDays() {
        return systemConfigService.getInt("subscription.invoice_due_days", 15);
    }

    /**
     * Check if auto-expire unpaid is enabled.
     */
    public boolean isAutoExpireUnpaidEnabled() {
        return systemConfigService.getBoolean("subscription.auto_expire_unpaid", true);
    }

    // ========== QUERIES (Read-only) ==========

    /**
     * Check if a tenant's subscription is active.
     */
    public boolean isActive(UUID facilityId) {
        return facilityRepository.findById(facilityId)
                .map(f -> {
                    String status = f.getSubscriptionStatus();
                    if (status == null)
                        return false;
                    return "ACTIVE".equals(status) || "TRIAL".equals(status);
                })
                .orElse(false);
    }

    /**
     * Get subscription status for a tenant.
     */
    public Facility.Status getStatus(UUID facilityId) {
        return facilityRepository.findById(facilityId)
                .map(Facility::getSubscriptionStatusEnum)
                .orElse(null);
    }

    /**
     * Check if a feature is enabled for a tenant.
     */
    public boolean isFeatureEnabled(UUID facilityId, String featureKey) {
        return featureFlagRepository.findByFacilityIdAndFeatureKey(facilityId, featureKey)
                .map(TenantFeatureFlag::getEnabled)
                .orElse(false);
    }

    /**
     * Get all enabled features for a tenant.
     */
    public Map<String, Boolean> getEnabledFeatures(UUID facilityId) {
        List<TenantFeatureFlag> flags = featureFlagRepository.findByFacilityId(facilityId);
        Map<String, Boolean> result = new HashMap<>();
        for (TenantFeatureFlag flag : flags) {
            result.put(flag.getFeatureKey(), flag.getEnabled());
        }
        return result;
    }

    /**
     * Find tenants expiring within N days.
     */
    public List<Facility> findExpiringTenants(int withinDays) {
        Instant threshold = Instant.now().plus(withinDays, ChronoUnit.DAYS);
        return facilityRepository.findBySubscriptionStatusAndSubscriptionExpiresAtBefore(
                "ACTIVE", threshold);
    }

    // ========== COMMANDS (State-changing, audited) ==========

    /**
     * Update subscription plan for a tenant.
     */
    @Transactional
    public Facility updateSubscriptionPlan(
            UUID facilityId,
            Facility.Plan newPlan,
            Instant expiresAt,
            UUID performedBy,
            String performedByUsername,
            String notes) {

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        String oldPlan = facility.getSubscriptionPlan();
        String oldStatus = facility.getSubscriptionStatus();

        facility.setSubscriptionPlanEnum(newPlan);
        facility.setSubscriptionExpiresAt(expiresAt);

        // Reactivate if was expired/suspended and paying
        if ("EXPIRED".equals(oldStatus) || "SUSPENDED".equals(oldStatus)) {
            facility.setSubscriptionStatusEnum(Facility.Status.ACTIVE);
        }

        facility = facilityRepository.save(facility);

        // Audit log
        SubscriptionAudit audit = SubscriptionAudit.forFacility(
                facilityId,
                SubscriptionAudit.Action.PLAN_CHANGED,
                oldPlan,
                newPlan.name(),
                performedBy,
                performedByUsername,
                "SUPER_ADMIN",
                notes);
        auditRepository.save(audit);

        log.info("Updated subscription for facility {} from {} to {}", facilityId, oldPlan, newPlan);
        return facility;
    }

    /**
     * Suspend a tenant.
     */
    @Transactional
    public Facility suspendTenant(
            UUID facilityId,
            UUID performedBy,
            String performedByUsername,
            String reason) {

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        String oldStatus = facility.getSubscriptionStatus();
        facility.setSubscriptionStatusEnum(Facility.Status.SUSPENDED);
        facility = facilityRepository.save(facility);

        SubscriptionAudit audit = SubscriptionAudit.forFacility(
                facilityId,
                SubscriptionAudit.Action.SUSPENDED,
                oldStatus,
                "SUSPENDED",
                performedBy,
                performedByUsername,
                "SUPER_ADMIN",
                reason);
        auditRepository.save(audit);

        log.info("Suspended facility {}: {}", facilityId, reason);
        return facility;
    }

    /**
     * Reactivate a suspended/expired tenant.
     */
    @Transactional
    public Facility reactivateTenant(
            UUID facilityId,
            Instant newExpiresAt,
            UUID performedBy,
            String performedByUsername,
            String notes) {

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        String oldStatus = facility.getSubscriptionStatus();
        facility.setSubscriptionStatusEnum(Facility.Status.ACTIVE);
        facility.setSubscriptionExpiresAt(newExpiresAt);
        facility = facilityRepository.save(facility);

        SubscriptionAudit audit = SubscriptionAudit.forFacility(
                facilityId,
                SubscriptionAudit.Action.REACTIVATED,
                oldStatus,
                "ACTIVE",
                performedBy,
                performedByUsername,
                "SUPER_ADMIN",
                notes);
        auditRepository.save(audit);

        log.info("Reactivated facility {} until {}", facilityId, newExpiresAt);
        return facility;
    }

    /**
     * Grant temporary access to an expired tenant.
     */
    @Transactional
    public Facility grantTemporaryAccess(
            UUID facilityId,
            int days,
            UUID performedBy,
            String performedByUsername,
            String reason) {
        int maxTempAccessDays = Math.max(1, getMaxTempAccessDays());
        if (days < 1 || days > maxTempAccessDays) {
            throw new IllegalArgumentException("Temporary access days must be between 1 and " + maxTempAccessDays);
        }

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        Instant tempExpiry = Instant.now().plus(days, ChronoUnit.DAYS);
        String oldStatus = facility.getSubscriptionStatus();

        facility.setSubscriptionStatusEnum(Facility.Status.ACTIVE);
        facility.setSubscriptionExpiresAt(tempExpiry);
        facility = facilityRepository.save(facility);

        SubscriptionAudit audit = SubscriptionAudit.forFacility(
                facilityId,
                SubscriptionAudit.Action.TEMP_ACCESS_GRANTED,
                oldStatus,
                "ACTIVE (temp " + days + " days)",
                performedBy,
                performedByUsername,
                "SUPER_ADMIN",
                reason);
        auditRepository.save(audit);

        log.info("Granted temporary access to facility {} for {} days: {}", facilityId, days, reason);
        return facility;
    }

    /**
     * Toggle a feature flag for a tenant.
     */
    @Transactional
    public void setFeatureEnabled(
            UUID facilityId,
            String featureKey,
            boolean enabled,
            UUID performedBy,
            String performedByUsername) {

        Optional<TenantFeatureFlag> existingFlag = featureFlagRepository.findByFacilityIdAndFeatureKey(facilityId,
                featureKey);

        boolean oldValue = existingFlag.map(TenantFeatureFlag::getEnabled).orElse(false);

        if (existingFlag.isPresent()) {
            TenantFeatureFlag flag = existingFlag.get();
            flag.setEnabled(enabled);
            featureFlagRepository.save(flag);
        } else {
            Facility facility = facilityRepository.findById(facilityId)
                    .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
            TenantFeatureFlag flag = new TenantFeatureFlag();
            flag.setFacility(facility);
            flag.setFeatureKey(featureKey);
            flag.setEnabled(enabled);
            featureFlagRepository.save(flag);
        }

        SubscriptionAudit audit = SubscriptionAudit.forFacility(
                facilityId,
                SubscriptionAudit.Action.FEATURE_CHANGED,
                featureKey + "=" + oldValue,
                featureKey + "=" + enabled,
                performedBy,
                performedByUsername,
                "SUPER_ADMIN",
                null);
        auditRepository.save(audit);

        log.info("Feature {} set to {} for facility {}", featureKey, enabled, facilityId);
    }

    // ========== SCHEDULED JOBS ==========

    /**
     * Expire subscriptions that have passed their expiry date.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        log.info("Running subscription expiry check");

        List<Facility> activeExpired = facilityRepository
                .findBySubscriptionStatusAndSubscriptionExpiresAtBefore("ACTIVE", Instant.now());

        for (Facility facility : activeExpired) {
            String oldStatus = facility.getSubscriptionStatus();
            facility.setSubscriptionStatusEnum(Facility.Status.EXPIRED);
            facilityRepository.save(facility);

            SubscriptionAudit audit = SubscriptionAudit.forFacility(
                    facility.getId(),
                    SubscriptionAudit.Action.EXPIRED,
                    oldStatus,
                    "EXPIRED",
                    null, // System action
                    "SYSTEM",
                    "SYSTEM",
                    "Automatic expiry - subscription period ended");
            auditRepository.save(audit);

            log.info("Expired subscription for facility {}", facility.getId());
        }

        log.info("Expired {} subscriptions", activeExpired.size());
    }
}
