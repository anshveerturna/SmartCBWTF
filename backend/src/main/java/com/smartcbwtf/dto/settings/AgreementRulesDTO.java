package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Section 4: Agreement & Contract Rules settings.
 */
public record AgreementRulesDTO(
        @NotNull @Min(1) @Max(60) Integer defaultAgreementValidityMonths,
        @NotNull @Min(7) @Max(90) Integer agreementRenewalWindowDays,
        @NotNull Boolean blockOverlappingAgreements) {
}
