package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public class InvoiceGenerateRequest {
    @NotNull
    private UUID hcfId;
    @NotNull
    private LocalDate periodStart;
    @NotNull
    private LocalDate periodEnd;
    private Double taxRate; // optional e.g., 0.18

    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
}
