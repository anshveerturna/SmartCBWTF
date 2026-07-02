package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HCF Waste Controller - Daily waste data view for HCF admins.
 * 
 * Regulatory restrictions:
 * - Only day-wise data allowed (no aggregation)
 * - Scoped to authenticated HCF only
 * - No export functionality (until dues cleared - Phase 4)
 */
@RestController
@RequestMapping("/api/hcf/waste")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfWasteController {

    private static final Logger log = LoggerFactory.getLogger(HcfWasteController.class);
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final String UNKNOWN_CATEGORY = "UNKNOWN";
    private static final int DEFAULT_DAILY_EVENT_LIMIT = 200;
    private static final int MAX_DAILY_EVENT_LIMIT = 500;

    private final BagEventRepository bagEventRepo;
    private final HcfAccessGuard accessGuard;

    public HcfWasteController(BagEventRepository bagEventRepo, HcfAccessGuard accessGuard) {
        this.bagEventRepo = bagEventRepo;
        this.accessGuard = accessGuard;
    }

    /**
     * Get waste collection data for a single day.
     * 
     * @param date The date to query (YYYY-MM-DD format)
     * @return List of waste collection events for that day
     */
    @GetMapping("/daily")
    public ResponseEntity<?> getDailyWaste(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "eventLimit", defaultValue = "200") int eventLimit) {

        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);
        int safeEventLimit = PaginationUtils.normalizeSize(eventLimit, DEFAULT_DAILY_EVENT_LIMIT,
                MAX_DAILY_EVENT_LIMIT);

        Instant startOfDay = startOfDay(date);
        Instant endOfDay = startOfDay(date.plusDays(1));

        long totalEvents = bagEventRepo.countByFacilityIdAndHcfIdAndEventTsBetween(
                facilityId, hcfId, startOfDay, endOfDay);
        BigDecimal totalWeight = bagEventRepo.sumWeightByFacilityIdAndHcfIdAndEventTsBetween(
                facilityId, hcfId, startOfDay, endOfDay);

        Map<String, Object> categorySummary = bagEventRepo
                .countAndSumWeightGroupedByCategoryForFacilityAndHcfBetweenIncludingUnknown(
                        facilityId, hcfId, startOfDay, endOfDay)
                .stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? row[0].toString() : UNKNOWN_CATEGORY,
                        row -> Map.of(
                                "count", row[1] instanceof Number number ? number.longValue() : 0L,
                                "weightKg", row[2] instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO)));

        List<BagEvent> events = bagEventRepo.findByFacilityIdAndHcfIdAndEventTsBetweenOrderByEventTsDesc(
                facilityId, hcfId, startOfDay, endOfDay, PageRequest.of(0, safeEventLimit));

        log.debug("Daily waste query for HCF {} on {}: {} events", hcfId, date, totalEvents);

        return ResponseEntity.ok(Map.of(
                "date", date.toString(),
                "totalEvents", totalEvents,
                "totalWeightKg", totalWeight,
                "byCategory", categorySummary,
                "eventLimit", safeEventLimit,
                "events", events.stream().map(e -> Map.of(
                        "id", e.getId().toString(),
                        "eventType", e.getEventType(),
                        "timestamp", e.getEventTs().toString(),
                        "category", categoryOf(e),
                        "weightKg", weightOf(e),
                        "anomalyState", e.getAnomalyState() != null ? e.getAnomalyState() : "OK")).toList()));
    }

    /**
     * Get waste summary for last 7 days.
     * This is allowed as a quick overview (still day-wise, not aggregated).
     */
    @GetMapping("/week-summary")
    public ResponseEntity<?> getWeekSummary() {
        UUID hcfId = TenantContext.getHcfId();
        UUID facilityId = TenantContext.getTenantId();
        accessGuard.assertPortalAccess(hcfId, facilityId);

        LocalDate today = LocalDate.now(REPORT_ZONE);
        LocalDate startDate = today.minusDays(6);
        Instant start = startOfDay(startDate);
        Instant end = startOfDay(today.plusDays(1));
        List<BagEvent> events = bagEventRepo.findByFacilityIdAndHcfIdAndEventTsBetween(
                facilityId, hcfId, start, end);
        Map<LocalDate, List<BagEvent>> eventsByDay = events.stream()
                .collect(Collectors.groupingBy(event -> event.getEventTs().atZone(REPORT_ZONE).toLocalDate()));

        List<Map<String, Object>> days = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<BagEvent> dayEvents = eventsByDay.getOrDefault(date, Collections.emptyList());
            BigDecimal totalWeight = dayEvents.stream()
                    .map(HcfWasteController::weightOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            days.add(Map.of(
                    "date", date.toString(),
                    "dayOfWeek", date.getDayOfWeek().toString(),
                    "eventCount", dayEvents.size(),
                    "totalWeightKg", totalWeight));
        }

        return ResponseEntity.ok(Map.of(
                "startDate", today.minusDays(6).toString(),
                "endDate", today.toString(),
                "days", days));
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(REPORT_ZONE).toInstant();
    }

    private static BigDecimal weightOf(BagEvent event) {
        return event.getWeightKg() != null ? event.getWeightKg() : BigDecimal.ZERO;
    }

    private String categoryOf(BagEvent event) {
        if (event.getBagLabel() == null || event.getBagLabel().getCategory() == null
                || event.getBagLabel().getCategory().isBlank()) {
            return UNKNOWN_CATEGORY;
        }
        return event.getBagLabel().getCategory();
    }
}
