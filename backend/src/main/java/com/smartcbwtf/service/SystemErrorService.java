package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing system errors and issues.
 * Includes auto-detection of common problems.
 */
@Service
public class SystemErrorService {

    private static final Logger log = LoggerFactory.getLogger(SystemErrorService.class);

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
        error.setSeverity(severity != null ? severity : "WARNING");
        error.setSourceEnum(SystemError.Source.USER_REPORTED);
        error.setStatusEnum(SystemError.Status.OPEN);

        if (reportedById != null) {
            userRepository.findById(reportedById).ifPresent(error::setReportedBy);
        }
        if (facilityId != null) {
            facilityRepository.findById(facilityId).ifPresent(error::setFacility);
        }
        if (hcfId != null) {
            hcfRepository.findById(hcfId).ifPresent(error::setHcf);
        }

        error = errorRepository.save(error);
        log.info("New error reported: {} [{}]", title, error.getId());
        return error;
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
        return errorRepository.findById(errorId)
                .map(error -> {
                    error.setStatus(status);
                    return errorRepository.save(error);
                })
                .orElseThrow(() -> new RuntimeException("Error not found: " + errorId));
    }

    // ========== Statistics ==========

    public long getOpenErrorCount() {
        return errorRepository.countOpen();
    }

    public long getCriticalErrorCount() {
        return errorRepository.countOpenCritical();
    }

    public List<SystemError> getRecentOpenErrors() {
        return errorRepository.findTop10OpenOrderedBySeverity();
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
        String title = "HCFs with no recent activity";
        if (errorRepository.hasOpenAutoDetectedError(title)) {
            return;
        }

        // Check for HCFs with no activity - simplified check based on updatedAt
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        // This would ideally check pickup activity, but we'll use a simple check for
        // now
        long totalHcfs = hcfRepository.count();
        if (totalHcfs > 0) {
            // Placeholder - in production, check against bag_event or pickup tables
            log.debug("HCF activity check: {} total HCFs", totalHcfs);
        }
    }

    private void createAutoDetectedError(String title, String description,
            String component, SystemError.Severity severity) {
        SystemError error = SystemError.autoDetected(title, description, component, severity);
        errorRepository.save(error);
        log.info("Auto-detected error created: {}", title);
    }
}
