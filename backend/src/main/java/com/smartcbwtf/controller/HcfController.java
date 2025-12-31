package com.smartcbwtf.controller;

import com.smartcbwtf.dto.*;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.service.AgreementService;
import com.smartcbwtf.service.HcfApprovalService;
import com.smartcbwtf.service.HcfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hcfs")
public class HcfController {

    private final HcfService hcfService;
    private final AgreementService agreementService;
    private final HcfApprovalService approvalService;

    public HcfController(HcfService hcfService, AgreementService agreementService, HcfApprovalService approvalService) {
        this.hcfService = hcfService;
        this.agreementService = agreementService;
        this.approvalService = approvalService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('DRIVER', 'CBWTF_ADMIN')")
    public ResponseEntity<HcfRegistrationResponse> register(@Valid @RequestBody HcfRegistrationRequest request) {
        HcfRegistrationResponse response = hcfService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * List all HCFs with active agreements for current CBWTF.
     * Used by Android app for attendance marking.
     * Returns HCFs with GPS coordinates for geofence validation.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR', 'CBWTF_ADMIN')")
    public ResponseEntity<List<MobileHcfDto>> listForAttendance() {
        UUID facilityId = com.smartcbwtf.config.TenantContext.getTenantId();
        if (facilityId == null) {
            throw new IllegalStateException("Tenant ID not found in context");
        }
        List<MobileHcfDto> hcfs = hcfService.listActiveHcfsForMobile(facilityId);
        return ResponseEntity.ok(hcfs);
    }

    public record MobileHcfDto(
            String id,
            String name,
            String address,
            String city,
            String state,
            String postalCode,
            String phone,
            Double latitude,
            Double longitude,
            boolean approved) {
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public List<HcfDetailResponse> pending() {
        return hcfService.listPending().stream()
                .map(HcfDetailResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get HCF details by ID.
     */
    @GetMapping("/{hcfId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse getById(@PathVariable UUID hcfId) {
        Hcf hcf = hcfService.findById(hcfId);
        return HcfDetailResponse.from(hcf);
    }

    /**
     * Update HCF billing model and fields.
     * Only allowed if status is PENDING or REJECTED.
     */
    @PutMapping("/{hcfId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse updateHcf(
            @PathVariable UUID hcfId,
            @Valid @RequestBody HcfUpdateRequest request) {
        request.validate();
        Hcf hcf = approvalService.updatePendingHcf(
                hcfId,
                request.billingModel(),
                request.numberOfBeds(),
                request.monthlyCharges());
        return HcfDetailResponse.from(hcf);
    }

    /**
     * Approve HCF with agreement terms.
     * WARNING: Billing model becomes IMMUTABLE after approval.
     */
    @PostMapping("/{hcfId}/approve")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfApprovalResponse approveWithAgreement(@PathVariable UUID hcfId,
            @Valid @RequestBody HcfApprovalRequest request) {
        return agreementService.approveHcf(hcfId, request);
    }

    /**
     * Simple approve without agreement (uses approvalService).
     */
    @PostMapping("/{hcfId}/simple-approve")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse simpleApprove(@PathVariable UUID hcfId) {
        Hcf hcf = approvalService.approve(hcfId);
        return HcfDetailResponse.from(hcf);
    }

    /**
     * Reject HCF with reason.
     */
    @PostMapping("/{hcfId}/reject")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse reject(
            @PathVariable UUID hcfId,
            @Valid @RequestBody HcfRejectRequest request) {
        Hcf hcf = approvalService.reject(hcfId, request.reason());
        return HcfDetailResponse.from(hcf);
    }

    /**
     * Resubmit a rejected HCF for approval.
     */
    @PostMapping("/{hcfId}/resubmit")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse resubmit(@PathVariable UUID hcfId) {
        Hcf hcf = approvalService.resubmit(hcfId);
        return HcfDetailResponse.from(hcf);
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
