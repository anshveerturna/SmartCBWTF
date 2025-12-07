package com.smartcbwtf.controller;

import com.smartcbwtf.dto.AlertMissingBagDto;
import com.smartcbwtf.dto.AlertMismatchedBagDto;
import com.smartcbwtf.service.AlertsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertsController {

    private final AlertsService alertsService;

    public AlertsController(AlertsService alertsService) {
        this.alertsService = alertsService;
    }

    @GetMapping("/missing_bags")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public List<AlertMissingBagDto> missing() {
        return alertsService.listMissingBags();
    }

    @GetMapping("/mismatched_bags")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public List<AlertMismatchedBagDto> mismatched() {
        return alertsService.listMismatchedBags();
    }
}
