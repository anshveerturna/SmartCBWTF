package com.smartcbwtf.controller;

import com.smartcbwtf.dto.AnalyticsResponse;
import com.smartcbwtf.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/hcf/{hcfId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public AnalyticsResponse hcf(@PathVariable UUID hcfId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return analyticsService.hcfAnalytics(hcfId, start, end);
    }

    @GetMapping("/facility/{facilityId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public AnalyticsResponse facility(@PathVariable UUID facilityId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return analyticsService.facilityAnalytics(facilityId, start, end);
    }
}
