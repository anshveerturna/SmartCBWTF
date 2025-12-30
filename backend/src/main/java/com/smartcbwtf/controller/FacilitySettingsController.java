package com.smartcbwtf.controller;

import com.smartcbwtf.dto.settings.*;
import com.smartcbwtf.service.FacilitySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for CBWTF Facility Settings.
 * All endpoints require CBWTF_ADMIN role.
 */
@RestController
@RequestMapping("/api/cbwtf/settings")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class FacilitySettingsController {

    private final FacilitySettingsService settingsService;

    public FacilitySettingsController(FacilitySettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Get all settings for the current facility.
     */
    @GetMapping
    public ResponseEntity<FacilitySettingsDTO> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    /**
     * Check system readiness.
     */
    @GetMapping("/readiness")
    public ResponseEntity<SystemReadinessResult> checkReadiness() {
        return ResponseEntity.ok(settingsService.checkSystemReadiness());
    }

    /**
     * Update Section 1: Legal & Entity Profile.
     */
    @PutMapping("/legal")
    public ResponseEntity<Void> updateLegalProfile(
            @Valid @RequestBody LegalProfileDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updateLegalProfile(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Update Section 2: Financial & Billing Settings.
     */
    @PutMapping("/financial")
    public ResponseEntity<Void> updateFinancialSettings(
            @Valid @RequestBody FinancialSettingsDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updateFinancialSettings(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Update Section 3: Payment & Reminder Settings.
     */
    @PutMapping("/payment-reminders")
    public ResponseEntity<Void> updatePaymentReminders(
            @Valid @RequestBody PaymentReminderDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updatePaymentReminders(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Update Section 4: Agreement Rules.
     */
    @PutMapping("/agreement-rules")
    public ResponseEntity<Void> updateAgreementRules(
            @Valid @RequestBody AgreementRulesDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updateAgreementRules(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Update Section 5: Operational Rules.
     */
    @PutMapping("/operational")
    public ResponseEntity<Void> updateOperationalRules(
            @Valid @RequestBody OperationalRulesDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updateOperationalRules(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Update Section 6: Compliance Settings.
     */
    @PutMapping("/compliance")
    public ResponseEntity<Void> updateComplianceSettings(
            @Valid @RequestBody ComplianceSettingsDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updateComplianceSettings(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Update Section 7: Email Settings.
     */
    @PutMapping("/email")
    public ResponseEntity<Void> updateEmailSettings(
            @Valid @RequestBody EmailSettingsDTO dto,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        settingsService.updateEmailSettings(dto, ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Get settings audit history.
     */
    @GetMapping("/audit-history")
    public ResponseEntity<Page<SettingsAuditDTO>> getAuditHistory(
            @RequestParam(name = "section", required = false) String section,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(settingsService.getAuditHistory(section, page, size));
    }

    /**
     * Extract client IP address from request.
     * Checks X-Forwarded-For header for reverse proxy scenarios.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take first IP if multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
