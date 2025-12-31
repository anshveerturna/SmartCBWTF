package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for Section 7: Email & Notification settings.
 * 
 * Note: Sender identity (resolvedSenderName, resolvedSenderEmail) is
 * system-controlled
 * and read-only. Only useGenericSender, notificationEmail, and toggle flags are
 * editable.
 */
public record EmailSettingsDTO(
                // Read-only: System-computed sender display name
                String resolvedSenderName,
                // Read-only: System-computed sender email address
                String resolvedSenderEmail,
                // Read-only: Whether sender slug is locked (after first email sent)
                Boolean senderSlugLocked,
                // Editable: Use generic sender (no-reply@smartcbwtf.com)
                @NotNull Boolean useGenericSender,
                // Editable: CBWTF notification receiving email
                @Email @Size(max = 255) String notificationEmail,
                // Editable: CC admin on HCF emails
                @NotNull Boolean ccAdminOnHcfEmails,
                // Editable: Enable email notifications
                @NotNull Boolean emailNotificationsEnabled,
                // Editable: Enable in-app alerts
                @NotNull Boolean inAppAlertsEnabled) {

        /**
         * Create a response DTO from FacilitySettings.
         */
        public static EmailSettingsDTO fromSettings(
                        String resolvedSenderName,
                        String resolvedSenderEmail,
                        boolean senderSlugLocked,
                        boolean useGenericSender,
                        String notificationEmail,
                        boolean ccAdminOnHcfEmails,
                        boolean emailNotificationsEnabled,
                        boolean inAppAlertsEnabled) {
                return new EmailSettingsDTO(
                                resolvedSenderName,
                                resolvedSenderEmail,
                                senderSlugLocked,
                                useGenericSender,
                                notificationEmail,
                                ccAdminOnHcfEmails,
                                emailNotificationsEnabled,
                                inAppAlertsEnabled);
        }
}
