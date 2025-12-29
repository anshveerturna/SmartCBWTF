package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.InvoiceRepository;
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
 * Invoice PDF Generation Service.
 * Generates GST-compliant invoices with embedded integrity hashes.
 */
@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillRepository billRepository;
    private final InvoiceRepository invoiceRepository;

    public InvoicePdfService(BillRepository billRepository, InvoiceRepository invoiceRepository) {
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Generate PDF for a bill.
     * 
     * @param billId Bill UUID
     * @return PDF bytes
     */
    public byte[] generatePdf(UUID billId) throws IOException {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        Invoice invoice = invoiceRepository.findByBillId(billId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found for bill"));

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
                y -= 15;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(margin, y);
                content.showText("GSTIN: N/A  |  PAN: N/A");
                content.endText();
                y -= 30;

                // Tax Invoice Title
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(pageWidth / 2 - 40, y);
                content.showText("TAX INVOICE");
                content.endText();
                y -= 30;

                // Invoice Details
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(margin, y);
                content.showText("Invoice No: " + invoice.getInvoiceNumber());
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(pageWidth - 150, y);
                content.showText(
                        "Date: " + (invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FMT) : ""));
                content.endText();
                y -= 15;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(margin, y);
                content.showText(
                        "Financial Year: " + (invoice.getFinancialYear() != null ? invoice.getFinancialYear() : ""));
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
                y -= 12;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(margin, y);
                content.showText("GSTIN: " + (hcf != null && hcf.getGstNo() != null ? hcf.getGstNo() : "N/A"));
                content.endText();
                y -= 30;

                // Billing Period
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 10);
                content.newLineAtOffset(margin, y);
                content.showText(
                        "Billing Period: " + bill.getBillingMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                content.endText();
                y -= 10;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(margin, y);
                content.showText("Agreement: " + (agreement != null ? agreement.getAgreementNumber() : "N/A"));
                content.endText();
                y -= 30;

                // Line separator
                content.setLineWidth(0.5f);
                content.moveTo(margin, y);
                content.lineTo(pageWidth - margin, y);
                content.stroke();
                y -= 20;

                // Items Table Header
                float col1 = margin;
                float col2 = 300;
                float col3 = 400;
                float col4 = pageWidth - margin - 50;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                content.newLineAtOffset(col1, y);
                content.showText("Description");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                content.newLineAtOffset(col2, y);
                content.showText("SAC Code");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 9);
                content.newLineAtOffset(col4, y);
                content.showText("Amount (\u20B9)");
                content.endText();
                y -= 15;

                // Line 1: Base Service
                int beds = snapshot != null ? snapshot.getBedCount() : 0;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col1, y);
                content.showText("Bio-Medical Waste Collection & Processing (" + beds + " beds)");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col2, y);
                content.showText("998539");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(col4, y);
                content.showText(formatAmount(bill.getBaseAmount()));
                content.endText();
                y -= 15;

                // Line 2: Excess Waste
                if (bill.getExcessAmount().compareTo(BigDecimal.ZERO) > 0) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 9);
                    content.newLineAtOffset(col1, y);
                    content.showText("Excess Waste Charges (" + bill.getExcessWeightKg() + " kg)");
                    content.endText();

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 9);
                    content.newLineAtOffset(col2, y);
                    content.showText("998539");
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

                // Total
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                content.newLineAtOffset(col2, y);
                content.showText("TOTAL PAYABLE:");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                content.newLineAtOffset(col4, y);
                content.showText("\u20B9 " + formatAmount(bill.getTotalAmount()));
                content.endText();
                y -= 50;

                // Declaration
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 8);
                content.newLineAtOffset(margin, y);
                content.showText(
                        "Declaration: This is a computer-generated invoice and does not require a physical signature.");
                content.endText();
                y -= 12;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 8);
                content.newLineAtOffset(margin, y);
                content.showText("Amount in words: " + amountInWords(bill.getTotalAmount()) + " Only");
                content.endText();
                y -= 40;

                // Integrity hash
                String integrityHash = computeIntegrityHash(billId, invoice.getInvoiceNumber(), bill.getTotalAmount());
                content.beginText();
                content.setFont(PDType1Font.COURIER, 6);
                content.newLineAtOffset(margin, 30);
                content.showText("Integrity: SHA256:" + integrityHash);
                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            log.info("Generated PDF for bill {} invoice {}", billId, invoice.getInvoiceNumber());
            return out.toByteArray();
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null)
            return "0.00";
        return String.format("%,.2f", amount);
    }

    private String computeIntegrityHash(UUID billId, String invoiceNumber, BigDecimal total) {
        try {
            String data = billId.toString() + invoiceNumber + total.toPlainString();
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

    private String amountInWords(BigDecimal amount) {
        long rupees = amount.longValue();
        if (rupees < 1000)
            return "Rupees " + rupees;
        if (rupees < 100000)
            return "Rupees " + (rupees / 1000) + " Thousand " + (rupees % 1000);
        return "Rupees " + (rupees / 100000) + " Lakh " + ((rupees % 100000) / 1000) + " Thousand";
    }
}
