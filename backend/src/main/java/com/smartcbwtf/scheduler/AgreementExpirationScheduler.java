package com.smartcbwtf.scheduler;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.HcfService;
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
    private final HcfService hcfService;

    public AgreementExpirationScheduler(AgreementRepository agreementRepository, AuditLogService auditLogService,
            HcfService hcfService) {
        this.agreementRepository = agreementRepository;
        this.auditLogService = auditLogService;
        this.hcfService = hcfService;
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

    /**
     * Run daily at 00:05 AM to activate UPCOMING agreements that have reached their
     * start date.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void activateUpcomingAgreements() {
        log.info("Running upcoming agreement activation check...");

        LocalDate today = LocalDate.now();
        List<Agreement> upcomingAgreements = agreementRepository.findByStatusAndStartDateLessThanEqual("UPCOMING",
                today);

        for (Agreement agreement : upcomingAgreements) {
            try {
                log.info("Activating UPCOMING agreement {} (started on {})", agreement.getAgreementNumber(),
                        agreement.getStartDate());

                agreement.setStatus("ACTIVE");
                agreement.getHcf().setStatus("ACTIVE"); // Ensure HCF reflects active status
                agreementRepository.save(agreement);

                auditLogService.log(
                        "AGREEMENT",
                        agreement.getId(),
                        "AGREEMENT_ACTIVATED",
                        null,
                        "Auto-activated UPCOMING agreement. Start date: " + agreement.getStartDate());

                // Dispatch deferred registration email
                Facility facility = agreement.getFacility();
                hcfService.sendRegistrationEmail(agreement.getHcf(), agreement, facility);

            } catch (Exception e) {
                log.error("Failed to activate UPCOMING agreement {}", agreement.getId(), e);
            }
        }

        if (!upcomingAgreements.isEmpty()) {
            log.info("Activated {} UPCOMING agreements.", upcomingAgreements.size());
        }
    }
}
