package com.smartcbwtf.service;

import com.smartcbwtf.domain.ApprovalStatus;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.HcfBedAccessCategory;
import com.smartcbwtf.repository.HcfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Central HCF access control service.
 * 
 * Enforces regulatory access rules:
 * - Only HCFs with 30+ beds (ABOVE_30_BEDS) can access the portal
 * - Only APPROVED HCFs can access the portal
 * 
 * This is the SINGLE source of truth for HCF portal eligibility.
 */
@Service
public class HcfAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(HcfAccessGuard.class);

    private final HcfRepository hcfRepository;
    private final com.smartcbwtf.repository.AgreementRepository agreementRepository;

    public HcfAccessGuard(HcfRepository hcfRepository,
            com.smartcbwtf.repository.AgreementRepository agreementRepository) {
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
    }

    /**
     * Check if an HCF is eligible for portal access.
     * 
     * @param hcfId The HCF UUID
     * @return AccessCheckResult with eligibility and reason
     */
    public AccessCheckResult checkPortalAccess(UUID hcfId) {
        if (hcfId == null) {
            return AccessCheckResult.denied("NO_HCF_LINKED",
                    "User is not linked to any healthcare facility.");
        }

        Hcf hcf = hcfRepository.findById(hcfId).orElse(null);
        if (hcf == null) {
            return AccessCheckResult.denied("HCF_NOT_FOUND",
                    "Healthcare facility not found.");
        }

        AccessCheckResult eligibility = checkHcfEligibility(hcfId, hcf);
        if (!eligibility.isAllowed()) {
            return eligibility;
        }

        // Check active agreement validity
        java.util.Optional<com.smartcbwtf.domain.Agreement> activeAgreementOpt = agreementRepository
                .findFirstByHcfIdAndStatusOrderByStartDateDesc(
                        hcfId, com.smartcbwtf.domain.Agreement.Status.ACTIVE.name());
        return checkAgreementValidity(hcfId, activeAgreementOpt);
    }

    /**
     * Check if an HCF is eligible for portal access within a specific CBWTF tenant.
     */
    public AccessCheckResult checkPortalAccess(UUID hcfId, UUID facilityId) {
        if (hcfId == null) {
            return AccessCheckResult.denied("NO_HCF_LINKED",
                    "User is not linked to any healthcare facility.");
        }
        if (facilityId == null) {
            return AccessCheckResult.denied("NO_TENANT_LINKED",
                    "User is not linked to any CBWTF tenant.");
        }

        Hcf hcf = hcfRepository.findByIdAndFacilityId(hcfId, facilityId).orElse(null);
        if (hcf == null) {
            return AccessCheckResult.denied("HCF_NOT_FOUND",
                    "Healthcare facility not found.");
        }

        AccessCheckResult eligibility = checkHcfEligibility(hcfId, hcf);
        if (!eligibility.isAllowed()) {
            return eligibility;
        }

        // Check active agreement validity
        java.util.Optional<com.smartcbwtf.domain.Agreement> activeAgreementOpt = agreementRepository
                .findActiveByHcfAndFacility(hcfId, facilityId);
        return checkAgreementValidity(hcfId, activeAgreementOpt);
    }

    private AccessCheckResult checkHcfEligibility(UUID hcfId, Hcf hcf) {
        if (!hcf.isPortalEligible()) {
            log.warn("HCF portal access denied - not eligible: hcfId={}, category={}, manuallyEnabled={}",
                    hcfId, hcf.getBedAccessCategory(), hcf.isPortalAccessManuallyEnabled());
            return AccessCheckResult.denied("ACCESS_DENIED_NOT_ELIGIBLE",
                    "Portal Access Unavailable — Facility Not Eligible");
        }

        // Check approval status
        if (hcf.getApprovalStatus() != ApprovalStatus.APPROVED) {
            log.warn("HCF portal access denied - not approved: hcfId={}, status={}",
                    hcfId, hcf.getApprovalStatus());
            return AccessCheckResult.denied("ACCESS_DENIED_NOT_APPROVED",
                    "Portal Access Unavailable — HCF Pending Approval");
        }
        return AccessCheckResult.granted();
    }

    private AccessCheckResult checkAgreementValidity(UUID hcfId,
            java.util.Optional<com.smartcbwtf.domain.Agreement> activeAgreementOpt) {
        if (activeAgreementOpt.isEmpty()) {
            log.warn("HCF portal access denied - no active agreement: hcfId={}", hcfId);
            return AccessCheckResult.denied("NO_ACTIVE_AGREEMENT",
                    "Portal Access Unavailable — No Active Service Agreement");
        }

        com.smartcbwtf.domain.Agreement agreement = activeAgreementOpt.get();
        if (agreement.getEndDate() != null && agreement.getEndDate().isBefore(java.time.LocalDate.now())) {
            log.warn("HCF portal access denied - agreement expired: hcfId={}, endDate={}", hcfId,
                    agreement.getEndDate());
            return AccessCheckResult.denied("AGREEMENT_EXPIRED",
                    "Portal Access Unavailable — Service Agreement Expired on " + agreement.getEndDate());
        }

        log.debug("HCF portal access granted: hcfId={}", hcfId);
        return AccessCheckResult.granted();
    }

    /**
     * Assert that an HCF is eligible for portal access.
     * Throws HcfAccessDeniedException if not eligible.
     * 
     * @param hcfId The HCF UUID
     * @throws HcfAccessDeniedException if access denied
     */
    public void assertPortalAccess(UUID hcfId) {
        AccessCheckResult result = checkPortalAccess(hcfId);
        if (!result.isAllowed()) {
            throw new HcfAccessDeniedException(result.getErrorCode(), result.getMessage());
        }
    }

    public void assertPortalAccess(UUID hcfId, UUID facilityId) {
        AccessCheckResult result = checkPortalAccess(hcfId, facilityId);
        if (!result.isAllowed()) {
            throw new HcfAccessDeniedException(result.getErrorCode(), result.getMessage());
        }
    }

    /**
     * Result of an access check.
     */
    public static class AccessCheckResult {
        private final boolean allowed;
        private final String errorCode;
        private final String message;

        private AccessCheckResult(boolean allowed, String errorCode, String message) {
            this.allowed = allowed;
            this.errorCode = errorCode;
            this.message = message;
        }

        public static AccessCheckResult granted() {
            return new AccessCheckResult(true, null, null);
        }

        public static AccessCheckResult denied(String errorCode, String message) {
            return new AccessCheckResult(false, errorCode, message);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Exception thrown when HCF portal access is denied.
     */
    public static class HcfAccessDeniedException extends RuntimeException {
        private final String errorCode;

        public HcfAccessDeniedException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
