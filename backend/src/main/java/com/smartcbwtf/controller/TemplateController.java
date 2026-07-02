package com.smartcbwtf.controller;

import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.dto.TemplateListItem;
import com.smartcbwtf.service.FacilityTemplateService;
import com.smartcbwtf.service.TenantAssertionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for Agreement Template management.
 */
@RestController
@RequestMapping("/api/facilities/{facilityId}/templates")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','CBWTF_ADMIN')")
@Validated
public class TemplateController {
    private static final int MAX_TEMPLATE_NAME_LENGTH = 120;
    private static final int MAX_TEMPLATE_VERSION_LENGTH = 80;
    private static final int MAX_TEMPLATE_TYPE_LENGTH = 10;
    private static final int MAX_HTML_TEMPLATE_CHARS = 1024 * 1024;

    private final FacilityTemplateService templateService;
    private final TenantAssertionService tenantAssertionService;

    public TemplateController(
            FacilityTemplateService templateService,
            TenantAssertionService tenantAssertionService) {
        this.templateService = templateService;
        this.tenantAssertionService = tenantAssertionService;
    }

    /**
     * List all templates for a facility.
     */
    @GetMapping
    public ResponseEntity<List<TemplateListItem>> listTemplates(@PathVariable UUID facilityId) {
        tenantAssertionService.assertCanAccessFacility(facilityId);
        return ResponseEntity.ok(templateService.listTemplates(facilityId));
    }

    /**
     * Upload a new template (HTML or PDF file).
     */
    @PostMapping
    public ResponseEntity<?> uploadTemplate(
            @PathVariable UUID facilityId,
            @RequestParam("name")
            @NotBlank(message = "Template name is required")
            @Size(max = MAX_TEMPLATE_NAME_LENGTH, message = "Template name must be 120 characters or fewer")
            String name,
            @RequestParam("templateType")
            @NotBlank(message = "Template type is required")
            @Size(max = MAX_TEMPLATE_TYPE_LENGTH, message = "Template type is invalid")
            @Pattern(regexp = "(?i)^(HTML|PDF)$", message = "Template type must be HTML or PDF")
            String templateType,
            @RequestParam("version")
            @NotBlank(message = "Template version is required")
            @Size(max = MAX_TEMPLATE_VERSION_LENGTH, message = "Template version must be 80 characters or fewer")
            String version,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "setActive", defaultValue = "false") boolean setActive) {

        tenantAssertionService.assertCanAccessFacility(facilityId);
        try {
            UUID actorUserId = com.smartcbwtf.config.TenantContext.getUserId();
            FacilityTemplate template = templateService.uploadTemplate(
                    facilityId,
                    name.strip(),
                    templateType.strip(),
                    version.strip(),
                    file,
                    actorUserId,
                    setActive);

            return ResponseEntity.status(201).body(Map.of(
                    "templateId", template.getId(),
                    "name", template.getName(),
                    "version", template.getVersion(),
                    "active", template.getActive(),
                    "message", "Template uploaded successfully"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to save template file"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new HTML template with content string.
     */
    @PostMapping("/html")
    public ResponseEntity<?> createHtmlTemplate(
            @PathVariable UUID facilityId,
            @Valid @RequestBody HtmlTemplateRequest request) {

        tenantAssertionService.assertCanAccessFacility(facilityId);

        if (request == null || request.name() == null || request.version() == null || request.htmlContent() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name, version, and htmlContent are required"));
        }

        try {
            UUID actorUserId = com.smartcbwtf.config.TenantContext.getUserId();
            FacilityTemplate template = templateService.createHtmlTemplate(
                    facilityId,
                    request.name().strip(),
                    request.version().strip(),
                    request.htmlContent(),
                    actorUserId,
                    Boolean.TRUE.equals(request.setActive()));

            return ResponseEntity.status(201).body(Map.of(
                    "templateId", template.getId(),
                    "name", template.getName(),
                    "version", template.getVersion(),
                    "active", template.getActive(),
                    "message", "HTML template created successfully"
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to save template"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record HtmlTemplateRequest(
            @NotBlank(message = "Template name is required")
            @Size(max = MAX_TEMPLATE_NAME_LENGTH, message = "Template name must be 120 characters or fewer")
            String name,

            @NotBlank(message = "Template version is required")
            @Size(max = MAX_TEMPLATE_VERSION_LENGTH, message = "Template version must be 80 characters or fewer")
            String version,

            @NotBlank(message = "HTML content is required")
            @Size(max = MAX_HTML_TEMPLATE_CHARS, message = "HTML template must be under 1MB")
            String htmlContent,

            Boolean setActive) {
    }

    /**
     * Activate a specific template.
     */
    @PatchMapping("/{templateId}/activate")
    public ResponseEntity<?> activateTemplate(
            @PathVariable UUID facilityId,
            @PathVariable UUID templateId) {

        tenantAssertionService.assertCanAccessFacility(facilityId);
        try {
            FacilityTemplate template = templateService.activateTemplate(facilityId, templateId);
            return ResponseEntity.ok(Map.of(
                    "templateId", template.getId(),
                    "name", template.getName(),
                    "version", template.getVersion(),
                    "active", template.getActive(),
                    "message", "Template activated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
