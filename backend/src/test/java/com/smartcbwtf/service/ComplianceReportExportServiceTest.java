package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.domain.AnnualComplianceReport;
import com.smartcbwtf.domain.DailyComplianceReport;
import com.smartcbwtf.domain.Facility;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplianceReportExportServiceTest {

    @Test
    void dailyPdfRendersMetadataAndStoredSnapshotData() throws Exception {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setCode("CBWTF-001");
        facility.setName("Smart CBWTF");

        DailyComplianceReport report = new DailyComplianceReport();
        report.setFacility(facility);
        report.setReportDate(LocalDate.of(2026, 6, 30));
        report.setGeneratedAt(Instant.parse("2026-07-01T01:00:00Z"));
        report.setStatus(DailyComplianceReport.Status.READY);
        report.setDataCompleteness(DailyComplianceReport.DataCompleteness.COMPLETE);
        report.setSourceWindowFrom(Instant.parse("2026-06-29T18:30:00Z"));
        report.setSourceWindowTo(Instant.parse("2026-06-30T18:29:59Z"));
        report.setChecksum("abc123");
        report.setDataJson("{\"totalWasteKg\":12.5,\"longQr\":\""
                + "QR-".repeat(120)
                + "\"}");

        byte[] pdf = new ComplianceReportExportService(new ObjectMapper()).dailyPdf(report);

        String text;
        try (PDDocument document = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(document);
        }

        assertTrue(text.contains("Daily Compliance Report"));
        assertTrue(text.contains("READY"));
        assertTrue(text.contains("totalWasteKg"));
        assertTrue(text.contains("12.5"));
    }

    @Test
    void annualExcelRendersMetadataSnapshotDataAndNeutralizesFormulaText() throws Exception {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setCode("=FAC");
        facility.setName("+Smart CBWTF");

        AnnualComplianceReport report = new AnnualComplianceReport();
        report.setFacility(facility);
        report.setFinancialYear("2025-26");
        report.setGeneratedAt(Instant.parse("2026-04-01T01:00:00Z"));
        report.setStatus(AnnualComplianceReport.Status.READY);
        report.setDataCompleteness(AnnualComplianceReport.DataCompleteness.COMPLETE);
        report.setSourceWindowFrom(Instant.parse("2025-03-31T18:30:00Z"));
        report.setSourceWindowTo(Instant.parse("2026-03-31T18:30:00Z"));
        report.setChecksum("@checksum");
        report.setDataJson("{\"totalWasteKg\":12.5,\"note\":\"=cmd\",\"nested\":{\"category\":\"+YELLOW\"}}");

        byte[] excel = new ComplianceReportExportService(new ObjectMapper()).annualExcel(report);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            var metadata = workbook.getSheet("Form IV Metadata");
            var snapshot = workbook.getSheet("Snapshot Data");

            assertEquals("'+Smart CBWTF (=FAC)", metadata.getRow(1).getCell(1).getStringCellValue());
            assertEquals("'@checksum", metadata.getRow(7).getCell(1).getStringCellValue());
            assertEquals("$.note", snapshot.getRow(2).getCell(0).getStringCellValue());
            assertEquals("'=cmd", snapshot.getRow(2).getCell(1).getStringCellValue());
            assertEquals("$.nested.category", snapshot.getRow(3).getCell(0).getStringCellValue());
            assertEquals("'+YELLOW", snapshot.getRow(3).getCell(1).getStringCellValue());
        }
    }
}
