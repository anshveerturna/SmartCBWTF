package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for renewing an expired agreement.
 * Creates a NEW agreement with a NEW agreement number.
 */
public class RenewAgreementRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Bed rate is required")
    @Positive(message = "Bed rate must be positive")
    private BigDecimal perBedPerDayRate;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getPerBedPerDayRate() {
        return perBedPerDayRate;
    }

    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) {
        this.perBedPerDayRate = perBedPerDayRate;
    }
}
