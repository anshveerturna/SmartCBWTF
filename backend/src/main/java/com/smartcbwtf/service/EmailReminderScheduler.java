package com.smartcbwtf.service;

import com.smartcbwtf.domain.AlertSeverity;
import com.smartcbwtf.domain.AlertType;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
    private static final int FACILITY_PAGE_SIZE = 100;

    private final FacilityRepository facilityRepository;
    private final AgreementRepository agreementRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationSettingsService settingsService;
    private final AlertService alertService;
    private final EmailService emailService;

    public EmailReminderScheduler(
            FacilityRepository facilityRepository,
            AgreementRepository agreementRepository,
            InvoiceRepository invoiceRepository,
            NotificationSettingsService settingsService,
            AlertService alertService,
            EmailService emailService) {
        this.facilityRepository = facilityRepository;
        this.agreementRepository = agreementRepository;
        this.invoiceRepository = invoiceRepository;
        this.settingsService = settingsService;
        this.alertService = alertService;
        this.emailService = emailService;
    }

    /**
     * Check for expiring agreements - Daily at 09:00 IST
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void checkAgreementExpiry() {
        log.info("=== AGREEMENT EXPIRY CHECK STARTED ===");

        LocalDate today = LocalDate.now(IST);

        forEachFacility(facility -> {
            try {
                FacilityNotificationSettings settings = settingsService.getSettings(facility.getId());
                int warningDays = settings.getAgreementExpiryWarningDays();
                LocalDate warningDate = today.plusDays(warningDays);
                List<Agreement> agreements = agreementRepository.findActiveExpiringBetweenByFacilityId(
                        facility.getId(), today, warningDate);

                int created = 0;
                for (Agreement agreement : agreements) {
                    created += processAgreementExpiryReminder(agreement, today, warningDays) ? 1 : 0;
                }

                log.debug("Checked facility {} for agreements expiring by {} ({} new reminders)",
                        facility.getId(), warningDate, created);
            } catch (Exception e) {
                log.error("Failed to check expiry for facility {}: {}", facility.getId(), e.getMessage());
            }
        });

        log.info("=== AGREEMENT EXPIRY CHECK COMPLETE ===");
    }

    /**
     * Send payment reminders - Daily at 10:00 IST
     */
    @Scheduled(cron = "0 0 10 * * ?", zone = "Asia/Kolkata")
    public void sendPaymentReminders() {
        log.info("=== PAYMENT REMINDER SCHEDULER STARTED ===");

        LocalDate today = LocalDate.now(IST);

        forEachFacility(facility -> {
            try {
                FacilityNotificationSettings settings = settingsService.getSettings(facility.getId());
                List<Invoice> invoices = invoiceRepository.findUnpaidByFacilityIdWithHcf(facility.getId());

                int created = 0;
                for (Invoice invoice : invoices) {
                    created += processPaymentReminder(invoice, today, settings) ? 1 : 0;
                }

                log.debug("Processed payment reminders for facility {} ({} new reminders)",
                        facility.getId(), created);
            } catch (Exception e) {
                log.error("Failed to process reminders for facility {}: {}", facility.getId(), e.getMessage());
            }
        });

        log.info("=== PAYMENT REMINDER SCHEDULER COMPLETE ===");
    }

    private void forEachFacility(Consumer<com.smartcbwtf.domain.Facility> consumer) {
        int pageNumber = 0;
        Page<com.smartcbwtf.domain.Facility> page;
        do {
            page = facilityRepository.findAll(PageRequest.of(pageNumber++, FACILITY_PAGE_SIZE));
            page.forEach(consumer);
        } while (page.hasNext());
    }

    private boolean processAgreementExpiryReminder(Agreement agreement, LocalDate today, int warningDays) {
        if (agreement.getEndDate() == null || agreement.getFacility() == null) {
            return false;
        }

        long daysRemaining = ChronoUnit.DAYS.between(today, agreement.getEndDate());
        if (daysRemaining < 0 || daysRemaining > warningDays) {
            return false;
        }

        UUID eventId = generateReminderEventId(agreement.getId(), "AGREEMENT_EXPIRING", warningDays);
        AlertSeverity severity = daysRemaining <= 7 ? AlertSeverity.CRITICAL : AlertSeverity.WARN;
        boolean created = alertService.createAlert(
                eventId,
                agreement.getFacility().getId(),
                AlertType.AGREEMENT_EXPIRING,
                severity,
                "Agreement expiring soon",
                "Agreement " + agreement.getAgreementNumber() + " expires on " + agreement.getEndDate()
                        + " (" + daysRemaining + " days remaining).",
                "Agreement",
                agreement.getId());

        if (created) {
            sendAgreementExpiryEmail(agreement, (int) daysRemaining);
        }
        return created;
    }

    private boolean processPaymentReminder(Invoice invoice, LocalDate today, FacilityNotificationSettings settings) {
        if (invoice.getFacility() == null) {
            return false;
        }
        LocalDate dueDate = paymentDueDate(invoice);
        if (dueDate == null) {
            return false;
        }

        int sequence = calculateReminderSequence(dueDate, today, settings);
        if (sequence < 0) {
            return false;
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        long daysOverdue = Math.max(0, -daysUntilDue);
        boolean overdue = daysOverdue > 0;
        AlertType type = overdue ? AlertType.PAYMENT_OVERDUE : AlertType.PAYMENT_DUE;
        AlertSeverity severity = overdue ? AlertSeverity.CRITICAL : AlertSeverity.WARN;
        UUID eventId = generateReminderEventId(invoice.getId(), "PAYMENT_REMINDER", sequence);
        String amount = invoice.getTotalAmount() == null ? "0.00" : invoice.getTotalAmount().toPlainString();

        boolean created = alertService.createAlert(
                eventId,
                invoice.getFacility().getId(),
                type,
                severity,
                overdue ? "Payment overdue" : "Payment due soon",
                "Invoice " + invoice.getInvoiceNumber() + " for Rs. " + amount
                        + " is due on " + dueDate + ".",
                "Invoice",
                invoice.getId());

        if (created) {
            sendPaymentReminderEmail(invoice, dueDate, (int) daysOverdue);
        }
        return created;
    }

    private void sendAgreementExpiryEmail(Agreement agreement, int daysRemaining) {
        Hcf hcf = agreement.getHcf();
        if (hcf == null || isBlank(hcf.getContactEmail())) {
            return;
        }
        try {
            String html = emailService.getTemplates().agreementExpiryWarning(
                    hcf.getName(),
                    agreement.getAgreementNumber(),
                    String.valueOf(agreement.getEndDate()),
                    daysRemaining);
            emailService.sendHtmlEmail(
                    hcf.getContactEmail(),
                    "Agreement Expiry Warning - " + agreement.getAgreementNumber(),
                    html);
        } catch (Exception e) {
            log.warn("Failed to send agreement expiry email for agreement {}: {}",
                    agreement.getId(), e.getMessage());
        }
    }

    private void sendPaymentReminderEmail(Invoice invoice, LocalDate dueDate, int daysOverdue) {
        Hcf hcf = invoice.getHcf();
        if (hcf == null || isBlank(hcf.getContactEmail())) {
            return;
        }
        try {
            String amount = invoice.getTotalAmount() == null ? "0.00" : invoice.getTotalAmount().toPlainString();
            String html = emailService.getTemplates().paymentReminder(
                    hcf.getName(),
                    invoice.getInvoiceNumber(),
                    amount,
                    String.valueOf(dueDate),
                    daysOverdue);
            emailService.sendHtmlEmail(
                    hcf.getContactEmail(),
                    "Payment Reminder - " + invoice.getInvoiceNumber(),
                    html);
        } catch (Exception e) {
            log.warn("Failed to send payment reminder email for invoice {}: {}", invoice.getId(), e.getMessage());
        }
    }

    private LocalDate paymentDueDate(Invoice invoice) {
        LocalDate base = invoice.getPeriodEnd() != null ? invoice.getPeriodEnd() : invoice.getInvoiceDate();
        return base == null ? null : base.plusDays(30);
    }

    /**
     * Calculate reminder sequence number.
     * Returns -1 if no reminder should be sent.
     */
    private int calculateReminderSequence(LocalDate dueDate, LocalDate today,
            FacilityNotificationSettings settings) {
        long daysUntilDue = dueDate.toEpochDay() - today.toEpochDay();
        int reminderStartDays = positiveOrDefault(settings.getPaymentReminderStartDays(), 7);
        int frequencyDays = positiveOrDefault(settings.getPaymentReminderFrequencyDays(), 3);
        int maxOverdueReminders = positiveOrDefault(settings.getMaxOverdueReminders(), 5);

        if (daysUntilDue > reminderStartDays) {
            // Not yet time to remind
            return -1;
        }

        if (daysUntilDue >= 0) {
            // Before due date - pre-due reminders
            long daysIntoReminderWindow = reminderStartDays - daysUntilDue;
            return (int) (daysIntoReminderWindow / frequencyDays) + 1;
        }

        // After due date - overdue reminders
        long daysOverdue = -daysUntilDue;
        int overdueSequence = (int) (daysOverdue / frequencyDays) + 1;

        if (overdueSequence > maxOverdueReminders) {
            // Max reminders reached
            return -1;
        }

        // Negative sequence numbers indicate overdue
        return 100 + overdueSequence; // 101, 102, 103... for overdue
    }

    /**
     * Generate deterministic event ID for a reminder.
     */
    private UUID generateReminderEventId(UUID entityId, String reminderType, int sequence) {
        // Deterministic UUID based on entity and sequence
        String input = entityId + ":" + reminderType + ":" + sequence;
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    private int positiveOrDefault(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
