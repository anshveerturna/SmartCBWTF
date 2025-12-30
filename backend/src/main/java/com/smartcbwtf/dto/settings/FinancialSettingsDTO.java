package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for Section 2: Financial & Billing settings.
 */
public record FinancialSettingsDTO(
        @NotNull @DecimalMin("0.00") @DecimalMax("28.00") BigDecimal cgstPercent,
        @NotNull @DecimalMin("0.00") @DecimalMax("28.00") BigDecimal sgstPercent,
        @NotNull @DecimalMin("0.00") @DecimalMax("28.00") BigDecimal igstPercent,
        @NotNull Boolean gstEnabled) {
}
