package com.smartcbwtf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Compliance Report Scheduler.
 * 
 * Generates reports automatically at configured times (IST):
 * - Daily reports: 01:00 AM
 * - Monthly reports: 1st of month, 01:30 AM
 * - Annual reports: April 1st, 02:00 AM
 * 
 * NO MANUAL GENERATION. Reports auto-exist.
 */
@Service
public class ComplianceReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReportScheduler.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyReportGenerationService dailyReportService;
    private final ComplianceReportGenerationService complianceReportGenerationService;

    public ComplianceReportScheduler(
            DailyReportGenerationService dailyReportService,
            ComplianceReportGenerationService complianceReportGenerationService) {
        this.dailyReportService = dailyReportService;
        this.complianceReportGenerationService = complianceReportGenerationService;
    }

    /**
     * Generate daily reports at 01:00 AM IST every day.
     * Generates report for PREVIOUS day.
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Kolkata")
    public void generateDailyReports() {
        log.info("=== DAILY COMPLIANCE REPORT GENERATION STARTED ===");

        LocalDate yesterday = LocalDate.now(IST).minusDays(1);

        try {
            int count = dailyReportService.generateReportsForAllFacilities(yesterday);
            log.info("=== DAILY REPORTS COMPLETE: {} reports generated for {} ===", count, yesterday);
        } catch (Exception e) {
            log.error("=== DAILY REPORT GENERATION FAILED: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Generate monthly reports on 1st of each month at 01:30 AM IST.
     * Generates report for PREVIOUS month.
     */
    @Scheduled(cron = "0 30 1 1 * ?", zone = "Asia/Kolkata")
    public void generateMonthlyReports() {
        log.info("=== MONTHLY COMPLIANCE REPORT GENERATION STARTED ===");

        LocalDate previousMonth = LocalDate.now(IST).minusMonths(1).withDayOfMonth(1);

        try {
            int count = complianceReportGenerationService.generateMonthlyReportsForAllFacilities(previousMonth);
            log.info("=== MONTHLY REPORTS COMPLETE: {} reports generated for {} ===", count, previousMonth);
        } catch (Exception e) {
            log.error("=== MONTHLY REPORT GENERATION FAILED: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Generate annual reports (Form IV) on April 1st at 02:00 AM IST.
     * Generates report for PREVIOUS financial year.
     */
    @Scheduled(cron = "0 0 2 1 4 ?", zone = "Asia/Kolkata")
    public void generateAnnualReports() {
        log.info("=== ANNUAL COMPLIANCE REPORT (FORM IV) GENERATION STARTED ===");

        // Calculate previous financial year
        LocalDate now = LocalDate.now(IST);
        int fyStartYear = now.getMonthValue() >= 4 ? now.getYear() - 1 : now.getYear() - 2;
        String financialYear = fyStartYear + "-" + String.format("%02d", (fyStartYear + 1) % 100);

        try {
            int count = complianceReportGenerationService.generateAnnualReportsForAllFacilities(fyStartYear);
            log.info("=== ANNUAL REPORTS COMPLETE: {} reports generated for FY {} ===", count, financialYear);
        } catch (Exception e) {
            log.error("=== ANNUAL REPORT GENERATION FAILED: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Generate barcode compliance reports daily at 01:15 AM IST.
     */
    @Scheduled(cron = "0 15 1 * * ?", zone = "Asia/Kolkata")
    public void generateBarcodeReports() {
        log.info("=== BARCODE COMPLIANCE REPORT GENERATION STARTED ===");

        LocalDate yesterday = LocalDate.now(IST).minusDays(1);

        try {
            int count = complianceReportGenerationService.generateBarcodeReportsForAllFacilities(yesterday);
            log.info("=== BARCODE REPORTS COMPLETE: {} reports generated for {} ===", count, yesterday);
        } catch (Exception e) {
            log.error("=== BARCODE REPORT GENERATION FAILED: {} ===", e.getMessage(), e);
        }
    }

    /**
     * Generate violation reports daily at 01:10 AM IST.
     */
    @Scheduled(cron = "0 10 1 * * ?", zone = "Asia/Kolkata")
    public void generateViolationReports() {
        log.info("=== VIOLATION REPORT GENERATION STARTED ===");

        LocalDate yesterday = LocalDate.now(IST).minusDays(1);

        try {
            int count = complianceReportGenerationService.generateViolationReportsForAllFacilities(yesterday);
            log.info("=== VIOLATION REPORTS COMPLETE: {} reports generated for {} ===", count, yesterday);
        } catch (Exception e) {
            log.error("=== VIOLATION REPORT GENERATION FAILED: {} ===", e.getMessage(), e);
        }
    }
}
