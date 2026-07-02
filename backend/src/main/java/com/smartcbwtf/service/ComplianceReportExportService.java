package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.smartcbwtf.domain.AnnualComplianceReport;
import com.smartcbwtf.domain.BarcodeComplianceReport;
import com.smartcbwtf.domain.DailyComplianceReport;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.MonthlyComplianceReport;
import com.smartcbwtf.domain.ViolationReport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComplianceReportExportService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss z").withZone(IST);

    private final ObjectMapper objectMapper;

    public ComplianceReportExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] dailyPdf(DailyComplianceReport report) {
        return render(
                "Daily Compliance Report",
                rows(
                        row("Facility", facilityLabel(report.getFacility())),
                        row("Report date", String.valueOf(report.getReportDate())),
                        row("Status", report.getStatus().name()),
                        row("Data completeness", report.getDataCompleteness().name()),
                        row("Source window", window(report.getSourceWindowFrom(), report.getSourceWindowTo())),
                        row("Generated at", DATETIME_FORMAT.format(report.getGeneratedAt())),
                        row("Checksum", report.getChecksum())),
                report.getDataJson());
    }

    public byte[] monthlyPdf(MonthlyComplianceReport report) {
        return render(
                "Monthly Compliance Report",
                rows(
                        row("Facility", facilityLabel(report.getFacility())),
                        row("Report month", String.valueOf(report.getReportMonth())),
                        row("Status", report.getStatus().name()),
                        row("Data completeness", report.getDataCompleteness().name()),
                        row("Source window", window(report.getSourceWindowFrom(), report.getSourceWindowTo())),
                        row("Generated at", DATETIME_FORMAT.format(report.getGeneratedAt())),
                        row("Checksum", report.getChecksum())),
                report.getDataJson());
    }

    public byte[] annualPdf(AnnualComplianceReport report) {
        return render(
                "Annual Compliance Report - Form IV",
                rows(
                        row("Facility", facilityLabel(report.getFacility())),
                        row("Financial year", report.getFinancialYear()),
                        row("Status", report.getStatus().name()),
                        row("Data completeness", report.getDataCompleteness().name()),
                        row("Source window", window(report.getSourceWindowFrom(), report.getSourceWindowTo())),
                        row("Generated at", DATETIME_FORMAT.format(report.getGeneratedAt())),
                        row("Checksum", report.getChecksum())),
                report.getDataJson());
    }

    public byte[] annualExcel(AnnualComplianceReport report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet metadata = workbook.createSheet("Form IV Metadata");
            int rowNum = 0;
            rowNum = excelKeyValue(metadata, rowNum, "Title", "Annual Compliance Report - Form IV", headerStyle);
            rowNum = excelKeyValue(metadata, rowNum, "Facility", facilityLabel(report.getFacility()), headerStyle);
            rowNum = excelKeyValue(metadata, rowNum, "Financial year", report.getFinancialYear(), headerStyle);
            rowNum = excelKeyValue(metadata, rowNum, "Status", report.getStatus().name(), headerStyle);
            rowNum = excelKeyValue(metadata, rowNum, "Data completeness", report.getDataCompleteness().name(),
                    headerStyle);
            rowNum = excelKeyValue(metadata, rowNum, "Source window",
                    window(report.getSourceWindowFrom(), report.getSourceWindowTo()), headerStyle);
            rowNum = excelKeyValue(metadata, rowNum, "Generated at", DATETIME_FORMAT.format(report.getGeneratedAt()),
                    headerStyle);
            excelKeyValue(metadata, rowNum, "Checksum", report.getChecksum(), headerStyle);
            metadata.autoSizeColumn(0);
            metadata.autoSizeColumn(1);

            Sheet snapshot = workbook.createSheet("Snapshot Data");
            Row header = snapshot.createRow(0);
            setExcelText(header.createCell(0), "Path");
            header.getCell(0).setCellStyle(headerStyle);
            setExcelText(header.createCell(1), "Value");
            header.getCell(1).setCellStyle(headerStyle);
            List<String[]> flattened = flattenedJson(report.getDataJson());
            for (int i = 0; i < flattened.size(); i++) {
                Row row = snapshot.createRow(i + 1);
                setExcelText(row.createCell(0), flattened.get(i)[0]);
                setExcelText(row.createCell(1), flattened.get(i)[1]);
            }
            snapshot.autoSizeColumn(0);
            snapshot.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render annual compliance report Excel", e);
        }
    }

    public byte[] barcodePdf(BarcodeComplianceReport report) {
        return render(
                "Barcode Compliance Report",
                rows(
                        row("Facility", facilityLabel(report.getFacility())),
                        row("Report date", String.valueOf(report.getReportDate())),
                        row("Report type", report.getReportType().name()),
                        row("Status", report.getStatus().name()),
                        row("Data completeness", report.getDataCompleteness().name()),
                        row("Source window", window(report.getSourceWindowFrom(), report.getSourceWindowTo())),
                        row("Generated at", DATETIME_FORMAT.format(report.getGeneratedAt())),
                        row("Checksum", report.getChecksum())),
                report.getDataJson());
    }

    public byte[] violationPdf(ViolationReport report) {
        return render(
                "Violation Report",
                rows(
                        row("Facility", facilityLabel(report.getFacility())),
                        row("Report date", String.valueOf(report.getReportDate())),
                        row("Violation count", String.valueOf(report.getViolationCount())),
                        row("Data completeness", report.getDataCompleteness().name()),
                        row("Source window", window(report.getSourceWindowFrom(), report.getSourceWindowTo())),
                        row("Generated at", DATETIME_FORMAT.format(report.getGeneratedAt())),
                        row("Checksum", report.getChecksum())),
                report.getDataJson());
    }

    private byte[] render(String title, List<String[]> metadataRows, String dataJson) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document, title);
            writer.title(title);
            writer.section("Report Metadata");
            for (String[] row : metadataRows) {
                writer.keyValue(row[0], row[1]);
            }
            writer.section("Immutable Snapshot Data");
            for (String line : prettyJson(dataJson).split("\\R", -1)) {
                writer.monospace(line);
            }
            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render compliance report PDF", e);
        }
    }

    private String prettyJson(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return "{}";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(dataJson));
        } catch (Exception e) {
            return dataJson;
        }
    }

    private int excelKeyValue(Sheet sheet, int rowNum, String key, String value, CellStyle keyStyle) {
        Row row = sheet.createRow(rowNum);
        Cell keyCell = row.createCell(0);
        setExcelText(keyCell, key);
        keyCell.setCellStyle(keyStyle);
        setExcelText(row.createCell(1), value);
        return rowNum + 1;
    }

    private List<String[]> flattenedJson(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return java.util.Collections.singletonList(row("$", "{}"));
        }
        try {
            List<String[]> rows = new ArrayList<>();
            flattenJson("$", objectMapper.readTree(dataJson), rows);
            return rows.isEmpty() ? java.util.Collections.singletonList(row("$", "{}")) : rows;
        } catch (Exception e) {
            return java.util.Collections.singletonList(row("$", dataJson));
        }
    }

    private void flattenJson(String path, JsonNode node, List<String[]> rows) {
        if (node == null || node.isNull()) {
            rows.add(row(path, "null"));
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> flattenJson(path + "." + entry.getKey(), entry.getValue(), rows));
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flattenJson(path + "[" + i + "]", node.get(i), rows);
            }
            return;
        }
        rows.add(row(path, node.isTextual() ? node.asText() : node.toString()));
    }

    private static void setExcelText(Cell cell, String value) {
        cell.setCellValue(spreadsheetSafe(value));
    }

    private static String spreadsheetSafe(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        String trimmedLeading = normalized.stripLeading();
        if (!trimmedLeading.isEmpty() && isSpreadsheetFormulaPrefix(trimmedLeading.charAt(0))) {
            return "'" + normalized;
        }
        return normalized;
    }

    private static boolean isSpreadsheetFormulaPrefix(char value) {
        return value == '=' || value == '+' || value == '-' || value == '@' || value == '\t';
    }

    private static String facilityLabel(Facility facility) {
        if (facility == null) {
            return "Unknown facility";
        }
        String code = facility.getCode() == null ? "" : " (" + facility.getCode() + ")";
        return nullToDash(facility.getName()) + code;
    }

    private static String window(java.time.Instant from, java.time.Instant to) {
        return DATETIME_FORMAT.format(from) + " to " + DATETIME_FORMAT.format(to);
    }

    private static List<String[]> rows(String[]... rows) {
        return List.of(rows);
    }

    private static String[] row(String key, String value) {
        return new String[] { key, nullToDash(value) };
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String pdfSafe(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value
                .replace('\u20b9', 'R')
                .replace('\u2192', '-')
                .replace('\u2022', '-')
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", " ");
        StringBuilder builder = new StringBuilder(cleaned.length());
        cleaned.codePoints().forEach(cp -> builder.append(cp >= 32 && cp <= 126 || cp == '\n' || cp == '\t'
                ? (char) cp
                : '?'));
        return builder.toString();
    }

    private static final class PdfWriter {
        private static final float MARGIN = 42;
        private static final float BOTTOM = 42;
        private static final float WIDTH = PDRectangle.A4.getWidth() - (MARGIN * 2);
        private static final float LEADING = 13;
        private static final PDFont FONT_REGULAR = PDType1Font.HELVETICA;
        private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
        private static final PDFont FONT_MONO = PDType1Font.COURIER;

        private final PDDocument document;
        private final String footerTitle;
        private PDPageContentStream content;
        private float y;
        private int pageNumber;

        private PdfWriter(PDDocument document, String footerTitle) throws IOException {
            this.document = document;
            this.footerTitle = footerTitle;
            newPage();
        }

        private void title(String text) throws IOException {
            writeWrapped(pdfSafe(text), FONT_BOLD, 18, new Color(20, 39, 68), WIDTH);
            y -= 10;
        }

        private void section(String text) throws IOException {
            ensureSpace(34);
            y -= 8;
            writeWrapped(pdfSafe(text), FONT_BOLD, 12, new Color(28, 79, 128), WIDTH);
            y -= 4;
        }

        private void keyValue(String key, String value) throws IOException {
            ensureSpace(24);
            String line = pdfSafe(key) + ": " + pdfSafe(value);
            writeWrapped(line, FONT_REGULAR, 9.5f, Color.DARK_GRAY, WIDTH);
        }

        private void monospace(String text) throws IOException {
            writeWrapped(pdfSafe(text), FONT_MONO, 8.2f, Color.BLACK, WIDTH);
        }

        private void writeWrapped(String text, PDFont font, float size, Color color, float width) throws IOException {
            for (String line : wrap(text, font, size, width)) {
                ensureSpace(LEADING);
                content.beginText();
                content.setFont(font, size);
                content.setNonStrokingColor(color);
                content.newLineAtOffset(MARGIN, y);
                content.showText(line);
                content.endText();
                y -= LEADING;
            }
        }

        private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
            String safe = text == null ? "" : text;
            if (safe.isBlank()) {
                return List.of("");
            }
            List<String> lines = new ArrayList<>();
            for (String sourceLine : safe.split("\\R", -1)) {
                StringBuilder current = new StringBuilder();
                for (String word : sourceLine.split(" ")) {
                    if (font.getStringWidth(word) / 1000 * size > maxWidth) {
                        if (!current.isEmpty()) {
                            lines.add(current.toString());
                            current.setLength(0);
                        }
                        lines.addAll(splitLongWord(word, font, size, maxWidth));
                        continue;
                    }
                    String candidate = current.isEmpty() ? word : current + " " + word;
                    if (font.getStringWidth(candidate) / 1000 * size <= maxWidth) {
                        current.setLength(0);
                        current.append(candidate);
                    } else {
                        if (!current.isEmpty()) {
                            lines.add(current.toString());
                        }
                        current.setLength(0);
                        current.append(word);
                    }
                }
                lines.add(current.toString());
            }
            return lines;
        }

        private List<String> splitLongWord(String word, PDFont font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                String candidate = current.toString() + word.charAt(i);
                if (font.getStringWidth(candidate) / 1000 * size > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(word.charAt(i));
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            closePage();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void close() throws IOException {
            closePage();
        }

        private void closePage() throws IOException {
            if (content == null) {
                return;
            }
            content.setStrokingColor(new Color(210, 216, 224));
            content.moveTo(MARGIN, 34);
            content.lineTo(PDRectangle.A4.getWidth() - MARGIN, 34);
            content.stroke();
            content.beginText();
            content.setFont(FONT_REGULAR, 7.5f);
            content.setNonStrokingColor(Color.GRAY);
            content.newLineAtOffset(MARGIN, 22);
            content.showText(pdfSafe(footerTitle) + " | Page " + pageNumber);
            content.endText();
            content.close();
            content = null;
        }
    }
}
