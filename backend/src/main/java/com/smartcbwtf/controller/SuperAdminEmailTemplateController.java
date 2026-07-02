package com.smartcbwtf.controller;

import com.smartcbwtf.domain.GlobalEmailTemplate;
import com.smartcbwtf.domain.enums.TemplateCode;
import com.smartcbwtf.service.GlobalEmailTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * SuperAdmin-only controller for managing global email templates.
 * CBWTF admins cannot access these endpoints.
 */
@RestController
@RequestMapping("/api/superadmin/email-templates")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Validated
public class SuperAdminEmailTemplateController {

    private static final int MAX_TEMPLATE_CODE_LENGTH = 50;
    private static final int MAX_SUBJECT_LENGTH = 200;
    private static final int MAX_BODY_HTML_LENGTH = 100_000;
    private static final int MAX_PLACEHOLDERS = 50;
    private static final int MAX_PLACEHOLDER_LENGTH = 64;
    private static final int MAX_SAMPLE_DATA_ENTRIES = 100;
    private static final int MAX_SAMPLE_DATA_VALUE_LENGTH = 2_000;
    private static final String TEMPLATE_CODE_PATTERN = "^[A-Z0-9_]+$";
    private static final String PLACEHOLDER_NAME_PATTERN = "^[A-Za-z][A-Za-z0-9_]{0,63}$";
    private static final java.util.regex.Pattern PLACEHOLDER_NAME_REGEX =
            java.util.regex.Pattern.compile(PLACEHOLDER_NAME_PATTERN);

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
        return ResponseEntity.ok(templateService.getActiveTemplate(cleanTemplateCode(code)));
    }

    /**
     * Get all versions of a template.
     */
    @GetMapping("/{code}/versions")
    public ResponseEntity<List<GlobalEmailTemplate>> getTemplateVersions(@PathVariable("code") String code) {
        return ResponseEntity.ok(templateService.getTemplateVersions(cleanTemplateCode(code)));
    }

    /**
     * Update a template (creates new version).
     */
    @PutMapping("/{code}")
    public ResponseEntity<GlobalEmailTemplate> updateTemplate(
            @PathVariable("code") String code,
            @Valid @RequestBody UpdateTemplateRequest request,
            @RequestAttribute(name = "userId", required = false) UUID userId) {
        GlobalEmailTemplate updated = templateService.updateTemplate(
                cleanTemplateCode(code),
                request.subject(),
                request.bodyHtml(),
                placeholderArray(request.requiredPlaceholders()),
                placeholderArray(request.optionalPlaceholders()),
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
        return ResponseEntity.ok(templateService.activateVersion(cleanTemplateCode(code), version));
    }

    /**
     * Preview a template with sample data.
     */
    @PostMapping("/{code}/preview")
    public ResponseEntity<GlobalEmailTemplateService.RenderedEmail> previewTemplate(
            @PathVariable("code") String code,
            @Valid @RequestBody
            @Size(max = MAX_SAMPLE_DATA_ENTRIES, message = "Sample data must contain 100 entries or fewer")
            Map<@NotBlank(message = "Placeholder name is required")
                    @Size(max = MAX_PLACEHOLDER_LENGTH, message = "Placeholder name must be 64 characters or less")
                    @Pattern(regexp = PLACEHOLDER_NAME_PATTERN, message = "Invalid placeholder name") String,
                    @Size(max = MAX_SAMPLE_DATA_VALUE_LENGTH, message = "Sample value must be 2000 characters or less") String> sampleData) {
        return ResponseEntity.ok(templateService.renderTemplate(cleanTemplateCode(code), sampleData));
    }

    /**
     * Validate template placeholders without saving.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateTemplate(@Valid @RequestBody ValidateTemplateRequest request) {
        try {
            templateService.validatePlaceholders(
                    TemplateCode.valueOf(cleanTemplateCode(request.templateCode())),
                    request.subject(),
                    request.bodyHtml(),
                    placeholderArray(request.requiredPlaceholders()),
                    placeholderArray(request.optionalPlaceholders()));
            return ResponseEntity.ok(new ValidationResult(true, null));
        } catch (GlobalEmailTemplateService.TemplateValidationException e) {
            return ResponseEntity.ok(new ValidationResult(false, e.getMessage()));
        }
    }

    private static String cleanTemplateCode(String code) {
        String cleaned = cleanLineRequired(code, "Template code", MAX_TEMPLATE_CODE_LENGTH).toUpperCase(Locale.ROOT);
        if (!cleaned.matches(TEMPLATE_CODE_PATTERN)) {
            throw new IllegalArgumentException("Template code contains invalid characters");
        }
        return cleaned;
    }

    private static String[] placeholderArray(List<String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return new String[0];
        }
        return placeholders.stream()
                .map(SuperAdminEmailTemplateController::cleanPlaceholderName)
                .distinct()
                .toArray(String[]::new);
    }

    private static String cleanPlaceholderName(String placeholder) {
        String cleaned = cleanLineRequired(placeholder, "Placeholder name", MAX_PLACEHOLDER_LENGTH);
        if (!PLACEHOLDER_NAME_REGEX.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid placeholder name");
        }
        return cleaned;
    }

    private static String cleanLineRequired(String value, String fieldName, int maxLength) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or less");
        }
        return cleaned;
    }

    private static String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    // Request/Response DTOs
    public record UpdateTemplateRequest(
            @NotBlank(message = "Subject is required")
            @Size(max = MAX_SUBJECT_LENGTH, message = "Subject must be 200 characters or less")
            String subject,
            @NotBlank(message = "Body HTML is required")
            @Size(max = MAX_BODY_HTML_LENGTH, message = "Body HTML must be 100000 characters or less")
            String bodyHtml,
            @Size(max = MAX_PLACEHOLDERS, message = "Required placeholders must contain 50 entries or fewer")
            List<@NotBlank(message = "Placeholder name is required")
                    @Size(max = MAX_PLACEHOLDER_LENGTH, message = "Placeholder name must be 64 characters or less")
                    @Pattern(regexp = PLACEHOLDER_NAME_PATTERN, message = "Invalid placeholder name") String> requiredPlaceholders,
            @Size(max = MAX_PLACEHOLDERS, message = "Optional placeholders must contain 50 entries or fewer")
            List<@NotBlank(message = "Placeholder name is required")
                    @Size(max = MAX_PLACEHOLDER_LENGTH, message = "Placeholder name must be 64 characters or less")
                    @Pattern(regexp = PLACEHOLDER_NAME_PATTERN, message = "Invalid placeholder name") String> optionalPlaceholders) {
    }

    public record ValidateTemplateRequest(
            @NotBlank(message = "Template code is required")
            @Size(max = MAX_TEMPLATE_CODE_LENGTH, message = "Template code must be 50 characters or less")
            @Pattern(regexp = TEMPLATE_CODE_PATTERN, message = "Template code contains invalid characters")
            String templateCode,
            @NotBlank(message = "Subject is required")
            @Size(max = MAX_SUBJECT_LENGTH, message = "Subject must be 200 characters or less")
            String subject,
            @NotBlank(message = "Body HTML is required")
            @Size(max = MAX_BODY_HTML_LENGTH, message = "Body HTML must be 100000 characters or less")
            String bodyHtml,
            @Size(max = MAX_PLACEHOLDERS, message = "Required placeholders must contain 50 entries or fewer")
            List<@NotBlank(message = "Placeholder name is required")
                    @Size(max = MAX_PLACEHOLDER_LENGTH, message = "Placeholder name must be 64 characters or less")
                    @Pattern(regexp = PLACEHOLDER_NAME_PATTERN, message = "Invalid placeholder name") String> requiredPlaceholders,
            @Size(max = MAX_PLACEHOLDERS, message = "Optional placeholders must contain 50 entries or fewer")
            List<@NotBlank(message = "Placeholder name is required")
                    @Size(max = MAX_PLACEHOLDER_LENGTH, message = "Placeholder name must be 64 characters or less")
                    @Pattern(regexp = PLACEHOLDER_NAME_PATTERN, message = "Invalid placeholder name") String> optionalPlaceholders) {
    }

    public record ValidationResult(boolean valid, String error) {
    }
}
