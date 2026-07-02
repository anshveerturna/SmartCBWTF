package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for managing system errors and issues.
 * Includes auto-detection of common problems.
 */
@Service
public class SystemErrorService {

    private static final Logger log = LoggerFactory.getLogger(SystemErrorService.class);
    private static final String INACTIVE_HCFS_TITLE = "HCFs with no recent activity";
    private static final int HCF_INACTIVITY_DAYS = 7;

    private final SystemErrorRepository errorRepository;
    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final HcfRepository hcfRepository;

    public SystemErrorService(
            SystemErrorRepository errorRepository,
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            HcfRepository hcfRepository) {
        this.errorRepository = errorRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.hcfRepository = hcfRepository;
    }

    // ========== Error Reporting ==========

    @Transactional
    public SystemError reportError(String title, String description, String component,
            String severity, UUID reportedById, UUID facilityId, UUID hcfId) {
        SystemError error = new SystemError();
        error.setTitle(title);
        error.setDescription(description);
        error.setComponent(component != null ? component : "USER_FEEDBACK");
        error.setSeverityEnum(normalizeSeverity(severity));
        error.setSourceEnum(SystemError.Source.USER_REPORTED);
        error.setStatusEnum(SystemError.Status.OPEN);

        if (reportedById != null) {
            userRepository.findById(reportedById).ifPresent(error::setReportedBy);
        }
        if (facilityId != null) {
            facilityRepository.findById(facilityId).ifPresent(error::setFacility);
        }
        attachScopedHcf(error, facilityId, hcfId);

        error = errorRepository.save(error);
        log.info("New error reported: {} [{}]", title, error.getId());
        return error;
    }

    private void attachScopedHcf(SystemError error, UUID facilityId, UUID hcfId) {
        if (hcfId == null) {
            return;
        }
        if (facilityId != null) {
            hcfRepository.findByIdAndFacilityId(hcfId, facilityId).ifPresent(error::setHcf);
            return;
        }
        hcfRepository.findById(hcfId).ifPresent(error::setHcf);
    }

    @Transactional
    public SystemError reportMobileAppError(String title, String stackTrace,
            String deviceInfo, UUID reportedById) {
        SystemError error = new SystemError();
        error.setTitle(title);
        error.setStackTrace(stackTrace);
        error.setDescription("Device: " + (deviceInfo != null ? deviceInfo : "Unknown"));
        error.setComponent("MOBILE_APP");
        error.setSeverityEnum(SystemError.Severity.ERROR);
        error.setSourceEnum(SystemError.Source.MOBILE_APP);
        error.setStatusEnum(SystemError.Status.OPEN);

        if (reportedById != null) {
            userRepository.findById(reportedById).ifPresent(error::setReportedBy);
        }

        error = errorRepository.save(error);
        log.warn("Mobile app error reported: {} [{}]", title, error.getId());
        return error;
    }

    // ========== Error Resolution ==========

    @Transactional
    public SystemError resolveError(UUID errorId, UUID resolvedById, String notes) {
        return errorRepository.findById(errorId)
                .map(error -> {
                    error.setStatusEnum(SystemError.Status.RESOLVED);
                    error.setResolvedAt(Instant.now());
                    error.setResolutionNotes(notes);
                    if (resolvedById != null) {
                        userRepository.findById(resolvedById).ifPresent(error::setResolvedBy);
                    }
                    log.info("Error resolved: {} [{}]", error.getTitle(), errorId);
                    return errorRepository.save(error);
                })
                .orElseThrow(() -> new RuntimeException("Error not found: " + errorId));
    }

    @Transactional
    public SystemError updateStatus(UUID errorId, String status) {
        SystemError.Status normalizedStatus = normalizeStatus(status);
        return errorRepository.findById(errorId)
                .map(error -> {
                    error.setStatusEnum(normalizedStatus);
                    return errorRepository.save(error);
                })
                .orElseThrow(() -> new RuntimeException("Error not found: " + errorId));
    }

