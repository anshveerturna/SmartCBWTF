package com.smartcbwtf.controller;

import com.smartcbwtf.dto.CBWTFDashboardDTO;
import com.smartcbwtf.service.CBWTFDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CBWTF Admin Portal Dashboard Controller.
 * All endpoints are tenant-scoped automatically via TenantContext.
 */
@RestController
@RequestMapping("/api/cbwtf/dashboard")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CBWTFDashboardController {

    private final CBWTFDashboardService dashboardService;

    public CBWTFDashboardController(CBWTFDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get complete dashboard metrics for the current CBWTF.
     * Automatically scoped to the tenant from JWT.
     */
    @GetMapping
    public ResponseEntity<CBWTFDashboardDTO> getDashboard() {
        CBWTFDashboardDTO metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get waste category breakdown for pie chart.
     */
    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdown>> getCategoryBreakdown() {
        List<CategoryBreakdown> breakdown = dashboardService.getCategoryBreakdown();
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Get weekly collection trend for area chart.
     */
    @GetMapping("/weekly-trend")
    public ResponseEntity<List<WeeklyTrend>> getWeeklyTrend() {
        List<WeeklyTrend> trend = dashboardService.getWeeklyTrend();
        return ResponseEntity.ok(trend);
    }

    /**
     * Get yesterday comparison for trend percentage.
     */
    @GetMapping("/trend-comparison")
    public ResponseEntity<Map<String, Object>> getTrendComparison() {
        Map<String, Object> comparison = dashboardService.getTrendComparison();
        return ResponseEntity.ok(comparison);
    }

    // DTO Records
    public record CategoryBreakdown(
            String name,
            long value,
            String color) {
    }

    public record WeeklyTrend(
            String date,
            long yellow,
            long red,
            long blue,
            long white) {
    }
}
