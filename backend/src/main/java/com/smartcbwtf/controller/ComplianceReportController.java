package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.ComplianceReportExportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Compliance Report Controller.
 * 
 * READ-ONLY endpoints. No POST for report generation.
 * Reports are generated automatically by scheduled jobs.
 */
@RestController
@RequestMapping("/api/cbwtf/compliance")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class ComplianceReportController {

    private final DailyComplianceReportRepository dailyReportRepository;
    private final MonthlyComplianceReportRepository monthlyReportRepository;
    private final AnnualComplianceReportRepository annualReportRepository;
    private final BarcodeComplianceReportRepository barcodeReportRepository;
    private final ViolationReportRepository violationReportRepository;
    private final ComplianceReportExportService reportExportService;

    public ComplianceReportController(
            DailyComplianceReportRepository dailyReportRepository,
            MonthlyComplianceReportRepository monthlyReportRepository,
            AnnualComplianceReportRepository annualReportRepository,
            BarcodeComplianceReportRepository barcodeReportRepository,
            ViolationReportRepository violationReportRepository,
            ComplianceReportExportService reportExportService) {
        this.dailyReportRepository = dailyReportRepository;
        this.monthlyReportRepository = monthlyReportRepository;
        this.annualReportRepository = annualReportRepository;
        this.barcodeReportRepository = barcodeReportRepository;
        this.violationReportRepository = violationReportRepository;
        this.reportExportService = reportExportService;
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
                pageRequest(page, size, 30, Sort.by(Sort.Direction.DESC, "reportDate")));
        return ResponseEntity.ok(reports.map(DailyReportDTO::from));
    }

    @GetMapping("/daily/{id}")
    public ResponseEntity<DailyReportDetailDTO> getDailyReport(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return dailyReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(DailyReportDetailDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/daily/{id}/pdf")
    public ResponseEntity<byte[]> downloadDailyReportPdf(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return dailyReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(r -> {
                    byte[] pdf = r.getPdfBytes();
                    if (pdf == null || pdf.length == 0) {
                        pdf = reportExportService.dailyPdf(r);
                        r.setPdfBytes(pdf);
                        dailyReportRepository.save(r);
                    }
                    return pdfResponse(pdf, "daily_report_" + r.getReportDate() + ".pdf");
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
                pageRequest(page, size, 12, Sort.by(Sort.Direction.DESC, "reportMonth")));
        return ResponseEntity.ok(reports.map(MonthlyReportDTO::from));
    }

    @GetMapping("/monthly/{id}/pdf")
    public ResponseEntity<byte[]> downloadMonthlyReportPdf(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return monthlyReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(r -> {
                    byte[] pdf = r.getPdfBytes();
                    if (pdf == null || pdf.length == 0) {
                        pdf = reportExportService.monthlyPdf(r);
                        r.setPdfBytes(pdf);
                        monthlyReportRepository.save(r);
                    }
                    return pdfResponse(pdf, "monthly_report_" + r.getReportMonth() + ".pdf");
                })
                .orElse(ResponseEntity.notFound().build());
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
                pageRequest(page, size, 10, Sort.by(Sort.Direction.DESC, "financialYear")));
        return ResponseEntity.ok(reports.map(AnnualReportDTO::from));
    }

    @GetMapping("/annual/{id}/excel")
    public ResponseEntity<byte[]> downloadAnnualReportExcel(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return annualReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(r -> {
                    byte[] excel = r.getExcelBytes();
                    if (excel == null || excel.length == 0) {
                        excel = reportExportService.annualExcel(r);
                        r.setExcelBytes(excel);
                        annualReportRepository.save(r);
                    }
                    String filename = "form_iv_" + r.getFinancialYear() + ".xlsx";
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.noStore())
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                            .contentType(MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                            .body(excel);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/annual/{id}/pdf")
    public ResponseEntity<byte[]> downloadAnnualReportPdf(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return annualReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(r -> {
                    byte[] pdf = r.getPdfBytes();
                    if (pdf == null || pdf.length == 0) {
                        pdf = reportExportService.annualPdf(r);
                        r.setPdfBytes(pdf);
                        annualReportRepository.save(r);
                    }
                    return pdfResponse(pdf, "form_iv_" + r.getFinancialYear() + ".pdf");
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
                pageRequest(page, size, 30, Sort.by(Sort.Direction.DESC, "reportDate")));
        return ResponseEntity.ok(reports.map(BarcodeReportDTO::from));
    }

    @GetMapping("/barcode/{id}/pdf")
    public ResponseEntity<byte[]> downloadBarcodeReportPdf(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return barcodeReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(r -> {
                    byte[] pdf = r.getPdfBytes();
                    if (pdf == null || pdf.length == 0) {
                        pdf = reportExportService.barcodePdf(r);
                        r.setPdfBytes(pdf);
                        barcodeReportRepository.save(r);
                    }
                    return pdfResponse(pdf, "barcode_report_" + r.getReportDate() + ".pdf");
                })
                .orElse(ResponseEntity.notFound().build());
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
                pageRequest(page, size, 30, Sort.by(Sort.Direction.DESC, "reportDate")));
        return ResponseEntity.ok(reports.map(ViolationReportDTO::from));
    }

    @GetMapping("/violations/{id}/pdf")
    public ResponseEntity<byte[]> downloadViolationReportPdf(@PathVariable UUID id) {
        UUID facilityId = TenantContext.getTenantId();
        return violationReportRepository.findByIdAndFacilityId(id, facilityId)
                .map(r -> {
                    byte[] pdf = r.getPdfBytes();
                    if (pdf == null || pdf.length == 0) {
                        pdf = reportExportService.violationPdf(r);
                        r.setPdfBytes(pdf);
                        violationReportRepository.save(r);
                    }
                    return pdfResponse(pdf, "violation_report_" + r.getReportDate() + ".pdf");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
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
                    true);
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
                    true,
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
