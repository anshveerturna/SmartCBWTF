package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for aggregating waste data into snapshot tables.
 * Runs scheduled jobs to pre-compute analytics for fast dashboard queries.
 */
@Service
public class AnalyticsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationService.class);
    private static final int FACILITY_PAGE_SIZE = 100;

    private final BagEventRepository bagEventRepository;
    private final FacilityRepository facilityRepository;
    private final DailyWasteSnapshotRepository dailySnapshotRepository;
    private final MonthlyWasteSnapshotRepository monthlySnapshotRepository;

    public AnalyticsAggregationService(
            BagEventRepository bagEventRepository,
            FacilityRepository facilityRepository,
            DailyWasteSnapshotRepository dailySnapshotRepository,
            MonthlyWasteSnapshotRepository monthlySnapshotRepository) {
        this.bagEventRepository = bagEventRepository;
        this.facilityRepository = facilityRepository;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
    }

    /**
     * Aggregate yesterday's data into daily snapshots.
     * Runs every day at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void aggregateDailySnapshots() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily snapshot aggregation for {}", yesterday);

        try {
            aggregateDailyForDate(yesterday);
            log.info("Completed daily snapshot aggregation for {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to aggregate daily snapshots for {}", yesterday, e);
        }
    }

    /**
     * Aggregate previous month's data into monthly snapshots.
     * Runs on the 1st of each month at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    @Transactional
    public void aggregateMonthlySnapshots() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        log.info("Starting monthly snapshot aggregation for {}", lastMonth);

        try {
            aggregateMonthlyForMonth(lastMonth);
            log.info("Completed monthly snapshot aggregation for {}", lastMonth);
        } catch (Exception e) {
            log.error("Failed to aggregate monthly snapshots for {}", lastMonth, e);
        }
    }

    /**
     * Aggregate daily data for a specific date.
     * Can be called manually for backfilling.
     */
    @Transactional
    public void aggregateDailyForDate(LocalDate date) {
        forEachFacility(facility -> aggregateDailyForFacility(facility, date));
    }

    /**
     * Aggregate daily data for all HCFs of a facility.
     */
    private void aggregateDailyForFacility(Facility facility, LocalDate date) {
        // Convert to Instant for query
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Get all bag events for this facility on this date
        List<BagEvent> events = bagEventRepository.findByFacilityIdAndEventTsBetween(
                facility.getId(), startOfDay, endOfDay);

        if (events.isEmpty()) {
            return; // No data to aggregate
        }

        // Group events by HCF
        Map<UUID, List<BagEvent>> eventsByHcf = events.stream()
                .filter(e -> e.getHcf() != null)
                .collect(Collectors.groupingBy(e -> e.getHcf().getId()));

        for (Map.Entry<UUID, List<BagEvent>> entry : eventsByHcf.entrySet()) {
            Hcf hcf = entry.getValue().get(0).getHcf();
            aggregateEventsForHcf(facility, hcf, date, entry.getValue());
        }
    }

    /**
     * Aggregate events for a specific HCF on a date.
     */
    private void aggregateEventsForHcf(Facility facility, Hcf hcf, LocalDate date, List<BagEvent> events) {
        // Aggregate counts
        int totalBags = 0, yellowBags = 0, redBags = 0, blueBags = 0, whiteBags = 0;
        long totalWeight = 0, yellowWeight = 0, redWeight = 0, blueWeight = 0, whiteWeight = 0;
        int verifiedBags = 0, discrepancyCount = 0;

        for (BagEvent event : events) {
            // Count collection events
            if ("HCF_COLLECTION".equals(event.getEventType())) {
                totalBags++;
                long weight = event.getWeightKg() != null
                        ? event.getWeightKg().multiply(BigDecimal.valueOf(1000)).longValue()
                        : 0;
                totalWeight += weight;

                BagLabel label = event.getBagLabel();
                if (label != null) {
                    String category = label.getCategory();
                    switch (category) {
                        case "YELLOW" -> {
                            yellowBags++;
                            yellowWeight += weight;
                        }
                        case "RED" -> {
                            redBags++;
                            redWeight += weight;
                        }
                        case "BLUE" -> {
                            blueBags++;
                            blueWeight += weight;
                        }
                        case "WHITE" -> {
                            whiteBags++;
                            whiteWeight += weight;
                        }
                    }
                }
            }

            // Count verification events
            if ("CBWTF_VERIFICATION".equals(event.getEventType())) {
                verifiedBags++;
                // Check for anomalies
                if (event.getAnomalyState() != null && !"OK".equals(event.getAnomalyState())) {
                    discrepancyCount++;
                }
            }
        }

        // Upsert snapshot
        DailyWasteSnapshot snapshot = dailySnapshotRepository
                .findByHcfIdAndSnapshotDate(hcf.getId(), date)
                .orElseGet(() -> {
                    DailyWasteSnapshot s = new DailyWasteSnapshot();
                    s.setFacility(facility);
                    s.setHcf(hcf);
                    s.setSnapshotDate(date);
                    return s;
                });

        snapshot.setTotalBags(totalBags);
        snapshot.setYellowBags(yellowBags);
        snapshot.setRedBags(redBags);
        snapshot.setBlueBags(blueBags);
        snapshot.setWhiteBags(whiteBags);
        snapshot.setTotalWeightGrams(totalWeight);
        snapshot.setYellowWeightGrams(yellowWeight);
        snapshot.setRedWeightGrams(redWeight);
        snapshot.setBlueWeightGrams(blueWeight);
        snapshot.setWhiteWeightGrams(whiteWeight);
        snapshot.setVerifiedBags(verifiedBags);
        snapshot.setDiscrepancyCount(discrepancyCount);
        snapshot.setUpdatedAt(LocalDateTime.now());

        dailySnapshotRepository.save(snapshot);
    }

    /**
     * Aggregate monthly data for a specific month.
     */
    @Transactional
    public void aggregateMonthlyForMonth(LocalDate firstDayOfMonth) {
        LocalDate lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1);
        forEachFacility(facility -> aggregateMonthlyForFacility(facility, firstDayOfMonth, lastDayOfMonth));
    }

    private void forEachFacility(Consumer<Facility> consumer) {
        int pageNumber = 0;
        Page<Facility> page;
        do {
            page = facilityRepository.findAll(PageRequest.of(pageNumber++, FACILITY_PAGE_SIZE));
            page.forEach(consumer);
        } while (page.hasNext());
    }

    /**
     * Aggregate monthly data for a specific facility.
     */
    private void aggregateMonthlyForFacility(Facility facility, LocalDate startDate, LocalDate endDate) {
        // Get all daily snapshots for this facility in the month
        List<DailyWasteSnapshot> dailySnapshots = dailySnapshotRepository
                .findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        facility.getId(), startDate, endDate);

        if (dailySnapshots.isEmpty()) {
            return;
        }

        // Aggregate from daily snapshots
        int totalBags = 0, yellowBags = 0, redBags = 0, blueBags = 0, whiteBags = 0;
        long totalWeight = 0, yellowWeight = 0, redWeight = 0, blueWeight = 0, whiteWeight = 0;
        int verifiedBags = 0, discrepancyCount = 0;
        long activeHcfCount = dailySnapshots.stream()
                .map(s -> s.getHcf().getId())
                .distinct()
                .count();

        for (DailyWasteSnapshot daily : dailySnapshots) {
            totalBags += daily.getTotalBags();
            yellowBags += daily.getYellowBags();
            redBags += daily.getRedBags();
            blueBags += daily.getBlueBags();
            whiteBags += daily.getWhiteBags();
            totalWeight += daily.getTotalWeightGrams();
            yellowWeight += daily.getYellowWeightGrams();
            redWeight += daily.getRedWeightGrams();
            blueWeight += daily.getBlueWeightGrams();
            whiteWeight += daily.getWhiteWeightGrams();
            verifiedBags += daily.getVerifiedBags();
            discrepancyCount += daily.getDiscrepancyCount();
        }

        // Calculate percentages
        BigDecimal bluePercentage = totalBags > 0
                ? BigDecimal.valueOf(blueBags).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalBags), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal verifiedPercentage = totalBags > 0
                ? BigDecimal.valueOf(verifiedBags).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalBags), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Upsert monthly snapshot
        MonthlyWasteSnapshot snapshot = monthlySnapshotRepository
                .findByFacilityIdAndSnapshotMonth(facility.getId(), startDate)
                .orElseGet(() -> {
                    MonthlyWasteSnapshot s = new MonthlyWasteSnapshot();
                    s.setFacility(facility);
                    s.setSnapshotMonth(startDate);
                    return s;
                });

        snapshot.setTotalHcfsActive((int) activeHcfCount);
        snapshot.setTotalPickups(dailySnapshots.size());
        snapshot.setTotalBags(totalBags);
        snapshot.setYellowBags(yellowBags);
        snapshot.setRedBags(redBags);
        snapshot.setBlueBags(blueBags);
        snapshot.setWhiteBags(whiteBags);
        snapshot.setTotalWeightGrams(totalWeight);
        snapshot.setYellowWeightGrams(yellowWeight);
        snapshot.setRedWeightGrams(redWeight);
        snapshot.setBlueWeightGrams(blueWeight);
        snapshot.setWhiteWeightGrams(whiteWeight);
        snapshot.setBlueWastePercentage(bluePercentage);
        snapshot.setVerifiedPercentage(verifiedPercentage);
        snapshot.setDiscrepancyCount(discrepancyCount);
        snapshot.setUpdatedAt(LocalDateTime.now());

        monthlySnapshotRepository.save(snapshot);
    }

    /**
     * Backfill snapshots for a date range.
     * Useful for initial setup or data recovery.
     */
    @Transactional
    public void backfillDailySnapshots(LocalDate startDate, LocalDate endDate) {
        log.info("Backfilling daily snapshots from {} to {}", startDate, endDate);
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            aggregateDailyForDate(current);
            current = current.plusDays(1);
        }
        log.info("Completed backfill of daily snapshots");
    }
}
