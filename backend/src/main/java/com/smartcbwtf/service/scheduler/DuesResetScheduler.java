package com.smartcbwtf.service.scheduler;

import com.smartcbwtf.domain.DuesClearStatus;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DuesResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(DuesResetScheduler.class);

    private final HcfRepository hcfRepository;
    private final AuditLogService auditLogService;

    public DuesResetScheduler(HcfRepository hcfRepository, AuditLogService auditLogService) {
        this.hcfRepository = hcfRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Resets HCF dues status to PENDING on the 1st of every month at midnight.
     * Cron expression: 0 0 0 1 * ? (Seconds Minutes Hours DayMonth Month DayWeek)
     */
    @Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Kolkata")
    @Transactional
    public void resetMonthlyDuesStatus() {
        LocalDate today = LocalDate.now();
        log.info("Starting monthly dues reset for period: {}", today.getMonth());

        // Find all HCFs that are not already PENDING
        // We only reset those who are CLEARED or REQUESTED, as they need to re-verify
        // for the new month
        List<Hcf> hcfsToReset = hcfRepository.findByDuesClearStatusNot(DuesClearStatus.PENDING);

        int count = 0;
        for (Hcf hcf : hcfsToReset) {
            hcf.setDuesClearStatus(DuesClearStatus.PENDING);
            hcfRepository.save(hcf);
            count++;

            // Audit log for trackability
            auditLogService.logSystemEvent("DUES_RESET", hcf.getId(), "Monthly reset to PENDING");
        }

        log.info("Completed monthly dues reset. Reset {} HCFs to PENDING status.", count);
    }
}
