package com.smartcbwtf.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HcfApprovalRequest {
    @NotNull
    private java.util.UUID facilityId;

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @FutureOrPresent
    private LocalDate endDate;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal perBedPerDayRate;

    public java.util.UUID getFacilityId() { return facilityId; }
    public void setFacilityId(java.util.UUID facilityId) { this.facilityId = facilityId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getPerBedPerDayRate() { return perBedPerDayRate; }
    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) { this.perBedPerDayRate = perBedPerDayRate; }
}
