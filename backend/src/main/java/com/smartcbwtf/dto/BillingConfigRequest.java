package com.smartcbwtf.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for creating/updating billing configuration.
 */
public class BillingConfigRequest {

    @Min(value = 1, message = "Base grams per bed per day must be at least 1")
    private Integer baseGramsPerBedPerDay = 270;

    @NotNull(message = "Base rate per bed per day is required")
    @DecimalMin(value = "0.01", message = "Base rate must be greater than 0")
    private BigDecimal baseRatePerBedPerDay;

    @NotNull(message = "Excess rate per kg is required")
    @DecimalMin(value = "0.01", message = "Excess rate must be greater than 0")
    private BigDecimal excessRatePerKg;

    // Getters and Setters
    public Integer getBaseGramsPerBedPerDay() {
        return baseGramsPerBedPerDay;
    }

    public void setBaseGramsPerBedPerDay(Integer baseGramsPerBedPerDay) {
        this.baseGramsPerBedPerDay = baseGramsPerBedPerDay;
    }

    public BigDecimal getBaseRatePerBedPerDay() {
        return baseRatePerBedPerDay;
    }

    public void setBaseRatePerBedPerDay(BigDecimal baseRatePerBedPerDay) {
        this.baseRatePerBedPerDay = baseRatePerBedPerDay;
    }

    public BigDecimal getExcessRatePerKg() {
        return excessRatePerKg;
    }

    public void setExcessRatePerKg(BigDecimal excessRatePerKg) {
        this.excessRatePerKg = excessRatePerKg;
    }
}
