package com.smartcbwtf.controller;

import com.smartcbwtf.dto.CBWTFDashboardDTO;
import com.smartcbwtf.service.CBWTFDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
