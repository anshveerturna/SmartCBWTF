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
        FacilityNotificationSettings settings = getSettings(facilityId);

        String oldValues = String.format(
                "startDays=%d, frequency=%d, maxOverdue=%d, expiryWarning=%d",
                settings.getPaymentReminderStartDays(),
                settings.getPaymentReminderFrequencyDays(),
                settings.getMaxOverdueReminders(),
                settings.getAgreementExpiryWarningDays());

        if (request.paymentReminderStartDays != null) {
            settings.setPaymentReminderStartDays(request.paymentReminderStartDays);
        }
        if (request.paymentReminderFrequencyDays != null) {
            settings.setPaymentReminderFrequencyDays(request.paymentReminderFrequencyDays);
        }
        if (request.maxOverdueReminders != null) {
            settings.setMaxOverdueReminders(request.maxOverdueReminders);
        }
        if (request.agreementExpiryWarningDays != null) {
            settings.setAgreementExpiryWarningDays(request.agreementExpiryWarningDays);
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

    public record UpdateRequest(
            Integer paymentReminderStartDays,
            Integer paymentReminderFrequencyDays,
            Integer maxOverdueReminders,
            Integer agreementExpiryWarningDays) {
    }
}
