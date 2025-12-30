package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for Section 6: Compliance & Reporting settings.
 */
public record ComplianceSettingsDTO(
        @NotNull LocalTime dailyReportTime,
        @NotNull @Min(1) @Max(28) Integer monthlyReportDay,
        LocalDate annualFormIvDate,
        @NotNull Boolean enforceChecksum) {
}
