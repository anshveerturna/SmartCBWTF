package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.exception.AgreementNotActiveException;
import com.smartcbwtf.repository.AgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Universal agreement gate service.
 * All operations (QR, pickup, verification, invoicing, reporting) must call
 * assertAgreementActive().
 * 
 * This is the single enforcement point for agreement status checks.
 */
@Service
public class AgreementGuardService {

    private static final Logger log = LoggerFactory.getLogger(AgreementGuardService.class);

    private final AgreementRepository agreementRepo;

    public AgreementGuardService(AgreementRepository agreementRepo) {
        this.agreementRepo = agreementRepo;
    }

    /**
     * Assert that agreement is ACTIVE.
     * Throws AgreementNotActiveException if not active.
     * 
     * @param agreementId The agreement to check
     * @throws AgreementNotActiveException if agreement is not active
     */
    public void assertAgreementActive(UUID agreementId) {
        Agreement agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> {
                    log.warn("Agreement not found: {}", agreementId);
                    return new IllegalArgumentException("Agreement not found: " + agreementId);
                });

        if (!agreement.isActive()) {
            log.warn("Agreement {} is not active (status: {})", agreementId, agreement.getStatus());
            throw new AgreementNotActiveException(agreementId, agreement.getStatusEnum());
        }
    }

    /**
     * Assert agreement active with operation context for better error messages.
     */
    public void assertAgreementActive(UUID agreementId, String operation) {
        Agreement agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new IllegalArgumentException("Agreement not found: " + agreementId));

        if (!agreement.isActive()) {
            log.warn("Operation '{}' blocked: agreement {} is not active (status: {})",
                    operation, agreementId, agreement.getStatus());
            throw new AgreementNotActiveException(agreementId, agreement.getStatusEnum(), operation);
        }
    }

    /**
     * Get active agreement for HCF under current CBWTF.
     * Throws if no active agreement exists.
     */
    public Agreement getActiveAgreement(UUID hcfId, UUID facilityId) {
        return agreementRepo.findActiveByHcfAndFacility(hcfId, facilityId)
                .orElseThrow(() -> {
                    log.warn("No active agreement found for HCF {} under facility {}", hcfId, facilityId);
                    return new IllegalStateException(
                            String.format("No active agreement for HCF %s under facility %s", hcfId, facilityId));
                });
    }

    /**
     * Check if agreement is active (non-throwing version).
     */
    public boolean isAgreementActive(UUID agreementId) {
        return agreementRepo.findById(agreementId)
                .map(Agreement::isActive)
                .orElse(false);
    }
}
