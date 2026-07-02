package com.smartcbwtf.service;

import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.Invoice;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilityBranding;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private final Path baseDir = Paths.get("files");
    private final Path agreementsDir;
    private final FacilitySettingsRepository facilitySettingsRepository;

    // Formatting Constants
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Layout Constants
    private static final float MARGIN_LEFT = 40;
    private static final float MARGIN_RIGHT = 40;
    private static final float MARGIN_TOP = 40;
    private static final float MARGIN_BOTTOM = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
    private static final float PRINT_LETTERHEAD_RESERVE = 92;
    private static final float AGREEMENT_BODY_BOTTOM = 92;
    private static final String UPDATED_AGREEMENT_TERMS = """
            TERMS OF PAYMENT:
            1. All payments shall be made in advance for the month. In case payments are not received within the month, service may be suspended and the HCF shall be responsible for non-compliance of BMW rules and regulations. Service shall resume only after payment, and charges shall apply from the date of stoppage/resumption as applicable.
            2. No cash payment shall be made to any company staff or to any personal account. If any such payment is made, the HCF shall be solely responsible for that transaction. Payment shall be made only in the company account by cheque, NEFT, RTGS, IMPS, UPI, or other approved online mode.
            3. Cheque bouncing charges of Rs. 500 shall be payable for each dishonoured cheque.
            4. GST on BMW services shall be charged extra as per applicable government rules.
            5. HCFs whose monthly billing charges are Rs. 2,500 or less may be billed quarterly and must pay within 7 days of billing.
            6. If the HCF wants to stop the service or switch to another service provider, all outstanding dues must be cleared first. Otherwise, dues may be recovered by legal action. All disputes shall be subject to Rudrapur jurisdiction.

            RESPONSIBILITIES OF THE SERVICE PROVIDER:
            7. The Service Provider shall comply with the provisions stipulated in Schedule-I of the Bio-medical Waste Management Rules, 2016, as amended from time to time.
            8. The Service Provider shall collect segregated bio-medical waste from one designated waste collection point within the premises of the Waste Generator on alternate days, except Sundays and national/government holidays.
            9. The Service Provider shall schedule the timings for collecting waste in consultation with the Waste Generator.
            10. The Service Provider shall transport segregated waste in a closed container vehicle to its treatment facility.
            11. The Service Provider shall not be held liable for violations made by the Waste Generator or its staff under the Bio-medical Waste Management Rules, 2016, as amended from time to time.

            RESPONSIBILITIES OF THE WASTE GENERATOR:
            12. The Waste Generator shall segregate bio-medical waste at the point of generation in accordance with the Bio-medical Waste Management Rules, 2016, as amended from time to time.
            13. The Waste Generator shall collect, pack, label, and hand over segregated BMW in non-chlorinated bags as stipulated under the Bio-medical Waste Management Rules, 2016, as amended from time to time, arranged by the Waste Generator at its own cost.
            14. The Waste Generator shall keep BMW under lock and key to protect it from mishandling before handover to the authorised person of the Service Provider.
            15. The Waste Generator shall disinfect and mutilate sharps and hand them over in sealed puncture-proof containers to the Service Provider.
            16. The Waste Generator shall take all necessary steps to ensure waste is handled without causing adverse effect to human health and environment.
            17. The Waste Generator shall establish a common secured waste collection point within its premises for collection and storage of BMW before handover to the Service Provider.
            18. The Waste Generator shall designate a Nodal Officer to interact with the Service Provider.
            19. The Waste Generator shall apply for and obtain necessary authorisation from the Prescribed Authority under the Bio-medical Waste Management Rules, 2016, as amended from time to time, or submit necessary returns to the Prescribed Authority from time to time as laid down in the said Rules.
            """;

    // Fonts
    private static final PDType1Font FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font FONT_REGULAR = PDType1Font.HELVETICA;
    private static final PDType1Font FONT_MONO = PDType1Font.COURIER;

    // Colors (SmartCBWTF Green Theme)
    private static final Color COL_PRIMARY = new Color(22, 163, 74); // SmartCBWTF Green
    private static final Color COL_ACCENT = new Color(21, 128, 61); // Dark Green Accent
    private static final Color COL_GREY_LIGHT = new Color(245, 245, 245);
    private static final Color COL_GREY_HEADER = new Color(230, 230, 230);
    private static final Color COL_DARK_TEXT = Color.BLACK;
    private static final Color COL_LIGHT_TEXT = new Color(100, 100, 100);
    private static final Color COL_AGREEMENT_BORDER = new Color(176, 202, 184);
    private static final Color COL_AGREEMENT_RULE = new Color(196, 216, 202);

    public PdfService(FacilitySettingsRepository facilitySettingsRepository) {
        this.facilitySettingsRepository = facilitySettingsRepository;
        this.agreementsDir = baseDir.resolve("agreements");
        try {
            Files.createDirectories(baseDir);
            Files.createDirectories(agreementsDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to init PDF directory", e);
        }
    }

    public Path generatedFilePath(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/files/")) {
            throw new IllegalArgumentException("Unexpected generated file URL");
        }

        String relative = publicUrl.substring("/files/".length());
        if (relative.isBlank() || relative.startsWith("/") || relative.contains("\\") || relative.contains("..")) {
            throw new IllegalArgumentException("Invalid generated file path");
        }

        Path root = baseDir.toAbsolutePath().normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid generated file path");
        }
        return resolved;
    }

    public Path storedGeneratedFilePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank() || storedPath.contains("\\")) {
            throw new IllegalArgumentException("Invalid generated file path");
        }
        if (storedPath.startsWith("/files/")) {
            return generatedFilePath(storedPath);
        }

        Path root = baseDir.toAbsolutePath().normalize();
        Path raw = Paths.get(storedPath);
        Path resolved;
        if (raw.isAbsolute()) {
            resolved = raw.normalize();
        } else {
            String relative = storedPath.startsWith("files/") ? storedPath.substring("files/".length()) : storedPath;
            if (relative.isBlank() || relative.startsWith("/") || relative.contains("..")) {
                throw new IllegalArgumentException("Invalid generated file path");
            }
            resolved = root.resolve(relative).normalize();
        }

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid generated file path");
        }
        return resolved;
    }

    public static String safeDownloadToken(String value) {
        return safeFileToken(value);
    }

    static String safeFileToken(String value) {
        String cleaned = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", "_");
        }
        cleaned = cleaned.replaceAll("^[._-]+", "").replaceAll("[._-]+$", "");
        if (cleaned.isBlank()) {
            return "unknown";
        }
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    public byte[] generateMonthlyCompliancePdf(Agreement agreement, LocalDate month, List<BagEvent> events) {
        try (PDDocument document = new PDDocument()) {

            // --- DATA PREPARATION ---
            Facility facility = agreement.getFacility();
            Hcf hcf = agreement.getHcf();
            String periodStr = month.getMonth().toString() + " " + month.getYear();

            // Calculate Summaries
            Map<String, java.math.BigDecimal> categoryTotals = new HashMap<>();
            Map<String, Integer> categoryCounts = new HashMap<>();

            for (BagEvent e : events) {
                String rawCategory = e.getBagLabel() != null ? e.getBagLabel().getCategory() : null;
                String cat = (rawCategory == null || rawCategory.isBlank())
                        ? "UNKNOWN"
                        : rawCategory.toUpperCase(Locale.ROOT);
                java.math.BigDecimal weight = e.getWeightKg() != null
                        ? e.getWeightKg()
                        : java.math.BigDecimal.ZERO;
                categoryTotals.merge(cat, weight, java.math.BigDecimal::add);
                categoryCounts.merge(cat, 1, Integer::sum);
            }

            java.math.BigDecimal grandTotal = categoryTotals.values().stream()
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            // --- PAGE 1: SUMMARY & CHART ---
            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            try (PDPageContentStream cs = new PDPageContentStream(document, page1)) {
                // Draw Header & Footer
                // Draw Header & Footer
                drawCommonHeader(cs, document, "MONTHLY COMPLIANCE REPORT", "Period: " + periodStr);
                drawCommonFooter(cs, document);

                float y = PAGE_HEIGHT - MARGIN_TOP - 70; // Start below header

                // 1. Facility Information Block
                y = drawFacilityInfoBlock(cs, y, facility, hcf, agreement);

                y -= 30;

                // 2. Waste Summary Table
                y = drawWasteSummaryTable(cs, y, categoryTotals, categoryCounts, grandTotal);

                y -= 40; // Spacing

                // 3. Bar Chart
                drawProfessionalBarChart(cs, MARGIN_LEFT, y - 200, CONTENT_WIDTH, 200, categoryTotals);
                y -= 220;

                // 4. Traceability Statement (if room, otherwise move to next page - unlikely to
                // overflow Page 1)
                drawTraceabilityStatement(cs, MARGIN_LEFT, y);
            }

            // --- PAGE 2+: DETAILED LOG ---
            // If we have events, start detailed log
            if (!events.isEmpty()) {
                drawDetailedLogPages(document, events, periodStr);
            }

            // Fix Footer Page Numbers (1 of N)
            // PDFBox doesn't support re-editing variable page numbers easily without
            // a second pass or knowing total pages upfront.
            // For simplicity in this v1, we draw "Page X" correctly by counting as we add
            // pages.
            // (Passed logic in drawDetailedLogPages)

            java.io.ByteArrayOutputStream shout = new java.io.ByteArrayOutputStream();
            document.save(shout);
            return shout.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generating compliance PDF", e);
        }
    }

    // ============================================================================================
    // SECTIONS & COMPONENTS
    // ============================================================================================

    private void drawCommonHeader(PDPageContentStream cs, PDDocument doc, String title, String subTitle)
            throws IOException {
        float y = PAGE_HEIGHT - MARGIN_TOP;

        // Logo
        try {
            // Try loading from classpath resource
            byte[] logoBytes = null;
            try (java.io.InputStream is = getClass().getResourceAsStream("/smartcbwtf_logo.png")) {
                if (is != null) {
                    logoBytes = is.readAllBytes();
                }
            }
            // If not found in classpath (e.g. IDE run without rebuild), try local file as
            // fallback
            if (logoBytes == null) {
                Path localPath = Paths.get("src/main/resources/smartcbwtf_logo.png");
                if (Files.exists(localPath)) {
                    logoBytes = Files.readAllBytes(localPath);
                }
            }

            if (logoBytes != null) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(doc, logoBytes, "logo");
                float imgW = logo.getWidth();
                float imgH = logo.getHeight();
                float scale = 1.0f;

                // Target height 50 (larger)
                float targetH = 50;
                scale = targetH / imgH;

                // Also check width constraint (e.g. don't exceed 150)
                if (imgW * scale > 150) {
                    scale = 150 / imgW;
                }

                float finalW = imgW * scale;
                float finalH = imgH * scale;
                cs.drawImage(logo, MARGIN_LEFT, y + 5 - (finalH / 2), finalW, finalH);
            }
        } catch (Exception e) {
            // Logo load failed
        }

        // Title Left (Adjusted for Logo)
        cs.beginText();
        cs.setFont(FONT_BOLD, 14);
        cs.setNonStrokingColor(COL_PRIMARY);
        cs.newLineAtOffset(MARGIN_LEFT + 65, y + 10);
        cs.showText("SmartCBWTF");
        cs.endText();

        cs.beginText();
        cs.setFont(FONT_REGULAR, 9);
        cs.setNonStrokingColor(COL_LIGHT_TEXT);
        cs.newLineAtOffset(MARGIN_LEFT + 65, y - 2);
        cs.showText("Bio-Medical Waste Compliance Platform");
        cs.endText();

        // Right Side
        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        cs.setNonStrokingColor(COL_DARK_TEXT);
        float rTitleW = FONT_BOLD.getStringWidth(title) / 1000 * 12;
        cs.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - rTitleW, y);
        cs.showText(title);
        cs.endText();

        if (subTitle != null && !subTitle.isEmpty()) {
            cs.beginText();
            cs.setFont(FONT_REGULAR, 10);
            float pTextW = FONT_REGULAR.getStringWidth(subTitle) / 1000 * 10;
            cs.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - pTextW, y - 12);
            cs.showText(subTitle);
            cs.endText();
        }

        // Divider
        y -= 25;
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(Color.LIGHT_GRAY);
        cs.moveTo(MARGIN_LEFT, y);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y);
        cs.stroke();
    }

    private void drawCommonFooter(PDPageContentStream cs, PDDocument doc)
            throws IOException {
        float y = MARGIN_BOTTOM / 2 + 10;

        cs.setLineWidth(0.5f);
        cs.setStrokingColor(Color.LIGHT_GRAY);
        cs.moveTo(MARGIN_LEFT, y + 10);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y + 10);
        cs.stroke();

        cs.beginText();
        cs.setFont(FONT_REGULAR, 8);
        cs.setNonStrokingColor(COL_LIGHT_TEXT);
        cs.newLineAtOffset(MARGIN_LEFT, y);
        cs.showText("Generated by SmartCBWTF System");
        cs.endText();

        // Page Number
        String pageStr = "Page " + (doc.getNumberOfPages());

        cs.beginText();
        cs.newLineAtOffset(PAGE_WIDTH / 2 - 20, y);
        cs.showText(pageStr);
        cs.endText();

        String dateStr = DATETIME_FMT.format(Instant.now().atZone(ZoneId.of("Asia/Kolkata")));
        float dateW = FONT_REGULAR.getStringWidth(dateStr) / 1000 * 8;
        cs.beginText();
        cs.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - dateW, y);
        cs.showText(dateStr);
        cs.endText();
    }

    private float drawFacilityInfoBlock(PDPageContentStream cs, float y, Facility facility, Hcf hcf,
            Agreement agreement) throws IOException {
        float halfWidth = CONTENT_WIDTH / 2 - 10;
        FacilitySettings settings = loadFacilitySettings(facility);

        // --- COL 1: CBWTF ---
        cs.setNonStrokingColor(COL_GREY_LIGHT);
        cs.addRect(MARGIN_LEFT, y - 100, halfWidth, 100); // Background
        cs.fill();

        float cx = MARGIN_LEFT + 10;
        float cy = y - 15;

        drawText(cs, FONT_BOLD, 10, COL_PRIMARY, cx, cy, "CBWTF DETAILS");
        cy -= 15;
        drawText(cs, FONT_BOLD, 11, COL_DARK_TEXT, cx, cy,
                firstNonBlank(settings != null ? settings.getLegalName() : null, facility.getName()));
        cy -= 12;
        drawText(cs, FONT_REGULAR, 9, COL_DARK_TEXT, cx, cy,
                truncate(firstNonBlank(settings != null ? settings.getRegisteredAddress() : null, facility.getAddress()), 45));
        cy -= 12;
        drawText(cs, FONT_REGULAR, 9, COL_LIGHT_TEXT, cx, cy,
                "Auth No: " + firstNonBlank(settings != null ? settings.getAuthorizationNumber() : null, "N/A"));
        cy -= 12;
        drawText(cs, FONT_REGULAR, 9, COL_LIGHT_TEXT, cx, cy,
                "Contact: " + firstNonBlank(settings != null ? settings.getOfficialPhone() : null,
                        facility.getContactPhone(), "N/A"));

        // --- COL 2: HCF ---
        float hcfX = MARGIN_LEFT + halfWidth + 20;
        cy = y - 15;

        cs.setNonStrokingColor(COL_GREY_LIGHT);
        cs.addRect(hcfX - 10, y - 100, halfWidth, 100);
        cs.fill();

        drawText(cs, FONT_BOLD, 10, COL_PRIMARY, hcfX, cy, "HCF DETAILS");
        cy -= 15;
        drawText(cs, FONT_BOLD, 11, COL_DARK_TEXT, hcfX, cy, nullSafe(hcf.getName()));
        cy -= 12;
        drawText(cs, FONT_REGULAR, 9, COL_DARK_TEXT, hcfX, cy,
                "Agreement No: " + nullSafe(agreement.getAgreementNumber()));
        cy -= 12;
        drawText(cs, FONT_REGULAR, 9, COL_DARK_TEXT, hcfX, cy, truncate(nullSafe(hcf.getAddress()), 45));
        cy -= 12;

        String beds = (hcf.getNumberOfBeds() != null) ? hcf.getNumberOfBeds().toString() : "N/A";
        drawText(cs, FONT_REGULAR, 9, COL_LIGHT_TEXT, hcfX, cy,
                "Beds: " + beds + " (" + (hcf.getBedded() ? "Bedded" : "Non-Bedded") + ")");

        return y - 110;
    }

    private float drawWasteSummaryTable(PDPageContentStream cs, float y, Map<String, java.math.BigDecimal> totals,
            Map<String, Integer> counts, java.math.BigDecimal grandTotal) throws IOException {

        drawText(cs, FONT_BOLD, 12, COL_PRIMARY, MARGIN_LEFT, y, "Waste Collection Summary");
        y -= 15;

        // Table Header
        float[] cols = { 100, 100, 100, 100 }; // Category, Weight, Pickups, % Share
        String[] headers = { "Category", "Quantity (kg)", "No. of Pickups", "Share (%)" };

        drawTableRow(cs, MARGIN_LEFT, y, 20, cols, headers, true, COL_GREY_HEADER);
        y -= 20;

        // Rows
        String[] cats = { "RED", "YELLOW", "BLUE", "WHITE" };
        boolean alt = false;

        for (String cat : cats) {
            java.math.BigDecimal wt = totals.getOrDefault(cat, java.math.BigDecimal.ZERO);
            int count = counts.getOrDefault(cat, 0);
            double share = (grandTotal.compareTo(java.math.BigDecimal.ZERO) > 0)
                    ? (wt.doubleValue() / grandTotal.doubleValue()) * 100.0
                    : 0.0;

            String[] rowData = {
                    cat,
                    String.format("%.2f", wt),
                    String.valueOf(count),
                    String.format("%.1f%%", share)
            };

            drawTableRow(cs, MARGIN_LEFT, y, 20, cols, rowData, false, alt ? COL_GREY_LIGHT : Color.WHITE);
            y -= 20;
            alt = !alt;
        }

        // Total Row
        drawTableRow(cs, MARGIN_LEFT, y, 20, cols,
                new String[] { "TOTAL", String.format("%.2f", grandTotal),
                        String.valueOf(counts.values().stream().mapToInt(Integer::intValue).sum()), "100%" },
                true, COL_GREY_LIGHT);
        y -= 20;

        return y;
    }

    private void drawProfessionalBarChart(PDPageContentStream cs, float x, float y, float w, float h,
            Map<String, java.math.BigDecimal> totals) throws IOException {
        // Background
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setStrokingColor(Color.LIGHT_GRAY);
        cs.addRect(x, y, w, h);
        cs.stroke();

        drawText(cs, FONT_BOLD, 10, COL_DARK_TEXT, x + 10, y + h - 15, "Category-wise Collection (kg)");

        // Axes
        float startX = x + 40;
        float startY = y + 30;
        float endX = x + w - 20;
        float endY = y + h - 30;

        cs.setStrokingColor(Color.GRAY);
        cs.setLineWidth(1f);
        cs.moveTo(startX, startY);
        cs.lineTo(startX, endY); // Y Axis
        cs.stroke();
        cs.moveTo(startX, startY);
        cs.lineTo(endX, startY); // X Axis
        cs.stroke();

        // Y-Axis Labels (Dynamic scale)
        java.math.BigDecimal max = totals.values().stream().max(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.TEN);
        if (max.compareTo(java.math.BigDecimal.ZERO) == 0)
            max = java.math.BigDecimal.TEN;
        double maxVal = max.doubleValue() * 1.2; // 20% headroom

        // Draw 4 grid lines
        cs.setStrokingColor(new Color(220, 220, 220));
        cs.setLineWidth(0.5f);
        cs.setFont(FONT_REGULAR, 8);
        cs.setNonStrokingColor(COL_LIGHT_TEXT);

        for (int i = 0; i <= 4; i++) {
            float lineY = startY + ((endY - startY) * i / 4.0f);
            double val = maxVal * i / 4.0;

            if (i > 0) {
                cs.moveTo(startX, lineY);
                cs.lineTo(endX, lineY);
                cs.stroke();
            }

            String label = String.format("%.0f", val);
            float lw = FONT_REGULAR.getStringWidth(label) / 1000 * 8;
            drawText(cs, FONT_REGULAR, 8, COL_LIGHT_TEXT, startX - lw - 5, lineY - 3, label);
        }

        // Bars
        String[] cats = { "RED", "YELLOW", "BLUE", "WHITE" };
        float slotW = (endX - startX) / 4.0f;
        float barW = Math.min(slotW * 0.6f, 50);
        float pad = (slotW - barW) / 2.0f;

        float currX = startX;

        for (String cat : cats) {
            double v = totals.getOrDefault(cat, java.math.BigDecimal.ZERO).doubleValue();
            float bh = (float) ((v / maxVal) * (endY - startY));

            // Color
            switch (cat) {
                case "RED":
                    cs.setNonStrokingColor(new Color(220, 38, 38));
                    break;
                case "YELLOW":
                    cs.setNonStrokingColor(new Color(245, 158, 11));
                    break;
                case "BLUE":
                    cs.setNonStrokingColor(new Color(37, 99, 235));
                    break;
                case "WHITE":
                    cs.setNonStrokingColor(new Color(255, 255, 255));
                    break;
            }

            cs.addRect(currX + pad, startY, barW, bh);
            cs.fill();
            cs.setStrokingColor(Color.DARK_GRAY);
            cs.setLineWidth(0.5f);
            cs.addRect(currX + pad, startY, barW, bh);
            cs.stroke();

            // X-Label
            float nameW = FONT_BOLD.getStringWidth(cat) / 1000 * 9;
            drawText(cs, FONT_BOLD, 9, COL_DARK_TEXT, currX + pad + (barW - nameW) / 2, startY - 12, cat);

            currX += slotW;
        }
    }

    private void drawTraceabilityStatement(PDPageContentStream cs, float x, float y) throws IOException {
        cs.setNonStrokingColor(COL_GREY_LIGHT);
        cs.addRect(x, y - 40, CONTENT_WIDTH, 40);
        cs.fill();

        cs.setStrokingColor(COL_PRIMARY);
        cs.setLineWidth(2f); // Accent line left
        cs.moveTo(x, y - 40);
        cs.lineTo(x, y);
        cs.stroke();

        drawText(cs, FONT_BOLD, 9, COL_DARK_TEXT, x + 10, y - 15, "QR Code Traceability Statement");
        drawText(cs, FONT_REGULAR, 8, COL_LIGHT_TEXT, x + 10, y - 28,
                "This report is generated using QR-code based waste tracking. Each pickup entry corresponds to a unique QR label scanned at collection.");
    }

    private void drawDetailedLogPages(PDDocument doc, List<BagEvent> events, String period) throws IOException {
        // Table Config
        float yStart = PAGE_HEIGHT - MARGIN_TOP - 60; // Leave room for header
        float[] cols = { 60, 40, 50, 200, 50, 50, 50 }; // Date, Time, Cat, QR, Wt, Route, Vehicle
        String[] headers = { "Date", "Time", "Category", "QR Code ID", "Wt(kg)", "Route", "Vehicle" };

        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);

        drawCommonHeader(cs, doc, "MONTHLY COMPLIANCE REPORT", "Period: " + period);
        drawCommonFooter(cs, doc);

        drawText(cs, FONT_BOLD, 12, COL_PRIMARY, MARGIN_LEFT, yStart + 20, "Detailed Waste Pickup Log");

        float y = yStart;
        drawTableRow(cs, MARGIN_LEFT, y, 20, cols, headers, true, COL_GREY_HEADER);
        y -= 20;

        for (BagEvent e : events) {
            // New Page needed
            if (y < MARGIN_BOTTOM + 40) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);

                drawCommonHeader(cs, doc, "MONTHLY COMPLIANCE REPORT", "Period: " + period);
                drawCommonFooter(cs, doc);

                y = yStart; // Reset Y
                drawTableRow(cs, MARGIN_LEFT, y, 20, cols, headers, true, COL_GREY_HEADER);
                y -= 20;
            }

            String date = e.getEventTs() != null
                    ? DATE_FMT.format(e.getEventTs().atZone(ZoneId.of("Asia/Kolkata")))
                    : "N/A";
            String time = e.getEventTs() != null
                    ? TIME_FMT.format(e.getEventTs().atZone(ZoneId.of("Asia/Kolkata")))
                    : "N/A";
            String cat = e.getBagLabel() != null && e.getBagLabel().getCategory() != null
                    ? e.getBagLabel().getCategory()
                    : "-";
            String qr = e.getBagLabel() != null && e.getBagLabel().getQrCode() != null
                    ? e.getBagLabel().getQrCode()
                    : "-";
            String wt = String.valueOf(e.getWeightKg() != null ? e.getWeightKg() : java.math.BigDecimal.ZERO);
            String route = "R-1";
            String veh = "V-01";

            // Determine Color
            Color rowBg = Color.WHITE;
            if (cat != null) {
                switch (cat.toUpperCase()) {
                    case "RED":
                        rowBg = new Color(254, 226, 226);
                        break; // Light Red
                    case "YELLOW":
                        rowBg = new Color(254, 243, 199);
                        break; // Light Yellow
                    case "BLUE":
                        rowBg = new Color(219, 234, 254);
                        break; // Light Blue
                    case "WHITE":
                        rowBg = new Color(249, 250, 251);
                        break; // Very Light Grey (effectively white)
                }
            }

            // Draw Row without drawing borders for everything, or with borders?
            // drawTableRow handles rect fill + text.
            String[] rowData = { date, time, cat, qr, wt, route, veh };

            // Use custom draw with monospace for QR?
            // We can modify drawTableRow's loop or just do it inline.
            // Inline is safer for specific font needs (QR code).

            cs.setNonStrokingColor(rowBg);
            cs.addRect(MARGIN_LEFT, y - 20 + 5, CONTENT_WIDTH, 20);
            cs.fill();

            float currX = MARGIN_LEFT;
            drawCell(cs, currX, y, cols[0], date, FONT_REGULAR, 8);
            currX += cols[0];
            drawCell(cs, currX, y, cols[1], time, FONT_REGULAR, 8);
            currX += cols[1];
            drawCell(cs, currX, y, cols[2], cat, FONT_BOLD, 8);
            currX += cols[2];
            drawCell(cs, currX, y, cols[3], qr, FONT_MONO, 8);
            currX += cols[3];
            drawCell(cs, currX, y, cols[4], wt, FONT_BOLD, 8);
            currX += cols[4];
            drawCell(cs, currX, y, cols[5], route, FONT_REGULAR, 8);
            currX += cols[5];
            drawCell(cs, currX, y, cols[6], veh, FONT_REGULAR, 8);

            y -= 20;
        }

        // Regulatory Disclaimer at bottom of last page
        y -= 30;
        if (y < MARGIN_BOTTOM + 50) {
            cs.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            drawCommonHeader(cs, doc, "MONTHLY COMPLIANCE REPORT", "Period: " + period);
            drawCommonFooter(cs, doc);
            y = PAGE_HEIGHT - MARGIN_TOP - 50;
        }

        cs.setNonStrokingColor(Color.LIGHT_GRAY);
        cs.moveTo(MARGIN_LEFT, y + 10);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y + 10);
        cs.stroke();

        drawText(cs, FONT_BOLD, 8, COL_DARK_TEXT, MARGIN_LEFT, y, "REGULATORY DISCLAIMER:");
        drawText(cs, FONT_REGULAR, 7, COL_LIGHT_TEXT, MARGIN_LEFT, y - 10,
                "This document is a system-generated compliance report for operational reference. Formal submissions must use CPCB formats.");
        drawText(cs, FONT_REGULAR, 7, COL_LIGHT_TEXT, MARGIN_LEFT, y - 20,
                "Report generated by SmartCBWTF based on QR-code verified collection data.");

        cs.close();
    }

    // --- Helpers ---

    private void drawTableRow(PDPageContentStream cs, float x, float y, float h, float[] cols, String[] data,
            boolean isHeader, Color bg) throws IOException {
        cs.setNonStrokingColor(bg);
        float totalW = 0;
        for (float w : cols)
            totalW += w;
        if (totalW < CONTENT_WIDTH)
            totalW = CONTENT_WIDTH; // Stretch last col?

        cs.addRect(x, y - h + 5, totalW, h);
        cs.fill();

        float currX = x;
        for (int i = 0; i < data.length; i++) {
            if (i >= cols.length)
                break;
            drawCell(cs, currX, y, cols[i], data[i], isHeader ? FONT_BOLD : FONT_REGULAR, isHeader ? 9 : 9);
            currX += cols[i];
        }
    }

    private void drawCell(PDPageContentStream cs, float x, float y, float w, String text, PDFont font, int size)
            throws IOException {
        drawText(cs, font, size, COL_DARK_TEXT, x + 4, y - 10, truncate(text, (int) (w / 4)));
    }

    private void drawText(PDPageContentStream cs, PDFont font, float size, Color color, float x, float y, String text)
            throws IOException {
        if (text == null)
            return;
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private String nullSafe(String val) {
        return val == null ? "" : val;
    }

    private String nullSafe(String val, String def) {
        return val == null ? def : val;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private FacilitySettings loadFacilitySettings(Facility facility) {
        if (facilitySettingsRepository == null || facility == null || facility.getId() == null) {
            return null;
        }
        return facilitySettingsRepository.findById(facility.getId()).orElse(null);
    }

    private String truncate(String val, int len) {
        if (val == null)
            return "";
        if (val.length() > len)
            return val.substring(0, len - 2) + "..";
        return val;
    }

    // ============================================================================================
    // AGREEMENT PDF GENERATION (Professional Enterprise-Grade)
    // ============================================================================================

    public String generateAgreementPdf(Agreement agreement) {
        return generateAgreementPdf(agreement, null, null, null, null);
    }

    public String generateAgreementPdf(Agreement agreement, FacilityTemplate template, String templateContent) {
        return generateAgreementPdf(agreement, template, templateContent, null, null);
    }

    /**
     * Generate a professional agreement PDF with CBWTF branding, full details, and
     * terms & conditions.
     * 
     * @return relative path to the generated PDF file
     */
    public String generateAgreementPdf(Agreement agreement, FacilityTemplate template, String templateContent,
            FacilityBranding branding, FacilitySettings settings) {
        Facility facility = agreement.getFacility();
        Hcf hcf = agreement.getHcf();

        String safeAgreementNumber = safeFileToken(agreement.getAgreementNumber());

        Path facilityDir = agreementsDir.resolve(safeFileToken(facility.getCode()));
        try {
            Files.createDirectories(facilityDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create facility directory", e);
        }

        String filename = safeAgreementNumber + ".pdf";
        Path path = facilityDir.resolve(filename);

        try {
            renderProfessionalAgreementPdf(path, agreement, hcf, facility, branding, settings, false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write agreement PDF", e);
        }

        return path.toAbsolutePath().toString();
    }

    /**
     * Generate a printable (letterhead) agreement PDF — no header/footer branding,
     * but includes declaration and signature blocks.
     */
    public String generatePrintableAgreementPdf(Agreement agreement, FacilityBranding branding,
            FacilitySettings settings) {
        Facility facility = agreement.getFacility();
        Hcf hcf = agreement.getHcf();

        String safeAgreementNumber = safeFileToken(agreement.getAgreementNumber());

        Path facilityDir = agreementsDir.resolve(safeFileToken(facility.getCode()));
        try {
            Files.createDirectories(facilityDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create facility directory", e);
        }

        String filename = safeAgreementNumber + "_print.pdf";
        Path path = facilityDir.resolve(filename);

        try {
            renderProfessionalAgreementPdf(path, agreement, hcf, facility, branding, settings, true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write printable agreement PDF", e);
        }

        return path.toAbsolutePath().toString();
    }

    /**
     * Renders a full professional agreement PDF document.
     * Multi-page capable with proper sections, branding, and T&C.
     */
    private void renderProfessionalAgreementPdf(Path path, Agreement agreement, Hcf hcf, Facility facility,
            FacilityBranding branding, FacilitySettings settings, boolean printMode) throws IOException {

        try (PDDocument document = new PDDocument()) {
            float margin = MARGIN_LEFT;
            float contentWidth = CONTENT_WIDTH;
            float pageTop = PAGE_HEIGHT - MARGIN_TOP;

            // ========= PAGE 1 =========
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(document, page);

            float y = pageTop;

            // === HEADER (Logo + Facility Info + Compact QR) ===
            if (!printMode) {
                y = drawAgreementHeader(cs, document, y, facility, branding, settings, agreement);
            } else {
                // In print mode, leave whitespace for pre-printed company letterhead.
                y -= PRINT_LETTERHEAD_RESERVE;
            }

            // === DOCUMENT TITLE ===
            y -= 16;
            String titleText = "AGREEMENT FOR BIO-MEDICAL WASTE MANAGEMENT SERVICES";
            float tW = FONT_BOLD.getStringWidth(titleText) / 1000 * 13;
            drawText(cs, FONT_BOLD, 13, COL_DARK_TEXT, margin + (contentWidth - tW) / 2, y, titleText);
            y -= 18;

            // === Reference line with decorative rules ===
            String refText = "Agreement No: " + nullSafe(agreement.getAgreementNumber());
            float refW = FONT_REGULAR.getStringWidth(refText) / 1000 * 9;
            float refX = margin + (contentWidth - refW) / 2;
            cs.setStrokingColor(new Color(200, 220, 200));
            cs.setLineWidth(0.5f);
            if (refX - margin > 50) {
                cs.moveTo(margin + 30, y + 4);
                cs.lineTo(refX - 10, y + 4);
                cs.stroke();
            }
            drawText(cs, FONT_REGULAR, 9, COL_ACCENT, refX, y, refText);
            if (PAGE_WIDTH - MARGIN_RIGHT - refX - refW > 50) {
                cs.moveTo(refX + refW + 10, y + 4);
                cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT - 30, y + 4);
                cs.stroke();
            }
            y -= 18;

            // === PARTY DETAILS (two professional cards side by side) ===
            y = drawPartyDetailsCards(cs, y, facility, hcf, settings);
            y -= 14;

            // === AGREEMENT DETAILS (styled card with 2-column layout) ===
            {
                float cardPadX = 10;
                float cardPadTop = 16;
                float cardRowH = 13;

                // Logic for Billing Model & Rate Display
                String billingModel = hcf.getBillingModel() != null ? hcf.getBillingModel().name() : "BEDDED";
                if (Boolean.FALSE.equals(hcf.getBedded())) {
                    billingModel = "FIXED (Non-Bedded)";
                } else if (Boolean.TRUE.equals(hcf.getBedded()) && hcf.getMonthlyCharges() != null
                        && hcf.getMonthlyCharges().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    billingModel = "FIXED (Bedded)";
                }

                String rateStr;
                // PRIORITIZE MONTHLY CHARGE DISPLAY IF PRESENT
                if (hcf.getMonthlyCharges() != null
                        && hcf.getMonthlyCharges().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    java.math.BigDecimal displayMonthly = hcf.getMonthlyCharges();
                    String occupancySuffix = "";
                    // Apply occupancy discount if set
                    if (hcf.getOccupancy() != null && hcf.getOccupancy() > 0) {
                        displayMonthly = displayMonthly.multiply(
                                java.math.BigDecimal.valueOf(hcf.getOccupancy() / 100.0))
                                .setScale(0, java.math.RoundingMode.HALF_UP);
                        occupancySuffix = " (Occupancy: " + String.format("%.0f%%", hcf.getOccupancy()) + ")";
                    }
                    double taxRate = hcf.getTaxRate() != null ? hcf.getTaxRate() : 5.0;
                    rateStr = "Rs. " + displayMonthly.toPlainString() + " /Month + "
                            + String.format("%.0f%%", taxRate) + " GST" + occupancySuffix;
                } else if (hcf.getBillingModel() == com.smartcbwtf.domain.BillingModel.FIXED_MONTHLY
                        || Boolean.FALSE.equals(hcf.getBedded())) {
                    String monthly = hcf.getMonthlyCharges() != null ? hcf.getMonthlyCharges().toString() : "0";
                    double taxRate = hcf.getTaxRate() != null ? hcf.getTaxRate() : 5.0;
                    rateStr = "Rs. " + monthly + " /Month + " + String.format("%.0f%%", taxRate) + " GST";
                } else {
                    rateStr = agreement.getPerBedPerDayRate() != null
                            ? "Rs. " + agreement.getPerBedPerDayRate().toPlainString() + " /bed/day"
                            : "N/A";
                }

                String validUntil = agreement.getEndDate() != null ? formatDate(agreement.getEndDate())
                        : "Until Terminated";

                // 2-column layout: 3 rows
                String[][] leftCol = {
                        { "Agreement No", nullSafe(agreement.getAgreementNumber()) },
                        { "Valid Until", validUntil },
                        { "Rate", rateStr },
                };
                String[][] rightCol = {
                        { "Effective From", formatDate(agreement.getStartDate()) },
                        { "Billing Model", billingModel },
                };

                int maxDetailRows = Math.max(leftCol.length, rightCol.length);
                float detailCardH = cardPadTop + maxDetailRows * cardRowH + 6;

                // Card background + accent bar + border
                cs.setNonStrokingColor(new Color(252, 253, 252));
                cs.addRect(margin, y - detailCardH, contentWidth, detailCardH);
                cs.fill();
                cs.setNonStrokingColor(COL_PRIMARY);
                cs.addRect(margin, y - 2, contentWidth, 2);
                cs.fill();
                cs.setStrokingColor(COL_AGREEMENT_BORDER);
                cs.setLineWidth(0.65f);
                cs.addRect(margin, y - detailCardH, contentWidth, detailCardH);
                cs.stroke();

                // Card header
                drawText(cs, FONT_BOLD, 9, COL_PRIMARY, margin + cardPadX, y - 13, "AGREEMENT DETAILS");
                cs.setStrokingColor(COL_AGREEMENT_RULE);
                cs.setLineWidth(0.45f);
                cs.moveTo(margin + 6, y - cardPadTop);
                cs.lineTo(margin + contentWidth - 6, y - cardPadTop);
                cs.stroke();

                // Render 2 columns
                float halfW = contentWidth / 2;
                float lblW = 82;
                float dY = y - cardPadTop - 10;
                for (int i = 0; i < maxDetailRows; i++) {
                    if (i < leftCol.length) {
                        drawText(cs, FONT_BOLD, 7.5f, COL_DARK_TEXT, margin + cardPadX, dY, leftCol[i][0]);
                        // Wrap rate string if it's long
                        String val = nullSafe(leftCol[i][1]);
                        if (val.length() > 35) {
                            drawText(cs, FONT_REGULAR, 6.8f, COL_DARK_TEXT, margin + cardPadX + lblW, dY, val);
                        } else {
                            drawText(cs, FONT_REGULAR, 7.5f, COL_DARK_TEXT, margin + cardPadX + lblW, dY, val);
                        }
                    }
                    if (i < rightCol.length) {
                        drawText(cs, FONT_BOLD, 7.5f, COL_DARK_TEXT, margin + halfW + cardPadX, dY, rightCol[i][0]);
                        drawText(cs, FONT_REGULAR, 7.5f, COL_DARK_TEXT, margin + halfW + cardPadX + lblW, dY,
                                truncate(nullSafe(rightCol[i][1]), 35));
                    }
                    dY -= cardRowH;
                }

                y -= detailCardH + 14;
            }

            // === TERMS & CONDITIONS (vector text, paginated for print quality) ===
            float termsBottomY = printMode ? 188 : 132;
            y = renderAgreementTermsSinglePage(cs, y, termsBottomY, agreement, settings, printMode);

            // === UPI PAYMENT QR + BANK DETAILS (below T&C) ===
            {
                float targetY = printMode ? 166 : 126;
                y = y > targetY ? targetY : y - 4;

                try {
                    byte[] qrBytes = null;
                    // Try loading QR from settings URL (uploaded file) first
                    String qrUrlFromSettings = settings != null ? settings.getPaymentQrUrl() : null;
                    if (qrUrlFromSettings != null && !qrUrlFromSettings.isBlank()) {
                        qrBytes = readUploadedAsset(qrUrlFromSettings, "/uploads/payment-qr/");
                    }
                    // Fallback: try classpath resource
                    if (qrBytes == null) {
                        try (java.io.InputStream is = getClass().getResourceAsStream("/upi_payment_qr.jpg")) {
                            if (is != null)
                                qrBytes = is.readAllBytes();
                        }
                    }
                    PDImageXObject qrImg = qrBytes != null
                            ? PDImageXObject.createFromByteArray(document, qrBytes, "upi-qr")
                            : null;
                    float qrSize = printMode ? 44 : 56;
                    float qrX = PAGE_WIDTH - MARGIN_RIGHT - qrSize;

                    if (qrImg != null) {
                        float qrY = y - qrSize;
                        cs.drawImage(qrImg, qrX, qrY, qrSize, qrSize);
                        float upiFont = printMode ? 6.8f : 7.4f;
                        String upiLabel = "UPI PAYMENT";
                        float upiLabelW = FONT_BOLD.getStringWidth(upiLabel) / 1000 * upiFont;
                        drawText(cs, FONT_BOLD, upiFont, COL_PRIMARY, qrX + (qrSize - upiLabelW) / 2, y + 4,
                                upiLabel);
                    }

                    // Bank details to the left of QR — use settings if available, else defaults
                    String bankAccName = (settings != null && settings.getBankAccountName() != null
                            && !settings.getBankAccountName().isBlank())
                                    ? settings.getBankAccountName()
                                    : facility.getName();
                    String bankAccNo = (settings != null && settings.getBankAccountNumber() != null
                            && !settings.getBankAccountNumber().isBlank())
                                    ? settings.getBankAccountNumber()
                                    : "505105010010646";
                    String bankIfsc = (settings != null && settings.getBankIfsc() != null
                            && !settings.getBankIfsc().isBlank())
                                    ? settings.getBankIfsc()
                                    : "UBIN0816914";
                    String bankNameStr = (settings != null && settings.getBankName() != null
                            && !settings.getBankName().isBlank())
                                    ? settings.getBankName()
                                    : "Union Bank of India";
                    String bankBranchStr = (settings != null && settings.getBankBranch() != null
                            && !settings.getBankBranch().isBlank())
                                    ? settings.getBankBranch()
                                    : "Rudrapur";

                    float bankX = margin;
                    float bankY = y - 2;
                    float bankFont = printMode ? 6.4f : 6.8f;
                    float bankLineH = printMode ? 8.2f : 9.2f;
                    drawText(cs, FONT_BOLD, printMode ? 7.4f : 8, COL_PRIMARY, bankX, bankY, "BANK DETAILS");
                    bankY -= bankLineH + 1.5f;
                    drawText(cs, FONT_REGULAR, bankFont, COL_DARK_TEXT, bankX, bankY,
                            "Account Name: " + bankAccName);
                    bankY -= bankLineH;
                    drawText(cs, FONT_REGULAR, bankFont, COL_DARK_TEXT, bankX, bankY, "Account No: " + bankAccNo);
                    bankY -= bankLineH;
                    drawText(cs, FONT_REGULAR, bankFont, COL_DARK_TEXT, bankX, bankY, "IFSC Code: " + bankIfsc);
                    bankY -= bankLineH;
                    drawText(cs, FONT_REGULAR, bankFont, COL_DARK_TEXT, bankX, bankY, "Bank: " + bankNameStr);
                    bankY -= bankLineH;
                    drawText(cs, FONT_REGULAR, bankFont, COL_DARK_TEXT, bankX, bankY, "Branch: " + bankBranchStr);

                    y = Math.min(y - qrSize - 8, bankY - 13);
                } catch (Exception ignored) {
                    // UPI QR render failed, continue
                }
            }

            // === DECLARATION + SIGNATURES (print mode only) ===
            if (printMode) {
                y -= 5;
                // Declaration text
                String decl = "I/We hereby declare that the information furnished above is true and correct "
                        + "to the best of my/our knowledge and belief. I/We agree to abide by the terms "
                        + "and conditions mentioned herein.";
                java.util.List<String> declLines = wordWrap(decl, FONT_REGULAR, 7.3f, CONTENT_WIDTH);
                for (String dl : declLines) {
                    drawText(cs, FONT_REGULAR, 7.3f, COL_DARK_TEXT, margin, y, dl);
                    y -= 8.6f;
                }

                y -= 6;

                // Signature blocks — two columns
                float sigColW = CONTENT_WIDTH / 2 - 10;
                float leftX = margin;
                float rightX = margin + sigColW + 20;

                // Left signature: Service Provider
                drawText(cs, FONT_BOLD, 7.6f, COL_DARK_TEXT, leftX, y, "For " + facility.getName());
                y -= 22;
                cs.setStrokingColor(COL_DARK_TEXT);
                cs.setLineWidth(0.5f);
                cs.moveTo(leftX, y);
                cs.lineTo(leftX + sigColW - 20, y);
                cs.stroke();
                drawText(cs, FONT_REGULAR, 6.5f, COL_LIGHT_TEXT, leftX, y - 7, "Authorised Signatory");

                // Right signature: Healthcare Facility
                float sigY = y + 22;
                drawText(cs, FONT_BOLD, 7.6f, COL_DARK_TEXT, rightX, sigY, "For Healthcare Facility");
                cs.moveTo(rightX, y);
                cs.lineTo(rightX + sigColW - 20, y);
                cs.stroke();
                drawText(cs, FONT_REGULAR, 6.5f, COL_LIGHT_TEXT, rightX, y - 7, "Authorised Signatory");

                drawAgreementVerificationQr(document, cs, agreement, PAGE_WIDTH - MARGIN_RIGHT - 30, 18, 30);
            }

            // Footer on the single page
            if (!printMode) {
                drawAgreementFooter(cs, document);
            }
            cs.close();

            document.save(path.toFile());
        }

    }

    private static class AgreementRenderCursor {
        private PDPageContentStream cs;
        private float y;

        AgreementRenderCursor(PDPageContentStream cs, float y) {
            this.cs = cs;
            this.y = y;
        }
    }

    private static class AgreementTermLine {
        private final String text;
        private final boolean heading;
        private final String number;
        private final String body;

        AgreementTermLine(String text, boolean heading, String number, String body) {
            this.text = text;
            this.heading = heading;
            this.number = number;
            this.body = body;
        }

        boolean numbered() {
            return number != null && !number.isBlank();
        }
    }

    private static class AgreementTermBlock {
        private final AgreementTermLine source;
        private final java.util.List<String> wrapped;
        private final float height;

        AgreementTermBlock(AgreementTermLine source, java.util.List<String> wrapped, float height) {
            this.source = source;
            this.wrapped = wrapped;
            this.height = height;
        }
    }

    private float renderAgreementTermsSinglePage(PDPageContentStream cs, float y, float bottomY, Agreement agreement,
            FacilitySettings settings, boolean printMode) throws IOException {
        String termsText = resolveAgreementTermsText(agreement, settings);

        y = drawAgreementSectionHeader(cs, y, "TERMS & CONDITIONS");

        float gap = 8;
        float colW = (CONTENT_WIDTH - gap - 16) / 2;
        float leftX = MARGIN_LEFT + 8;
        float rightX = leftX + colW + gap;
        float topY = y;
        float availableH = Math.max(120, topY - bottomY);

        float selectedFont = printMode ? 6.3f : 6.3f;
        float selectedLineH = selectedFont * 1.18f;
        java.util.List<AgreementTermBlock> selectedBlocks = java.util.List.of();

        for (float fontSize = printMode ? 7.35f : 7.45f; fontSize >= 5.2f; fontSize -= 0.1f) {
            float lineH = fontSize * 1.14f;
            java.util.List<AgreementTermBlock> blocks = buildTermBlocks(termsText, fontSize, lineH, colW);
            float totalHeight = 0;
            for (AgreementTermBlock block : blocks) {
                totalHeight += block.height;
            }
            if (totalHeight <= (availableH * 2) - 16) {
                selectedFont = fontSize;
                selectedLineH = lineH;
                selectedBlocks = blocks;
                break;
            }
            selectedBlocks = blocks;
        }

        float termsCardTop = topY + 7;
        float termsCardBottom = bottomY - 4;
        cs.setNonStrokingColor(new Color(252, 254, 252));
        cs.addRect(MARGIN_LEFT, termsCardBottom, CONTENT_WIDTH, termsCardTop - termsCardBottom);
        cs.fill();
        cs.setStrokingColor(COL_AGREEMENT_BORDER);
        cs.setLineWidth(0.65f);
        cs.addRect(MARGIN_LEFT, termsCardBottom, CONTENT_WIDTH, termsCardTop - termsCardBottom);
        cs.stroke();
        cs.setStrokingColor(COL_AGREEMENT_RULE);
        cs.setLineWidth(0.5f);
        cs.moveTo(leftX + colW + gap / 2, topY + 1);
        cs.lineTo(leftX + colW + gap / 2, bottomY);
        cs.stroke();

        drawAgreementWatermark(cs, MARGIN_LEFT + 38, bottomY + 82);

        int splitIndex = findBalancedTermsSplit(selectedBlocks);
        float leftEnd = renderTermsColumn(cs, selectedBlocks.subList(0, splitIndex), leftX, topY, colW,
                selectedFont, selectedLineH);
        float rightEnd = renderTermsColumn(cs, selectedBlocks.subList(splitIndex, selectedBlocks.size()), rightX,
                topY, colW, selectedFont, selectedLineH);

        float finalY = Math.min(leftEnd, rightEnd);
        if (finalY < bottomY) {
            finalY = bottomY;
        }

        return finalY;
    }

    private void drawAgreementWatermark(PDPageContentStream cs, float x, float y) throws IOException {
        String watermark = "SmartCBWTF";
        float fontSize = 58;
        float angle = (float) Math.toRadians(18);

        cs.saveGraphicsState();
        cs.beginText();
        cs.setFont(FONT_BOLD, fontSize);
        cs.setNonStrokingColor(new Color(232, 244, 236));
        cs.setTextMatrix(Matrix.getRotateInstance(angle, x, y));
        cs.showText(watermark);
        cs.endText();
        cs.restoreGraphicsState();
    }

    private java.util.List<AgreementTermBlock> buildTermBlocks(String termsText, float fontSize, float lineH, float colW)
            throws IOException {
        java.util.List<AgreementTermBlock> blocks = new java.util.ArrayList<>();
        for (AgreementTermLine line : parseAgreementTerms(termsText)) {
            if (line.heading) {
                blocks.add(new AgreementTermBlock(line, java.util.List.of(line.text), lineH + 1.2f));
                continue;
            }

            if (line.numbered()) {
                float numWidth = FONT_BOLD.getStringWidth(line.number + " ") / 1000 * fontSize;
                java.util.List<String> wrapped = wordWrap(line.body, FONT_REGULAR, fontSize, colW - numWidth - 1.5f);
                blocks.add(new AgreementTermBlock(line, wrapped, wrapped.size() * lineH + 1.4f));
                continue;
            }

            java.util.List<String> wrapped = wordWrap(line.text, FONT_REGULAR, fontSize, colW);
            blocks.add(new AgreementTermBlock(line, wrapped, wrapped.size() * lineH + 1.4f));
        }
        return blocks;
    }

    private int findBalancedTermsSplit(java.util.List<AgreementTermBlock> blocks) {
        float total = 0;
        for (AgreementTermBlock block : blocks) {
            total += block.height;
        }

        float running = 0;
        int bestIndex = 1;
        float bestDelta = Float.MAX_VALUE;
        for (int i = 1; i < blocks.size(); i++) {
            running += blocks.get(i - 1).height;
            AgreementTermLine next = blocks.get(i).source;
            if (next.heading && i > 1) {
                continue;
            }
            float delta = Math.abs((total / 2) - running);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestIndex = i;
            }
        }
        return Math.max(1, Math.min(bestIndex, blocks.size() - 1));
    }

    private float renderTermsColumn(PDPageContentStream cs, java.util.List<AgreementTermBlock> blocks,
            float x, float y, float colW, float bodyFont, float lineH) throws IOException {
        float headingFont = bodyFont + 0.4f;
        for (AgreementTermBlock block : blocks) {
            AgreementTermLine line = block.source;
            if (line.heading) {
                y -= 1.5f;
                drawText(cs, FONT_BOLD, headingFont, COL_PRIMARY, x, y, line.text);
                cs.setStrokingColor(new Color(191, 222, 200));
                cs.setLineWidth(0.35f);
                cs.moveTo(x, y - 2.1f);
                cs.lineTo(x + Math.min(colW, FONT_BOLD.getStringWidth(line.text) / 1000 * headingFont + 20), y - 2.1f);
                cs.stroke();
                y -= lineH + 2.2f;
                continue;
            }

            if (line.numbered()) {
                float numWidth = FONT_BOLD.getStringWidth(line.number + " ") / 1000 * bodyFont;
                float bodyX = x + numWidth + 2;
                boolean first = true;
                for (String wl : block.wrapped) {
                    if (first) {
                        drawText(cs, FONT_BOLD, bodyFont + 0.05f, COL_PRIMARY, x, y, line.number);
                        first = false;
                    }
                    drawText(cs, FONT_REGULAR, bodyFont, COL_DARK_TEXT, bodyX, y, wl);
                    y -= lineH;
                }
                y -= 1.9f;
                continue;
            }

            for (String wl : block.wrapped) {
                drawText(cs, FONT_REGULAR, bodyFont, COL_DARK_TEXT, x, y, wl);
                y -= lineH;
            }
            y -= 1.9f;
        }
        return y;
    }

    private String resolveAgreementTermsText(Agreement agreement, FacilitySettings settings) {
        String termsText = null;
        if (settings != null && settings.getAgreementTermsTemplate() != null
                && !settings.getAgreementTermsTemplate().isBlank()) {
            termsText = settings.getAgreementTermsTemplate();
        } else if (agreement.getTermsText() != null && !agreement.getTermsText().isBlank()) {
            termsText = agreement.getTermsText();
        }

        if (termsText == null || termsText.isBlank() || isStaleAgreementTerms(termsText)) {
            return UPDATED_AGREEMENT_TERMS;
        }
        return termsText;
    }

    private boolean isStaleAgreementTerms(String termsText) {
        String normalized = normalizeAgreementTermsText(termsText).toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("payment shall be made on a monthly basis")
                || normalized.contains("any delay in payment beyond the due date")
                || normalized.contains("the cbwtf agrees to collect, transport, and dispose")
                || normalized.contains("the hcf shall clear all outstanding dues before the expiry or termination");
    }

    private AgreementRenderCursor renderAgreementTerms(PDDocument document, PDPageContentStream cs, float y,
            Agreement agreement, Facility facility, FacilitySettings settings, boolean printMode) throws IOException {
        AgreementRenderCursor cursor = new AgreementRenderCursor(cs, y);
        String termsText = resolveAgreementTermsText(agreement, settings);

        cursor = ensureAgreementSpace(document, cursor, facility, agreement, printMode, 52, null);
        cursor.y = drawAgreementSectionHeader(cursor.cs, cursor.y, "TERMS & CONDITIONS");

        float bodyFontSize = printMode ? 8.4f : 8.2f;
        float headingFontSize = 8.8f;
        float lineH = 10.8f;
        float paragraphGap = 3.2f;
        float x = MARGIN_LEFT + 8;
        float wrapW = CONTENT_WIDTH - 16;

        for (AgreementTermLine line : parseAgreementTerms(termsText)) {
            if (line.heading) {
                cursor = ensureAgreementSpace(document, cursor, facility, agreement, printMode, 24,
                        "TERMS & CONDITIONS (CONTINUED)");
                cursor.y -= 2;
                drawText(cursor.cs, FONT_BOLD, headingFontSize, COL_PRIMARY, x, cursor.y, line.text);
                cursor.y -= lineH + 1;
                continue;
            }

            if (line.numbered()) {
                float numWidth = FONT_BOLD.getStringWidth(line.number + " ") / 1000 * bodyFontSize;
                float bodyX = x + numWidth + 2;
                java.util.List<String> wrapped = wordWrap(line.body, FONT_REGULAR, bodyFontSize,
                        wrapW - numWidth - 2);
                if (wrapped.isEmpty()) {
                    wrapped = java.util.List.of("");
                }

                boolean firstLine = true;
                for (String wl : wrapped) {
                    cursor = ensureAgreementSpace(document, cursor, facility, agreement, printMode, lineH,
                            "TERMS & CONDITIONS (CONTINUED)");
                    if (firstLine) {
                        drawText(cursor.cs, FONT_BOLD, bodyFontSize, COL_DARK_TEXT, x, cursor.y, line.number);
                        firstLine = false;
                    }
                    drawText(cursor.cs, FONT_REGULAR, bodyFontSize, COL_DARK_TEXT, bodyX, cursor.y, wl);
                    cursor.y -= lineH;
                }
                cursor.y -= paragraphGap;
                continue;
            }

            java.util.List<String> wrapped = wordWrap(line.text, FONT_REGULAR, bodyFontSize, wrapW);
            for (String wl : wrapped) {
                cursor = ensureAgreementSpace(document, cursor, facility, agreement, printMode, lineH,
                        "TERMS & CONDITIONS (CONTINUED)");
                drawText(cursor.cs, FONT_REGULAR, bodyFontSize, COL_DARK_TEXT, x, cursor.y, wl);
                cursor.y -= lineH;
            }
            cursor.y -= paragraphGap;
        }

        cursor.y -= 8;
        return cursor;
    }

    private AgreementRenderCursor ensureAgreementSpace(PDDocument document, AgreementRenderCursor cursor,
            Facility facility, Agreement agreement, boolean printMode, float neededHeight, String continuationTitle)
            throws IOException {
        if (cursor.y < AGREEMENT_BODY_BOTTOM + neededHeight) {
            return startAgreementContinuationPage(document, cursor.cs, facility, agreement, printMode,
                    continuationTitle);
        }
        return cursor;
    }

    private AgreementRenderCursor startAgreementContinuationPage(PDDocument document, PDPageContentStream currentCs,
            Facility facility, Agreement agreement, boolean printMode, String title) throws IOException {
        if (!printMode) {
            drawAgreementFooter(currentCs, document);
        }
        currentCs.close();

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream nextCs = new PDPageContentStream(document, page);
        float y = PAGE_HEIGHT - MARGIN_TOP;

        if (printMode) {
            y -= PRINT_LETTERHEAD_RESERVE;
        } else {
            String facilityName = nullSafe(facility.getName());
            String ref = "Agreement No: " + nullSafe(agreement.getAgreementNumber());
            drawText(nextCs, FONT_BOLD, 10, COL_PRIMARY, MARGIN_LEFT, y, facilityName);
            float refW = FONT_REGULAR.getStringWidth(ref) / 1000 * 8;
            drawText(nextCs, FONT_REGULAR, 8, COL_LIGHT_TEXT, PAGE_WIDTH - MARGIN_RIGHT - refW, y, ref);
            y -= 12;
            nextCs.setStrokingColor(COL_AGREEMENT_BORDER);
            nextCs.setLineWidth(0.65f);
            nextCs.moveTo(MARGIN_LEFT, y);
            nextCs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y);
            nextCs.stroke();
            y -= 18;
        }

        if (title != null && !title.isBlank()) {
            y = drawAgreementSectionHeader(nextCs, y, title);
        }
        return new AgreementRenderCursor(nextCs, y);
    }

    private float drawAgreementSectionHeader(PDPageContentStream cs, float y, String title) throws IOException {
        cs.setNonStrokingColor(COL_PRIMARY);
        cs.addRect(MARGIN_LEFT, y - 2, CONTENT_WIDTH, 2);
        cs.fill();
        drawText(cs, FONT_BOLD, 9, COL_PRIMARY, MARGIN_LEFT, y - 15, title);
        cs.setStrokingColor(COL_AGREEMENT_RULE);
        cs.setLineWidth(0.45f);
        cs.moveTo(MARGIN_LEFT, y - 20);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y - 20);
        cs.stroke();
        return y - 32;
    }

    private java.util.List<AgreementTermLine> parseAgreementTerms(String termsText) {
        java.util.List<AgreementTermLine> result = new java.util.ArrayList<>();
        String normalized = normalizeAgreementTermsText(termsText);
        String[] rawLines = normalized.split("\\n+");
        for (String rawLine : rawLines) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            if (line.isBlank()) {
                continue;
            }
            String upper = line.toUpperCase(java.util.Locale.ROOT);
            if (upper.equals("TERMS & CONDITIONS") || upper.equals("TERMS AND CONDITIONS")
                    || upper.equals("BIO MEDICAL WASTE COLLECTION & DISPOSAL SERVICES")) {
                continue;
            }

            java.util.regex.Matcher numbered = java.util.regex.Pattern
                    .compile("^(\\d+)[\\.)-]?\\s+(.+)$")
                    .matcher(line);
            if (numbered.matches()) {
                result.add(new AgreementTermLine(line, false, numbered.group(1) + ".", numbered.group(2).trim()));
                continue;
            }

            boolean heading = line.endsWith(":") || line.endsWith(":-")
                    || (line.length() < 85 && upper.equals(line) && line.matches(".*[A-Z].*"));
            result.add(new AgreementTermLine(line, heading, null, line));
        }
        return result;
    }

    private String normalizeAgreementTermsText(String termsText) {
        String text = termsText == null ? "" : termsText;
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        if (text.contains("<") && text.contains(">")) {
            text = text.replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</p>", "\n")
                    .replaceAll("(?i)</h[1-6]>", "\n")
                    .replaceAll("(?i)<li[^>]*>", "\n")
                    .replaceAll("(?i)</li>", "\n")
                    .replaceAll("(?i)</ol>|</ul>", "\n")
                    .replaceAll("<[^>]+>", " ");
        }
        return text.replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\t]+", "\n")
                .trim();
    }

    /**
     * Draw the agreement header with CBWTF logo and branding.
     */
    private float drawAgreementHeader(PDPageContentStream cs, PDDocument doc, float y,
            Facility facility, FacilityBranding branding, FacilitySettings settings,
            Agreement agreement) throws IOException {

        float logoEndX = MARGIN_LEFT;

        // Try to load CBWTF logo from branding or settings
        byte[] logoBytes = null;
        if (branding != null && branding.getLogoUrl() != null) {
            logoBytes = readUploadedAsset(branding.getLogoUrl(), "/uploads/branding/");
        }
        // Fallback: try logo from settings
        if (logoBytes == null && settings != null && settings.getLogoUrl() != null) {
            logoBytes = readUploadedAsset(settings.getLogoUrl(), "/uploads/branding/");
        }
        if (logoBytes != null) {
            try {
                PDImageXObject logo = PDImageXObject.createFromByteArray(doc, logoBytes, "cbwtf-logo");
                float imgW = logo.getWidth();
                float imgH = logo.getHeight();
                float targetH = 48;
                float scale = targetH / imgH;
                if (imgW * scale > 110) {
                    scale = 110 / imgW;
                }
                float finalW = imgW * scale;
                float finalH = imgH * scale;
                cs.drawImage(logo, MARGIN_LEFT, y - finalH + 5, finalW, finalH);
                logoEndX = MARGIN_LEFT + finalW + 10;
            } catch (Exception e) {
                // Logo render failed, continue without it
            }
        }

        // Verification QR in top-right corner — trust mark
        if (agreement != null) {
            try {
                String verUrl = "https://portal.smartcbwtf.com/verify/agreement/" + agreement.getId();
                byte[] qrBytes = generateQrImage(verUrl, 200, 200);
                PDImageXObject qrImg = PDImageXObject.createFromByteArray(doc, qrBytes, "header-qr");
                float qrSz = 52;
                cs.drawImage(qrImg, PAGE_WIDTH - MARGIN_RIGHT - qrSz, y - qrSz + 6, qrSz, qrSz);
            } catch (Exception e) {
                // QR not critical for header
            }
        }

        // Facility Name (bold)
        String facilityName = settings != null && settings.getLegalName() != null
                ? settings.getLegalName()
                : facility.getName();
        drawText(cs, FONT_BOLD, 13, COL_PRIMARY, logoEndX, y, nullSafe(facilityName));

        // Subtitle
        drawText(cs, FONT_REGULAR, 8, COL_LIGHT_TEXT, logoEndX, y - 15,
                "Common Bio-Medical Waste Treatment Facility");

        // Address - Multi-line wrapping
        String address = settings != null && settings.getRegisteredAddress() != null
                ? settings.getRegisteredAddress()
                : nullSafe(facility.getAddress());

        float addrW = (PAGE_WIDTH - MARGIN_RIGHT - 70) - logoEndX; // Available width for address
        java.util.List<String> addrLines = wordWrap(address, FONT_REGULAR, 8, addrW);

        float addrY = y - 27;
        for (String line : addrLines) {
            drawText(cs, FONT_REGULAR, 8, COL_DARK_TEXT, logoEndX, addrY, line);
            addrY -= 10;
        }

        // Phone + Email on one line (below address)
        String phone = settings != null && settings.getOfficialPhone() != null ? settings.getOfficialPhone()
                : nullSafe(facility.getContactPhone(), "");
        String email = settings != null && settings.getOfficialEmail() != null ? settings.getOfficialEmail()
                : nullSafe(facility.getContactEmail(), "");
        StringBuilder contactBuilder = new StringBuilder();
        if (!phone.isEmpty())
            contactBuilder.append("Ph: ").append(phone);
        if (!email.isEmpty()) {
            if (contactBuilder.length() > 0)
                contactBuilder.append("  |  ");
            contactBuilder.append("Email: ").append(email);
        }
        if (contactBuilder.length() > 0) {
            drawText(cs, FONT_REGULAR, 8, COL_DARK_TEXT, logoEndX, addrY - 2, contactBuilder.toString());
        }

        y -= Math.max(48, (y - addrY) + 20); // Dynamic height adjustment

        // Divider line
        cs.setLineWidth(1.2f);
        cs.setStrokingColor(COL_PRIMARY);
        cs.moveTo(MARGIN_LEFT, y);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y);
        cs.stroke();

        // Thin accent line below
        cs.setLineWidth(0.4f);
        cs.setStrokingColor(COL_ACCENT);
        cs.moveTo(MARGIN_LEFT, y - 2);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y - 2);
        cs.stroke();

        return y - 4;
    }

    private void drawAgreementVerificationQr(PDDocument doc, PDPageContentStream cs, Agreement agreement,
            float x, float y, float size) {
        if (agreement == null || agreement.getId() == null) {
            return;
        }
        try {
            String verUrl = "https://portal.smartcbwtf.com/verify/agreement/" + agreement.getId();
            byte[] qrBytes = generateQrImage(verUrl, 180, 180);
            PDImageXObject qrImg = PDImageXObject.createFromByteArray(doc, qrBytes, "agreement-verify-qr");
            cs.drawImage(qrImg, x, y, size, size);
            String label = "VERIFY";
            float labelFont = 5.2f;
            float labelW = FONT_BOLD.getStringWidth(label) / 1000 * labelFont;
            drawText(cs, FONT_BOLD, labelFont, COL_PRIMARY, x + (size - labelW) / 2, y + size + 3, label);
        } catch (Exception e) {
            // Verification QR is a convenience mark; never fail PDF rendering for it.
        }
    }

    /**
     * Draw SmartCBWTF footer with branding, digital signature notice, and
     * copyright.
     */
    private void drawAgreementFooter(PDPageContentStream cs, PDDocument doc) throws IOException {
        float y = MARGIN_BOTTOM / 2 + 14;

        // Green thin separator
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(COL_PRIMARY);
        cs.moveTo(MARGIN_LEFT, y + 20);
        cs.lineTo(PAGE_WIDTH - MARGIN_RIGHT, y + 20);
        cs.stroke();

        // Left: SmartCBWTF logo + branding
        float logoDrawnWidth = 0;
        try {
            byte[] logoBytes = null;
            try (java.io.InputStream is = getClass().getResourceAsStream("/smartcbwtf_logo.png")) {
                if (is != null)
                    logoBytes = is.readAllBytes();
            }
            if (logoBytes == null) {
                Path localPath = Paths.get("src/main/resources/smartcbwtf_logo.png");
                if (Files.exists(localPath))
                    logoBytes = Files.readAllBytes(localPath);
            }
            if (logoBytes != null) {
                PDImageXObject logo = PDImageXObject.createFromByteArray(doc, logoBytes, "footer-logo");
                float maxH = 12;
                float scale = maxH / logo.getHeight();
                float w = logo.getWidth() * scale;
                cs.drawImage(logo, MARGIN_LEFT, y + 6, w, maxH);
                logoDrawnWidth = w + 4;
            }
        } catch (Exception ignored) {
        }

        drawText(cs, FONT_BOLD, 6, COL_PRIMARY, MARGIN_LEFT + logoDrawnWidth, y + 10,
                "SmartCBWTF");
        drawText(cs, FONT_REGULAR, 6, new Color(120, 120, 120), MARGIN_LEFT + logoDrawnWidth + 38, y + 10,
                "| Bio-Medical Waste Compliance Platform");

        // Right: website URL
        String url = "www.smartcbwtf.com";
        float uw = FONT_REGULAR.getStringWidth(url) / 1000 * 6;
        drawText(cs, FONT_REGULAR, 6, COL_LIGHT_TEXT, PAGE_WIDTH - MARGIN_RIGHT - uw, y + 10, url);

        // Center: digitally generated notice
        String digitalText = "This is a digitally generated document and does not require a physical signature.";
        float dtW = FONT_REGULAR.getStringWidth(digitalText) / 1000 * 6;
        drawText(cs, FONT_REGULAR, 6, new Color(140, 140, 140),
                MARGIN_LEFT + (CONTENT_WIDTH - dtW) / 2, y, digitalText);

        // Center: copyright
        String copyright = "Copyright " + java.time.Year.now().getValue() + " SmartCBWTF. All rights reserved.";
        float cpW = FONT_REGULAR.getStringWidth(copyright) / 1000 * 6;
        drawText(cs, FONT_REGULAR, 6, new Color(160, 160, 160),
                MARGIN_LEFT + (CONTENT_WIDTH - cpW) / 2, y - 8, copyright);
    }

    /**
     * Draw a section title with colored left bar.
     */
    private float drawSectionTitle(PDPageContentStream cs, float y, String title) throws IOException {
        // Left accent bar
        cs.setNonStrokingColor(COL_PRIMARY);
        cs.addRect(MARGIN_LEFT, y - 18, 3, 18);
        cs.fill();

        // Light green background
        cs.setNonStrokingColor(new Color(245, 251, 245));
        cs.addRect(MARGIN_LEFT + 3, y - 18, CONTENT_WIDTH - 3, 18);
        cs.fill();

        // Subtle bottom border
        cs.setStrokingColor(new Color(215, 230, 215));
        cs.setLineWidth(0.3f);
        cs.moveTo(MARGIN_LEFT, y - 18);
        cs.lineTo(MARGIN_LEFT + CONTENT_WIDTH, y - 18);
        cs.stroke();

        drawText(cs, FONT_BOLD, 10, COL_ACCENT, MARGIN_LEFT + 10, y - 13, title);
        return y - 24;
    }

    /**
     * Draw a two-column key-value table.
     */
    private float drawKeyValueTable(PDPageContentStream cs, float y, float x, float width, String[][] rows)
            throws IOException {
        float labelWidth = 140;
        float rowHeight = 16;
        boolean alt = false;

        for (String[] row : rows) {
            // Alternate row background
            if (alt) {
                cs.setNonStrokingColor(new Color(250, 250, 252));
                cs.addRect(x, y - rowHeight + 3, width, rowHeight);
                cs.fill();
            }

            drawText(cs, FONT_BOLD, 9, COL_LIGHT_TEXT, x + 8, y - 9, row[0]);
            // Value — handle long text
            String value = row[1];
            if (value != null && value.length() > 65) {
                value = value.substring(0, 63) + "..";
            }
            drawText(cs, FONT_REGULAR, 9, COL_DARK_TEXT, x + labelWidth, y - 9, nullSafe(value));

            y -= rowHeight;
            alt = !alt;
        }

        return y;
    }

    /**
     * Draw two side-by-side professional detail cards for CBWTF and HCF parties.
     */
    private float drawPartyDetailsCards(PDPageContentStream cs, float y,
            Facility facility, Hcf hcf, FacilitySettings settings) throws IOException {

        float colGap = 12;
        float colWidth = (CONTENT_WIDTH - colGap) / 2;
        float leftX = MARGIN_LEFT;
        float rightX = MARGIN_LEFT + colWidth + colGap;

        // CBWTF data (compact — merge PAN/GSTIN)
        String cbwtfPanGstin = (settings != null ? nullSafe(settings.getPan(), "-")
                : nullSafe(facility.getPanNumber(), "-"))
                + " / "
                + (settings != null ? nullSafe(settings.getGstin(), "-") : nullSafe(facility.getGstNumber(), "-"));

        // Addrs
        String cbwtfAddr = settings != null && settings.getRegisteredAddress() != null
                ? settings.getRegisteredAddress()
                : nullSafe(facility.getAddress());

        String[][] cbwtfData = {
                { "Name", nullSafe(facility.getName()) },
                { "Code", nullSafe(facility.getCode()) },
                { "Address", cbwtfAddr }, // Will wrap this
                { "Phone", settings != null && settings.getOfficialPhone() != null
                        ? settings.getOfficialPhone()
                        : nullSafe(facility.getContactPhone()) },
                { "Email", settings != null && settings.getOfficialEmail() != null
                        ? settings.getOfficialEmail()
                        : nullSafe(facility.getContactEmail()) },
                { "PAN / GSTIN", cbwtfPanGstin },
                { "Auth No", settings != null ? nullSafe(settings.getAuthorizationNumber(), "N/A") : "N/A" },
        };

        // HCF data (compact — merge related fields)
        String hcfType = hcf.getHcfType() != null ? hcf.getHcfType().name().replace("_", " ") : "N/A";
        String bedInfo = (hcf.getNumberOfBeds() != null ? hcf.getNumberOfBeds().toString() : "0")
                + " (" + (Boolean.TRUE.equals(hcf.getBedded()) ? "Bedded" : "Non-Bedded") + ")";
        String hcfAddr = nullSafe(hcf.getAddress());
        String statePin = nullSafe(hcf.getState(), "") + " " + nullSafe(hcf.getPincode(), "");
        if (!statePin.isBlank())
            hcfAddr = hcfAddr + ", " + statePin.trim();

        String occupancyStr = hcf.getOccupancy() != null ? String.format("%.0f%%", hcf.getOccupancy()) : "N/A";
        String[][] hcfData = {
                { "Name", nullSafe(hcf.getName()) },
                { "Code", nullSafe(hcf.getCode()) },
                { "Doctor", nullSafe(hcf.getDoctorName(), "N/A") },
                { "Address", hcfAddr }, // Will wrap this
                { "Phone", nullSafe(hcf.getContactPhone(), "N/A") },
                { "Email", nullSafe(hcf.getContactEmail(), "N/A") },
                { "PAN / GSTIN", nullSafe(hcf.getPanNo(), "-") + " / " + nullSafe(hcf.getGstNo(), "-") },
                { "Type / Beds", hcfType + " / " + bedInfo },
                { "PCB Auth", nullSafe(hcf.getPcbAuthorizationNo(), "N/A") },
                { "Occupancy", occupancyStr },
        };

        // Prepare Wrapping for Adresses to determine height
        float rowH = 9.8f;
        float headerH = 17;
        float padBottom = 5;
        float labelW = 62;
        float valW = colWidth - labelW - 10;

        // Calculate dynamic height based on wrapped lines
        int cbwtfTotalRows = 0;
        for (String[] row : cbwtfData) {
            if (row[0].equals("Address")) {
                cbwtfTotalRows += wordWrap(row[1], FONT_REGULAR, 6.8f, valW).size();
            } else {
                cbwtfTotalRows++;
            }
        }

        int hcfTotalRows = 0;
        for (String[] row : hcfData) {
            if (row[0].equals("Address")) {
                hcfTotalRows += wordWrap(row[1], FONT_REGULAR, 6.8f, valW).size();
            } else {
                hcfTotalRows++;
            }
        }

        int maxRows = Math.max(cbwtfTotalRows, hcfTotalRows);
        float cardH = headerH + (maxRows * rowH) + padBottom;

        // Left card background
        cs.setNonStrokingColor(new Color(252, 253, 252));
        cs.addRect(leftX, y - cardH, colWidth, cardH);
        cs.fill();
        // Green top accent
        cs.setNonStrokingColor(COL_PRIMARY);
        cs.addRect(leftX, y - 2, colWidth, 2);
        cs.fill();
        // Border
        cs.setStrokingColor(COL_AGREEMENT_BORDER);
        cs.setLineWidth(0.65f);
        cs.addRect(leftX, y - cardH, colWidth, cardH);
        cs.stroke();

        // Right card background
        cs.setNonStrokingColor(new Color(252, 253, 252));
        cs.addRect(rightX, y - cardH, colWidth, cardH);
        cs.fill();
        cs.setNonStrokingColor(COL_PRIMARY);
        cs.addRect(rightX, y - 2, colWidth, 2);
        cs.fill();
        cs.setStrokingColor(COL_AGREEMENT_BORDER);
        cs.setLineWidth(0.65f);
        cs.addRect(rightX, y - cardH, colWidth, cardH);
        cs.stroke();

        // Card headers
        float headY = y - 12;
        drawText(cs, FONT_BOLD, 8, COL_PRIMARY, leftX + 8, headY, "WASTE TREATMENT FACILITY");
        drawText(cs, FONT_BOLD, 8, COL_PRIMARY, rightX + 8, headY, "HEALTHCARE FACILITY");

        // Header separator
        float sepY = y - headerH;
        cs.setStrokingColor(COL_AGREEMENT_RULE);
        cs.setLineWidth(0.45f);
        cs.moveTo(leftX + 6, sepY);
        cs.lineTo(leftX + colWidth - 6, sepY);
        cs.stroke();
        cs.moveTo(rightX + 6, sepY);
        cs.lineTo(rightX + colWidth - 6, sepY);
        cs.stroke();

        // Render CBWTF data with wrapping
        float dY = sepY - 11;
        for (String[] row : cbwtfData) {
            String label = row[0];
            String val = nullSafe(row[1]);

            drawText(cs, FONT_BOLD, 6.8f, COL_DARK_TEXT, leftX + 8, dY, label);

            if (label.equals("Address")) {
                java.util.List<String> lines = wordWrap(val, FONT_REGULAR, 6.8f, valW);
                for (String line : lines) {
                    drawText(cs, FONT_REGULAR, 6.8f, COL_DARK_TEXT, leftX + labelW + 8, dY, line);
                    dY -= rowH;
                }
            } else {
                drawText(cs, FONT_REGULAR, 6.8f, COL_DARK_TEXT, leftX + labelW + 8, dY, truncate(val, 42));
                dY -= rowH;
            }
        }

        // Render HCF data with wrapping
        dY = sepY - 11;
        for (String[] row : hcfData) {
            String label = row[0];
            String val = nullSafe(row[1]);

            drawText(cs, FONT_BOLD, 6.8f, COL_DARK_TEXT, rightX + 8, dY, label);

            if (label.equals("Address")) {
                java.util.List<String> lines = wordWrap(val, FONT_REGULAR, 6.8f, valW);
                for (String line : lines) {
                    drawText(cs, FONT_REGULAR, 6.8f, COL_DARK_TEXT, rightX + labelW + 8, dY, line);
                    dY -= rowH;
                }
            } else {
                drawText(cs, FONT_REGULAR, 6.8f, COL_DARK_TEXT, rightX + labelW + 8, dY, truncate(val, 42));
                dY -= rowH;
            }
        }

        return y - cardH - 3;
    }

    /**
     * Word-wrap text to fit within a given width.
     */
    private java.util.List<String> wordWrap(String text, PDFont font, float fontSize, float maxWidth)
            throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (testWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Default terms & conditions text used when no custom template is set.
     */
    private String getDefaultTermsText() {
        return UPDATED_AGREEMENT_TERMS;
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

    public String generateInvoicePdf(Invoice invoice) {
        String filename = "invoice-" + safeFileToken(invoice.getInvoiceNumber()) + ".pdf";
        Path path = baseDir.resolve(filename);
        try {
            renderSimplePdf(path, "Invoice " + invoice.getInvoiceNumber(), new String[] {
                    "HCF: " + invoice.getHcf().getName(),
                    "Facility: " + invoice.getFacility().getName(),
                    "Period: " + DATE_FMT.format(invoice.getPeriodStart()) + " to "
                            + DATE_FMT.format(invoice.getPeriodEnd()),
                    "Total: " + invoice.getTotalAmount()
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to write invoice PDF", e);
        }
        return path.toString();
    }

    public String generateLabelBatchPdf(Hcf hcf, Facility facility, String category, String[] qrCodes,
            java.time.LocalDate validUntil) {
        String filename = "labels-" + safeFileToken(hcf.getCode()) + "-" + safeFileToken(category) + "-"
                + System.currentTimeMillis() + ".pdf";
        Path path = baseDir.resolve(filename);

        try (PDDocument document = new PDDocument()) {
            // Layout Configuration
            float margin = 20;
            float headerHeight = 80; // Space for header
            float footerHeight = 40; // Space for footer
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float contentWidth = pageWidth - (2 * margin);
            // Effective content height between header and footer
            float contentHeight = pageHeight - (2 * margin) - headerHeight - footerHeight;

            float startY = pageHeight - margin - headerHeight;

            // Grid Config: 3 Columns x 3 Rows (more space per label for QR + HCF details)
            int cols = 3;
            int rows = 3;
            float gap = 12;
            float labelWidth = (contentWidth - ((cols - 1) * gap)) / cols;
            float labelHeight = (contentHeight - ((rows - 1) * gap)) / rows;

            int totalLabels = qrCodes.length;
            int labelsPerPage = cols * rows;
            int numPages = (int) Math.ceil((double) totalLabels / labelsPerPage);

            String dateStr = DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDate.now());

            for (int p = 0; p < numPages; p++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    // Draw Branding
                    drawCommonHeader(cs, document, "QR LABELS BATCH", "Generated: " + dateStr);
                    drawCommonFooter(cs, document);

                    int startIdx = p * labelsPerPage;
                    int endIdx = Math.min(startIdx + labelsPerPage, totalLabels);

                    for (int i = startIdx; i < endIdx; i++) {
                        int pageIndex = i - startIdx;
                        int row = pageIndex / cols;
                        int col = pageIndex % cols;

                        float x = margin + (col * (labelWidth + gap));
                        float y = startY - ((row + 1) * labelHeight) - (row * gap);

                        drawProfessionalLabel(document, cs, x, y, labelWidth, labelHeight, hcf, category, qrCodes[i],
                                validUntil);
                    }
                    // Cut lines logic adjusted for new Y range?
                    // To keep it simple, we draw cut lines for the whole grid area
                    // But we need to offset Y to start below header
                    drawCutLines(cs, margin, pageWidth, startY, rows, cols, labelWidth, labelHeight, gap);
                }
            }

            document.save(path.toFile());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write label PDF", e);
        }
        return "/files/" + filename;
    }

    /**
     * Generates a single PDF with QR labels for multiple waste categories.
     * Labels for each category are laid out sequentially across pages.
     *
     * @param hcf             the HCF entity
     * @param facility        the facility
     * @param categoryQrCodes ordered map of category -> qrCode arrays
     * @return relative URL to the generated PDF
     */
    public String generateMultiCategoryLabelBatchPdf(Hcf hcf, Facility facility,
            Map<String, String[]> categoryQrCodes, java.time.LocalDate validUntil) {
        String filename = "labels-" + safeFileToken(hcf.getCode()) + "-MULTI-" + System.currentTimeMillis() + ".pdf";
        Path path = baseDir.resolve(filename);

        try (PDDocument document = new PDDocument()) {
            float margin = 20;
            float headerHeight = 80;
            float footerHeight = 40;
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float contentWidth = pageWidth - (2 * margin);
            float contentHeight = pageHeight - (2 * margin) - headerHeight - footerHeight;
            float startY = pageHeight - margin - headerHeight;

            int cols = 3;
            int rows = 3;
            float gap = 12;
            float labelWidth = (contentWidth - ((cols - 1) * gap)) / cols;
            float labelHeight = (contentHeight - ((rows - 1) * gap)) / rows;
            int labelsPerPage = cols * rows;

            String dateStr = DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDate.now());

            // Flatten all categories' QR codes into a single ordered list of (category,
            // qrCode)
            java.util.List<String[]> allLabels = new java.util.ArrayList<>();
            for (var entry : categoryQrCodes.entrySet()) {
                String cat = entry.getKey();
                for (String qr : entry.getValue()) {
                    allLabels.add(new String[] { cat, qr });
                }
            }

            int totalLabels = allLabels.size();
            int numPages = (int) Math.ceil((double) totalLabels / labelsPerPage);

            for (int p = 0; p < numPages; p++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    drawCommonHeader(cs, document, "QR LABELS BATCH", "Generated: " + dateStr);
                    drawCommonFooter(cs, document);

                    int startIdx = p * labelsPerPage;
                    int endIdx = Math.min(startIdx + labelsPerPage, totalLabels);

                    for (int i = startIdx; i < endIdx; i++) {
                        int pageIndex = i - startIdx;
                        int row = pageIndex / cols;
                        int col = pageIndex % cols;

                        float x = margin + (col * (labelWidth + gap));
                        float y = startY - ((row + 1) * labelHeight) - (row * gap);

                        String cat = allLabels.get(i)[0];
                        String qr = allLabels.get(i)[1];
                        drawProfessionalLabel(document, cs, x, y, labelWidth, labelHeight, hcf, cat, qr, validUntil);
                    }
                    drawCutLines(cs, margin, pageWidth, startY, rows, cols, labelWidth, labelHeight, gap);
                }
            }

            document.save(path.toFile());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write multi-category label PDF", e);
        }
        return "/files/" + filename;
    }

    /**
     * Generates a single QR label PDF using the exact same layout as the batch PDF.
     * The label is rendered at the same size as in the 3x3 grid, centered on an A4
     * page.
     */
    public String generateSingleLabelPdf(Hcf hcf, Facility facility, String category, String qrCodeText,
            java.time.LocalDate validUntil) {
        String filename = "label-" + safeFileToken(hcf.getCode()) + "-" + safeFileToken(category) + "-"
                + System.currentTimeMillis() + ".pdf";
        Path path = baseDir.resolve(filename);

        try (PDDocument document = new PDDocument()) {
            // Use the exact same layout constants as generateLabelBatchPdf
            float margin = 20;
            float headerHeight = 80;
            float footerHeight = 40;
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float contentWidth = pageWidth - (2 * margin);
            float contentHeight = pageHeight - (2 * margin) - headerHeight - footerHeight;
            float startY = pageHeight - margin - headerHeight;

            int cols = 3;
            int rows = 3;
            float gap = 12;
            float labelWidth = (contentWidth - ((cols - 1) * gap)) / cols;
            float labelHeight = (contentHeight - ((rows - 1) * gap)) / rows;

            String dateStr = DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDate.now());

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawCommonHeader(cs, document, "QR LABEL", "Generated: " + dateStr);
                drawCommonFooter(cs, document);

                // Place the single label in the top-left cell (same position as first label in
                // batch)
                float x = margin;
                float y = startY - labelHeight;

                drawProfessionalLabel(document, cs, x, y, labelWidth, labelHeight, hcf, category, qrCodeText,
                        validUntil);
            }

            document.save(path.toFile());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write single label PDF", e);
        }
        return "/files/" + filename;
    }

    private void drawProfessionalLabel(PDDocument doc, PDPageContentStream cs, float x, float y, float w, float h,
            Hcf hcf, String category, String qrCodeText, java.time.LocalDate validUntil)
            throws IOException, com.google.zxing.WriterException {
        // Outline
        cs.setStrokingColor(Color.LIGHT_GRAY);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();

        // Category Header Color
        Color catColor = Color.GRAY;
        switch (category.toUpperCase()) {
            case "YELLOW":
                catColor = new Color(255, 235, 59);
                break;
            case "RED":
                catColor = new Color(244, 67, 54);
                break;
            case "BLUE":
                catColor = new Color(33, 150, 243);
                break;
            case "WHITE":
                catColor = Color.WHITE;
                break;
        }

        // Header Background
        float headerH = 20;
        cs.setNonStrokingColor(catColor);
        cs.addRect(x, y + h - headerH, w, headerH);
        cs.fill();

        // Header Text (Category)
        cs.beginText();
        cs.setFont(FONT_BOLD, 10);
        cs.setNonStrokingColor(category.equalsIgnoreCase("WHITE") ? Color.BLACK : Color.DARK_GRAY);
        float tw = FONT_BOLD.getStringWidth(category) / 1000 * 10;
        cs.newLineAtOffset(x + (w - tw) / 2, y + h - 14);
        cs.showText(category);
        cs.endText();

        // HCF Name
        cs.beginText();
        cs.setFont(FONT_BOLD, 8);
        cs.setNonStrokingColor(Color.BLACK);
        String hcfName = truncate(hcf.getName(), 28);
        float nw = FONT_BOLD.getStringWidth(hcfName) / 1000 * 8;
        cs.newLineAtOffset(x + (w - nw) / 2, y + h - 34);
        cs.showText(hcfName);
        cs.endText();

        // --- Bottom details section (built from bottom up) ---
        float detailFontSize = 5.5f;
        float lineSp = 8f;
        float bottomY = y + 4;

        // Serial / QR ID at very bottom
        cs.beginText();
        cs.setFont(FONT_MONO, 6);
        cs.setNonStrokingColor(Color.DARK_GRAY);
        String shortCode;
        if (qrCodeText.contains("|")) {
            // Legacy pipe-delimited: CBWTF|CODE|CAT|SERIAL
            shortCode = qrCodeText.substring(qrCodeText.lastIndexOf("|") + 1);
        } else {
            // Signed JSON payload — extract qrId for display
            try {
                int idx = qrCodeText.indexOf("\"qrId\":\"");
                if (idx >= 0) {
                    int start = idx + 8;
                    int end = qrCodeText.indexOf("\"", start);
                    String qrId = qrCodeText.substring(start, end).replace("-", "");
                    shortCode = qrId.substring(qrId.length() - 8).toUpperCase();
                } else {
                    shortCode = "QR-LABEL";
                }
            } catch (Exception e) {
                shortCode = "QR-LABEL";
            }
        }
        float cw = FONT_MONO.getStringWidth(shortCode) / 1000 * 6;
        cs.newLineAtOffset(x + (w - cw) / 2, bottomY);
        cs.showText(shortCode);
        cs.endText();
        bottomY += lineSp;

        // Doctor / Owner name
        if (hcf.getDoctorName() != null && !hcf.getDoctorName().isBlank()) {
            cs.beginText();
            cs.setFont(FONT_REGULAR, detailFontSize);
            cs.setNonStrokingColor(Color.DARK_GRAY);
            String text = truncate(hcf.getDoctorName(), 28);
            float dtw = FONT_REGULAR.getStringWidth(text) / 1000 * detailFontSize;
            cs.newLineAtOffset(x + (w - dtw) / 2, bottomY);
            cs.showText(text);
            cs.endText();
            bottomY += lineSp;
        }

        // Contact Phone
        if (hcf.getContactPhone() != null && !hcf.getContactPhone().isBlank()) {
            cs.beginText();
            cs.setFont(FONT_REGULAR, detailFontSize);
            cs.setNonStrokingColor(Color.DARK_GRAY);
            String text = "Ph: " + hcf.getContactPhone();
            float ptw = FONT_REGULAR.getStringWidth(text) / 1000 * detailFontSize;
            cs.newLineAtOffset(x + (w - ptw) / 2, bottomY);
            cs.showText(text);
            cs.endText();
            bottomY += lineSp;
        }

        // Contact Email
        if (hcf.getContactEmail() != null && !hcf.getContactEmail().isBlank()) {
            cs.beginText();
            cs.setFont(FONT_REGULAR, detailFontSize);
            cs.setNonStrokingColor(Color.DARK_GRAY);
            String text = truncate(hcf.getContactEmail(), 30);
            float etw = FONT_REGULAR.getStringWidth(text) / 1000 * detailFontSize;
            cs.newLineAtOffset(x + (w - etw) / 2, bottomY);
            cs.showText(text);
            cs.endText();
            bottomY += lineSp;
        }

        if (hcf.getAddress() != null && !hcf.getAddress().isBlank()) {
            cs.beginText();
            cs.setFont(FONT_REGULAR, detailFontSize);
            cs.setNonStrokingColor(Color.DARK_GRAY);
            String text = truncate(hcf.getAddress(), 32);
            float atw = FONT_REGULAR.getStringWidth(text) / 1000 * detailFontSize;
            cs.newLineAtOffset(x + (w - atw) / 2, bottomY);
            cs.showText(text);
            cs.endText();
            bottomY += lineSp;
        }

        // Valid Until
        if (validUntil != null) {
            cs.beginText();
            cs.setFont(FONT_BOLD, detailFontSize);
            cs.setNonStrokingColor(Color.RED.darker());
            String text = "Valid Until: "
                    + java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy").format(validUntil);
            float vtw = FONT_BOLD.getStringWidth(text) / 1000 * detailFontSize;
            cs.newLineAtOffset(x + (w - vtw) / 2, bottomY);
            cs.showText(text);
            cs.endText();
            bottomY += lineSp;
        }

        // QR Code Image - fills remaining space between header section and detail
        // section
        float qrAvailableHeight = (y + h - 38) - bottomY - 2;
        float qrSize = Math.min(qrAvailableHeight, w - 16);
        if (qrSize < 40)
            qrSize = 40;

        byte[] qrBytes = generateQrImage(qrCodeText, 300, 300);
        PDImageXObject qrImage = PDImageXObject.createFromByteArray(doc, qrBytes, "qr");
        float qrX = x + (w - qrSize) / 2;
        float qrY = bottomY + 1;
        cs.drawImage(qrImage, qrX, qrY, qrSize, qrSize);
    }

    private void drawCutLines(PDPageContentStream cs, float margin, float pageWidth, float startY, int rows, int cols,
            float lw, float lh, float gap) throws IOException {
        cs.setStrokingColor(Color.LIGHT_GRAY);
        cs.setLineDashPattern(new float[] { 3, 3 }, 0);
        cs.setLineWidth(0.2f);

        // Vertical lines
        for (int c = 1; c < cols; c++) {
            float x = margin + (c * lw) + ((c - 1) * gap) + (gap / 2);
            // Draw from startY down to end of grid
            float gridHeight = rows * lh + (rows - 1) * gap;
            cs.moveTo(x, startY);
            cs.lineTo(x, startY - gridHeight);
            cs.stroke();
        }

        // Horizontal lines
        for (int r = 1; r < rows; r++) {
            float y = startY - (r * lh) - ((r - 1) * gap) - (gap / 2);
            cs.moveTo(margin, y);
            cs.lineTo(pageWidth - margin, y);
            cs.stroke();
        }
        cs.setLineDashPattern(new float[] {}, 0);
    }

    private byte[] readUploadedAsset(String publicUrl, String expectedPublicPrefix) {
        try {
            Path path = UploadFileValidator.uploadedAssetPath(publicUrl, expectedPublicPrefix);
            if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
                return Files.readAllBytes(path);
            }
        } catch (Exception e) {
            // Invalid or missing uploaded assets should not block document rendering.
        }
        return null;
    }

    private byte[] generateQrImage(String text, int width, int height)
            throws com.google.zxing.WriterException, IOException {
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE,
                width, height);

        java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
        com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    private void renderSimplePdf(Path path, String title, String[] lines) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(FONT_BOLD, 18);
                contentStream.setLeading(16f);
                contentStream.setNonStrokingColor(Color.BLACK);
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

    private String formatDate(LocalDate date) {
        return date != null ? DATE_FMT.format(date) : "N/A";
    }

    private String formatInstant(Instant instant) {
        return instant != null ? DATETIME_FMT.format(instant.atZone(ZoneId.of("Asia/Kolkata"))) : "N/A";
    }
}
