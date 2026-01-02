package com.smartcbwtf.controller;

import com.smartcbwtf.domain.GlobalEmailTemplate;
import com.smartcbwtf.service.GlobalEmailTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SuperAdmin-only controller for managing global email templates.
 * CBWTF admins cannot access these endpoints.
 */
@RestController
@RequestMapping("/api/superadmin/email-templates")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminEmailTemplateController {

    private final GlobalEmailTemplateService templateService;

    public SuperAdminEmailTemplateController(GlobalEmailTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Get all active templates (one per code).
     */
    @GetMapping
    public ResponseEntity<List<GlobalEmailTemplate>> getAllActiveTemplates() {
        return ResponseEntity.ok(templateService.getAllActiveTemplates());
    }

    /**
     * Get active template by code.
     */
    @GetMapping("/{code}")
    public ResponseEntity<GlobalEmailTemplate> getActiveTemplate(@PathVariable("code") String code) {
        return ResponseEntity.ok(templateService.getActiveTemplate(code));
    }

    /**
     * Get all versions of a template.
     */
    @GetMapping("/{code}/versions")
    public ResponseEntity<List<GlobalEmailTemplate>> getTemplateVersions(@PathVariable("code") String code) {
        return ResponseEntity.ok(templateService.getTemplateVersions(code));
    }

    /**
     * Update a template (creates new version).
     */
    @PutMapping("/{code}")
    public ResponseEntity<GlobalEmailTemplate> updateTemplate(
            @PathVariable("code") String code,
            @RequestBody UpdateTemplateRequest request,
            @RequestAttribute(name = "userId", required = false) UUID userId) {
        GlobalEmailTemplate updated = templateService.updateTemplate(
                code,
                request.subject(),
                request.bodyHtml(),
                request.requiredPlaceholders(),
                request.optionalPlaceholders(),
                userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Activate a specific version.
     */
    @PostMapping("/{code}/activate/{version}")
    public ResponseEntity<GlobalEmailTemplate> activateVersion(
            @PathVariable("code") String code,
            @PathVariable("version") int version) {
        return ResponseEntity.ok(templateService.activateVersion(code, version));
    }

    /**
     * Preview a template with sample data.
     */
    @PostMapping("/{code}/preview")
    public ResponseEntity<GlobalEmailTemplateService.RenderedEmail> previewTemplate(
            @PathVariable("code") String code,
            @RequestBody Map<String, String> sampleData) {
        return ResponseEntity.ok(templateService.renderTemplate(code, sampleData));
    }

    /**
     * Validate template placeholders without saving.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateTemplate(@RequestBody ValidateTemplateRequest request) {
        try {
            templateService.validatePlaceholders(
                    com.smartcbwtf.domain.enums.TemplateCode.valueOf(request.templateCode()),
                    request.subject(),
                    request.bodyHtml(),
                    request.requiredPlaceholders(),
                    request.optionalPlaceholders());
            return ResponseEntity.ok(new ValidationResult(true, null));
        } catch (GlobalEmailTemplateService.TemplateValidationException e) {
            return ResponseEntity.ok(new ValidationResult(false, e.getMessage()));
        }
    }

    // Request/Response DTOs
    public record UpdateTemplateRequest(
            String subject,
            String bodyHtml,
            String[] requiredPlaceholders,
            String[] optionalPlaceholders) {
    }

    public record ValidateTemplateRequest(
            String templateCode,
            String subject,
            String bodyHtml,
            String[] requiredPlaceholders,
            String[] optionalPlaceholders) {
    }

    public record ValidationResult(boolean valid, String error) {
    }
}
