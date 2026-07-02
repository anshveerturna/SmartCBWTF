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
import com.smartcbwtf.service.HcfAccessGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final int MAX_ANALYTICS_RANGE_DAYS = 366;

    private final AnalyticsService analyticsService;
    private final DailyWasteSnapshotRepository dailySnapshotRepository;
    private final MonthlyWasteSnapshotRepository monthlySnapshotRepository;
    private final com.smartcbwtf.service.AnalyticsPageService analyticsPageService;
    private final HcfAccessGuard hcfAccessGuard;

    public AnalyticsController(
            AnalyticsService analyticsService,
            DailyWasteSnapshotRepository dailySnapshotRepository,
            MonthlyWasteSnapshotRepository monthlySnapshotRepository,
            com.smartcbwtf.service.AnalyticsPageService analyticsPageService,
            HcfAccessGuard hcfAccessGuard) {
        this.analyticsService = analyticsService;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
        this.analyticsPageService = analyticsPageService;
        this.hcfAccessGuard = hcfAccessGuard;
    }

    // Existing endpoints
    @GetMapping("/hcf/{hcfId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public AnalyticsResponse hcf(@PathVariable UUID hcfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID facilityId = requireTenantId();
        validateDateRange(start, end);
        return analyticsService.hcfAnalytics(hcfId, facilityId, start, end);
    }

    @GetMapping("/facility/{facilityId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public AnalyticsResponse facility(@PathVariable UUID facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        UUID tenantFacilityId = requireTenantId();
        if (!tenantFacilityId.equals(facilityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analytics not found");
        }
        validateDateRange(start, end);
        return analyticsService.facilityAnalytics(tenantFacilityId, start, end);
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
        LocalDate startDate = endDate.minusDays(validateDaysWindow(days));

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
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.badRequest().build();
        }
        hcfAccessGuard.assertPortalAccess(hcfId, facilityId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(validateDaysWindow(days));

        List<DailyWasteSnapshot> snapshots = dailySnapshotRepository
                .findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                        facilityId, hcfId, startDate, endDate);

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
        LocalDate startDate = endDate.minusDays(validateDaysWindow(days));

        List<DailyWasteSnapshot> snapshots;
        if (hcfId != null) {
            if (tenantId == null) {
                return ResponseEntity.badRequest().build();
            }
            hcfAccessGuard.assertPortalAccess(hcfId, tenantId);
            snapshots = dailySnapshotRepository
                    .findByFacilityIdAndHcfIdAndSnapshotDateBetweenOrderBySnapshotDateDesc(
                            tenantId, hcfId, startDate, endDate);
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

    // =====================================================
    // ANALYTICS PAGE ENDPOINTS - Dedicated APIs
    // =====================================================

    /**
     * Get total waste collected for the Analytics Page.
     * Endpoint: GET /api/analytics/page/total-waste
     */
    @GetMapping("/page/total-waste")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public ResponseEntity<com.smartcbwtf.dto.AnalyticsPageDTO.TotalWasteResponse> getPageTotalWaste(
            @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "hcfId", required = false) UUID hcfId) {

        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        validateDateRange(from, to);
        var response = analyticsPageService.getTotalWaste(facilityId, from, to, hcfId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get waste breakdown by category for the Analytics Page.
     * Endpoint: GET /api/analytics/page/waste-by-category
     */
    @GetMapping("/page/waste-by-category")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public ResponseEntity<com.smartcbwtf.dto.AnalyticsPageDTO.WasteByCategoryResponse> getPageWasteByCategory(
            @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "hcfId", required = false) UUID hcfId) {

        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        validateDateRange(from, to);
        var response = analyticsPageService.getWasteByCategory(facilityId, from, to, hcfId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get list of HCFs with ACTIVE agreements for the HCF dropdown.
     * Endpoint: GET /api/analytics/page/hcfs/active
     */
    @GetMapping("/page/hcfs/active")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public ResponseEntity<List<com.smartcbwtf.dto.AnalyticsPageDTO.HcfOption>> getActiveHcfs() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        var response = analyticsPageService.getActiveHcfs(facilityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated list of processed bags for the Analytics Page.
     * Endpoint: GET /api/analytics/page/processed-bags
     */
    @GetMapping("/page/processed-bags")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public ResponseEntity<com.smartcbwtf.dto.AnalyticsPageDTO.ProcessedBagsResponse> getPageProcessedBags(
            @RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "hcfId", required = false) UUID hcfId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {

        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            return ResponseEntity.status(403).build();
        }

        validateDateRange(from, to);
        // Limit page size to prevent abuse
        page = Math.max(0, page);
        pageSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);

        var response = analyticsPageService.getProcessedBags(facilityId, from, to, hcfId, page, pageSize);
        return ResponseEntity.ok(response);
    }

    private int validateDaysWindow(int days) {
        if (days < 0 || days > MAX_ANALYTICS_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Analytics range must be between 0 and " + MAX_ANALYTICS_RANGE_DAYS + " days");
        }
        return days;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range is required");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be on or before end date");
        }
        if (days > MAX_ANALYTICS_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Analytics date range must be " + MAX_ANALYTICS_RANGE_DAYS + " days or less");
        }
    }

    private UUID requireTenantId() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context is required");
        }
        return facilityId;
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
