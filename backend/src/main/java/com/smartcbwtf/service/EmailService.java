package com.smartcbwtf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${app.email.from:noreply@smartcbwtf.com}")
    private String fromAddress;

    /**
     * Send a simple text email.
     */
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("[DEV-EMAIL] to={} subject={} body={}", to, subject, body);
            return;
        }
        // TODO: Integrate SMTP/SendGrid/SES in production
        log.info("[EMAIL] Sending to={} subject={}", to, subject);
    }
    
    /**
     * Send an email with PDF attachment.
     */
    public void sendEmailWithAttachment(String to, String subject, String body, String attachmentPath) {
        sendEmailWithAttachments(to, subject, body, List.of(attachmentPath));
    }
    
    /**
     * Send an email with multiple attachments.
     */
    public void sendEmailWithAttachments(String to, String subject, String body, List<String> attachmentPaths) {
        if (!emailEnabled) {
            StringBuilder attachInfo = new StringBuilder();
            for (String path : attachmentPaths) {
                File f = new File(path);
                attachInfo.append(String.format("\n  - %s (exists=%s, size=%d bytes)", 
                    path, f.exists(), f.exists() ? f.length() : 0));
            }
            log.info("[DEV-EMAIL] to={} subject={} body={}\n  attachments:{}", 
                to, subject, body, attachInfo);
            return;
        }
        
        // TODO: Integrate SMTP/SendGrid/SES with attachment support in production
        // Example with Spring Mail:
        // MimeMessage message = mailSender.createMimeMessage();
        // MimeMessageHelper helper = new MimeMessageHelper(message, true);
        // helper.setTo(to);
        // helper.setSubject(subject);
        // helper.setText(body, true); // true for HTML
        // for (String attachmentPath : attachmentPaths) {
        //     File file = new File(attachmentPath);
        //     helper.addAttachment(file.getName(), file);
        // }
        // mailSender.send(message);
        
        log.info("[EMAIL] Sending to={} subject={} with {} attachment(s)", 
            to, subject, attachmentPaths.size());
    }
    
    /**
     * Send HCF registration confirmation email with agreement PDF.
     */
    public void sendHcfRegistrationEmail(String hcfEmail, String hcfName, String agreementNumber, 
                                         String facilityName, String pdfPath) {
        String subject = String.format("HCF Registration Confirmation - %s", agreementNumber);
        
        String body = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
            <h2>Healthcare Facility Registration Confirmation</h2>
            <p>Dear %s,</p>
            <p>Your registration with <strong>%s</strong> has been successfully submitted.</p>
            <p><strong>Agreement Number:</strong> %s</p>
            <p>Please find attached your signed agreement document for your records.</p>
            <h3>Next Steps:</h3>
            <ul>
                <li>Our team will review your registration within 2-3 business days</li>
                <li>You will receive approval notification via email</li>
                <li>Once approved, you can begin using the biomedical waste collection services</li>
            </ul>
            <p>If you have any questions, please contact our support team.</p>
            <br>
            <p>Best regards,<br>%s Team</p>
            </body>
            </html>
            """, hcfName, facilityName, agreementNumber, facilityName);
        
        sendEmailWithAttachment(hcfEmail, subject, body, pdfPath);
    }
}
