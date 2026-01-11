package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.domain.Hcf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfService {

    private final Path baseDir = Paths.get("files");
    private final Path agreementsDir;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // PDFBox 2.x font constants
    private static final PDType1Font FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font FONT_REGULAR = PDType1Font.HELVETICA;

    public PdfService() {
        this.agreementsDir = baseDir.resolve("agreements");
        try {
            Files.createDirectories(baseDir);
            Files.createDirectories(agreementsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to init PDF directory", e);
        }
    }

    public String generateAgreementPdf(Agreement agreement) {
        return generateAgreementPdf(agreement, null, null);
    }

    public String generateAgreementPdf(Agreement agreement, FacilityTemplate template, String templateContent) {
        Facility facility = agreement.getFacility();
        Hcf hcf = agreement.getHcf();

        Path facilityDir = agreementsDir.resolve(facility.getCode());
        try {
            Files.createDirectories(facilityDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create facility directory", e);
        }

        String filename = agreement.getAgreementNumber() + ".pdf";
        Path path = facilityDir.resolve(filename);

        Map<String, String> variables = buildTemplateVariables(agreement, hcf, facility);

        try {
            if (template != null && templateContent != null && "HTML".equals(template.getTemplateType())) {
                String processedHtml = processTemplate(templateContent, variables);
                renderHtmlToPdf(processedHtml, path);
            } else {
                renderAgreementPdf(path, agreement, hcf, facility, variables);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write agreement PDF", e);
        }

        return "/files/agreements/" + facility.getCode() + "/" + filename;
    }

    private Map<String, String> buildTemplateVariables(Agreement agreement, Hcf hcf, Facility facility) {
        Map<String, String> vars = new HashMap<>();

        vars.put("HCF_NAME", nullSafe(hcf.getName()));
        vars.put("HCF_ADDRESS", nullSafe(hcf.getAddress()));
        vars.put("DOCTOR_NAME", nullSafe(hcf.getDoctorName()));
        vars.put("CONTACT_PHONE", nullSafe(hcf.getContactPhone()));
        vars.put("EMAIL", nullSafe(hcf.getContactEmail()));
        vars.put("PAN_NO", nullSafe(hcf.getPanNo()));
        vars.put("GST_NO", nullSafe(hcf.getGstNo()));
        vars.put("AADHAR_NO", nullSafe(hcf.getAadharNo()));
        vars.put("NO_OF_BEDS", hcf.getNumberOfBeds() != null ? String.valueOf(hcf.getNumberOfBeds()) : "N/A");
        vars.put("BEDDED", hcf.getBedded() != null && hcf.getBedded() ? "Yes" : "No");
        vars.put("MONTHLY_CHARGES",
                hcf.getMonthlyCharges() != null ? "Rs. " + hcf.getMonthlyCharges().toString() + "/Month" : "N/A");
        vars.put("PCB_AUTHORIZATION_NO", nullSafe(hcf.getPcbAuthorizationNo(), "N/A"));
        vars.put("OTHER_NOTES", nullSafe(hcf.getOtherNotes(), ""));

        vars.put("AGREEMENT_NUMBER", agreement.getAgreementNumber());
        vars.put("AGREEMENT_DATE", formatDate(LocalDate.now()));
        vars.put("START_DATE", formatDate(agreement.getStartDate()));
        vars.put("END_DATE", agreement.getEndDate() != null ? formatDate(agreement.getEndDate()) : "N/A");

        vars.put("TERMS_ACCEPTED_LINE",
                Boolean.TRUE.equals(agreement.getTermsAccepted()) ? "Terms & Conditions: Accepted"
                        : "Terms & Conditions: Not Accepted");
        vars.put("TERMS_VERSION", nullSafe(agreement.getTermsVersion(), "N/A"));
        vars.put("TERMS_ACCEPTED_AT",
                agreement.getTermsAcceptedAt() != null ? formatInstant(agreement.getTermsAcceptedAt()) : "N/A");

        vars.put("FACILITY_NAME", nullSafe(facility.getName()));
        vars.put("FACILITY_ADDRESS", nullSafe(facility.getAddress()));
        vars.put("FACILITY_CODE", nullSafe(facility.getCode()));
        vars.put("FACILITY_PHONE", nullSafe(facility.getContactPhone()));
        vars.put("FACILITY_EMAIL", nullSafe(facility.getContactEmail()));

        return vars;
    }

    private String processTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private void renderHtmlToPdf(String html, Path outputPath) throws IOException {
        String text = html.replaceAll("<[^>]+>", "\n")
                .replaceAll("&amp;", "&")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\n+", "\n")
                .trim();

        String[] lines = text.split("\n");
        renderSimplePdf(outputPath, "AGREEMENT", lines);
    }

    private void renderAgreementPdf(Path path, Agreement agreement, Hcf hcf, Facility facility,
            Map<String, String> vars) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 780;
                float margin = 50;
                float lineHeight = 14;

                cs.beginText();
                cs.setFont(FONT_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText(facility.getName());
                y -= lineHeight;
                cs.setFont(FONT_REGULAR, 9);
                cs.newLineAtOffset(0, -lineHeight);
                cs.showText(nullSafe(facility.getAddress()));
                cs.endText();

                y -= 30;

                cs.beginText();
                cs.setFont(FONT_BOLD, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText("AGREEMENT FORM");
                cs.endText();

                cs.beginText();
                cs.setFont(FONT_BOLD, 11);
                cs.newLineAtOffset(350, y);
                cs.showText("Agreement No: " + agreement.getAgreementNumber());
                cs.endText();

                y -= 25;

                cs.beginText();
                cs.setFont(FONT_BOLD, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("BIO MEDICAL WASTE COLLECTION & DISPOSAL SERVICES");
                cs.endText();

                y -= 25;

                cs.beginText();
                cs.setFont(FONT_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("HEALTH CARE FACILITY DETAILS");
                cs.endText();

                y -= 5;

                cs.setLineWidth(0.5f);
                cs.addRect(margin - 5, y - 180, 500, 185);
                cs.stroke();

                y -= lineHeight;

                String[][] hcfFields = {
                        { "HCF Name", vars.get("HCF_NAME") },
                        { "HCF Address", vars.get("HCF_ADDRESS") },
                        { "Doctor/Owner Name", vars.get("DOCTOR_NAME") },
                        { "Contact No", vars.get("CONTACT_PHONE") },
                        { "Email", vars.get("EMAIL") },
                        { "PAN No", vars.get("PAN_NO") },
                        { "GST No", vars.get("GST_NO") },
                        { "Aadhar No", vars.get("AADHAR_NO") },
                        { "Monthly Charges", vars.get("MONTHLY_CHARGES") },
                        { "Bedded/Non-Bedded", vars.get("BEDDED") },
                        { "No. of Beds", vars.get("NO_OF_BEDS") },
                        { "PCB Authorization No", vars.get("PCB_AUTHORIZATION_NO") }
                };

                for (String[] field : hcfFields) {
                    cs.beginText();
                    cs.setFont(FONT_BOLD, 9);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(field[0] + ":");
                    cs.setFont(FONT_REGULAR, 9);
                    cs.newLineAtOffset(130, 0);
                    cs.showText(truncate(field[1], 60));
                    cs.endText();
                    y -= lineHeight;
                }

                y -= 20;

                cs.beginText();
                cs.setFont(FONT_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Agreement Period: ");
                cs.setFont(FONT_REGULAR, 10);
                cs.showText(vars.get("START_DATE") + " to " + vars.get("END_DATE"));
                cs.endText();

                y -= 25;

                cs.beginText();
                cs.setFont(FONT_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(vars.get("TERMS_ACCEPTED_LINE"));
                cs.endText();

                y -= lineHeight;

                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText(
                        "Version: " + vars.get("TERMS_VERSION") + " | Accepted at: " + vars.get("TERMS_ACCEPTED_AT"));
                cs.endText();

                y -= 40;

                cs.setLineWidth(0.5f);
                cs.moveTo(margin, y);
                cs.lineTo(margin + 150, y);
                cs.stroke();

                cs.moveTo(350, y);
                cs.lineTo(500, y);
                cs.stroke();

                y -= 15;

                cs.beginText();
                cs.setFont(FONT_BOLD, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("FOR " + facility.getCode());
                cs.newLineAtOffset(300, 0);
                cs.showText("FOR HEALTH CARE FACILITY");
                cs.endText();

                y -= 12;

                cs.beginText();
                cs.setFont(FONT_REGULAR, 8);
                cs.newLineAtOffset(margin, y);
                cs.showText("AUTHORIZED SIGNATORY");
                cs.newLineAtOffset(300, 0);
                cs.showText("AUTHORIZED SIGNATORY");
                cs.endText();

                cs.beginText();
                cs.setFont(FONT_REGULAR, 7);
                cs.newLineAtOffset(margin, 30);
                cs.showText("Generated on: " + formatInstant(Instant.now()) + " | Document ID: " + agreement.getId());
                cs.endText();
            }

            document.save(path.toFile());
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String nullSafe(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    private String formatDate(LocalDate date) {
        return date != null ? DATE_FMT.format(date) : "N/A";
    }

    private String formatInstant(Instant instant) {
        return instant != null ? DATETIME_FMT.format(instant.atZone(ZoneId.of("Asia/Kolkata"))) : "N/A";
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return "";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }

    public String generateInvoicePdf(Invoice invoice) {
        String filename = "invoice-" + invoice.getInvoiceNumber() + ".pdf";
        Path path = baseDir.resolve(filename);
        try {
            renderSimplePdf(path, "Invoice " + invoice.getInvoiceNumber(), new String[] {
                    "HCF: " + invoice.getHcf().getName(),
                    "Facility: " + invoice.getFacility().getName(),
                    "Period: " + DATE_FMT.format(invoice.getPeriodStart()) + " to "
                            + DATE_FMT.format(invoice.getPeriodEnd()),
                    "Beds: " + invoice.getBeds(),
                    "Rate (per bed/day): " + invoice.getPerBedPerDayRate(),
                    "Base: " + invoice.getBaseAmount(),
                    "Tax: " + invoice.getTaxAmount(),
                    "Total: " + invoice.getTotalAmount()
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to write invoice PDF", e);
        }
        return path.toString();
    }

    public String generateLabelBatchPdf(Hcf hcf, Facility facility, String category, String[] qrCodes) {
        String filename = "labels-" + hcf.getCode() + "-" + category + "-" + System.currentTimeMillis() + ".pdf";
        Path path = baseDir.resolve(filename);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 16);
                contentStream.setLeading(16f);
                contentStream.newLineAtOffset(50, 770);
                contentStream.showText("Label Batch - " + hcf.getCode() + " (" + category + ")");
                contentStream.setFont(FONT_REGULAR, 12);
                contentStream.newLine();
                contentStream.showText("HCF: " + hcf.getName());
                contentStream.newLine();
                contentStream.showText("Facility: " + facility.getName());
                for (String qr : qrCodes) {
                    contentStream.newLine();
                    contentStream.showText(qr);
                }
                contentStream.endText();
            }
            document.save(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write label PDF", e);
        }
        return path.toString();
    }

    private void renderSimplePdf(Path path, String title, String[] lines) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 18);
                contentStream.setLeading(16f);
                contentStream.newLineAtOffset(50, 770);
                contentStream.showText(title);
                contentStream.setFont(FONT_REGULAR, 12);
                for (String line : lines) {
                    contentStream.newLine();
                    contentStream.showText(line);
                }
                contentStream.endText();
            }
            document.save(path.toFile());
        }
    }

    public byte[] generateMonthlyCompliancePdf(Hcf hcf, Facility facility, LocalDate month,
            java.util.List<com.smartcbwtf.domain.BagEvent> events) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Calculate Totals for Chart
            Map<String, java.math.BigDecimal> totals = new HashMap<>();
            events.forEach(e -> {
                String cat = (e.getBagLabel() != null) ? e.getBagLabel().getCategory() : "UNKNOWN";
                totals.merge(cat, e.getWeightKg(), java.math.BigDecimal::add);
            });

            float y = 780;
            float margin = 40;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // HEADER
                cs.beginText();
                cs.setFont(FONT_BOLD, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText(facility.getName().toUpperCase());
                cs.endText();
                y -= 20;

                cs.beginText();
                cs.setFont(FONT_REGULAR, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(nullSafe(facility.getAddress()));
                cs.endText();
                y -= 30;

                cs.beginText();
                cs.setFont(FONT_BOLD, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText("MONTHLY BIO-MEDICAL WASTE COMPLIANCE REPORT");
                cs.endText();
                y -= 25;

                cs.setFont(FONT_BOLD, 10);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("HCF: " + hcf.getName() + " (" + hcf.getCode() + ")");
                cs.newLineAtOffset(300, 0);
                cs.showText("Period: " + month.getMonth().toString() + " " + month.getYear());
                cs.endText();
                y -= 50;

                // Chart on Right
                float chartHeight = 150;
                drawBarChart(cs, 300, y - chartHeight + 20, 200, chartHeight, totals);

                // Summary Table on Left
                cs.beginText();
                cs.setFont(FONT_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Waste Summary");
                cs.endText();
                y -= 20;

                float[] sumColWidths = { 100, 100 };
                float rowHeight = 20;
                drawTableOk(cs, margin, y, sumColWidths, rowHeight, FONT_BOLD, 10, "Category", "Total (kg)");
                y -= rowHeight;

                for (Map.Entry<String, java.math.BigDecimal> entry : totals.entrySet()) {
                    drawTableOk(cs, margin, y, sumColWidths, rowHeight, FONT_REGULAR, 10, entry.getKey(),
                            String.format("%.2f", entry.getValue()));
                    y -= rowHeight;
                }
            }

            // DETAILED HISTORY ITEMS
            PDPage detailsPage = new PDPage(PDRectangle.A4);
            document.addPage(detailsPage);

            PDPageContentStream cs = new PDPageContentStream(document, detailsPage);

            try {
                y = 750;
                margin = 40;
                float[] colWidths = { 70, 50, 70, 250, 60 };
                float rowHeight = 18;

                cs.beginText();
                cs.setFont(FONT_BOLD, 12);
                cs.newLineAtOffset(margin, y + 10);
                cs.showText("Detailed Waste Pickup History");
                cs.endText();

                drawTableOk(cs, margin, y, colWidths, rowHeight, FONT_BOLD, 9, "Date", "Time", "Category", "QR Code",
                        "Weight (kg)");
                y -= rowHeight;

                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

                for (com.smartcbwtf.domain.BagEvent e : events) {
                    if (y < 60) {
                        cs.close();
                        detailsPage = new PDPage(PDRectangle.A4);
                        document.addPage(detailsPage);
                        cs = new PDPageContentStream(document, detailsPage);

                        y = 750;
                        margin = 40;
                        drawTableOk(cs, margin, y, colWidths, rowHeight, FONT_BOLD, 9, "Date", "Time", "Category",
                                "QR Code", "Weight (kg)");
                        y -= rowHeight;
                    }

                    String dateStr = formatDate(LocalDate.ofInstant(e.getEventTs(), ZoneId.of("Asia/Kolkata")));
                    String timeStr = e.getEventTs().atZone(ZoneId.of("Asia/Kolkata")).format(timeFmt);
                    String cat = (e.getBagLabel() != null) ? e.getBagLabel().getCategory() : "-";
                    String qr = (e.getBagLabel() != null) ? e.getBagLabel().getQrCode() : "-";
                    String wt = (e.getWeightKg() != null) ? String.valueOf(e.getWeightKg()) : "0";

                    if (qr.length() > 35)
                        qr = qr.substring(0, 32) + "...";

                    drawTableOk(cs, margin, y, colWidths, rowHeight, FONT_REGULAR, 9, dateStr, timeStr, cat, qr, wt);
                    y -= rowHeight;
                }
            } finally {
                cs.close();
            }

            java.io.ByteArrayOutputStream shout = new java.io.ByteArrayOutputStream();
            document.save(shout);
            return shout.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating monthly report PDF", e);
        }
    }

    private void drawBarChart(PDPageContentStream cs, float x, float y, float width, float height,
            Map<String, java.math.BigDecimal> data) throws IOException {
        if (data.isEmpty())
            return;

        cs.setNonStrokingColor(250 / 255f, 250 / 255f, 250 / 255f);
        cs.addRect(x, y, width, height);
        cs.fill();
        cs.setStrokingColor(200 / 255f, 200 / 255f, 200 / 255f);
        cs.addRect(x, y, width, height);
        cs.stroke();

        cs.setStrokingColor(0, 0, 0);

        java.math.BigDecimal maxVal = data.values().stream().max(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ONE);
        if (maxVal.compareTo(java.math.BigDecimal.ZERO) == 0)
            maxVal = java.math.BigDecimal.ONE;

        float barSlotWidth = (width - 20) / data.size();
        float barWidth = Math.min(barSlotWidth - 10, 40);
        float spacing = (barSlotWidth - barWidth) / 2;

        float currentX = x + 10;

        for (Map.Entry<String, java.math.BigDecimal> entry : data.entrySet()) {
            float val = entry.getValue().floatValue();
            float barHeight = (val / maxVal.floatValue()) * (height - 30);

            switch (entry.getKey().toUpperCase()) {
                case "RED":
                    cs.setNonStrokingColor(220 / 255f, 38 / 255f, 38 / 255f);
                    break;
                case "YELLOW":
                    cs.setNonStrokingColor(217 / 255f, 119 / 255f, 6 / 255f);
                    break;
                case "BLUE":
                    cs.setNonStrokingColor(37 / 255f, 99 / 255f, 235 / 255f);
                    break;
                case "WHITE":
                    cs.setNonStrokingColor(255 / 255f, 255 / 255f, 255 / 255f);
                    break;
                default:
                    cs.setNonStrokingColor(107 / 255f, 114 / 255f, 128 / 255f);
                    break;
            }

            cs.addRect(currentX + spacing, y + 20, barWidth, barHeight);
            cs.fill();

            cs.setStrokingColor(50 / 255f, 50 / 255f, 50 / 255f);
            cs.setLineWidth(0.5f);
            cs.addRect(currentX + spacing, y + 20, barWidth, barHeight);
            cs.stroke();

            cs.setNonStrokingColor(0, 0, 0);
            cs.beginText();
            cs.setFont(FONT_REGULAR, 8);
            cs.newLineAtOffset(currentX + spacing, y + 8);
            String label = entry.getKey();
            if (label.length() > 5)
                label = label.substring(0, 3) + ".";
            cs.showText(label);
            cs.endText();

            currentX += barSlotWidth;
        }
    }

    private void drawTableOk(PDPageContentStream cs, float x, float y, float[] colWidths, float height,
            PDFont font, int fontSize, String... values) throws IOException {
        float currentX = x;
        for (int i = 0; i < values.length && i < colWidths.length; i++) {
            cs.setLineWidth(0.5f);
            cs.setStrokingColor(0, 0, 0); // Ensure black border
            cs.addRect(currentX, y - height + 5, colWidths[i], height);
            cs.stroke();

            cs.beginText();
            cs.setFont(font, fontSize); // Explicitly set font here!
            cs.newLineAtOffset(currentX + 4, y - height + 10);
            cs.showText(values[i] != null ? values[i] : "");
            cs.endText();

            currentX += colWidths[i];
        }
    }
}
