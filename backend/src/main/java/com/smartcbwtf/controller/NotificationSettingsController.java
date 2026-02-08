package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.service.NotificationSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Notification Settings Controller.
 * Manages per-CBWTF notification preferences.
 */
@RestController
@RequestMapping("/api/cbwtf/settings/notifications")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    public NotificationSettingsController(NotificationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Get current notification settings.
     */
    @GetMapping
    public ResponseEntity<SettingsDTO> getSettings() {
        UUID facilityId = TenantContext.getTenantId();
        FacilityNotificationSettings settings = settingsService.getSettings(facilityId);
        return ResponseEntity.ok(SettingsDTO.from(settings));
    }

    /**
     * Update notification settings.
     */
    @PutMapping
    public ResponseEntity<SettingsDTO> updateSettings(@RequestBody UpdateSettingsRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        var updateRequest = new NotificationSettingsService.UpdateRequest(
                request.paymentReminderStartDays,
                request.paymentReminderFrequencyDays,
                request.maxOverdueReminders,
                request.agreementExpiryWarningDays);

        FacilityNotificationSettings settings = settingsService.updateSettings(facilityId, updateRequest);
        return ResponseEntity.ok(SettingsDTO.from(settings));
    }

    public record UpdateSettingsRequest(
            Integer paymentReminderStartDays,
            Integer paymentReminderFrequencyDays,
            Integer maxOverdueReminders,
            Integer agreementExpiryWarningDays) {
    }

    public record SettingsDTO(
            Integer paymentReminderStartDays,
            Integer paymentReminderFrequencyDays,
            Integer maxOverdueReminders,
            Integer agreementExpiryWarningDays,
            String updatedAt) {
        public static SettingsDTO from(FacilityNotificationSettings s) {
            return new SettingsDTO(
                    s.getPaymentReminderStartDays(),
                    s.getPaymentReminderFrequencyDays(),
                    s.getMaxOverdueReminders(),
                    s.getAgreementExpiryWarningDays(),
                    s.getUpdatedAt().toString());
        }
    }
}
