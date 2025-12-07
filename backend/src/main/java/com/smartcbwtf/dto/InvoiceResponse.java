package com.smartcbwtf.dto;

import com.smartcbwtf.domain.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class InvoiceResponse {
    private UUID id;
    private String invoiceNumber;
    private UUID hcfId;
    private UUID facilityId;
    private UUID agreementId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer beds;
    private BigDecimal perBedPerDayRate;
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String status;
    private String pdfUrl;

    public InvoiceResponse(Invoice invoice) {
        this.id = invoice.getId();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.hcfId = invoice.getHcf().getId();
        this.facilityId = invoice.getFacility().getId();
        this.agreementId = invoice.getAgreement().getId();
        this.periodStart = invoice.getPeriodStart();
        this.periodEnd = invoice.getPeriodEnd();
        this.beds = invoice.getBeds();
        this.perBedPerDayRate = invoice.getPerBedPerDayRate();
        this.baseAmount = invoice.getBaseAmount();
        this.taxAmount = invoice.getTaxAmount();
        this.totalAmount = invoice.getTotalAmount();
        this.status = invoice.getStatus();
        this.pdfUrl = invoice.getPdfUrl();
    }

    public UUID getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public UUID getHcfId() { return hcfId; }
    public UUID getFacilityId() { return facilityId; }
    public UUID getAgreementId() { return agreementId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public Integer getBeds() { return beds; }
    public BigDecimal getPerBedPerDayRate() { return perBedPerDayRate; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public String getPdfUrl() { return pdfUrl; }
}
