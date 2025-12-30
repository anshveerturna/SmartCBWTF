package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Section 3: Payment & Reminder settings.
 */
public record PaymentReminderDTO(
        @NotNull @Min(0) @Max(30) Integer gracePeriodDays,
        @NotNull Boolean autoAlertEscalation) {
}
