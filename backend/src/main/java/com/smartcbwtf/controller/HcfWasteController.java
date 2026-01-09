package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BagEvent;
import com.smartcbwtf.repository.BagEventRepository;
import com.smartcbwtf.service.HcfAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        // Calculate day boundaries in UTC
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<BagEvent> events = bagEventRepo.findByHcfIdAndEventTsBetween(hcfId, startOfDay, endOfDay);

        // Calculate summary stats
        BigDecimal totalWeight = events.stream()
                .map(BagEvent::getWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by category
        Map<String, List<BagEvent>> byCategory = events.stream()
                .filter(e -> e.getBagLabel() != null)
                .collect(Collectors.groupingBy(e -> e.getBagLabel().getCategory()));

        Map<String, Object> categorySummary = byCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Map.of(
                                "count", e.getValue().size(),
                                "weightKg", e.getValue().stream()
                                        .map(BagEvent::getWeightKg)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))));

        log.debug("Daily waste query for HCF {} on {}: {} events", hcfId, date, events.size());

        return ResponseEntity.ok(Map.of(
                "date", date.toString(),
                "totalEvents", events.size(),
                "totalWeightKg", totalWeight,
                "byCategory", categorySummary,
                "events", events.stream().map(e -> Map.of(
                        "id", e.getId().toString(),
                        "eventType", e.getEventType(),
                        "timestamp", e.getEventTs().toString(),
                        "category", e.getBagLabel() != null ? e.getBagLabel().getCategory() : "UNKNOWN",
                        "weightKg", e.getWeightKg(),
                        "anomalyState", e.getAnomalyState() != null ? e.getAnomalyState() : "OK")).toList()));
    }

    /**
     * Get waste summary for last 7 days.
     * This is allowed as a quick overview (still day-wise, not aggregated).
     */
    @GetMapping("/week-summary")
    public ResponseEntity<?> getWeekSummary() {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> days = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            List<BagEvent> events = bagEventRepo.findByHcfIdAndEventTsBetween(hcfId, start, end);
            BigDecimal totalWeight = events.stream()
                    .map(BagEvent::getWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            days.add(Map.of(
                    "date", date.toString(),
                    "dayOfWeek", date.getDayOfWeek().toString(),
                    "eventCount", events.size(),
                    "totalWeightKg", totalWeight));
        }

        return ResponseEntity.ok(Map.of(
                "startDate", today.minusDays(6).toString(),
                "endDate", today.toString(),
                "days", days));
    }
}
