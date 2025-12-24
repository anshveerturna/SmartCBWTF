package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.exception.AgreementBlockedException;
import com.smartcbwtf.exception.AgreementBlockedException.BlockReason;
import com.smartcbwtf.repository.AgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for validating agreement eligibility.
 * Enforces the core anti-leakage invariants:
 * - Only ONE ACTIVE agreement per HCF globally
 * - No new agreement if previous is ACTIVE
 * - No new agreement if dues_status != CLEAR
 */
@Service
public class AgreementValidationService {

    private static final Logger log = LoggerFactory.getLogger(AgreementValidationService.class);

    private final AgreementRepository agreementRepo;
    private final AuditLogService auditLog;

    public AgreementValidationService(AgreementRepository agreementRepo, AuditLogService auditLog) {
        this.agreementRepo = agreementRepo;
        this.auditLog = auditLog;
    }

    /**
     * Check if HCF can have a new agreement.
     * Returns eligibility result with block reason if not eligible.
     */
    public AgreementEligibility checkEligibility(UUID hcfId) {
        // 1. Check for ACTIVE agreement (any CBWTF)
        Optional<Agreement> active = agreementRepo.findActiveByHcfId(hcfId);
        if (active.isPresent()) {
            log.info("HCF {} blocked: active agreement exists ({})", hcfId, active.get().getId());
            return AgreementEligibility.blocked(BlockReason.ACTIVE_AGREEMENT_EXISTS, active.get().getId());
        }

        // 2. Check for pending dues on any previous agreement
        List<Agreement> withDues = agreementRepo.findByHcfIdAndDuesStatus(hcfId,
                Agreement.DuesStatus.PENDING.name());
        if (!withDues.isEmpty()) {
            log.info("HCF {} blocked: unpaid dues on agreement {}", hcfId, withDues.get(0).getId());
            return AgreementEligibility.blocked(BlockReason.UNPAID_DUES, withDues.get(0).getId());
        }

        // 3. Check for open disputes
        List<Agreement> disputed = agreementRepo.findByHcfIdAndStatus(hcfId,
                Agreement.Status.DISPUTED.name());
        if (!disputed.isEmpty()) {
            log.info("HCF {} blocked: dispute open on agreement {}", hcfId, disputed.get(0).getId());
            return AgreementEligibility.blocked(BlockReason.DISPUTE_OPEN, disputed.get(0).getId());
        }

        // 4. Blacklist check (future enhancement)
        // if (blacklistService.isBlacklisted(hcfId)) {
        // return AgreementEligibility.blocked(BlockReason.BLACKLISTED, null);
        // }

        log.debug("HCF {} is eligible for new agreement", hcfId);
        return AgreementEligibility.eligible();
    }

    /**
     * Assert that HCF can have a new agreement.
     * Throws AgreementBlockedException if not eligible.
     */
    public void assertCanCreateAgreement(UUID hcfId) {
        AgreementEligibility eligibility = checkEligibility(hcfId);
        if (!eligibility.isEligible()) {
            auditLog.log("AGREEMENT", hcfId, "AGREEMENT_CREATION_BLOCKED", null, eligibility.getBlockReason().name());
            throw new AgreementBlockedException(eligibility.getBlockReason(), eligibility.getBlockingAgreementId());
        }
    }

    /**
     * Eligibility result DTO.
     */
    public static class AgreementEligibility {
        private final boolean eligible;
        private final BlockReason blockReason;
        private final UUID blockingAgreementId;

        private AgreementEligibility(boolean eligible, BlockReason blockReason, UUID blockingAgreementId) {
            this.eligible = eligible;
            this.blockReason = blockReason;
            this.blockingAgreementId = blockingAgreementId;
        }

        public static AgreementEligibility eligible() {
            return new AgreementEligibility(true, null, null);
        }

        public static AgreementEligibility blocked(BlockReason reason, UUID blockingId) {
            return new AgreementEligibility(false, reason, blockingId);
        }

        public boolean isEligible() {
            return eligible;
        }

        public BlockReason getBlockReason() {
            return blockReason;
        }

        public UUID getBlockingAgreementId() {
            return blockingAgreementId;
        }
    }
}
