package com.smartcbwtf.service;

import com.smartcbwtf.service.GlobalEmailTemplateService.RenderedEmail;
import com.smartcbwtf.service.email.BrevoEmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central email service for SmartCBWTF.
 * 
 * All email sending goes through this service.
 * Uses BrevoEmailProvider for actual email delivery.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:info@smartcbwtf.com}")
    private String fromAddress;

    private final GlobalEmailTemplateService templateService;
    private final BrevoEmailProvider brevoProvider;

    public EmailService(GlobalEmailTemplateService templateService, BrevoEmailProvider brevoProvider) {
        this.templateService = templateService;
        this.brevoProvider = brevoProvider;
    }

    /**
     * Send a simple text email.
     * Text is converted to HTML for consistent rendering.
     */
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("[EMAIL-DEV] to={} subject={} body={}", to, subject, body);
            return;
        }

        String messageId = brevoProvider.sendSimpleEmail(to, subject, body);
        if (messageId != null) {
            log.info("[EMAIL] Sent simple email to={} messageId={}", to, messageId);
        } else {
            log.error("[EMAIL] Failed to send simple email to={}", to);
        }
    }

    /**
     * Send email with attachment.
     */
    public void sendEmailWithAttachment(String to, String subject, String body, String attachmentPath) {
        if (!emailEnabled) {
            log.info("[EMAIL-DEV] to={} subject={} attachment={}", to, subject, attachmentPath);
            return;
        }

        // Convert text to HTML
        String htmlContent = "<div style=\"font-family: Arial, sans-serif; font-size: 14px;\">" +
                body.replace("\n", "<br>") + "</div>";

        List<String> attachments = attachmentPath != null ? List.of(attachmentPath) : null;
        String messageId = brevoProvider.sendEmail(to, subject, htmlContent, attachments);

        if (messageId != null) {
            log.info("[EMAIL] Sent email with attachment to={} messageId={}", to, messageId);
        } else {
            log.error("[EMAIL] Failed to send email with attachment to={}", to);
        }
    }

    /**
     * Send an email using a global template.
     * Templates are managed by SuperAdmin and support placeholders.
     */
    public void sendTemplateEmail(String to, String templateCode, Map<String, String> data,
            List<String> attachmentPaths) {
        try {
            // Render template (fail-closed if not found)
            RenderedEmail rendered = templateService.renderTemplate(templateCode, data);

            log.info("[EMAIL_DISPATCH] Template={} Version={} Checksum={} To={}",
                    templateCode, rendered.templateVersion(), rendered.templateChecksum(), to);

            if (!emailEnabled) {
                log.info("[EMAIL-DEV] Template email:\nTO: {}\nSUBJECT: {}\nBODY:\n{}\nATTACHMENTS: {}",
                        to, rendered.subject(), rendered.bodyHtml(), attachmentPaths);
                return;
            }

            // Send via Brevo
            String messageId = brevoProvider.sendEmail(to, rendered.subject(), rendered.bodyHtml(), attachmentPaths);

            if (messageId != null) {
                log.info("[EMAIL] Template email sent successfully to={} template={} messageId={}",
                        to, templateCode, messageId);
            } else {
                log.error("[EMAIL] Failed to send template email to={} template={}", to, templateCode);
                throw new RuntimeException("Failed to send email via Brevo");
            }

        } catch (Exception e) {
            log.error("[EMAIL_FAILURE] Failed to send email {} to {}: {}", templateCode, to, e.getMessage());
            throw e;
        }
    }

    /**
     * Send HCF registration confirmation email using template.
     */
    public void sendHcfRegistrationEmail(String hcfEmail, String hcfName, String agreementNumber,
            String facilityName, String pdfPath) {
        Map<String, String> data = new HashMap<>();
        data.put("hcfName", hcfName);
        data.put("facilityName", facilityName);
        data.put("agreementNumber", agreementNumber);
        data.put("submittedDate", java.time.LocalDate.now().toString());

        sendTemplateEmail(hcfEmail, "AGREEMENT_SUBMITTED", data, List.of(pdfPath));
    }

    /**
     * Check if email sending is enabled and ready.
     */
    public boolean isReady() {
        return emailEnabled && brevoProvider.isReady();
    }
}
