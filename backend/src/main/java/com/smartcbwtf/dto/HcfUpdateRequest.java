package com.smartcbwtf.dto;

import com.smartcbwtf.domain.BillingModel;
import java.math.BigDecimal;

/**
 * Request DTO for updating HCF details before approval.
 * Allows CBWTF admin to edit all HCF info when status is PENDING or REJECTED.
 */
public record HcfUpdateRequest(
        // Contact Info
        String name,
        String doctorName,
        String contactPhone,
        String contactEmail,

        // Address
        String address,
        String pincode,
        String state,

        // Identity Documents
        String panNo,
        String gstNo,
        String aadharNo,

        // Billing Configuration
        BillingModel billingModel,
        Integer numberOfBeds,
        BigDecimal monthlyCharges,

        // Notes
        String otherNotes) {
    /**
     * Validate billing model constraints.
     */
    public void validate() {
        if (billingModel != null) {
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
}
