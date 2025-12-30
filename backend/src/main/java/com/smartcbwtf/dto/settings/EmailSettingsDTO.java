package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for Section 7: Email & Notification settings.
 */
public record EmailSettingsDTO(
        @NotBlank @Size(max = 100) String senderName,
        @NotBlank @Email @Size(max = 255) String senderEmail,
        @NotNull Boolean ccAdminOnHcfEmails,
        @NotNull Boolean emailNotificationsEnabled,
        @NotNull Boolean inAppAlertsEnabled) {
}
