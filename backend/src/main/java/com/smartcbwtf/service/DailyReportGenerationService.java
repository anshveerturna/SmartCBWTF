package com.smartcbwtf.service;

import com.smartcbwtf.domain.DailyComplianceReport;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.DailyComplianceReportRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Daily Report Generation Service.
 * 
 * Generates immutable daily compliance reports.
 * Called by scheduler at 01:00 AM IST for previous day.
 */
@Service
public class DailyReportGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DailyReportGenerationService.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final String REPORT_TYPE = "DAILY";
    private static final int FACILITY_PAGE_SIZE = 100;

    private final DailyComplianceReportRepository reportRepository;
    private final ReportGenerationLockService lockService;
    private final FacilityRepository facilityRepository;
    private final ComplianceDataAggregator aggregator;
    private final ComplianceReportExportService reportExportService;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;

    public DailyReportGenerationService(
            DailyComplianceReportRepository reportRepository,
            ReportGenerationLockService lockService,
            FacilityRepository facilityRepository,
            ComplianceDataAggregator aggregator,
            ComplianceReportExportService reportExportService,
            AuditLogService auditLogService,
            TransactionTemplate transactionTemplate) {
        this.reportRepository = reportRepository;
        this.lockService = lockService;
        this.facilityRepository = facilityRepository;
        this.aggregator = aggregator;
        this.reportExportService = reportExportService;
        this.auditLogService = auditLogService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Generate daily report for a facility.
     * 
     * @param facilityId Target facility
     * @param reportDate Date to generate report for
     * @return true if report was generated, false if already exists
     */
    public boolean generateReport(UUID facilityId, LocalDate reportDate) {
        String periodKey = reportDate.toString();

        if (!lockService.acquire(REPORT_TYPE, periodKey, facilityId)) {
            log.info("Daily report generation already in progress for facility {} date {}", facilityId, reportDate);
            return false;
        }

        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> generateReportLocked(facilityId, reportDate)));
        } catch (DataIntegrityViolationException e) {
            log.warn("Daily report already generated concurrently for facility {} date {}", facilityId, reportDate);
            return false;
        } finally {
            lockService.release(REPORT_TYPE, periodKey, facilityId);
        }
    }

    private boolean generateReportLocked(UUID facilityId, LocalDate reportDate) {
        // Check if already exists
        if (reportRepository.existsByFacilityIdAndReportDate(facilityId, reportDate)) {
            log.info("Daily report already exists for facility {} date {}", facilityId, reportDate);
            return false;
        }

        // Calculate source window (full day in IST)
        ZonedDateTime dayStart = reportDate.atStartOfDay(IST);
        ZonedDateTime dayEnd = dayStart.plusDays(1).minusNanos(1);
        Instant sourceFrom = dayStart.toInstant();
        Instant sourceTo = dayEnd.toInstant();

        // Load facility
        Facility facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility == null) {
            log.error("Facility not found: {}", facilityId);
            return false;
        }

        // Aggregate data
        var aggregation = aggregator.aggregateDaily(facilityId, reportDate, sourceFrom, sourceTo);

        // Convert to JSON
        String dataJson = aggregator.toJson(aggregation);

        // Compute checksum
        String checksum = computeChecksum(dataJson);

        // Determine status and completeness
        DailyComplianceReport.Status status = aggregation.hasViolations()
                ? DailyComplianceReport.Status.FLAGGED
                : DailyComplianceReport.Status.READY;

        DailyComplianceReport.DataCompleteness completeness = aggregation.unverifiedBags() > 0
                ? DailyComplianceReport.DataCompleteness.PARTIAL
                : DailyComplianceReport.DataCompleteness.COMPLETE;

        // Create report
        DailyComplianceReport report = new DailyComplianceReport();
        report.setFacility(facility);
        report.setReportDate(reportDate);
        report.setReportVersion(1);
        report.setGeneratedAt(Instant.now());
        report.setStatus(status);
        report.setDataCompleteness(completeness);
        report.setSourceWindowFrom(sourceFrom);
        report.setSourceWindowTo(sourceTo);
        report.setDataJson(dataJson);
        report.setChecksum(checksum);
        report.setCreatedBy("SYSTEM");
        report.setPdfBytes(reportExportService.dailyPdf(report));

        reportRepository.save(report);

        // Audit log
        auditLogService.log("DailyComplianceReport", report.getId(), "REPORT_GENERATED", null,
                String.format("Daily report for %s status=%s", reportDate, status));

        log.info("Generated daily report for facility {} date {} status={}", facilityId, reportDate, status);
        return true;
    }

    /**
     * Generate reports for all facilities for a given date.
     */
    public int generateReportsForAllFacilities(LocalDate reportDate) {
        int count = 0;
        int pageNumber = 0;
        Page<Facility> page;

        do {
            page = facilityRepository.findAll(PageRequest.of(pageNumber++, FACILITY_PAGE_SIZE));
            for (var facility : page) {
                try {
                    if (generateReport(facility.getId(), reportDate)) {
                        count++;
                    }
                } catch (Exception e) {
                    log.error("Failed to generate daily report for facility {}: {}", facility.getId(), e.getMessage());
                }
            }
        } while (page.hasNext());

        log.info("Generated {} daily reports for date {}", count, reportDate);
        return count;
    }

    private String computeChecksum(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }
}
