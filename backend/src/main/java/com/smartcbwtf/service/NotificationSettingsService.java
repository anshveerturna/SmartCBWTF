package com.smartcbwtf.service;

import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.repository.FacilityNotificationSettingsRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Notification Settings Service.
 * Manages per-facility notification preferences.
 */
@Service
public class NotificationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSettingsService.class);
    private static final int MIN_PAYMENT_REMINDER_START_DAYS = 1;
    private static final int MAX_PAYMENT_REMINDER_START_DAYS = 30;
    private static final int MIN_PAYMENT_REMINDER_FREQUENCY_DAYS = 1;
    private static final int MAX_PAYMENT_REMINDER_FREQUENCY_DAYS = 14;
    private static final int MIN_MAX_OVERDUE_REMINDERS = 1;
    private static final int MAX_MAX_OVERDUE_REMINDERS = 10;
    private static final int MIN_AGREEMENT_EXPIRY_WARNING_DAYS = 7;
    private static final int MAX_AGREEMENT_EXPIRY_WARNING_DAYS = 90;

    private final FacilityNotificationSettingsRepository settingsRepository;
    private final FacilityRepository facilityRepository;
    private final AuditLogService auditLogService;

    public NotificationSettingsService(
            FacilityNotificationSettingsRepository settingsRepository,
            FacilityRepository facilityRepository,
            AuditLogService auditLogService) {
        this.settingsRepository = settingsRepository;
        this.facilityRepository = facilityRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Get settings for facility (creates default if not exists).
     */
    @Transactional
    public FacilityNotificationSettings getSettings(UUID facilityId) {
        return settingsRepository.findById(facilityId)
                .orElseGet(() -> createDefaultSettings(facilityId));
    }

    private FacilityNotificationSettings createDefaultSettings(UUID facilityId) {
        Facility facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility == null) {
            throw new IllegalArgumentException("Facility not found: " + facilityId);
        }

        FacilityNotificationSettings settings = new FacilityNotificationSettings();
        settings.setFacility(facility);
        settings.setPaymentReminderStartDays(7);
        settings.setPaymentReminderFrequencyDays(3);
        settings.setMaxOverdueReminders(5);
        settings.setAgreementExpiryWarningDays(30);

        return settingsRepository.save(settings);
    }

    /**
     * Update settings.
     */
    @Transactional
    public FacilityNotificationSettings updateSettings(UUID facilityId, UpdateRequest request) {
        UpdateRequest safeRequest = validateRequest(request);
        FacilityNotificationSettings settings = getSettings(facilityId);

        String oldValues = String.format(
                "startDays=%d, frequency=%d, maxOverdue=%d, expiryWarning=%d",
                settings.getPaymentReminderStartDays(),
                settings.getPaymentReminderFrequencyDays(),
                settings.getMaxOverdueReminders(),
                settings.getAgreementExpiryWarningDays());

        if (safeRequest.paymentReminderStartDays != null) {
            settings.setPaymentReminderStartDays(safeRequest.paymentReminderStartDays);
        }
        if (safeRequest.paymentReminderFrequencyDays != null) {
            settings.setPaymentReminderFrequencyDays(safeRequest.paymentReminderFrequencyDays);
        }
        if (safeRequest.maxOverdueReminders != null) {
            settings.setMaxOverdueReminders(safeRequest.maxOverdueReminders);
        }
        if (safeRequest.agreementExpiryWarningDays != null) {
            settings.setAgreementExpiryWarningDays(safeRequest.agreementExpiryWarningDays);
        }

        String newValues = String.format(
                "startDays=%d, frequency=%d, maxOverdue=%d, expiryWarning=%d",
                settings.getPaymentReminderStartDays(),
                settings.getPaymentReminderFrequencyDays(),
                settings.getMaxOverdueReminders(),
                settings.getAgreementExpiryWarningDays());

        // Log the settings change
        String dataJson = String.format("{\"old\": \"%s\", \"new\": \"%s\"}", oldValues, newValues);
        auditLogService.log("FacilityNotificationSettings", facilityId, "SETTINGS_UPDATED",
                null, dataJson);

        log.info("Updated notification settings for facility {}", facilityId);
        return settingsRepository.save(settings);
    }

    private UpdateRequest validateRequest(UpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification settings request is required");
        }
        validateRange(
                request.paymentReminderStartDays,
                MIN_PAYMENT_REMINDER_START_DAYS,
                MAX_PAYMENT_REMINDER_START_DAYS,
                "Payment reminder start days");
        validateRange(
                request.paymentReminderFrequencyDays,
                MIN_PAYMENT_REMINDER_FREQUENCY_DAYS,
                MAX_PAYMENT_REMINDER_FREQUENCY_DAYS,
                "Payment reminder frequency days");
        validateRange(
                request.maxOverdueReminders,
                MIN_MAX_OVERDUE_REMINDERS,
                MAX_MAX_OVERDUE_REMINDERS,
                "Max overdue reminders");
        validateRange(
                request.agreementExpiryWarningDays,
                MIN_AGREEMENT_EXPIRY_WARNING_DAYS,
                MAX_AGREEMENT_EXPIRY_WARNING_DAYS,
                "Agreement expiry warning days");
        return request;
    }

    private void validateRange(Integer value, int min, int max, String fieldName) {
        if (value == null) {
            return;
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max);
        }
    }

    public record UpdateRequest(
            Integer paymentReminderStartDays,
            Integer paymentReminderFrequencyDays,
            Integer maxOverdueReminders,
            Integer agreementExpiryWarningDays) {
    }
}
