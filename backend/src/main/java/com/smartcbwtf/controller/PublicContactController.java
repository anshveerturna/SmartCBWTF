package com.smartcbwtf.controller;

import com.smartcbwtf.domain.ContactMessage;
import com.smartcbwtf.dto.ContactRequestDTO;
import com.smartcbwtf.repository.ContactMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicContactController {

    private final ContactMessageRepository contactMessageRepository;

    public PublicContactController(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
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

        return ResponseEntity.ok().body("Message sent successfully");
    }
}
