package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Invoice - GST-compliant legal document derived from Bill.
 * 
 * IMMUTABLE: NEVER update or delete.
 * Total amount MUST EXACTLY MATCH bill total.
 * Invoice number follows format: FACILITY_CODE/YYYY-YY/NNNNNN
 */
@Entity
@Table(name = "invoice", indexes = {
        @Index(name = "idx_invoice_facility_fy", columnList = "facility_id, financial_year"),
        @Index(name = "idx_invoice_date", columnList = "invoice_date")
})
public class Invoice {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", unique = true)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // Legacy fields (for backward compatibility during migration)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "financial_year", length = 10)
    private String financialYear;

    // Legacy fields
    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    private Integer beds;

    @Column(name = "per_bed_per_day_rate")
    private BigDecimal perBedPerDayRate;

    @Column(name = "base_amount")
    private BigDecimal baseAmount;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "integrity_hash", length = 64)
    private String integrityHash;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(length = 20)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Constructor
    public Invoice() {
        this.id = UUID.randomUUID();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public Facility getFacility() {
        return facility;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public Integer getBeds() {
        return beds;
    }

    public BigDecimal getPerBedPerDayRate() {
        return perBedPerDayRate;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(UUID id) {
        this.id = id;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public void setBeds(Integer beds) {
        this.beds = beds;
    }

    public void setPerBedPerDayRate(BigDecimal perBedPerDayRate) {
        this.perBedPerDayRate = perBedPerDayRate;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
