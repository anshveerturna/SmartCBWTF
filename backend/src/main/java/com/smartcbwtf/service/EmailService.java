package com.smartcbwtf.service;

import com.smartcbwtf.service.GlobalEmailTemplateService.RenderedEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:noreply@smartcbwtf.com}")
    private String fromAddress;

    private final GlobalEmailTemplateService templateService;

    public EmailService(GlobalEmailTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Legacy method for sending simple text email.
     * TODO: Refactor callers to use global templates.
     */
    public void sendEmail(String to, String subject, String body) {
        sendRawEmail(to, subject, body);
    }

    /**
     * Legacy method for sending email with attachment.
     * TODO: Refactor callers to use global templates.
     */
    public void sendEmailWithAttachment(String to, String subject, String body, String attachmentPath) {
        // For now just log it as raw email with attachment info
        if (!emailEnabled) {
            log.info("[DEV-EMAIL-ATTACHMENT] to={} subject={} body={} attachment={}", to, subject, body,
                    attachmentPath);
            return;
        }
        log.info("[EMAIL-ATTACHMENT] Sending to={} subject={} attachment={}", to, subject, attachmentPath);
    }

    /**
     * Send an email using a global template.
     */
    public void sendTemplateEmail(String to, String templateCode, Map<String, String> data,
            List<String> attachmentPaths) {
        try {
            // Render template (fail-closed if not found)
            RenderedEmail rendered = templateService.renderTemplate(templateCode, data);

            log.info("[EMAIL_DISPATCH] Template={} Version={} Checksum={} To={}",
                    templateCode, rendered.templateVersion(), rendered.templateChecksum(), to);

            if (!emailEnabled) {
                log.info("[DEV-EMAIL] \nTO: {}\nSUBJECT: {}\nBODY_HTML:\n{}\nATTACHMENTS: {}",
                        to, rendered.subject(), rendered.bodyHtml(), attachmentPaths);
                return;
            }

            // TODO: Integrate SMTP/SendGrid/SES
            // When integrating, ensure the checksum is logged/durable

            log.info("[EMAIL] Sent successfully to={}", to);

        } catch (Exception e) {
            log.error("[EMAIL_FAILURE] Failed to send email {} to {}", templateCode, to, e);
            // Fail closed - do not send partial email
            throw e;
        }
    }

    /**
     * Send HCF registration confirmation email using generic template system.
     */
    public void sendHcfRegistrationEmail(String hcfEmail, String hcfName, String agreementNumber,
            String facilityName, String pdfPath) {
        Map<String, String> data = new HashMap<>();
        data.put("hcfName", hcfName);
        data.put("facilityName", facilityName);
        data.put("agreementNumber", agreementNumber);
        data.put("submittedDate", java.time.LocalDate.now().toString()); // Default for template

        // Use AGREEMENT_SUBMITTED template
        sendTemplateEmail(hcfEmail, "AGREEMENT_SUBMITTED", data, List.of(pdfPath));
    }

    // Keep basic raw send for system alerts/dev use if needed, but discourage
    // direct usage
    public void sendRawEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("[DEV-EMAIL-RAW] to={} subject={} body={}", to, subject, body);
            return;
        }
        log.info("[EMAIL-RAW] Sending to={} subject={}", to, subject);
    }
}
