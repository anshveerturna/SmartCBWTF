package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Receipt - IMMUTABLE.
 * One receipt per payment. Never modified after generation.
 */
@Entity
@Table(name = "payment_receipt")
public class PaymentReceipt {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "receipt_number", nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "pdf_bytes", columnDefinition = "bytea")
    private byte[] pdfBytes;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public void setPdfBytes(byte[] pdfBytes) {
        this.pdfBytes = pdfBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
