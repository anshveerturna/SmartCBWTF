package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.domain.Facility;
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
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private final Path baseDir = Paths.get("files");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;

    public PdfService() {
        try {
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to init PDF directory", e);
        }
    }

    public String generateAgreementPdf(Agreement agreement) {
        String filename = "agreement-" + agreement.getAgreementNumber() + ".pdf";
        Path path = baseDir.resolve(filename);
        try {
            renderSimplePdf(path, "Agreement " + agreement.getAgreementNumber(), new String[]{
                    "Facility: " + agreement.getFacility().getName(),
                    "HCF: " + agreement.getHcf().getName(),
                    "Start: " + agreement.getStartDate(),
                    "End: " + agreement.getEndDate(),
                    "Per-bed/day rate: " + agreement.getPerBedPerDayRate()
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to write agreement PDF", e);
        }
        return path.toString();
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
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.setLeading(16f);
                contentStream.newLineAtOffset(50, 770);
                contentStream.showText("Label Batch - " + hcf.getCode() + " (" + category + ")");
                contentStream.setFont(PDType1Font.HELVETICA, 12);
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
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.setLeading(16f);
                contentStream.newLineAtOffset(50, 770);
                contentStream.showText(title);
                contentStream.setFont(PDType1Font.HELVETICA, 12);
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
