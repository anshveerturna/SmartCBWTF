package com.smartcbwtf.controller;

import com.smartcbwtf.dto.*;
import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.service.HcfService;
import com.smartcbwtf.service.UploadFileValidator;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hcfs")
public class HcfController {
    private static final int DEFAULT_PENDING_LIMIT = 100;
    private static final int MAX_PENDING_LIMIT = 250;

    private final HcfService hcfService;
    private final AgreementRepository agreementRepository;

    public HcfController(HcfService hcfService, AgreementRepository agreementRepository) {
        this.hcfService = hcfService;
        this.agreementRepository = agreementRepository;
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('DRIVER', 'CBWTF_ADMIN')")
    public ResponseEntity<HcfRegistrationResponse> register(@Valid @RequestBody HcfRegistrationRequest request) {
        bindRegistrationToAuthenticatedTenant(request);
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
        UUID facilityId = requireTenantId();
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
    public List<HcfDetailResponse> pending(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = requireTenantId();
        return agreementRepository.findLatestPendingHcfAgreementsByFacilityId(facilityId, firstPage(limit)).stream()
                .map(agreement -> agreement.getHcf())
                .map(HcfDetailResponse::from)
                .toList();
    }

    private static PageRequest firstPage(int requestedLimit) {
        int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_PENDING_LIMIT, MAX_PENDING_LIMIT);
        return PageRequest.of(0, limit);
    }

    /**
     * Get HCF details by ID.
     */
    @GetMapping("/{hcfId}")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse getById(@PathVariable UUID hcfId) {
        requireHcfInTenant(hcfId);
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
        throw legacyWorkflowGone("/api/cbwtf/hcfs/" + hcfId + "/billing-model");
    }

    /**
     * Approve HCF with agreement terms.
     * WARNING: Billing model becomes IMMUTABLE after approval.
     */
    @PostMapping("/{hcfId}/approve")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfApprovalResponse approveWithAgreement(@PathVariable UUID hcfId,
            @Valid @RequestBody HcfApprovalRequest request) {
        throw legacyWorkflowGone("/api/cbwtf/hcfs/" + hcfId + "/approve");
    }

    /**
     * Simple approve without agreement (uses approvalService).
     */
    @PostMapping("/{hcfId}/simple-approve")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse simpleApprove(@PathVariable UUID hcfId) {
        throw legacyWorkflowGone("/api/cbwtf/hcfs/" + hcfId + "/approve");
    }

    /**
     * Reject HCF with reason.
     */
    @PostMapping("/{hcfId}/reject")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse reject(
            @PathVariable UUID hcfId,
            @Valid @RequestBody HcfRejectRequest request) {
        throw legacyWorkflowGone("/api/cbwtf/hcfs/" + hcfId + "/reject");
    }

    /**
     * Resubmit a rejected HCF for approval.
     */
    @PostMapping("/{hcfId}/resubmit")
    @PreAuthorize("hasRole('CBWTF_ADMIN')")
    public HcfDetailResponse resubmit(@PathVariable UUID hcfId) {
        throw legacyWorkflowGone("/api/cbwtf/hcfs/" + hcfId + "/resubmit");
    }

    /**
     * Upload rent agreement document (PDF or image).
     * Max size: 10MB.
     */
    @PostMapping("/rent-agreement")
    @PreAuthorize("hasAnyRole('DRIVER', 'CBWTF_ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> uploadRentAgreement(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UUID facilityId = requireTenantId();

        String ext = UploadFileValidator.rentAgreementExtension(file, 10L * 1024L * 1024L);

        try {
            String uploadDir = "uploads/rent-agreements";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String filename = facilityId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/rent-agreements/" + filename;
            return ResponseEntity.ok(java.util.Map.of("url", fileUrl));

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    private void bindRegistrationToAuthenticatedTenant(HcfRegistrationRequest request) {
        UUID facilityId = requireTenantId();
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user context is required");
        }
        if (request.getFacilityId() != null && !facilityId.equals(request.getFacilityId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration facility does not match tenant");
        }
        if (request.getRegisteredByUserId() != null && !userId.equals(request.getRegisteredByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registration user does not match token");
        }
        request.setFacilityId(facilityId);
        request.setRegisteredByUserId(userId);
    }

    private UUID requireTenantId() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context is required");
        }
        return facilityId;
    }

    private void requireHcfInTenant(UUID hcfId) {
        UUID facilityId = requireTenantId();
        if (agreementRepository.findAllByHcfIdAndFacilityId(hcfId, facilityId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "HCF not found");
        }
    }

    private ResponseStatusException legacyWorkflowGone(String replacementEndpoint) {
        return new ResponseStatusException(HttpStatus.GONE,
                "This legacy workflow endpoint has been retired. Use " + replacementEndpoint);
    }
}
