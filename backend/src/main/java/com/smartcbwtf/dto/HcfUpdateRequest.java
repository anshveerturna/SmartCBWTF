package com.smartcbwtf.dto;

import com.smartcbwtf.domain.BillingModel;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for updating HCF billing model and related fields.
 * Used by admin before approval.
 */
public record HcfUpdateRequest(
        @NotNull(message = "Billing model is required") BillingModel billingModel,

        Integer numberOfBeds,

        BigDecimal monthlyCharges) {
    /**
     * Validate billing model constraints.
     */
    public void validate() {
        if (billingModel == BillingModel.BEDDED) {
            if (numberOfBeds == null || numberOfBeds <= 0) {
                throw new IllegalArgumentException("BEDDED billing model requires numberOfBeds > 0");
            }
        } else if (billingModel == BillingModel.FIXED_MONTHLY) {
            if (monthlyCharges == null || monthlyCharges.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("FIXED_MONTHLY billing model requires monthlyCharges > 0");
            }
        }
    }
}
