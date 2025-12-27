package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled task to expire agreements that have passed their end date.
 */
@Component
public class AgreementExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgreementExpirationScheduler.class);

    private final AgreementRepository agreementRepository;
    private final HcfRepository hcfRepository;
    private final AuditLogService auditLogService;

    public AgreementExpirationScheduler(
            AgreementRepository agreementRepository,
            HcfRepository hcfRepository,
            AuditLogService auditLogService) {
        this.agreementRepository = agreementRepository;
        this.hcfRepository = hcfRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Run daily at midnight to check for expired agreements.
     * Expire agreements where endDate < today.
     */
    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    @Transactional
    public void expireAgreements() {
        log.info("Running daily agreement expiration check...");

        // Find active agreements that have expired (endDate < today)
        LocalDate today = LocalDate.now();
        List<Agreement> expiredAgreements = agreementRepository.findByStatusAndEndDateBefore(
                Agreement.Status.ACTIVE.name(), today);

        log.info("Found {} active agreements past their end date", expiredAgreements.size());

        for (Agreement agreement : expiredAgreements) {
            expireAgreement(agreement);
        }

        log.info("Agreement expiration check completed.");
    }

    private void expireAgreement(Agreement agreement) {
        try {
            log.info("Expiring agreement {} for HCF {}", agreement.getAgreementNumber(), agreement.getHcf().getId());

            // 1. Update Agreement Status
            agreement.setStatus(Agreement.Status.EXPIRED.name());
            agreement.setUpdatedAt(Instant.now());
            agreementRepository.save(agreement);

            // 2. Update HCF Status to INACTIVE
            Hcf hcf = agreement.getHcf();
            hcf.setStatus("INACTIVE");
            hcf.setUpdatedAt(Instant.now());
            hcfRepository.save(hcf);

            // 3. Audit Log
            auditLogService.log("HCF", hcf.getId(), "AGREEMENT_EXPIRED", null,
                    "Agreement " + agreement.getAgreementNumber() + " auto-expired due to end date "
                            + agreement.getEndDate());

        } catch (Exception e) {
            log.error("Failed to expire agreement {}", agreement.getId(), e);
        }
    }
}
