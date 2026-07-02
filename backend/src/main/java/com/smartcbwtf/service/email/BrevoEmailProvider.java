package com.smartcbwtf.service.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Brevo Email Provider using direct REST API.
 * 
 * Handles email sending via Brevo's Transactional Email API v3.
 * Uses Spring's RestTemplate for HTTP calls - no external SDK needed.
 */
@Service
public class BrevoEmailProvider {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailProvider.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;

    @Value("${app.email.brevo.api-key:}")
    private String apiKey;

    @Value("${app.email.from:info@smartcbwtf.com}")
    private String senderEmail;

    @Value("${app.email.brevo.sender-name:SmartCBWTF}")
    private String senderName;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.attachment-root:files}")
    private String attachmentRoot;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[BREVO] API key not configured - emails will be logged only");
            return;
        }

        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        initialized = true;
        log.info("[BREVO] Email provider initialized. Sender: {} <{}>", senderName, senderEmail);
    }

    /**
     * Send a transactional email via Brevo REST API.
     */
    public String sendEmail(String to, String subject, String htmlContent, List<String> attachmentPaths) {
        if (!emailEnabled) {
            log.info("[BREVO-DEV] Email disabled - logging only:\nTO: {}\nSUBJECT: {}", to, subject);
            return "dev-mode-" + System.currentTimeMillis();
        }

        if (!initialized) {
            log.error("[BREVO] Provider not initialized - cannot send email to {}", to);
            return null;
        }

        try {
            // Build request body
            BrevoEmailRequest request = new BrevoEmailRequest();
            request.sender = new EmailAddress(senderName, senderEmail);
            request.to = List.of(new EmailAddress(null, to));
            request.subject = subject;
            request.htmlContent = htmlContent;

            // Add attachments if any
            if (attachmentPaths != null && !attachmentPaths.isEmpty()) {
                request.attachment = new ArrayList<>();
                for (String path : attachmentPaths) {
                    try {
                        Path file = resolveAllowedAttachmentPath(path);
                        if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
                            log.warn("[BREVO] Attachment file not found or unreadable: {}", safeAttachmentName(path));
                            continue;
                        }
                        long size = Files.size(file);
                        if (size > MAX_ATTACHMENT_BYTES) {
                            log.warn("[BREVO] Attachment skipped because it is too large: {} bytes={}",
                                    file.getFileName(), size);
                            continue;
                        }
                        byte[] content = Files.readAllBytes(file);
                        String base64 = Base64.getEncoder().encodeToString(content);
                        request.attachment.add(new Attachment(file.getFileName().toString(), base64));
                        log.debug("[BREVO] Added attachment: {}", file.getFileName());
                    } catch (IllegalArgumentException | IOException e) {
                        log.warn("[BREVO] Attachment skipped: {} ({})", safeAttachmentName(path), e.getMessage());
                    }
                }
            }

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            headers.set("Accept", "application/json");

            String jsonBody = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // Send request
            ResponseEntity<BrevoResponse> response = restTemplate.exchange(
                    BREVO_API_URL,
                    HttpMethod.POST,
                    entity,
                    BrevoResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = response.getBody().messageId;
                log.info("[BREVO] Email sent successfully to={} messageId={}", to, messageId);
                return messageId;
            } else {
                log.error("[BREVO] Unexpected response: status={}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("[BREVO] Error sending email to {}: {}", to, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Send a simple text email (converted to HTML).
     */
    public String sendSimpleEmail(String to, String subject, String textBody) {
        String htmlContent = wrapInHtmlTemplate(textBody);
        return sendEmail(to, subject, htmlContent, null);
    }

    Path resolveAllowedAttachmentPath(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank() || requestedPath.indexOf('\0') >= 0
                || requestedPath.contains("\\")) {
            throw new IllegalArgumentException("Invalid attachment path");
        }

        Path root = Paths.get(attachmentRoot).toAbsolutePath().normalize();
        Path resolved;
        if (requestedPath.startsWith("/files/")) {
            resolved = root.resolve(requestedPath.substring("/files/".length())).normalize();
        } else if (requestedPath.startsWith("files/")) {
            resolved = root.resolve(requestedPath.substring("files/".length())).normalize();
        } else {
            Path raw = Paths.get(requestedPath);
            resolved = raw.isAbsolute() ? raw.normalize() : raw.toAbsolutePath().normalize();
        }

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Attachment path is outside the allowed root");
        }
        return resolved;
    }

    private String safeAttachmentName(String path) {
        if (path == null || path.isBlank()) {
            return "(blank)";
        }
        try {
            Path filename = Paths.get(path).getFileName();
            return filename != null ? filename.toString() : "(unknown)";
        } catch (Exception e) {
            return "(invalid)";
        }
    }

    /**
     * Wrap plain text in a clean HTML template.
     */
    private String wrapInHtmlTemplate(String text) {
        String escapedText = text.replace("\n", "<br>");
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #1976d2 0%%, #2196f3 100%%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
                        .content { background: #ffffff; padding: 24px; border: 1px solid #e0e0e0; border-top: none; border-radius: 0 0 8px 8px; }
                        .footer { text-align: center; padding-top: 20px; color: #757575; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h2 style="margin: 0;">SmartCBWTF</h2>
                    </div>
                    <div class="content">
                        """
                + escapedText + """
                            </div>
                            <div class="footer">
                                <p>This is an automated email from SmartCBWTF. Please do not reply.</p>
                            </div>
                        </body>
                        </html>
                        """;
    }

    /**
     * Check if provider is ready to send emails.
     */
    public boolean isReady() {
        return emailEnabled && initialized;
    }

    // DTO classes for Brevo API
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class BrevoEmailRequest {
        public EmailAddress sender;
        public List<EmailAddress> to;
        public String subject;
        public String htmlContent;
        public List<Attachment> attachment;
    }

    static class EmailAddress {
        public String name;
        public String email;

        public EmailAddress(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    static class Attachment {
        public String name;
        public String content; // Base64 encoded

        public Attachment(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    static class BrevoResponse {
        @JsonProperty("messageId")
        public String messageId;
    }
}
