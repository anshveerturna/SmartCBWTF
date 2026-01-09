package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consumable Order - HCF places orders for consumables.
 * 
 * Regulatory Note: Consumable ordering is INDEPENDENT of dues status.
 * HCFs can order consumables even with pending dues.
 */
@Entity
@Table(name = "consumable_order", indexes = {
        @Index(name = "idx_consumable_order_hcf", columnList = "hcf_id"),
        @Index(name = "idx_consumable_order_facility", columnList = "facility_id"),
        @Index(name = "idx_consumable_order_status", columnList = "status")
})
public class ConsumableOrder {

    public enum Status {
        PENDING, // Order placed by HCF
        CONFIRMED, // CBWTF confirmed order
        DISPATCHED, // Items shipped
        DELIVERED, // Delivered to HCF
        CANCELLED // Order cancelled
    }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private String status = Status.PENDING.name();

    // Amounts
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 12, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Notes
    @Column(name = "hcf_notes")
    private String hcfNotes;

    @Column(name = "cbwtf_notes")
    private String cbwtfNotes;

    // Lifecycle timestamps
    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt = Instant.now();

    @Column(name = "ordered_by", nullable = false)
    private UUID orderedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsumableOrderItem> items = new ArrayList<>();

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

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Status getStatusEnum() {
        return status != null ? Status.valueOf(status) : null;
    }

    public void setStatusEnum(Status status) {
        this.status = status != null ? status.name() : null;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getHcfNotes() {
        return hcfNotes;
    }

    public void setHcfNotes(String hcfNotes) {
        this.hcfNotes = hcfNotes;
    }

    public String getCbwtfNotes() {
        return cbwtfNotes;
    }

    public void setCbwtfNotes(String cbwtfNotes) {
        this.cbwtfNotes = cbwtfNotes;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(Instant orderedAt) {
        this.orderedAt = orderedAt;
    }

    public UUID getOrderedBy() {
        return orderedBy;
    }

    public void setOrderedBy(UUID orderedBy) {
        this.orderedBy = orderedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(UUID confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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

    public List<ConsumableOrderItem> getItems() {
        return items;
    }

    public void setItems(List<ConsumableOrderItem> items) {
        this.items = items;
    }

    // Business methods
    public void addItem(ConsumableOrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = items.stream()
                .map(ConsumableOrderItem::getLineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.gstAmount = items.stream()
                .map(ConsumableOrderItem::getLineGst)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotal.add(gstAmount);
    }

    public boolean canCancel() {
        return Status.PENDING.name().equals(status) || Status.CONFIRMED.name().equals(status);
    }
}
