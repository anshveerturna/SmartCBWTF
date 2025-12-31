package com.smartcbwtf.controller;

import com.smartcbwtf.dto.settings.EmailTemplateDTO;
import com.smartcbwtf.service.EmailTemplateService;
import com.smartcbwtf.service.TemplateValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Email Template management.
 */
@RestController
@RequestMapping("/api/cbwtf/email-templates")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final TemplateValidationService validationService;

    public EmailTemplateController(EmailTemplateService emailTemplateService,
            TemplateValidationService validationService) {
        this.emailTemplateService = emailTemplateService;
        this.validationService = validationService;
    }

    /**
     * List all email templates for the facility.
     */
    @GetMapping
    public ResponseEntity<List<EmailTemplateDTO>> listTemplates() {
        return ResponseEntity.ok(emailTemplateService.listTemplates());
    }

    /**
     * Get a specific template by code.
     */
    @GetMapping("/{templateCode}")
    public ResponseEntity<EmailTemplateDTO> getTemplate(@PathVariable String templateCode) {
        return ResponseEntity.ok(emailTemplateService.getTemplate(templateCode));
    }

    /**
     * Update a template.
     */
    @PutMapping("/{templateCode}")
    public ResponseEntity<Void> updateTemplate(
            @PathVariable String templateCode,
            @Valid @RequestBody EmailTemplateDTO dto,
            HttpServletRequest request) {
        emailTemplateService.updateTemplate(templateCode, dto, extractIpAddress(request));
        return ResponseEntity.ok().build();
    }

    /**
     * Preview a template with sample data.
     */
    @PostMapping("/{templateCode}/preview")
    public ResponseEntity<Map<String, String>> previewTemplate(
            @PathVariable String templateCode,
            @RequestBody Map<String, Object> request) {

        String bodyTemplate = (String) request.get("bodyTemplate");
        @SuppressWarnings("unchecked")
        Map<String, String> sampleData = (Map<String, String>) request.get("sampleData");

        String rendered = emailTemplateService.previewTemplate(templateCode, bodyTemplate, sampleData);
        return ResponseEntity.ok(Map.of("html", rendered));
    }

    /**
     * Reset template to system default.
     */
    @PostMapping("/{templateCode}/reset")
    public ResponseEntity<Void> resetToDefault(
            @PathVariable String templateCode,
            HttpServletRequest request) {
        emailTemplateService.resetToDefault(templateCode, extractIpAddress(request));
        return ResponseEntity.ok().build();
    }

    /**
     * Get available placeholders for a template type.
     */
    @GetMapping("/{templateCode}/placeholders")
    public ResponseEntity<Map<String, Object>> getPlaceholders(@PathVariable String templateCode) {
        return ResponseEntity.ok(Map.of(
                "required", validationService.getRequiredPlaceholders(templateCode),
                "available", validationService.getAllKnownPlaceholders()));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
