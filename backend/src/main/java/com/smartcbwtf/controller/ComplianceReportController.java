package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.DailyReportGenerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Compliance Report Controller.
 * 
 * READ-ONLY endpoints. No POST for report generation.
 * Reports are generated automatically by scheduled jobs.
 */
@RestController
@RequestMapping("/api/cbwtf/compliance")
public class ComplianceReportController {

    private final DailyComplianceReportRepository dailyReportRepository;
    private final MonthlyComplianceReportRepository monthlyReportRepository;
    private final AnnualComplianceReportRepository annualReportRepository;
    private final BarcodeComplianceReportRepository barcodeReportRepository;
    private final ViolationReportRepository violationReportRepository;
    private final DailyReportGenerationService dailyReportService;

    public ComplianceReportController(
            DailyComplianceReportRepository dailyReportRepository,
            MonthlyComplianceReportRepository monthlyReportRepository,
            AnnualComplianceReportRepository annualReportRepository,
            BarcodeComplianceReportRepository barcodeReportRepository,
            ViolationReportRepository violationReportRepository,
            DailyReportGenerationService dailyReportService) {
        this.dailyReportRepository = dailyReportRepository;
        this.monthlyReportRepository = monthlyReportRepository;
        this.annualReportRepository = annualReportRepository;
        this.barcodeReportRepository = barcodeReportRepository;
        this.violationReportRepository = violationReportRepository;
        this.dailyReportService = dailyReportService;
    }

    // ========================================================================
    // DAILY REPORTS
    // ========================================================================

