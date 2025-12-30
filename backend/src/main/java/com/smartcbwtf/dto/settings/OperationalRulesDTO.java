package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for Section 5: QR & Operational Rules settings.
 */
public record OperationalRulesDTO(
        @NotNull @Min(1) @Max(365) Integer qrValidityDays,
        @NotNull Boolean allowMultipleActiveQrs,
        @NotNull Boolean requireCbwtfVerification,
        @NotNull @Min(50) @Max(5000) Integer gpsGeofenceRadiusM,
        @NotNull @Min(1) @Max(500) Integer maxUnverifiedBags,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal blueWasteMinPercent) {
}
