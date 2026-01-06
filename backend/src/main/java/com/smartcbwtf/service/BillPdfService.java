package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.BillRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Bill PDF Generation Service.
 * Generates OPERATIONAL bills (NOT legal GST invoices).
 * GST invoice generation is handled externally via Tally.
 */
@Service
public class BillPdfService {

    private static final Logger log = LoggerFactory.getLogger(BillPdfService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private static final String DISCLAIMER = "This is a system-generated operational bill. " +
            "GST invoice will be issued separately by the accounting department.";

    private final BillRepository billRepository;

    public BillPdfService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    /**
     * Generate operational bill PDF.
     * 
     * @param billId Bill UUID
     * @return PDF bytes
     */
    public byte[] generatePdf(UUID billId) throws IOException {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        BillingSnapshot snapshot = bill.getSnapshot();
        Facility facility = bill.getFacility();
        Agreement agreement = bill.getAgreement();
        Hcf hcf = agreement != null ? agreement.getHcf() : null;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                float margin = 50;
                float pageWidth = PDRectangle.A4.getWidth();
                float col4 = pageWidth - margin - 50;

                // Header - CBWTF Details
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(margin, y);
                content.showText(facility != null ? facility.getName() : "CBWTF");
                content.endText();
                y -= 20;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(margin, y);
                content.showText(facility != null && facility.getAddress() != null ? facility.getAddress() : "");
                content.endText();
                y -= 30;

                // Title - OPERATIONAL BILL (NOT Invoice)
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(pageWidth / 2 - 100, y);
                content.showText("MONTHLY WASTE MANAGEMENT BILL");
                content.endText();
                y -= 30;

                // Bill Reference (not invoice number)
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(margin, y);
                content.showText("Bill Reference: BILL-" + billId.toString().substring(0, 8).toUpperCase());
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(pageWidth - 150, y);
                content.showText("Date: " + bill.getCreatedAt().toString().substring(0, 10));
                content.endText();
                y -= 25;

                // Bill To Section
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(margin, y);
                content.showText("Bill To:");
                content.endText();
                y -= 15;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(margin, y);
                content.showText(hcf != null ? hcf.getName() : "Healthcare Facility");
                content.endText();
                y -= 12;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(margin, y);
                content.showText(hcf != null && hcf.getAddress() != null ? hcf.getAddress() : "");
                content.endText();
                y -= 30;

                // Billing Period
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(margin, y);
                content.showText("Billing Period: " + bill.getBillingMonth().format(MONTH_FMT));
                content.endText();
                y -= 15;

                // Billing Model
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(margin, y);
                String billingModelDesc = getBillingModelDescription(bill);
                content.showText("Billing Model: " + billingModelDesc);
                content.endText();
                y -= 10;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(margin, y);
                content.showText("Agreement: " + (agreement != null ? agreement.getAgreementNumber() : "N/A"));
                content.endText();
                y -= 25;

                // Line separator
                content.setLineWidth(0.5f);
                content.moveTo(margin, y);
                content.lineTo(pageWidth - margin, y);
                content.stroke();
                y -= 20;

                // Items Table Header
                float col1 = margin;
                float col2 = 300;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                content.newLineAtOffset(col1, y);
                content.showText("Description");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                content.newLineAtOffset(col4, y);
                content.showText("Amount (Rs)");
                content.endText();
                y -= 15;

                // Line 1: Base Service
                int beds = snapshot != null ? snapshot.getBedCount()
                        : (bill.getSnapshotBeds() != null ? bill.getSnapshotBeds() : 0);
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col1, y);
                content.showText("Bio-Medical Waste Collection & Processing (" + beds + " beds)");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col4, y);
                content.showText(formatAmount(bill.getBaseAmount()));
                content.endText();
                y -= 15;

