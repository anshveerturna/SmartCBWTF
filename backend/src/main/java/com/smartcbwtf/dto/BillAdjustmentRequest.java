package com.smartcbwtf.dto;

import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Request DTO for applying bill adjustment (concession).
 * 
 * Adjustments are always negative (concessions only).
 * Reason is mandatory for audit purposes.
 */
public record BillAdjustmentRequest(
        @NegativeOrZero(message = "Adjustment amount must be zero or negative (concession only)") BigDecimal adjustmentAmount,

        @NotBlank(message = "Adjustment reason is mandatory") String reason) {
}
