package com.smartcbwtf.scheduler;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled task to expire agreements that have passed their end date.
 * Runs daily.
 */
@Component
public class AgreementExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgreementExpirationScheduler.class);

    private final AgreementRepository agreementRepository;
    private final AuditLogService auditLogService;

    public AgreementExpirationScheduler(AgreementRepository agreementRepository, AuditLogService auditLogService) {
        this.agreementRepository = agreementRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Run daily at 00:01 AM to expire agreements.
     */
    @Scheduled(cron = "0 1 0 * * ?")
    @Transactional
    public void expireAgreements() {
        log.info("Running agreement expiration check...");

        LocalDate today = LocalDate.now();
        // Find ACTIVE agreements where endDate is before today
        List<Agreement> expiredAgreements = agreementRepository.findByStatusAndEndDateBefore("ACTIVE", today);

        for (Agreement agreement : expiredAgreements) {
            try {
                log.info("Expiring agreement {} (ended on {})", agreement.getAgreementNumber(), agreement.getEndDate());

                agreement.setStatus("EXPIRED");
                agreementRepository.save(agreement);

                auditLogService.log(
                        "AGREEMENT",
                        agreement.getId(),
                        "AGREEMENT_EXPIRED",
                        null,
                        "Auto-expired by system. End date: " + agreement.getEndDate());
            } catch (Exception e) {
                log.error("Failed to expire agreement {}", agreement.getId(), e);
            }
        }

        if (!expiredAgreements.isEmpty()) {
            log.info("Expired {} agreements.", expiredAgreements.size());
        }
    }
}
