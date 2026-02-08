package com.smartcbwtf.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for Section 4: Agreement & Contract Rules settings.
 * Includes agreement number format configuration.
 */
public record AgreementRulesDTO(
        @NotNull @Min(1) @Max(60) Integer defaultAgreementValidityMonths,
        @NotNull @Min(7) @Max(90) Integer agreementRenewalWindowDays,
        @NotNull Boolean blockOverlappingAgreements,
        // Agreement Number Format
        @Size(min = 1, max = 20) String agreementNumberPrefix,
        @Size(min = 1, max = 5) String agreementNumberSeparator,
        @Min(1) @Max(10) Integer agreementNumberSequenceDigits,
        Boolean agreementNumberIncludeFacilityCode,
        Boolean agreementNumberIncludeYear,
        // Agreement Terms Template
        String agreementTermsTemplate) {
}
