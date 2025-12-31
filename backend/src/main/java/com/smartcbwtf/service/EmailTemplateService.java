package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.FacilityEmailTemplate;
import com.smartcbwtf.domain.SettingsAuditLog;
import com.smartcbwtf.dto.settings.EmailTemplateDTO;
import com.smartcbwtf.repository.FacilityEmailTemplateRepository;
import com.smartcbwtf.repository.SettingsAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing email templates.
 */
@Service
@Transactional
public class EmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateService.class);

    private final FacilityEmailTemplateRepository templateRepository;
    private final TemplateValidationService validationService;
    private final SettingsAuditLogRepository auditLogRepository;

    public EmailTemplateService(FacilityEmailTemplateRepository templateRepository,
            TemplateValidationService validationService,
            SettingsAuditLogRepository auditLogRepository) {
        this.templateRepository = templateRepository;
        this.validationService = validationService;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Get all templates for the current facility.
     */
    public List<EmailTemplateDTO> listTemplates() {
        UUID facilityId = TenantContext.getTenantId();
        List<FacilityEmailTemplate> templates = templateRepository
                .findByFacilityIdOrderByTemplateCodeAscVersionDesc(facilityId);

        // Get only active version per template code
        Map<String, FacilityEmailTemplate> activeByCode = new LinkedHashMap<>();
        for (FacilityEmailTemplate t : templates) {
            if (t.getIsActive() && !activeByCode.containsKey(t.getTemplateCode())) {
                activeByCode.put(t.getTemplateCode(), t);
            }
        }

        return activeByCode.values().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific template by code.
     */
    public EmailTemplateDTO getTemplate(String templateCode) {
        UUID facilityId = TenantContext.getTenantId();
        FacilityEmailTemplate template = templateRepository
                .findActiveTemplate(facilityId, templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateCode));
        return toDTO(template);
    }

    /**
     * Update a template.
     */
    public void updateTemplate(String templateCode, EmailTemplateDTO dto, String ipAddress) {
        UUID facilityId = TenantContext.getTenantId();

        // Validate template
        validationService.validateTemplate(templateCode, dto.getBodyTemplate());

        // Sanitize HTML
        String sanitizedBody = validationService.sanitizeHtml(dto.getBodyTemplate());

        // Deactivate current version
        Optional<FacilityEmailTemplate> current = templateRepository.findActiveTemplate(facilityId, templateCode);
        String oldBody = null;
        if (current.isPresent()) {
            oldBody = current.get().getBodyTemplate();
            current.get().setIsActive(false);
            templateRepository.save(current.get());
        }

        // Get next version number
        int nextVersion = templateRepository.findMaxVersion(facilityId, templateCode).orElse(0) + 1;

        // Create new version
        FacilityEmailTemplate newTemplate = new FacilityEmailTemplate();
        newTemplate.setFacilityId(facilityId);
        newTemplate.setTemplateCode(templateCode);
        newTemplate.setSubjectTemplate(dto.getSubjectTemplate());
        newTemplate.setBodyTemplate(sanitizedBody);
        newTemplate.setIsActive(true);
        newTemplate.setVersion(nextVersion);
        newTemplate.setCreatedAt(Instant.now());
        newTemplate.setUpdatedAt(Instant.now());

        templateRepository.save(newTemplate);

        // Audit log
        auditLog(facilityId, "email_templates", templateCode, oldBody, sanitizedBody, ipAddress);

        log.info("Updated email template {} to version {} for facility {}", templateCode, nextVersion, facilityId);
    }

    /**
     * Reset template to system default.
     */
    public void resetToDefault(String templateCode, String ipAddress) {
        UUID facilityId = TenantContext.getTenantId();

        // Get default template
        String defaultBody = getSystemDefault(templateCode);
        String defaultSubject = getSystemDefaultSubject(templateCode);

        EmailTemplateDTO dto = new EmailTemplateDTO();
        dto.setTemplateCode(templateCode);
        dto.setSubjectTemplate(defaultSubject);
        dto.setBodyTemplate(defaultBody);

        updateTemplate(templateCode, dto, ipAddress);

        log.info("Reset template {} to default for facility {}", templateCode, facilityId);
    }

    /**
     * Preview a template with sample data.
     */
    public String previewTemplate(String templateCode, String bodyTemplate, Map<String, String> sampleData) {
        // Validate first
        validationService.validateTemplate(templateCode, bodyTemplate);

        // Sanitize
        String sanitized = validationService.sanitizeHtml(bodyTemplate);

        // Render with sample data
        return validationService.render(sanitized, sampleData);
    }

    /**
     * Get system default template body.
     */
    private String getSystemDefault(String templateCode) {
        return switch (templateCode) {
            case "HCF_WELCOME" -> """
                    <html><body style="font-family: Arial, sans-serif;">
                    <h2>Welcome!</h2>
                    <p>Dear {{hcfName}},</p>
                    <p>Your registration with <strong>{{facilityName}}</strong> has been completed.</p>
                    <p>Best regards,<br>{{facilityName}} Team</p>
                    </body></html>
                    """;
            case "PAYMENT_REMINDER" ->
                """
                        <html><body style="font-family: Arial, sans-serif;">
                        <h2>Payment Reminder</h2>
                        <p>Dear {{hcfName}},</p>
                        <p>Invoice <strong>{{invoiceNumber}}</strong> for <strong>₹{{amountDue}}</strong> is due on <strong>{{dueDate}}</strong>.</p>
                        <p>Best regards,<br>{{facilityName}} Team</p>
                        </body></html>
                        """;
            case "INVOICE_GENERATED" ->
                """
                        <html><body style="font-family: Arial, sans-serif;">
                        <h2>Invoice Generated</h2>
                        <p>Dear {{hcfName}},</p>
                        <p>Invoice <strong>{{invoiceNumber}}</strong> for <strong>₹{{amount}}</strong> is due on <strong>{{dueDate}}</strong>.</p>
                        <p>Best regards,<br>{{facilityName}} Team</p>
                        </body></html>
                        """;
            case "PAYMENT_OVERDUE" -> """
                    <html><body style="font-family: Arial, sans-serif;">
                    <h2 style="color: #d32f2f;">Payment Overdue</h2>
                    <p>Dear {{hcfName}},</p>
                    <p>Invoice <strong>{{invoiceNumber}}</strong> is <strong>{{daysOverdue}} days overdue</strong>.</p>
                    <p>Best regards,<br>{{facilityName}} Team</p>
                    </body></html>
                    """;
            case "AGREEMENT_EXPIRING" -> """
                    <html><body style="font-family: Arial, sans-serif;">
                    <h2>Agreement Expiring</h2>
                    <p>Dear {{hcfName}},</p>
                    <p>Agreement <strong>{{agreementNumber}}</strong> expires on <strong>{{expiryDate}}</strong>.</p>
                    <p>Best regards,<br>{{facilityName}} Team</p>
                    </body></html>
                    """;
            case "HCF_CREDENTIALS" -> """
                    <html><body style="font-family: Arial, sans-serif;">
                    <h2>Your Credentials</h2>
                    <p>Dear {{hcfName}},</p>
                    <p>Username: <strong>{{username}}</strong><br>
                    Login: <a href="{{loginUrl}}">{{loginUrl}}</a></p>
                    <p>Best regards,<br>{{facilityName}} Team</p>
                    </body></html>
                    """;
            default -> "<html><body><p>Template not found.</p></body></html>";
        };
    }

    private String getSystemDefaultSubject(String templateCode) {
        return switch (templateCode) {
            case "HCF_WELCOME" -> "Welcome to {{facilityName}}";
            case "PAYMENT_REMINDER" -> "Payment Reminder - Invoice {{invoiceNumber}}";
            case "INVOICE_GENERATED" -> "New Invoice {{invoiceNumber}} - {{facilityName}}";
            case "PAYMENT_OVERDUE" -> "URGENT: Payment Overdue - Invoice {{invoiceNumber}}";
            case "AGREEMENT_EXPIRING" -> "Agreement Expiring Soon - {{agreementNumber}}";
            case "HCF_CREDENTIALS" -> "Your Login Credentials - {{facilityName}}";
            default -> "Notification from {{facilityName}}";
        };
    }

    private EmailTemplateDTO toDTO(FacilityEmailTemplate t) {
        EmailTemplateDTO dto = new EmailTemplateDTO();
        dto.setId(t.getId());
        dto.setTemplateCode(t.getTemplateCode());
        dto.setSubjectTemplate(t.getSubjectTemplate());
        dto.setBodyTemplate(t.getBodyTemplate());
        dto.setVersion(t.getVersion());
        dto.setIsActive(t.getIsActive());
        dto.setRequiredPlaceholders(validationService.getRequiredPlaceholders(t.getTemplateCode()));
        dto.setAvailablePlaceholders(validationService.getAllKnownPlaceholders());
        return dto;
    }

    private void auditLog(UUID facilityId, String section, String key, String oldValue, String newValue,
            String ipAddress) {
        SettingsAuditLog log = new SettingsAuditLog();
        log.setFacilityId(facilityId);
        log.setSection(section);
        log.setSettingKey(key);
        log.setOldValue(oldValue != null && oldValue.length() > 500 ? oldValue.substring(0, 500) + "..." : oldValue);
        log.setNewValue(newValue != null && newValue.length() > 500 ? newValue.substring(0, 500) + "..." : newValue);
        log.setChangedBy(TenantContext.getUserId());
        log.setIpAddress(ipAddress);
        log.setChangedAt(Instant.now());
        auditLogRepository.save(log);
    }
}
