package com.smartcbwtf.dto.settings;

import com.smartcbwtf.domain.AgreementNumberResetFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
        @Size(max = 120)
        @Pattern(regexp = "^(|.*\\{\\{sequence\\}\\}.*)$", message = "Agreement number template must include {{sequence}}")
        String agreementNumberTemplate,
        AgreementNumberResetFrequency agreementNumberResetFrequency,
        // Agreement Terms Template
        @Size(max = 20000, message = "Agreement terms template must be 20000 characters or less")
        String agreementTermsTemplate) {
}
