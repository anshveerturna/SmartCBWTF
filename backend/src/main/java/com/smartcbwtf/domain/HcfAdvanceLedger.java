package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * HCF Advance Ledger - tracks overpayments immutably.
 * advance_balance(hcf) = SUM(amount) - NEVER stored as column.
 * Positive = credit from overpayment.
 * Negative = used for future invoice.
 */
@Entity
@Table(name = "hcf_advance_ledger")
public class HcfAdvanceLedger {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hcf_id", nullable = false)
    private Hcf hcf;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_payment_id", nullable = false)
    private Payment sourcePayment;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // positive = credit, negative = used

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false, length = 64)
    private String checksum;

    // Getters and setters
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

    public Payment getSourcePayment() {
        return sourcePayment;
    }

    public void setSourcePayment(Payment sourcePayment) {
        this.sourcePayment = sourcePayment;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
