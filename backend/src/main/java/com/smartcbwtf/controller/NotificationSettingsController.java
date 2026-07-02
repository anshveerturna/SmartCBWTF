package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.FacilityNotificationSettings;
import com.smartcbwtf.service.NotificationSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    public ResponseEntity<SettingsDTO> updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
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
            @Min(value = 1, message = "Payment reminder start days must be at least 1")
            @Max(value = 30, message = "Payment reminder start days must be 30 or less")
            Integer paymentReminderStartDays,
            @Min(value = 1, message = "Payment reminder frequency days must be at least 1")
            @Max(value = 14, message = "Payment reminder frequency days must be 14 or less")
            Integer paymentReminderFrequencyDays,
            @Min(value = 1, message = "Max overdue reminders must be at least 1")
            @Max(value = 10, message = "Max overdue reminders must be 10 or less")
            Integer maxOverdueReminders,
            @Min(value = 7, message = "Agreement expiry warning days must be at least 7")
            @Max(value = 90, message = "Agreement expiry warning days must be 90 or less")
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
