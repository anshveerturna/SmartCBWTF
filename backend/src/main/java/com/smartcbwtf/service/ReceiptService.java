package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Receipt Service - Generates IMMUTABLE payment receipts.
 * 
 * CRITICAL RULES:
 * - Receipt is generated SYNCHRONOUSLY inside payment transaction
 * - If receipt fails → payment fails
 * - Checksum covers payment + allocations
 * - Never regenerate, never edit
 */
@Service
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private final PaymentReceiptRepository receiptRepository;
    private final PaymentReceiptSequenceRepository sequenceRepository;
    private final FacilityRepository facilityRepository;

    public ReceiptService(
            PaymentReceiptRepository receiptRepository,
            PaymentReceiptSequenceRepository sequenceRepository,
            FacilityRepository facilityRepository) {
        this.receiptRepository = receiptRepository;
        this.sequenceRepository = sequenceRepository;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Generate receipt synchronously inside payment transaction.
     * MUST be called within @Transactional context.
     */
    @Transactional
    public PaymentReceipt generateReceipt(Payment payment, List<InvoicePayment> allocations) {
        // Check if receipt already exists
        if (receiptRepository.existsByPaymentId(payment.getId())) {
            throw new IllegalStateException("Receipt already exists for payment: " + payment.getId());
        }

        // 1. Get next receipt number (FY-scoped with lock)
        String receiptNumber = getNextReceiptNumber(payment.getFacility().getId());

        // 2. Calculate checksum BEFORE generating PDF (includes allocations)
        String checksum = calculateChecksum(payment, allocations);

        // 3. Generate PDF
        byte[] pdfBytes;
        try {
            pdfBytes = generatePdf(payment, allocations, receiptNumber, checksum);
        } catch (Exception e) {
            log.error("Failed to generate receipt PDF for payment {}", payment.getId(), e);
            throw new RuntimeException("Receipt generation failed - payment aborted", e);
        }

        // 4. Store immutably
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setPayment(payment);
        receipt.setReceiptNumber(receiptNumber);
        receipt.setPdfBytes(pdfBytes);
        receipt.setChecksum(checksum);

        receipt = receiptRepository.save(receipt);
        log.info("Generated receipt {} for payment {}", receiptNumber, payment.getId());

        return receipt;
    }

    /**
     * Get receipt for a payment (for download).
     */
    public PaymentReceipt getReceipt(UUID paymentId) {
        return receiptRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("No receipt found for payment: " + paymentId));
    }

    /**
     * Get next receipt number (FY-scoped).
     * Uses pessimistic locking to prevent duplicates.
     */
    @Transactional
    public String getNextReceiptNumber(UUID facilityId) {
        String fy = getCurrentFinancialYear();

        var seqOpt = sequenceRepository.findByFacilityIdAndFinancialYearForUpdate(facilityId, fy);
        int nextNumber;

        if (seqOpt.isPresent()) {
            var seq = seqOpt.get();
            nextNumber = seq.getLastNumber() + 1;
            seq.setLastNumber(nextNumber);
            sequenceRepository.save(seq);
        } else {
            // First receipt of FY
            nextNumber = 1;
            PaymentReceiptSequence newSeq = new PaymentReceiptSequence();
            newSeq.setFacilityId(facilityId);
            newSeq.setFinancialYear(fy);
            newSeq.setLastNumber(nextNumber);
            sequenceRepository.save(newSeq);
        }

        return String.format("RCP/%s/%05d", fy, nextNumber);
    }

    /**
     * Get current financial year (e.g., "2024-2025").
     */
    private String getCurrentFinancialYear() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        // FY starts April 1
        if (month >= 4) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    /**
     * Calculate checksum covering payment + allocations.
     * Prevents tampering and reuse.
     */
    private String calculateChecksum(Payment payment, List<InvoicePayment> allocations) {
        try {
            StringBuilder data = new StringBuilder();
            data.append(payment.getId()).append("|");
            data.append(payment.getAmount()).append("|");
            data.append(payment.getPaymentDate()).append("|");
            data.append(payment.getMode()).append("|");
            data.append(payment.getBankAccount() != null ? payment.getBankAccount().getId() : "null").append("|");
            data.append(payment.getHcf().getId()).append("|");

            // Include all allocations
            BigDecimal totalAllocated = BigDecimal.ZERO;
            for (InvoicePayment alloc : allocations) {
                data.append(alloc.getInvoice().getId()).append(":");
                data.append(alloc.getAllocatedAmount()).append(",");
                totalAllocated = totalAllocated.add(alloc.getAllocatedAmount());
            }
            data.append("|total:").append(totalAllocated);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Checksum calculation failed", e);
        }
    }

    /**
     * Generate PDF receipt.
     */
    private byte[] generatePdf(Payment payment, List<InvoicePayment> allocations,
            String receiptNumber, String checksum) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            Facility facility = payment.getFacility();
            Hcf hcf = payment.getHcf();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 780;
                float margin = 50;
                float width = page.getMediaBox().getWidth() - 2 * margin;

                PDType1Font fontBold = PDType1Font.HELVETICA_BOLD;
                PDType1Font fontNormal = PDType1Font.HELVETICA;

                // Header
                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText("PAYMENT RECEIPT");
                cs.endText();
                y -= 30;

                // Receipt number
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Receipt #: " + receiptNumber);
                cs.endText();
                y -= 20;

                // Date
                cs.beginText();
                cs.setFont(fontNormal, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Date: " + payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                cs.endText();
                y -= 30;

                // Facility details
                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("From: " + facility.getName());
                cs.endText();
                y -= 30;

                // HCF details
                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Received From:");
                cs.endText();
                y -= 15;

                cs.beginText();
                cs.setFont(fontNormal, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(hcf.getName());
                cs.endText();
                y -= 30;

                // Payment details
                cs.beginText();
                cs.setFont(fontBold, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Payment Details:");
                cs.endText();
                y -= 15;

                cs.beginText();
                cs.setFont(fontNormal, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Amount: Rs. " + payment.getAmount().setScale(2).toPlainString());
                cs.endText();
                y -= 15;

                cs.beginText();
                cs.setFont(fontNormal, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Mode: " + payment.getMode().name().replace("_", " "));
                cs.endText();
                y -= 15;

                if (payment.getReferenceNumber() != null) {
                    cs.beginText();
                    cs.setFont(fontNormal, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Reference: " + payment.getReferenceNumber());
                    cs.endText();
                    y -= 15;
                }

                if (payment.getBankAccount() != null) {
                    cs.beginText();
                    cs.setFont(fontNormal, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Receiving Bank: " + payment.getBankAccount().getBankName() +
                            " ****" + payment.getBankAccount().getAccountNumber()
                                    .substring(Math.max(0, payment.getBankAccount().getAccountNumber().length() - 4)));
                    cs.endText();
                    y -= 15;
                }
                y -= 15;

                // Allocations
                if (!allocations.isEmpty()) {
                    cs.beginText();
                    cs.setFont(fontBold, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Applied to Invoices:");
                    cs.endText();
                    y -= 15;

                    for (InvoicePayment alloc : allocations) {
                        cs.beginText();
                        cs.setFont(fontNormal, 9);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("  - " + alloc.getInvoice().getInvoiceNumber() +
                                ": Rs. " + alloc.getAllocatedAmount().setScale(2).toPlainString());
                        cs.endText();
                        y -= 12;
                    }
                }
                y -= 30;

                // Checksum footer
                cs.beginText();
                cs.setFont(fontNormal, 7);
                cs.newLineAtOffset(margin, 50);
                cs.showText("Verification: " + checksum.substring(0, 16) + "...");
                cs.endText();

                cs.beginText();
                cs.setFont(fontNormal, 7);
                cs.newLineAtOffset(margin, 40);
                cs.showText("This is a computer-generated receipt. No signature required.");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