    private SystemError.Severity normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return SystemError.Severity.WARNING;
        }
        try {
            return SystemError.Severity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid error severity: " + severity);
        }
    }

    private SystemError.Status normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Error status is required");
        }
        try {
            return SystemError.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid error status: " + status);
        }
    }

    // ========== Statistics ==========

    public long getOpenErrorCount() {
        return errorRepository.countOpen();
    }

    public long getCriticalErrorCount() {
        return errorRepository.countOpenCritical();
    }

    public List<SystemError> getRecentOpenErrors() {
        return errorRepository.findTop10OpenOrderedBySeverity(PageRequest.of(0, 10));
    }

    // ========== Auto-Detection (Scheduled Job) ==========

    /**
     * Runs every 6 hours to detect system issues automatically.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000, initialDelay = 60000) // 6 hours, 1 min initial delay
    @Transactional
    public void autoDetectErrors() {
        log.info("Running auto-detection for system errors...");

        checkExpiredSubscriptions();
        checkInactiveUsers();
        checkInactiveHcfs();

        log.info("Auto-detection completed");
    }

    private void checkExpiredSubscriptions() {
        String title = "Expired subscription detected";
        if (errorRepository.hasOpenAutoDetectedError(title)) {
            return; // Already reported
        }

        long expiredCount = facilityRepository.countBySubscriptionStatus("EXPIRED");
        if (expiredCount > 0) {
            createAutoDetectedError(
                    title,
                    expiredCount + " CBWTF(s) have expired subscriptions and may have limited access.",
                    "SUBSCRIPTION",
                    SystemError.Severity.WARNING);
        }
    }

    private void checkInactiveUsers() {
        // Skipped - AppUser doesn't have lastLogin field yet
        // TODO: Add lastLogin tracking to AppUser entity
        log.debug("Inactive user check skipped - lastLogin field not available");
    }

    private void checkInactiveHcfs() {
        if (errorRepository.hasOpenAutoDetectedError(INACTIVE_HCFS_TITLE)) {
            return;
        }

        Instant cutoff = Instant.now().minus(HCF_INACTIVITY_DAYS, ChronoUnit.DAYS);
        long inactiveCount = hcfRepository.countActiveHcfsWithoutRecentCollection(cutoff);
        if (inactiveCount > 0) {
            createAutoDetectedError(
                    INACTIVE_HCFS_TITLE,
                    inactiveCount + " active HCF(s) have no HCF collection event in the last "
                            + HCF_INACTIVITY_DAYS + " days.",
                    "HCF_ACTIVITY",
                    SystemError.Severity.WARNING);
        }
    }

    private void createAutoDetectedError(String title, String description,
            String component, SystemError.Severity severity) {
        SystemError error = SystemError.autoDetected(title, description, component, severity);
        errorRepository.save(error);
        log.info("Auto-detected error created: {}", title);
    }

    // ========== Auto-Resolution (Scheduled Job) ==========

    /**
     * Runs every 5 minutes to check if auto-detected errors are still valid.
     * If the underlying condition is fixed, the error is auto-resolved.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 120000) // 5 min, 2 min delay
    @Transactional
    public void autoResolveErrors() {
        log.info("Running auto-resolution check for system errors...");

        List<SystemError> openAutoErrors = errorRepository.findOpenAutoDetectedErrors();
        int resolved = 0;

        for (SystemError error : openAutoErrors) {
            if (shouldAutoResolve(error)) {
                error.setStatusEnum(SystemError.Status.RESOLVED);
                error.setResolvedAt(Instant.now());
                error.setResolutionNotes("Auto-resolved: condition no longer detected");
                errorRepository.save(error);
                log.info("Auto-resolved error: {} [{}]", error.getTitle(), error.getId());
                resolved++;
            }
        }

        log.info("Auto-resolution completed: {} errors resolved", resolved);
    }

    /**
     * Determine if an auto-detected error should be auto-resolved.
     */
    private boolean shouldAutoResolve(SystemError error) {
        String title = error.getTitle();

        // Check expired subscriptions
        if ("Expired subscription detected".equals(title)) {
            long expiredCount = facilityRepository.countBySubscriptionStatus("EXPIRED");
            return expiredCount == 0;
        }

        // Check suspended CBWTFs
        if ("Suspended CBWTF detected".equals(title)) {
            long suspendedCount = facilityRepository.countBySubscriptionStatus("SUSPENDED");
            return suspendedCount == 0;
        }

        // Check inactive HCFs
        if (INACTIVE_HCFS_TITLE.equals(title)) {
            Instant cutoff = Instant.now().minus(HCF_INACTIVITY_DAYS, ChronoUnit.DAYS);
            return hcfRepository.countActiveHcfsWithoutRecentCollection(cutoff) == 0;
        }

        // Default: don't auto-resolve unknown error types
        return false;
    }
}
