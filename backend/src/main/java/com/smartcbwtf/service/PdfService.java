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

    /**
     * Generate agreement PDF using facility-specific template if available.
     * Template placeholders are replaced with actual values.
     * 
     * @param agreement The agreement entity
     * @param template  The facility template (optional, uses default if null)
     * @param templateContent The template HTML content (optional)
     * @return Path to the generated PDF
     */
    public String generateAgreementPdf(Agreement agreement, FacilityTemplate template, String templateContent) {
        Facility facility = agreement.getFacility();
        Hcf hcf = agreement.getHcf();
        
        // Create facility-specific subdirectory
        Path facilityDir = agreementsDir.resolve(facility.getCode());
        try {
            Files.createDirectories(facilityDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create facility directory", e);
        }

        String filename = agreement.getAgreementNumber() + ".pdf";
        Path path = facilityDir.resolve(filename);

        // Build template variables
        Map<String, String> variables = buildTemplateVariables(agreement, hcf, facility);

        try {
            if (template != null && templateContent != null && "HTML".equals(template.getTemplateType())) {
                // Use HTML template with variable substitution
                String processedHtml = processTemplate(templateContent, variables);
                renderHtmlToPdf(processedHtml, path);
            } else {
                // Use default PDF generation
                renderAgreementPdf(path, agreement, hcf, facility, variables);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write agreement PDF", e);
        }

        // Return relative URL path
        return "/files/agreements/" + facility.getCode() + "/" + filename;
    }

    /**
     * Build template variables map from agreement, HCF, and facility data.
     */
    private Map<String, String> buildTemplateVariables(Agreement agreement, Hcf hcf, Facility facility) {
        Map<String, String> vars = new HashMap<>();
        
        // HCF fields
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
        vars.put("MONTHLY_CHARGES", hcf.getMonthlyCharges() != null ? 
                "Rs. " + hcf.getMonthlyCharges().toString() + "/Month" : "N/A");
        vars.put("PCB_AUTHORIZATION_NO", nullSafe(hcf.getPcbAuthorizationNo(), "N/A"));
        vars.put("OTHER_NOTES", nullSafe(hcf.getOtherNotes(), ""));
        
        // Agreement fields
        vars.put("AGREEMENT_NUMBER", agreement.getAgreementNumber());
        vars.put("AGREEMENT_DATE", formatDate(LocalDate.now()));
        vars.put("START_DATE", formatDate(agreement.getStartDate()));
        vars.put("END_DATE", agreement.getEndDate() != null ? formatDate(agreement.getEndDate()) : "N/A");
        
        // Terms acceptance
        vars.put("TERMS_ACCEPTED_LINE", Boolean.TRUE.equals(agreement.getTermsAccepted()) ? 
                "Terms & Conditions: Accepted" : "Terms & Conditions: Not Accepted");
        vars.put("TERMS_VERSION", nullSafe(agreement.getTermsVersion(), "N/A"));
        vars.put("TERMS_ACCEPTED_AT", agreement.getTermsAcceptedAt() != null ? 
                formatInstant(agreement.getTermsAcceptedAt()) : "N/A");
        
        // Facility fields
        vars.put("FACILITY_NAME", nullSafe(facility.getName()));
        vars.put("FACILITY_ADDRESS", nullSafe(facility.getAddress()));
        vars.put("FACILITY_CODE", nullSafe(facility.getCode()));
        vars.put("FACILITY_PHONE", nullSafe(facility.getContactPhone()));
        vars.put("FACILITY_EMAIL", nullSafe(facility.getContactEmail()));
        
        return vars;
    }

    /**
     * Process HTML template by replacing {{VARIABLE}} placeholders.
     */
    private String processTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    /**
     * Render HTML to PDF using simple text extraction.
     * For production, consider using OpenHTMLToPDF or Flying Saucer.
     */
    private void renderHtmlToPdf(String html, Path outputPath) throws IOException {
        // Simple HTML to text conversion for now
        // In production, use OpenHTMLToPDF for proper HTML rendering
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

    /**
     * Render agreement PDF with proper formatting matching physical form.
     */
    private void renderAgreementPdf(Path path, Agreement agreement, Hcf hcf, Facility facility, 
                                     Map<String, String> vars) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 780;
                float margin = 50;
                float lineHeight = 14;
                
                // Header
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
                
                // Agreement Number box
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
                
                // Title
                cs.beginText();
                cs.setFont(FONT_BOLD, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("BIO MEDICAL WASTE COLLECTION & DISPOSAL SERVICES");
                cs.endText();
                
                y -= 25;
                
                // HCF Details Section
                cs.beginText();
                cs.setFont(FONT_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("HEALTH CARE FACILITY DETAILS");
                cs.endText();
                
                y -= 5;
                
                // Draw box around HCF details
                cs.setLineWidth(0.5f);
                cs.addRect(margin - 5, y - 180, 500, 185);
                cs.stroke();
                
                y -= lineHeight;
                
                // HCF fields
                String[][] hcfFields = {
                    {"HCF Name", vars.get("HCF_NAME")},
                    {"HCF Address", vars.get("HCF_ADDRESS")},
                    {"Doctor/Owner Name", vars.get("DOCTOR_NAME")},
                    {"Contact No", vars.get("CONTACT_PHONE")},
                    {"Email", vars.get("EMAIL")},
                    {"PAN No", vars.get("PAN_NO")},
                    {"GST No", vars.get("GST_NO")},
                    {"Aadhar No", vars.get("AADHAR_NO")},
                    {"Monthly Charges", vars.get("MONTHLY_CHARGES")},
                    {"Bedded/Non-Bedded", vars.get("BEDDED")},
                    {"No. of Beds", vars.get("NO_OF_BEDS")},
                    {"PCB Authorization No", vars.get("PCB_AUTHORIZATION_NO")}
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
                
                // Agreement dates
                cs.beginText();
                cs.setFont(FONT_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Agreement Period: ");
                cs.setFont(FONT_REGULAR, 10);
                cs.showText(vars.get("START_DATE") + " to " + vars.get("END_DATE"));
                cs.endText();
                
                y -= 25;
                
                // Terms acceptance
                cs.beginText();
                cs.setFont(FONT_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText(vars.get("TERMS_ACCEPTED_LINE"));
                cs.endText();
                
                y -= lineHeight;
                
                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Version: " + vars.get("TERMS_VERSION") + " | Accepted at: " + vars.get("TERMS_ACCEPTED_AT"));
                cs.endText();
                
                y -= 40;
                
                // Signature lines
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
                
                // Footer with generation info
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
        return instant != null ? 
                DATETIME_FMT.format(instant.atZone(ZoneId.of("Asia/Kolkata"))) : "N/A";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
    }

    public String generateInvoicePdf(Invoice invoice) {
        String filename = "invoice-" + invoice.getInvoiceNumber() + ".pdf";
        Path path = baseDir.resolve(filename);
        try {
            renderSimplePdf(path, "Invoice " + invoice.getInvoiceNumber(), new String[]{
                    "HCF: " + invoice.getHcf().getName(),
                    "Facility: " + invoice.getFacility().getName(),
                    "Period: " + DATE_FMT.format(invoice.getPeriodStart()) + " to " + DATE_FMT.format(invoice.getPeriodEnd()),
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
}
