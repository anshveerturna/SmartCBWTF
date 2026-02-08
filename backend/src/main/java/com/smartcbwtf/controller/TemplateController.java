package com.smartcbwtf.controller;

import com.smartcbwtf.domain.FacilityTemplate;
import com.smartcbwtf.dto.TemplateListItem;
import com.smartcbwtf.service.FacilityTemplateService;
import com.smartcbwtf.service.TenantAssertionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class TemplateController {

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
            @RequestParam("name") String name,
            @RequestParam("templateType") String templateType,
            @RequestParam("version") String version,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "createdByUserId", required = false) UUID createdByUserId,
            @RequestParam(value = "setActive", defaultValue = "false") boolean setActive) {

        tenantAssertionService.assertCanAccessFacility(facilityId);
        try {
            UUID actorUserId = com.smartcbwtf.config.TenantContext.getUserId();
            FacilityTemplate template = templateService.uploadTemplate(
                    facilityId, name, templateType, version, file, actorUserId, setActive);

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
            @RequestBody Map<String, Object> request) {

        tenantAssertionService.assertCanAccessFacility(facilityId);
        String name = (String) request.get("name");
        String version = (String) request.get("version");
        String htmlContent = (String) request.get("htmlContent");
        boolean setActive = Boolean.TRUE.equals(request.get("setActive"));

        if (name == null || version == null || htmlContent == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name, version, and htmlContent are required"));
        }

        try {
            UUID actorUserId = com.smartcbwtf.config.TenantContext.getUserId();
            FacilityTemplate template = templateService.createHtmlTemplate(
                    facilityId, name, version, htmlContent, actorUserId, setActive);

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