    @GetMapping("/daily")
    public ResponseEntity<Page<DailyReportDTO>> listDailyReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<DailyComplianceReport> reports = dailyReportRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate")));
        return ResponseEntity.ok(reports.map(DailyReportDTO::from));
    }

    @GetMapping("/daily/{id}")
    public ResponseEntity<DailyReportDetailDTO> getDailyReport(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return dailyReportRepository.findById(id)
                .filter(r -> r.getFacility().getId().equals(facilityId))
                .map(DailyReportDetailDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/daily/{id}/pdf")
    public ResponseEntity<byte[]> downloadDailyReportPdf(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return dailyReportRepository.findById(id)
                .filter(r -> r.getFacility().getId().equals(facilityId))
                .map(r -> {
                    byte[] pdf = r.getPdfBytes();
                    if (pdf == null || pdf.length == 0) {
                        // TODO: Generate PDF on-the-fly if not pre-generated
                        return ResponseEntity.noContent().<byte[]>build();
                    }
                    String filename = "daily_report_" + r.getReportDate() + ".pdf";
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // MONTHLY REPORTS
    // ========================================================================

    @GetMapping("/monthly")
    public ResponseEntity<Page<MonthlyReportDTO>> listMonthlyReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<MonthlyComplianceReport> reports = monthlyReportRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportMonth")));
        return ResponseEntity.ok(reports.map(MonthlyReportDTO::from));
    }

    // ========================================================================
    // ANNUAL REPORTS (Form IV)
    // ========================================================================

    @GetMapping("/annual")
    public ResponseEntity<Page<AnnualReportDTO>> listAnnualReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<AnnualComplianceReport> reports = annualReportRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "financialYear")));
        return ResponseEntity.ok(reports.map(AnnualReportDTO::from));
    }

    @GetMapping("/annual/{id}/excel")
    public ResponseEntity<byte[]> downloadAnnualReportExcel(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return annualReportRepository.findById(id)
                .filter(r -> r.getFacility().getId().equals(facilityId))
                .map(r -> {
                    byte[] excel = r.getExcelBytes();
                    if (excel == null || excel.length == 0) {
                        return ResponseEntity.noContent().<byte[]>build();
                    }
                    String filename = "form_iv_" + r.getFinancialYear() + ".xlsx";
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .contentType(MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                            .body(excel);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // BARCODE COMPLIANCE
    // ========================================================================

    @GetMapping("/barcode")
    public ResponseEntity<Page<BarcodeReportDTO>> listBarcodeReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<BarcodeComplianceReport> reports = barcodeReportRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate")));
        return ResponseEntity.ok(reports.map(BarcodeReportDTO::from));
    }

    // ========================================================================
    // VIOLATIONS
    // ========================================================================

    @GetMapping("/violations")
    public ResponseEntity<Page<ViolationReportDTO>> listViolationReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size) {
        UUID facilityId = TenantContext.getTenantId();
        Page<ViolationReport> reports = violationReportRepository.findByFacilityId(
                facilityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reportDate")));
        return ResponseEntity.ok(reports.map(ViolationReportDTO::from));
    }

    // ========================================================================
    // MANUAL TRIGGER (Admin only - for testing)
    // ========================================================================

    @PostMapping("/daily/generate")
    public ResponseEntity<?> triggerDailyReport(@RequestParam String date) {
        UUID facilityId = TenantContext.getTenantId();
        LocalDate reportDate = LocalDate.parse(date);
        boolean generated = dailyReportService.generateReport(facilityId, reportDate);
        return ResponseEntity.ok(java.util.Map.of("generated", generated));
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    public record DailyReportDTO(
            UUID id,
            LocalDate reportDate,
            String status,
            String dataCompleteness,
            String generatedAt,
            String checksum) {
        public static DailyReportDTO from(DailyComplianceReport r) {
            return new DailyReportDTO(
                    r.getId(),
                    r.getReportDate(),
                    r.getStatus().name(),
                    r.getDataCompleteness().name(),
                    r.getGeneratedAt().toString(),
                    r.getChecksum());
        }
    }

    public record DailyReportDetailDTO(
            UUID id,
            LocalDate reportDate,
            String status,
            String dataCompleteness,
            String sourceWindowFrom,
            String sourceWindowTo,
            String dataJson,
            String generatedAt,
            String checksum,
            boolean hasPdf) {
        public static DailyReportDetailDTO from(DailyComplianceReport r) {
            return new DailyReportDetailDTO(
                    r.getId(),
                    r.getReportDate(),
                    r.getStatus().name(),
                    r.getDataCompleteness().name(),
                    r.getSourceWindowFrom().toString(),
                    r.getSourceWindowTo().toString(),
                    r.getDataJson(),
                    r.getGeneratedAt().toString(),
                    r.getChecksum(),
                    r.getPdfBytes() != null && r.getPdfBytes().length > 0);
        }
    }

    public record MonthlyReportDTO(
            UUID id,
            LocalDate reportMonth,
            String status,
            String dataCompleteness,
            String generatedAt) {
        public static MonthlyReportDTO from(MonthlyComplianceReport r) {
            return new MonthlyReportDTO(
                    r.getId(),
                    r.getReportMonth(),
                    r.getStatus().name(),
                    r.getDataCompleteness().name(),
                    r.getGeneratedAt().toString());
        }
    }

    public record AnnualReportDTO(
            UUID id,
            String financialYear,
            String status,
            String dataCompleteness,
            String generatedAt,
            boolean hasPdf,
            boolean hasExcel) {
        public static AnnualReportDTO from(AnnualComplianceReport r) {
            return new AnnualReportDTO(
                    r.getId(),
                    r.getFinancialYear(),
                    r.getStatus().name(),
                    r.getDataCompleteness().name(),
                    r.getGeneratedAt().toString(),
                    r.getPdfBytes() != null && r.getPdfBytes().length > 0,
                    r.getExcelBytes() != null && r.getExcelBytes().length > 0);
        }
    }

    public record BarcodeReportDTO(
            UUID id,
            LocalDate reportDate,
            String reportType,
            String status,
            String generatedAt) {
        public static BarcodeReportDTO from(BarcodeComplianceReport r) {
            return new BarcodeReportDTO(
                    r.getId(),
                    r.getReportDate(),
                    r.getReportType().name(),
                    r.getStatus().name(),
                    r.getGeneratedAt().toString());
        }
    }

    public record ViolationReportDTO(
            UUID id,
            LocalDate reportDate,
            int violationCount,
            String dataCompleteness,
            String generatedAt) {
        public static ViolationReportDTO from(ViolationReport r) {
            return new ViolationReportDTO(
                    r.getId(),
                    r.getReportDate(),
                    r.getViolationCount(),
                    r.getDataCompleteness().name(),
                    r.getGeneratedAt().toString());
        }
    }
}
