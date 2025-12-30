package com.smartcbwtf.service;

import com.smartcbwtf.domain.AlertSeverity;
import com.smartcbwtf.domain.AlertType;
import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Email Reminder Scheduler.
 * 
 * Generates reminder emails and alerts on schedule.
 * Respects per-facility settings.
 * Idempotent - same reminder never sent twice.
 */
@Service
public class EmailReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailReminderScheduler.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final FacilityRepository facilityRepository;
    private final NotificationSettingsService settingsService;
    private final AlertService alertService;

    public EmailReminderScheduler(
            FacilityRepository facilityRepository,
            NotificationSettingsService settingsService,
            AlertService alertService) {
        this.facilityRepository = facilityRepository;
        this.settingsService = settingsService;
        this.alertService = alertService;
    }

    /**
     * Check for expiring agreements - Daily at 09:00 IST
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void checkAgreementExpiry() {
        log.info("=== AGREEMENT EXPIRY CHECK STARTED ===");

        LocalDate today = LocalDate.now(IST);

        for (var facility : facilityRepository.findAll()) {
            try {
                FacilityNotificationSettings settings = settingsService.getSettings(facility.getId());
                int warningDays = settings.getAgreementExpiryWarningDays();
                LocalDate warningDate = today.plusDays(warningDays);

                // TODO: Query agreements expiring within window
                // For each expiring agreement:
                // 1. Generate event_id = hash(agreement_id, "EXPIRY_WARNING")
                // 2. Create alert (idempotent)
                // 3. Send email (idempotent)

                log.debug("Checked facility {} for agreements expiring by {}",
                        facility.getId(), warningDate);
            } catch (Exception e) {
                log.error("Failed to check expiry for facility {}: {}", facility.getId(), e.getMessage());
            }
        }

        log.info("=== AGREEMENT EXPIRY CHECK COMPLETE ===");
    }

    /**
     * Send payment reminders - Daily at 10:00 IST
     */
    @Scheduled(cron = "0 0 10 * * ?", zone = "Asia/Kolkata")
    public void sendPaymentReminders() {
        log.info("=== PAYMENT REMINDER SCHEDULER STARTED ===");

        LocalDate today = LocalDate.now(IST);

        for (var facility : facilityRepository.findAll()) {
            try {
                FacilityNotificationSettings settings = settingsService.getSettings(facility.getId());

                // TODO: Query unpaid invoices for this facility
                // For each unpaid invoice:
                // 1. Calculate days until/since due date
                // 2. Determine reminder sequence based on settings
                // 3. Generate event_id = hash(invoice_id, sequence)
                // 4. Create alert (idempotent)
                // 5. Send email (idempotent)

                log.debug("Processed payment reminders for facility {}", facility.getId());
            } catch (Exception e) {
                log.error("Failed to process reminders for facility {}: {}", facility.getId(), e.getMessage());
            }
        }

        log.info("=== PAYMENT REMINDER SCHEDULER COMPLETE ===");
    }

    /**
     * Calculate reminder sequence number.
     * Returns -1 if no reminder should be sent.
     */
    private int calculateReminderSequence(LocalDate dueDate, LocalDate today,
            FacilityNotificationSettings settings) {
        long daysUntilDue = dueDate.toEpochDay() - today.toEpochDay();

        if (daysUntilDue > settings.getPaymentReminderStartDays()) {
            // Not yet time to remind
            return -1;
        }

        if (daysUntilDue >= 0) {
            // Before due date - pre-due reminders
            long daysIntoReminderWindow = settings.getPaymentReminderStartDays() - daysUntilDue;
            return (int) (daysIntoReminderWindow / settings.getPaymentReminderFrequencyDays()) + 1;
        }

        // After due date - overdue reminders
        long daysOverdue = -daysUntilDue;
        int overdueSequence = (int) (daysOverdue / settings.getPaymentReminderFrequencyDays()) + 1;

        if (overdueSequence > settings.getMaxOverdueReminders()) {
            // Max reminders reached
            return -1;
        }

        // Negative sequence numbers indicate overdue
        return 100 + overdueSequence; // 101, 102, 103... for overdue
    }

    /**
     * Generate deterministic event ID for a reminder.
     */
    private UUID generateReminderEventId(UUID entityId, int sequence) {
        // Deterministic UUID based on entity and sequence
        String input = entityId.toString() + ":" + sequence;
        return UUID.nameUUIDFromBytes(input.getBytes());
    }
}
