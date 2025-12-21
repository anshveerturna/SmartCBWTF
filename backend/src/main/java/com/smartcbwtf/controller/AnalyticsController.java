package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.DailyWasteSnapshot;
import com.smartcbwtf.dto.AnalyticsResponse;
import com.smartcbwtf.dto.DashboardMetricsDTO;
import com.smartcbwtf.dto.CategoryBreakdownDTO;
import com.smartcbwtf.dto.TrendDataPointDTO;
import com.smartcbwtf.repository.DailyWasteSnapshotRepository;
import com.smartcbwtf.repository.MonthlyWasteSnapshotRepository;
import com.smartcbwtf.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final DailyWasteSnapshotRepository dailySnapshotRepository;
    private final MonthlyWasteSnapshotRepository monthlySnapshotRepository;

    public AnalyticsController(
            AnalyticsService analyticsService,
            DailyWasteSnapshotRepository dailySnapshotRepository,
            MonthlyWasteSnapshotRepository monthlySnapshotRepository) {
        this.analyticsService = analyticsService;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
    }

    // Existing endpoints
    @GetMapping("/hcf/{hcfId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public AnalyticsResponse hcf(@PathVariable UUID hcfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID facilityId = TenantContext.getTenantId();
        return analyticsService.hcfAnalytics(hcfId, facilityId, start, end);
    }

    @GetMapping("/facility/{facilityId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public AnalyticsResponse facility(@PathVariable UUID facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return analyticsService.facilityAnalytics(facilityId, start, end);
    }

    // New snapshot-based endpoints
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('CBWTF_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics(
            @RequestParam(name = "days", defaultValue = "30") int days) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null && !TenantContext.isSuperAdmin()) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        if (TenantContext.isSuperAdmin()) {
            return ResponseEntity.ok(createEmptyMetrics());
        }

        List<DailyWasteSnapshot> snapshots = dailySnapshotRepository
                .findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        tenantId, startDate, endDate);

        return ResponseEntity.ok(aggregateMetrics(snapshots));
    }

    @GetMapping("/my-hcf")
    @PreAuthorize("hasAnyRole('HCF_ADMIN', 'CBWTF_ADMIN')")
    public ResponseEntity<DashboardMetricsDTO> getMyHcfMetrics(
            @RequestParam(name = "days", defaultValue = "30") int days) {

        UUID hcfId = TenantContext.getHcfId();
        if (hcfId == null) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<DailyWasteSnapshot> snapshots = dailySnapshotRepository
                .findByHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        hcfId, startDate, endDate);

        return ResponseEntity.ok(aggregateMetrics(snapshots));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('CBWTF_ADMIN', 'HCF_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<TrendDataPointDTO>> getTrends(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "metric", defaultValue = "weight") String metric) {

        UUID tenantId = TenantContext.getTenantId();
        UUID hcfId = TenantContext.getHcfId();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        List<DailyWasteSnapshot> snapshots;
        if (hcfId != null) {
            snapshots = dailySnapshotRepository
                    .findByHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                            hcfId, startDate, endDate);
        } else if (tenantId != null) {
            snapshots = dailySnapshotRepository
                    .findByFacilityIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                            tenantId, startDate, endDate);
        } else {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<TrendDataPointDTO> trends = snapshots.stream()
                .collect(Collectors.groupingBy(DailyWasteSnapshot::getSnapshotDate))
                .entrySet().stream()
                .map(entry -> {
                    double value = entry.getValue().stream()
                            .mapToDouble(s -> switch (metric) {
                                case "weight" -> s.getTotalWeightKg();
                                case "bags" -> s.getTotalBags();
                                case "verified" -> s.getVerifiedBags();
                                default -> s.getTotalWeightKg();
                            })
                            .sum();
                    return new TrendDataPointDTO(entry.getKey().toString(), value, null);
                })
                .sorted(Comparator.comparing(TrendDataPointDTO::date))
                .collect(Collectors.toList());

        return ResponseEntity.ok(trends);
    }

    // Helper methods
    private DashboardMetricsDTO aggregateMetrics(List<DailyWasteSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return createEmptyMetrics();
        }

        int totalBags = snapshots.stream().mapToInt(DailyWasteSnapshot::getTotalBags).sum();
        long totalWeight = snapshots.stream().mapToLong(DailyWasteSnapshot::getTotalWeightGrams).sum();
        int verifiedBags = snapshots.stream().mapToInt(DailyWasteSnapshot::getVerifiedBags).sum();
        int discrepancy = snapshots.stream().mapToInt(DailyWasteSnapshot::getDiscrepancyCount).sum();
        int missing = snapshots.stream().mapToInt(DailyWasteSnapshot::getMissingBags).sum();

        int yellowBags = snapshots.stream().mapToInt(DailyWasteSnapshot::getYellowBags).sum();
        int redBags = snapshots.stream().mapToInt(DailyWasteSnapshot::getRedBags).sum();
        int blueBags = snapshots.stream().mapToInt(DailyWasteSnapshot::getBlueBags).sum();
        int whiteBags = snapshots.stream().mapToInt(DailyWasteSnapshot::getWhiteBags).sum();

        double bluePercentage = totalBags > 0 ? (blueBags * 100.0 / totalBags) : 0;

        List<CategoryBreakdownDTO> categories = List.of(
                new CategoryBreakdownDTO("YELLOW", yellowBags,
                        snapshots.stream().mapToLong(DailyWasteSnapshot::getYellowWeightGrams).sum() / 1000.0,
                        totalBags > 0 ? yellowBags * 100.0 / totalBags : 0),
                new CategoryBreakdownDTO("RED", redBags,
                        snapshots.stream().mapToLong(DailyWasteSnapshot::getRedWeightGrams).sum() / 1000.0,
                        totalBags > 0 ? redBags * 100.0 / totalBags : 0),
                new CategoryBreakdownDTO("BLUE", blueBags,
                        snapshots.stream().mapToLong(DailyWasteSnapshot::getBlueWeightGrams).sum() / 1000.0,
                        totalBags > 0 ? blueBags * 100.0 / totalBags : 0),
                new CategoryBreakdownDTO("WHITE", whiteBags,
                        snapshots.stream().mapToLong(DailyWasteSnapshot::getWhiteWeightGrams).sum() / 1000.0,
                        totalBags > 0 ? whiteBags * 100.0 / totalBags : 0));

        return new DashboardMetricsDTO(
                totalBags,
                totalWeight / 1000.0,
                verifiedBags,
                discrepancy,
                missing,
                bluePercentage,
                categories,
                0, 0, 0);
    }

    private DashboardMetricsDTO createEmptyMetrics() {
        return new DashboardMetricsDTO(0, 0, 0, 0, 0, 0, List.of(), 0, 0, 0);
    }
}