                // Line 2: Excess Waste (if applicable)
                if (bill.getExcessAmount() != null && bill.getExcessAmount().compareTo(BigDecimal.ZERO) > 0) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 9);
                    content.newLineAtOffset(col1, y);
                    content.showText("Excess Waste Charges (" + bill.getExcessWeightKg() + " kg)");
                    content.endText();

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 9);
                    content.newLineAtOffset(col4, y);
                    content.showText(formatAmount(bill.getExcessAmount()));
                    content.endText();
                    y -= 15;
                }

                y -= 10;
                content.moveTo(margin, y);
                content.lineTo(pageWidth - margin, y);
                content.stroke();
                y -= 15;

                // Subtotal
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col2, y);
                content.showText("Subtotal:");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col4, y);
                content.showText(formatAmount(bill.getSubtotal()));
                content.endText();
                y -= 12;

                // GST (informational - shown for accounting reference)
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                content.newLineAtOffset(col2, y);
                content.showText("(GST shown for accounting reference only)");
                content.endText();
                y -= 12;

                // CGST
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col2, y);
                content.showText("CGST @ 9%:");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col4, y);
                content.showText(formatAmount(bill.getCgst()));
                content.endText();
                y -= 12;

                // SGST
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col2, y);
                content.showText("SGST @ 9%:");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col4, y);
                content.showText(formatAmount(bill.getSgst()));
                content.endText();
                y -= 15;

                content.moveTo(margin, y);
                content.lineTo(pageWidth - margin, y);
                content.stroke();
                y -= 15;

                // Total Amount
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(col2, y);
                content.showText("Total Amount:");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(col4, y);
                content.showText("Rs " + formatAmount(bill.getTotalAmount()));
                content.endText();
                y -= 20;

                // Adjustment section (if applicable)
                if (bill.hasAdjustment()) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                    content.newLineAtOffset(col2, y);
                    content.showText("Adjustment (Concession):");
                    content.endText();

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 9);
                    content.newLineAtOffset(col4, y);
                    content.showText("Rs " + formatAmount(bill.getAdjustmentAmount()));
                    content.endText();
                    y -= 12;

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                    content.newLineAtOffset(col2, y);
                    content.showText("Reason: "
                            + (bill.getAdjustmentReason() != null ? truncateString(bill.getAdjustmentReason(), 50)
                                    : "N/A"));
                    content.endText();
                    y -= 15;

                    content.moveTo(margin, y);
                    content.lineTo(pageWidth - margin, y);
                    content.stroke();
                    y -= 15;

                    // Final Payable Amount
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    content.newLineAtOffset(col2, y);
                    content.showText("FINAL PAYABLE:");
                    content.endText();

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    content.newLineAtOffset(col4, y);
                    content.showText("Rs " + formatAmount(bill.getFinalPayableAmount()));
                    content.endText();
                    y -= 20;
                }

                // Disclaimer
                y -= 30;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                content.newLineAtOffset(margin, y);
                content.showText(DISCLAIMER);
                content.endText();
                y -= 20;

                // Integrity hash
                String integrityHash = computeIntegrityHash(billId, bill.getTotalAmount());
                content.beginText();
                content.setFont(PDType1Font.COURIER, 6);
                content.newLineAtOffset(margin, 30);
                content.showText("Bill Integrity: SHA256:" + integrityHash);
                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            log.info("Generated operational bill PDF for bill {}", billId);
            return out.toByteArray();
        }
    }

    private String getBillingModelDescription(Bill bill) {
        String model = bill.getBillingModel();
        if ("BEDDED".equals(model)) {
            return String.format("BEDDED (%d beds x Rs %s/bed)",
                    bill.getSnapshotBeds() != null ? bill.getSnapshotBeds() : 0,
                    bill.getSnapshotRatePerBed() != null ? bill.getSnapshotRatePerBed().toPlainString() : "0");
        } else if ("FIXED_MONTHLY".equals(model)) {
            return String.format("FIXED MONTHLY (Rs %s)",
                    bill.getSnapshotMonthlyCharge() != null ? bill.getSnapshotMonthlyCharge().toPlainString() : "0");
        }
        return model != null ? model : "N/A";
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null)
            return "0.00";
        return String.format("%,.2f", amount);
    }

    private String truncateString(String str, int maxLength) {
        if (str == null)
            return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    private String computeIntegrityHash(UUID billId, BigDecimal total) {
        try {
            String data = billId.toString() + (total != null ? total.toPlainString() : "0");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return "hash_error";
        }
    }
}
