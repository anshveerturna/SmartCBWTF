package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AgreementBillingConfig;
import com.smartcbwtf.dto.BillingConfigRequest;
import com.smartcbwtf.dto.HcfDetailDTO;
import com.smartcbwtf.repository.AgreementBillingConfigRepository;
import com.smartcbwtf.repository.AgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for managing agreement billing configurations.
 * 
 * Business Rules:
 * - Only ONE active config per agreement at a time
 * - New config auto-expires previous
 * - Cannot modify if agreement is EXPIRED/TERMINATED
 * - All changes audit logged
 */
@Service
public class BillingConfigService {

    private static final Logger log = LoggerFactory.getLogger(BillingConfigService.class);

    private final AgreementRepository agreementRepository;
    private final AgreementBillingConfigRepository billingConfigRepository;
    private final AuditLogService auditLogService;

    public BillingConfigService(
            AgreementRepository agreementRepository,
            AgreementBillingConfigRepository billingConfigRepository,
            AuditLogService auditLogService) {
        this.agreementRepository = agreementRepository;
        this.billingConfigRepository = billingConfigRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Get current active billing config for HCF's agreement.
     */
    @Transactional(readOnly = true)
    public HcfDetailDTO.BillingConfigInfo getCurrentConfig(UUID hcfId, UUID facilityId) {
        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or agreement not active"));

        AgreementBillingConfig config = billingConfigRepository
                .findActiveByAgreementId(agreement.getId())
                .orElse(null);

        return HcfDetailDTO.BillingConfigInfo.from(config);
    }

    /**
     * Create new billing config (expires previous if exists).
     * Only allowed if agreement is ACTIVE.
     */
    @Transactional
    public HcfDetailDTO.BillingConfigInfo createConfig(UUID hcfId, UUID facilityId, BillingConfigRequest request) {
        // Verify access and get agreement
        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcfId, facilityId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found or agreement not active"));

        // Check agreement is active
        if (!agreement.isActive()) {
            throw new IllegalStateException("Cannot modify billing config: Agreement is not active");
        }

        LocalDate today = LocalDate.now();

        // Expire previous active config if exists
        billingConfigRepository.findActiveByAgreementId(agreement.getId())
                .ifPresent(existingConfig -> {
                    existingConfig.expire(today.minusDays(1));
                    billingConfigRepository.save(existingConfig);
                    log.info("Expired previous billing config {} for agreement {}",
                            existingConfig.getId(), agreement.getAgreementNumber());
                });

        // Create new config
        AgreementBillingConfig newConfig = new AgreementBillingConfig();
        newConfig.setAgreement(agreement);
        newConfig.setBaseGramsPerBedPerDay(request.getBaseGramsPerBedPerDay());
        newConfig.setBaseRatePerBedPerDay(request.getBaseRatePerBedPerDay());
        newConfig.setExcessRatePerKg(request.getExcessRatePerKg());
        newConfig.setEffectiveFrom(today);
        // effectiveTo stays null (active)
        newConfig.setCreatedBy(UUID.randomUUID()); // TODO: Get from security context

        billingConfigRepository.save(newConfig);

        // Audit log
        String details = String.format("Base: %s/bed/day, Excess: %s/kg, Allowance: %dg/bed/day",
                request.getBaseRatePerBedPerDay(),
                request.getExcessRatePerKg(),
                request.getBaseGramsPerBedPerDay());
        auditLogService.log("AGREEMENT", agreement.getId(), "BILLING_CONFIG_CREATED", null, details);
        log.info("Created billing config for agreement {}: {}", agreement.getAgreementNumber(), details);

        return HcfDetailDTO.BillingConfigInfo.from(newConfig);
    }
}
