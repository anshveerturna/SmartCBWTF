package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.dto.settings.*;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.service.AgreementNumberGeneratorService;
import com.smartcbwtf.service.FacilitySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for CBWTF Facility Settings.
 * All endpoints require CBWTF_ADMIN role.
 */
@RestController
@RequestMapping("/api/cbwtf/settings")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class FacilitySettingsController {

    private final FacilitySettingsService settingsService;
    private final AgreementNumberGeneratorService agreementNumberGenerator;
    private final FacilityRepository facilityRepository;
    private final FacilitySettingsRepository facilitySettingsRepository;

    public FacilitySettingsController(
            FacilitySettingsService settingsService,
            AgreementNumberGeneratorService agreementNumberGenerator,
            FacilityRepository facilityRepository,
            FacilitySettingsRepository facilitySettingsRepository) {
        this.settingsService = settingsService;
        this.agreementNumberGenerator = agreementNumberGenerator;
        this.facilityRepository = facilityRepository;
        this.facilitySettingsRepository = facilitySettingsRepository;
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
     * Preview the next agreement number with current or custom format settings.
     * Does NOT consume a sequence number.
     */
    @GetMapping("/agreement-number-preview")
    public ResponseEntity<Map<String, String>> previewAgreementNumber(
            @RequestParam(name = "prefix", required = false) String prefix,
            @RequestParam(name = "separator", required = false) String separator,
            @RequestParam(name = "digits", required = false) Integer digits,
            @RequestParam(name = "includeFacilityCode", required = false) Boolean includeFacilityCode,
            @RequestParam(name = "includeYear", required = false) Boolean includeYear) {
        UUID facilityId = TenantContext.getTenantId();
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found"));

        // If no params provided, use saved settings (or defaults)
        FacilitySettings settings = facilitySettingsRepository.findById(facilityId).orElse(null);
        String effectivePrefix = prefix != null ? prefix : (settings != null ? settings.getAgreementNumberPrefix() : "HCF");
        String effectiveSeparator = separator != null ? separator : (settings != null ? settings.getAgreementNumberSeparator() : "-");
        int effectiveDigits = digits != null ? digits : (settings != null ? settings.getAgreementNumberSequenceDigits() : 5);
        boolean effectiveIncludeFacilityCode = includeFacilityCode != null ? includeFacilityCode : (settings != null ? settings.getAgreementNumberIncludeFacilityCode() : true);
        boolean effectiveIncludeYear = includeYear != null ? includeYear : (settings != null ? settings.getAgreementNumberIncludeYear() : true);

        String preview = agreementNumberGenerator.previewNextAgreementNumber(
                facility, effectivePrefix, effectiveSeparator, effectiveDigits,
                effectiveIncludeFacilityCode, effectiveIncludeYear);

        return ResponseEntity.ok(Map.of("preview", preview));
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
     * Get the agreement terms template.
     */
    @GetMapping("/agreement-terms")
    public ResponseEntity<Map<String, String>> getAgreementTerms() {
        UUID facilityId = TenantContext.getTenantId();
        FacilitySettings settings = facilitySettingsRepository.findById(facilityId).orElse(null);
        String terms = settings != null ? settings.getAgreementTermsTemplate() : null;
        return ResponseEntity.ok(Map.of("termsTemplate", terms != null ? terms : ""));
    }

    /**
     * Update the agreement terms template.
     * This is the default T&C text that gets embedded into new agreement PDFs.
     */
    @PutMapping("/agreement-terms")
    public ResponseEntity<Void> updateAgreementTerms(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String ipAddress = extractIpAddress(request);
        String termsTemplate = body.get("termsTemplate");
        settingsService.updateAgreementTermsTemplate(termsTemplate, ipAddress);
        return ResponseEntity.ok().build();
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
