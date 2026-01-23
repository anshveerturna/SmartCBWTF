package com.smartcbwtf.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for CBWTF Admin to approve a pending HCF.
 * 
 * This is a simplified version used by CbwtfHcfController.
 * - facilityId comes from TenantContext (not from request body)
 * - startDate is set to LocalDate.now() in the service
 * - endDate is set to 1 year from start in the service
 */
public class CbwtfHcfApprovalRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal perBedPerDayRate;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal excessRatePerKg;

    public BigDecimal getPerBedPerDayRate() {
        return perBedPerDayRate;
    }

    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) {
        this.perBedPerDayRate = perBedPerDayRate;
    }

    public BigDecimal getExcessRatePerKg() {
        return excessRatePerKg;
    }

    public void setExcessRatePerKg(BigDecimal excessRatePerKg) {
        this.excessRatePerKg = excessRatePerKg;
    }
}
