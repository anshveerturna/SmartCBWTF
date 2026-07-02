package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartcbwtf.domain.AnnualComplianceReport;
import com.smartcbwtf.domain.BarcodeComplianceReport;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.MonthlyComplianceReport;
import com.smartcbwtf.domain.ViolationReport;
import com.smartcbwtf.repository.AnnualComplianceReportRepository;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.repository.BagLabelRepository;
import com.smartcbwtf.repository.BarcodeComplianceReportRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.MonthlyComplianceReportRepository;
import com.smartcbwtf.repository.ViolationReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class ComplianceReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReportGenerationService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int FACILITY_PAGE_SIZE = 100;

    private final MonthlyComplianceReportRepository monthlyReportRepository;
    private final AnnualComplianceReportRepository annualReportRepository;
    private final BarcodeComplianceReportRepository barcodeReportRepository;
    private final ViolationReportRepository violationReportRepository;
    private final ReportGenerationLockService lockService;
    private final FacilityRepository facilityRepository;
    private final BagEventRepository bagEventRepository;
    private final BagLabelRepository bagLabelRepository;
    private final ComplianceDataAggregator aggregator;
    private final AuditLogService auditLogService;
    private final ComplianceReportExportService exportService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public ComplianceReportGenerationService(
            MonthlyComplianceReportRepository monthlyReportRepository,
            AnnualComplianceReportRepository annualReportRepository,
            BarcodeComplianceReportRepository barcodeReportRepository,
            ViolationReportRepository violationReportRepository,
            ReportGenerationLockService lockService,
            FacilityRepository facilityRepository,
            BagEventRepository bagEventRepository,
            BagLabelRepository bagLabelRepository,
            ComplianceDataAggregator aggregator,
            AuditLogService auditLogService,
            ComplianceReportExportService exportService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.monthlyReportRepository = monthlyReportRepository;
        this.annualReportRepository = annualReportRepository;
        this.barcodeReportRepository = barcodeReportRepository;
        this.violationReportRepository = violationReportRepository;
        this.lockService = lockService;
        this.facilityRepository = facilityRepository;
        this.bagEventRepository = bagEventRepository;
        this.bagLabelRepository = bagLabelRepository;
        this.aggregator = aggregator;
        this.auditLogService = auditLogService;
        this.exportService = exportService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public int generateMonthlyReportsForAllFacilities(LocalDate monthStart) {
        return generateForAllFacilities(
                facility -> generateMonthlyReport(facility, monthStart),
                "monthly report");
    }

    public int generateAnnualReportsForAllFacilities(int fyStartYear) {
        return generateForAllFacilities(
                facility -> generateAnnualReport(facility, fyStartYear),
                "annual report");
    }

    public int generateBarcodeReportsForAllFacilities(LocalDate reportDate) {
        return generateForAllFacilities(
                facility -> generateBarcodeReport(facility, reportDate),
                "barcode report");
    }

    public int generateViolationReportsForAllFacilities(LocalDate reportDate) {
        return generateForAllFacilities(
                facility -> generateViolationReport(facility, reportDate),
                "violation report");
    }

    private int generateForAllFacilities(FacilityReportGenerator generator, String reportLabel) {
        int count = 0;
        int pageNumber = 0;
        Page<Facility> page;
        do {
            page = facilityRepository.findAll(PageRequest.of(pageNumber++, FACILITY_PAGE_SIZE));
            for (Facility facility : page) {
                try {
                    if (generator.generate(facility)) {
                        count++;
                    }
                } catch (Exception e) {
                    log.error("Failed to generate {} for facility {}: {}",
                            reportLabel, facility.getId(), e.getMessage(), e);
                }
            }
        } while (page.hasNext());
        return count;
    }

    @FunctionalInterface
    private interface FacilityReportGenerator {
        boolean generate(Facility facility);
    }

    private boolean generateMonthlyReport(Facility facility, LocalDate monthStart) {
        UUID facilityId = facility.getId();
        String periodKey = monthStart.toString();
        if (!lockService.acquire("MONTHLY", periodKey, facilityId)) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(
                    status -> generateMonthlyReportLocked(facility, monthStart)));
        } catch (DataIntegrityViolationException e) {
            log.warn("Monthly report already generated concurrently for facility {} month {}", facilityId, monthStart);
            return false;
        } finally {
            lockService.release("MONTHLY", periodKey, facilityId);
        }
    }

    private boolean generateMonthlyReportLocked(Facility facility, LocalDate monthStart) {
        UUID facilityId = facility.getId();
        if (monthlyReportRepository.existsByFacilityIdAndReportMonth(facilityId, monthStart)) {
            return false;
        }

        Instant from = monthStart.atStartOfDay(IST).toInstant();
        Instant to = monthStart.plusMonths(1).atStartOfDay(IST).toInstant();
        var data = aggregator.aggregateMonthly(facilityId, monthStart, from, to);
        String json = aggregator.toJson(data);

        MonthlyComplianceReport report = new MonthlyComplianceReport();
        report.setFacility(facility);
        report.setReportMonth(monthStart);
        report.setGeneratedAt(Instant.now());
        report.setStatus(MonthlyComplianceReport.Status.READY);
        report.setDataCompleteness(MonthlyComplianceReport.DataCompleteness.COMPLETE);
        report.setSourceWindowFrom(from);
        report.setSourceWindowTo(to);
        report.setDataJson(json);
        report.setChecksum(computeChecksum(json));
        monthlyReportRepository.save(report);
        auditLogService.log("MonthlyComplianceReport", report.getId(), "REPORT_GENERATED", null,
                "Monthly report for " + monthStart);
        return true;
    }

    private boolean generateAnnualReport(Facility facility, int fyStartYear) {
        UUID facilityId = facility.getId();
        String financialYear = fyStartYear + "-" + String.format("%02d", (fyStartYear + 1) % 100);
        if (!lockService.acquire("ANNUAL", financialYear, facilityId)) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(
                    status -> generateAnnualReportLocked(facility, fyStartYear)));
        } catch (DataIntegrityViolationException e) {
            log.warn("Annual report already generated concurrently for facility {} FY {}", facilityId, financialYear);
            return false;
        } finally {
            lockService.release("ANNUAL", financialYear, facilityId);
        }
    }

    private boolean generateAnnualReportLocked(Facility facility, int fyStartYear) {
        UUID facilityId = facility.getId();
        String financialYear = fyStartYear + "-" + String.format("%02d", (fyStartYear + 1) % 100);
        if (annualReportRepository.existsByFacilityIdAndFinancialYear(facilityId, financialYear)) {
            return false;
        }

        LocalDate fyStart = LocalDate.of(fyStartYear, 4, 1);
        LocalDate fyEndExclusive = fyStart.plusYears(1);
        Instant from = fyStart.atStartOfDay(IST).toInstant();
        Instant to = fyEndExclusive.atStartOfDay(IST).toInstant();
        var data = aggregator.aggregateMonthly(facilityId, fyStart, from, to);
        String json = annualJson(financialYear, data);

        AnnualComplianceReport report = new AnnualComplianceReport();
        report.setFacility(facility);
        report.setFinancialYear(financialYear);
        report.setGeneratedAt(Instant.now());
        report.setStatus(AnnualComplianceReport.Status.READY);
        report.setDataCompleteness(AnnualComplianceReport.DataCompleteness.COMPLETE);
        report.setSourceWindowFrom(from);
        report.setSourceWindowTo(to);
        report.setDataJson(json);
        report.setChecksum(computeChecksum(json));
        report.setExcelBytes(exportService.annualExcel(report));
        annualReportRepository.save(report);
        auditLogService.log("AnnualComplianceReport", report.getId(), "REPORT_GENERATED", null,
                "Annual report for FY " + financialYear);
        return true;
    }

    private boolean generateBarcodeReport(Facility facility, LocalDate reportDate) {
        UUID facilityId = facility.getId();
        String periodKey = reportDate.toString();
        if (!lockService.acquire("BARCODE_DAILY", periodKey, facilityId)) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(
                    status -> generateBarcodeReportLocked(facility, reportDate)));
        } catch (DataIntegrityViolationException e) {
            log.warn("Barcode report already generated concurrently for facility {} date {}", facilityId, reportDate);
            return false;
        } finally {
            lockService.release("BARCODE_DAILY", periodKey, facilityId);
        }
    }

    private boolean generateBarcodeReportLocked(Facility facility, LocalDate reportDate) {
        UUID facilityId = facility.getId();
        if (barcodeReportRepository.existsByFacilityIdAndReportDateAndReportType(
                facilityId, reportDate, BarcodeComplianceReport.ReportType.DAILY)) {
            return false;
        }

        Instant from = reportDate.atStartOfDay(IST).toInstant();
        Instant to = reportDate.plusDays(1).atStartOfDay(IST).toInstant();
        long labelsGenerated = bagLabelRepository.countByFacilityIdAndIssuedAtBetween(facilityId, from, to);
        long collections = bagEventRepository.countByFacilityIdAndEventTypeBetween(
                facilityId, "HCF_COLLECTION", from, to);
        long verifications = bagEventRepository.countByFacilityIdAndEventTypeBetween(
                facilityId, "CBWTF_VERIFICATION", from, to);
        long unverified = Math.max(0, collections - verifications);
        String json = barcodeJson(reportDate, from, to, labelsGenerated, collections, verifications, unverified);

        BarcodeComplianceReport report = new BarcodeComplianceReport();
        report.setFacility(facility);
        report.setReportDate(reportDate);
        report.setReportType(BarcodeComplianceReport.ReportType.DAILY);
        report.setGeneratedAt(Instant.now());
        report.setStatus(unverified > 0
                ? BarcodeComplianceReport.Status.FLAGGED
                : BarcodeComplianceReport.Status.READY);
        report.setDataCompleteness(BarcodeComplianceReport.DataCompleteness.COMPLETE);
        report.setSourceWindowFrom(from);
        report.setSourceWindowTo(to);
        report.setDataJson(json);
        report.setChecksum(computeChecksum(json));
        barcodeReportRepository.save(report);
        auditLogService.log("BarcodeComplianceReport", report.getId(), "REPORT_GENERATED", null,
                "Barcode report for " + reportDate);
        return true;
    }

    private boolean generateViolationReport(Facility facility, LocalDate reportDate) {
        UUID facilityId = facility.getId();
        String periodKey = reportDate.toString();
        if (!lockService.acquire("VIOLATION_DAILY", periodKey, facilityId)) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(
                    status -> generateViolationReportLocked(facility, reportDate)));
        } catch (DataIntegrityViolationException e) {
            log.warn("Violation report already generated concurrently for facility {} date {}", facilityId, reportDate);
            return false;
        } finally {
            lockService.release("VIOLATION_DAILY", periodKey, facilityId);
        }
    }

    private boolean generateViolationReportLocked(Facility facility, LocalDate reportDate) {
        UUID facilityId = facility.getId();
        if (violationReportRepository.existsByFacilityIdAndReportDate(facilityId, reportDate)) {
            return false;
        }

        Instant from = reportDate.atStartOfDay(IST).toInstant();
        Instant to = reportDate.plusDays(1).atStartOfDay(IST).toInstant();
        var data = aggregator.aggregateDaily(facilityId, reportDate, from, to);
        String json = aggregator.toJson(data);

        ViolationReport report = new ViolationReport();
        report.setFacility(facility);
        report.setReportDate(reportDate);
        report.setGeneratedAt(Instant.now());
        report.setViolationCount(data.violations().size());
        report.setDataCompleteness(data.unverifiedBags() > 0
                ? ViolationReport.DataCompleteness.PARTIAL
                : ViolationReport.DataCompleteness.COMPLETE);
        report.setSourceWindowFrom(from);
        report.setSourceWindowTo(to);
        report.setDataJson(json);
        report.setChecksum(computeChecksum(json));
        violationReportRepository.save(report);
        auditLogService.log("ViolationReport", report.getId(), "REPORT_GENERATED", null,
                "Violation report for " + reportDate);
        return true;
    }

    private String annualJson(String financialYear, ComplianceDataAggregator.MonthlyAggregation data) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("financialYear", financialYear);
            root.put("totalWasteKg", data.totalWasteKg());
            ObjectNode categories = root.putObject("categoryWise");
            data.categoryWiseKg().forEach(categories::put);
            root.put("hcfCount", data.hcfCount());
            ObjectNode sourceWindow = root.putObject("sourceWindow");
            sourceWindow.put("from", data.sourceFrom().toString());
            sourceWindow.put("to", data.sourceTo().toString());
            root.put("generatedAt", Instant.now().toString());
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize annual compliance report", e);
        }
    }

    private String barcodeJson(LocalDate reportDate, Instant from, Instant to, long labelsGenerated,
            long collections, long verifications, long unverified) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("reportDate", reportDate.toString());
            root.put("labelsGenerated", labelsGenerated);
            root.put("collections", collections);
            root.put("verifications", verifications);
            root.put("unverifiedCollections", unverified);
            ObjectNode sourceWindow = root.putObject("sourceWindow");
            sourceWindow.put("from", from.toString());
            sourceWindow.put("to", to.toString());
            root.put("generatedAt", Instant.now().toString());
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize barcode compliance report", e);
        }
    }

    private String computeChecksum(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }
}
