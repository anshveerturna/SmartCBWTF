package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Reversal - links original payment to reversal payment.
 * This is how banks handle chargebacks - no mutation of original payment.
 */
@Entity
@Table(name = "payment_reversal")
public class PaymentReversal {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "original_payment_id", nullable = false, unique = true)
    private Payment originalPayment;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_payment_id", nullable = false)
    private Payment reversalPayment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Payment getOriginalPayment() {
        return originalPayment;
    }

    public void setOriginalPayment(Payment originalPayment) {
        this.originalPayment = originalPayment;
    }

    public Payment getReversalPayment() {
        return reversalPayment;
    }

    public void setReversalPayment(Payment reversalPayment) {
        this.reversalPayment = reversalPayment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
