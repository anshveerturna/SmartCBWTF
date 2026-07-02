package com.smartcbwtf.service;

import com.smartcbwtf.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Monthly Billing Scheduler.
 * 
 * Runs on the 1st of each month at 2:00 AM IST.
 * Generates bills for all active facilities for the previous month.
 */
@Service
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);
    private static final int FACILITY_PAGE_SIZE = 100;

    private final BillGenerationService billGenerationService;
    private final FacilityRepository facilityRepository;

    public BillingScheduler(
            BillGenerationService billGenerationService,
            FacilityRepository facilityRepository) {
        this.billGenerationService = billGenerationService;
        this.facilityRepository = facilityRepository;
    }

    /**
     * Scheduled job: Generate bills for all facilities.
     * Runs at 2:00 AM on the 1st of every month (IST).
     * 
     * Cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Kolkata")
    public void generateMonthlyBills() {
        log.info("Starting scheduled monthly billing...");

        // Bill for previous month
        LocalDate billingMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);

        int totalBills = 0;
        int successFacilities = 0;
        int failedFacilities = 0;

        FacilityBillingTotals totals = processFacilities(facility -> {
            try {
                int bills = billGenerationService.generateBillsForMonth(
                        facility.getId(),
                        billingMonth,
                        null // System triggered
                );
                log.info("Generated {} bills for facility {}", bills, facility.getName());
                return new FacilityBillingTotals(bills, 1, 0);
            } catch (Exception e) {
                log.error("Failed billing for facility {}: {}", facility.getName(), e.getMessage());
                return new FacilityBillingTotals(0, 0, 1);
            }
        });

        log.info("Monthly billing complete: {} bills, {} facilities success, {} failed",
                totals.totalBills(), totals.successFacilities(), totals.failedFacilities());
    }

    private FacilityBillingTotals processFacilities(
            Function<com.smartcbwtf.domain.Facility, FacilityBillingTotals> processor) {
        int pageNumber = 0;
        int totalBills = 0;
        int successFacilities = 0;
        int failedFacilities = 0;
        Page<com.smartcbwtf.domain.Facility> page;

        do {
            page = facilityRepository.findAll(PageRequest.of(pageNumber++, FACILITY_PAGE_SIZE));
            for (var facility : page) {
                FacilityBillingTotals totals = processor.apply(facility);
                totalBills += totals.totalBills();
                successFacilities += totals.successFacilities();
                failedFacilities += totals.failedFacilities();
            }
        } while (page.hasNext());

        return new FacilityBillingTotals(totalBills, successFacilities, failedFacilities);
    }

    private record FacilityBillingTotals(int totalBills, int successFacilities, int failedFacilities) {
    }
}
