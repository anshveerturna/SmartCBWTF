package com.smartcbwtf.controller;

import com.smartcbwtf.domain.ContactMessage;
import com.smartcbwtf.dto.ContactRequestDTO;
import com.smartcbwtf.repository.ContactMessageRepository;
import com.smartcbwtf.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicContactController {

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;

    public PublicContactController(ContactMessageRepository contactMessageRepository, EmailService emailService) {
        this.contactMessageRepository = contactMessageRepository;
        this.emailService = emailService;
    }

    @PostMapping("/contact")
    public ResponseEntity<?> submitContactForm(@RequestBody ContactRequestDTO request) {
        ContactMessage message = new ContactMessage();
        message.setName(request.getName());
        message.setEmail(request.getEmail());
        message.setPhone(request.getPhone());
        message.setOrganization(request.getOrganization());
        message.setMessage(request.getMessage());

        contactMessageRepository.save(message);

        // Send email notification
        String subject = "New Contact Request from " + request.getName();
        String body = String.format("""
                New contact request received:

                Name: %s
                Organization: %s
                Email: %s
                Phone: %s

                Message:
                %s
                """,
                request.getName(),
                request.getOrganization() != null ? request.getOrganization() : "N/A",
                request.getEmail(),
                request.getPhone() != null ? request.getPhone() : "N/A",
                request.getMessage());

        emailService.sendEmail("info@smartcbwtf.com", subject, body);

        return ResponseEntity.ok().body("Message sent successfully");
    }
}
