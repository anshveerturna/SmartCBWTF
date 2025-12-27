package com.smartcbwtf.controller;

import com.smartcbwtf.dto.HcfApprovalRequest;
import com.smartcbwtf.dto.HcfApprovalResponse;
import com.smartcbwtf.dto.HcfRegistrationRequest;
import com.smartcbwtf.dto.HcfRegistrationResponse;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.HcfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hcfs")
public class HcfController {

    private final HcfService hcfService;
    private final AgreementService agreementService;

    public HcfController(HcfService hcfService, AgreementService agreementService) {
        this.hcfService = hcfService;
        this.agreementService = agreementService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('DRIVER', 'CBWTF_ADMIN')")
    public ResponseEntity<HcfRegistrationResponse> register(@Valid @RequestBody HcfRegistrationRequest request) {
        HcfRegistrationResponse response = hcfService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public List<Hcf> pending() {
        return hcfService.listPending();
    }

    @PostMapping("/{hcfId}/approve")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfApprovalResponse approve(@PathVariable UUID hcfId, @Valid @RequestBody HcfApprovalRequest request) {
        return agreementService.approveHcf(hcfId, request);
    }

    /**
     * Upload rent agreement document (PDF or image).
     * Max size: 10MB.
     */
    @PostMapping("/rent-agreement")
    @PreAuthorize("hasAnyRole('DRIVER', 'CBWTF_ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> uploadRentAgreement(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        // Validate file type (PDF or image)
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("application/pdf") && !contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Only PDF or image files are allowed");
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }

        String ext = switch (contentType) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> "pdf";
        };

        try {
            String uploadDir = "uploads/rent-agreements";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "." + ext;
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/rent-agreements/" + filename;
            return ResponseEntity.ok(java.util.Map.of("url", fileUrl));

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }
}
