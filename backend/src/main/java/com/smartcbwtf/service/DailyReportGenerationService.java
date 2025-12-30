package com.smartcbwtf.service;

import com.smartcbwtf.domain.DailyComplianceReport;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.ReportGenerationLock;
import com.smartcbwtf.repository.DailyComplianceReportRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.ReportGenerationLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final DailyComplianceReportRepository reportRepository;
    private final ReportGenerationLockRepository lockRepository;
    private final FacilityRepository facilityRepository;
    private final ComplianceDataAggregator aggregator;
    private final AuditLogService auditLogService;

    public DailyReportGenerationService(
            DailyComplianceReportRepository reportRepository,
            ReportGenerationLockRepository lockRepository,
            FacilityRepository facilityRepository,
            ComplianceDataAggregator aggregator,
            AuditLogService auditLogService) {
        this.reportRepository = reportRepository;
        this.lockRepository = lockRepository;
        this.facilityRepository = facilityRepository;
        this.aggregator = aggregator;
        this.auditLogService = auditLogService;
    }

    /**
     * Generate daily report for a facility.
     * 
     * @param facilityId Target facility
     * @param reportDate Date to generate report for
     * @return true if report was generated, false if already exists
     */
    @Transactional
    public boolean generateReport(UUID facilityId, LocalDate reportDate) {
        String periodKey = reportDate.toString();

        // Check if already exists
        if (reportRepository.existsByFacilityIdAndReportDate(facilityId, reportDate)) {
            log.info("Daily report already exists for facility {} date {}", facilityId, reportDate);
            return false;
        }

        // Acquire lock
        try {
            ReportGenerationLock lock = new ReportGenerationLock();
            lock.setReportType(REPORT_TYPE);
            lock.setPeriodKey(periodKey);
            lock.setFacilityId(facilityId);
            lock.setLockedAt(Instant.now());
            lockRepository.save(lock);
        } catch (Exception e) {
            log.warn("Failed to acquire lock for daily report {} {}: {}", facilityId, reportDate, e.getMessage());
            return false;
        }

        try {
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

            // TODO: Generate PDF bytes here for byte-identical re-downloads
            // report.setPdfBytes(generatePdf(report));

            reportRepository.save(report);

            // Audit log
            auditLogService.log("DailyComplianceReport", report.getId(), "REPORT_GENERATED", null,
                    String.format("Daily report for %s status=%s", reportDate, status));

            log.info("Generated daily report for facility {} date {} status={}", facilityId, reportDate, status);
            return true;

        } finally {
            // Release lock
            lockRepository.deleteByReportTypeAndPeriodKeyAndFacilityId(REPORT_TYPE, periodKey, facilityId);
        }
    }

    /**
     * Generate reports for all facilities for a given date.
     */
    @Transactional
    public int generateReportsForAllFacilities(LocalDate reportDate) {
        int count = 0;
        var facilities = facilityRepository.findAll();

        for (var facility : facilities) {
            try {
                if (generateReport(facility.getId(), reportDate)) {
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to generate daily report for facility {}: {}", facility.getId(), e.getMessage());
            }
        }

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
