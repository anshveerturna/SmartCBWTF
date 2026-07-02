package com.smartcbwtf.controller;

import com.smartcbwtf.domain.ContactMessage;
import com.smartcbwtf.dto.ContactRequestDTO;
import com.smartcbwtf.repository.ContactMessageRepository;
import com.smartcbwtf.service.EmailService;
import com.smartcbwtf.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/public")
public class PublicContactController {

    private static final Logger log = LoggerFactory.getLogger(PublicContactController.class);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final long MAX_SUBMISSIONS_PER_EMAIL = 3;
    private static final long MAX_SUBMISSIONS_PER_SOURCE = 5;
    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final String SUCCESS_MESSAGE = "Message sent successfully";
    private static final Set<String> ORGANIZATION_TYPES = Set.of(
            "CBWTF Operator",
            "Healthcare Facility",
            "Student / Researcher",
            "Consultant / Partner",
            "Other");
    private static final Set<String> INQUIRY_TYPES = Set.of(
            "Request Demo",
            "Pricing",
            "Platform Question",
            "Student / Research",
            "Support",
            "Partnership",
            "Other");

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;

    public PublicContactController(ContactMessageRepository contactMessageRepository, EmailService emailService) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailService = emailService;
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, String>> submitContactForm(
            @Valid @RequestBody ContactRequestDTO request,
            HttpServletRequest servletRequest) {
        if (!isBlank(request.getWebsite())) {
            return successResponse();
        }

        String organizationType = cleanLine(request.getOrganizationType());
        String inquiryType = cleanLine(request.getInquiryType());
        if (!ORGANIZATION_TYPES.contains(organizationType) || !INQUIRY_TYPES.contains(inquiryType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid contact category or inquiry type."));
        }

        String email = cleanLine(request.getEmail()).toLowerCase(Locale.ROOT);
        String sourceIp = ClientIpResolver.resolve(servletRequest);
        Instant windowStart = Instant.now().minus(RATE_LIMIT_WINDOW);

        if (contactMessageRepository.countByEmailIgnoreCaseAndCreatedAtAfter(email, windowStart)
                >= MAX_SUBMISSIONS_PER_EMAIL
                || contactMessageRepository.countBySourceIpAndCreatedAtAfter(sourceIp, windowStart)
                        >= MAX_SUBMISSIONS_PER_SOURCE) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many submissions. Please try again later."));
        }

        ContactMessage message = new ContactMessage();
        message.setName(cleanLine(request.getName()));
        message.setEmail(email);
        message.setPhone(cleanLine(request.getPhone()));
        message.setOrganization(cleanLine(request.getOrganization()));
        message.setOrganizationType(organizationType);
        message.setInquiryType(inquiryType);
        message.setMessage(request.getMessage().trim());
        message.setSourceIp(sourceIp);
        message.setUserAgent(truncate(cleanLine(servletRequest.getHeader("User-Agent")), MAX_USER_AGENT_LENGTH));

        contactMessageRepository.save(message);

        String subject = "New Contact Request from " + message.getName();
        String body = String.format("""
                New contact request received:

                Name: %s
                Organization: %s
                Contact Category: %s
                Inquiry Type: %s
                Email: %s
                Phone: %s

                Message:
                %s
                """,
                message.getName(),
                message.getOrganization(),
                message.getOrganizationType(),
                message.getInquiryType(),
                message.getEmail(),
                message.getPhone(),
                message.getMessage());

        try {
            emailService.sendEmail("info@smartcbwtf.com", subject, body);
        } catch (RuntimeException e) {
            log.warn("Saved contact message {} but notification email failed: {}", message.getId(), e.getMessage());
        }

        return successResponse();
    }

    private static ResponseEntity<Map<String, String>> successResponse() {
        return ResponseEntity.ok(Map.of("message", SUCCESS_MESSAGE));
    }

    private static String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
