package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * QR Label Order - Tracks requests for QR labels from HCFs.
 * 
 * Two order types:
 * - HCF_SELF: HCF generates on their portal (cheaper)
 * - CBWTF_REQUEST: HCF requests from CBWTF (expensive)
 */
@Entity
@Table(name = "qr_label_order")
public class QrLabelOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id", nullable = false)
    private Hcf hcf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Column(name = "waste_category", nullable = false)
    private String wasteCategory;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private QrOrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QrOrderStatus status = QrOrderStatus.PENDING;

    private String notes;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @Column(name = "fulfilled_by")
    private UUID fulfilledBy;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Enums
    public enum QrOrderType {
        HCF_SELF, // HCF self-generates (cheaper)
        CBWTF_REQUEST // HCF requests from CBWTF (expensive)
    }

    public enum QrOrderStatus {
        PENDING,
        APPROVED,
        FULFILLED,
        REJECTED,
        CANCELLED
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Agreement getAgreement() {
        return agreement;
    }

    public void setAgreement(Agreement agreement) {
        this.agreement = agreement;
    }

    public String getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(String wasteCategory) {
        this.wasteCategory = wasteCategory;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public QrOrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(QrOrderType orderType) {
        this.orderType = orderType;
    }

    public QrOrderStatus getStatus() {
        return status;
    }

    public void setStatus(QrOrderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(Instant fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }

    public UUID getFulfilledBy() {
        return fulfilledBy;
    }

    public void setFulfilledBy(UUID fulfilledBy) {
        this.fulfilledBy = fulfilledBy;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = Instant.now();
        if (requestedAt == null)
            requestedAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
