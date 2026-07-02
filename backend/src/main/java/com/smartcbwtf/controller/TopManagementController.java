package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.UploadFileValidator;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/top-mgmt/approvals")
@PreAuthorize("hasRole('TOP_MANAGEMENT')")
public class TopManagementController {
    private static final int DEFAULT_REQUEST_LIST_LIMIT = 100;
    private static final int MAX_REQUEST_LIST_LIMIT = 250;
    private static final int APPROVE_ALL_BATCH_SIZE = 250;

    private final DuesClearanceRequestRepository requestRepository;
    private final AgreementCorrectionRequestRepository correctionRequestRepository;
    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final com.smartcbwtf.service.CbwtfHcfService cbwtfHcfService;

    public TopManagementController(
            DuesClearanceRequestRepository requestRepository,
            AgreementCorrectionRequestRepository correctionRequestRepository,
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            com.smartcbwtf.service.CbwtfHcfService cbwtfHcfService) {
        this.requestRepository = requestRepository;
        this.correctionRequestRepository = correctionRequestRepository;
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.cbwtfHcfService = cbwtfHcfService;
    }

    // ==================== DUES CLEARANCE APPROVALS ====================

    @GetMapping("/dues")
    public ResponseEntity<List<Map<String, Object>>> listPendingApprovals(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = requireTenantId();
        List<DuesClearanceRequest> requests = requestRepository
                .findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                        facilityId, "SUBMITTED", firstPage(limit));
        return ResponseEntity.ok(requests.stream().map(this::toDTO).toList());
    }

    @PostMapping("/dues/{id}/approve")
    @Transactional
    public ResponseEntity<?> approveRequest(@PathVariable UUID id) {
        return processApproval(id, true, null);
    }

    @PostMapping("/dues/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest payload) {
        return processApproval(id, false, cleanReason(payload.reason()));
    }

    @PostMapping("/dues/approve-all")
    @Transactional
    public ResponseEntity<?> approveAll() {
        UUID facilityId = requireTenantId();
        UUID userId = requireUserId();

        int count = 0;
        List<DuesClearanceRequest> pending;
        do {
            pending = requestRepository.findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                    facilityId, "SUBMITTED", PageRequest.of(0, APPROVE_ALL_BATCH_SIZE));
            Instant now = Instant.now();
            for (DuesClearanceRequest req : pending) {
                approveSingle(req, userId, now);
                count++;
            }
        } while (!pending.isEmpty());

        return ResponseEntity.ok(Map.of("message", "Approved " + count + " requests"));
    }

    private PageRequest firstPage(int requestedLimit) {
        int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_REQUEST_LIST_LIMIT, MAX_REQUEST_LIST_LIMIT);
        return PageRequest.of(0, limit);
    }

    private ResponseEntity<?> processApproval(UUID id, boolean approve, String reason) {
        UUID facilityId = requireTenantId();
        UUID userId = requireUserId();
        DuesClearanceRequest request = requestRepository.findByIdAndFacilityId(id, facilityId).orElse(null);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"SUBMITTED".equals(request.getManagementStatus())) {
            return ResponseEntity.badRequest().body("Request is not in SUBMITTED state");
        }

        if (approve) {
            approveSingle(request, userId, Instant.now());
        } else {
            request.setManagementStatusEnum(DuesClearanceRequest.Status.REJECTED);
            request.setRejectionReason(reason);
            request.setApprovedBy(userId); // Log who rejected
            request.setApprovedAt(Instant.now());
            requestRepository.save(request);

            if (isGlobalDuesRequest(request)) {
                Hcf hcf = request.getHcf();
                hcf.setDuesClearStatus(DuesClearStatus.PENDING);
                hcfRepository.save(hcf);
            }
        }

        return ResponseEntity.ok(toDTO(request));
    }

    private void approveSingle(DuesClearanceRequest req, UUID userId, Instant now) {
        req.setManagementStatusEnum(DuesClearanceRequest.Status.APPROVED);
        req.setApprovedBy(userId);
        req.setApprovedAt(now);
        req.grantReportAccess(); // Sets access timestamps
        requestRepository.save(req);

        if (isGlobalDuesRequest(req)) {
            Hcf hcf = req.getHcf();
            hcf.setDuesClearStatus(DuesClearStatus.CLEARED);
            hcfRepository.save(hcf);
        }
    }

    private Map<String, Object> toDTO(DuesClearanceRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", req.getId());
        map.put("hcfName", req.getHcf().getName());
        map.put("requestedAt", req.getRequestedAt());
        map.put("amount", req.getAmountCleared());
        map.put("cbwtfNotes", req.getCbwtfNotes());
        // Add CBWTF Admin info if needed
        return map;
    }

    // ==================== HCF REGISTRATION APPROVALS ====================

    @GetMapping("/hcfs")
    public ResponseEntity<List<Map<String, Object>>> listPendingHcfApprovals(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = requireTenantId();
        List<Agreement> pendingAgreements = agreementRepository.findPendingApprovalAgreementsByFacilityId(
                facilityId, firstPage(limit));

        return ResponseEntity.ok(pendingAgreements.stream()
                .map(agreement -> {
                    Hcf hcf = agreement.getHcf();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", hcf.getId());
                    map.put("name", hcf.getName());
                    map.put("code", hcf.getCode());
                    map.put("address", hcf.getAddress());
                    map.put("contactEmail", hcf.getContactEmail());
                    map.put("contactPhone", hcf.getContactPhone());
                    map.put("numberOfBeds", hcf.getNumberOfBeds());
                    map.put("monthlyCharges", hcf.getMonthlyCharges());
                    map.put("requestedAt", hcf.getCreatedAt());
                    map.put("agreementNumber", agreement.getAgreementNumber());
                    map.put("agreementStartDate", agreement.getStartDate());
                    map.put("agreementEndDate", agreement.getEndDate());

                    return map;
                })
                .toList());
    }

    @GetMapping("/hcfs/{id}")
    public ResponseEntity<com.smartcbwtf.dto.HcfDetailDTO> getHcfDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(cbwtfHcfService.getHcfDetailForTopManagement(id, requireTenantId()));
    }

    @GetMapping("/hcfs/{id}/rent-agreement")
    public ResponseEntity<Resource> downloadRentAgreement(@PathVariable UUID id) {
        com.smartcbwtf.dto.HcfDetailDTO detail = cbwtfHcfService.getHcfDetailForTopManagement(id, requireTenantId());
        String rentAgreementUrl = detail.getRentAgreementUrl();
        if (rentAgreementUrl == null || rentAgreementUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = UploadFileValidator.uploadedAssetPath(rentAgreementUrl, "/uploads/rent-agreements/");
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }
            String filename = "Rent_Agreement_" + id + extensionFor(filePath);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(mediaTypeFor(filePath))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new FileSystemResource(filePath));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/hcfs/{id}/approve")
    public ResponseEntity<?> approveHcfRegistration(@PathVariable UUID id) {
        cbwtfHcfService.approveHcfByTopManagement(id, requireTenantId());
        return ResponseEntity.ok(Map.of("message", "HCF successfully approved."));
    }

    @PostMapping("/hcfs/{id}/reject")
    public ResponseEntity<?> rejectHcfRegistration(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest payload) {
        cbwtfHcfService.rejectHcfByTopManagement(id, requireTenantId(), cleanReason(payload.reason()));
        return ResponseEntity.ok(Map.of("message", "HCF successfully rejected."));
    }

    // ==================== CORRECTION REQUESTS ====================

    @GetMapping("/corrections")
    public ResponseEntity<List<Map<String, Object>>> listPendingCorrections(
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        UUID facilityId = requireTenantId();
        List<AgreementCorrectionRequest> pending = correctionRequestRepository
                .findByFacilityIdAndStatusOrderByRequestedAtDesc(
                        facilityId, AgreementCorrectionRequest.Status.PENDING, firstPage(limit));

        return ResponseEntity.ok(pending.stream().map(req -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", req.getId());
            map.put("hcfName", req.getHcf().getName());
            map.put("hcfCode", req.getHcf().getCode());
            map.put("doctorName", req.getHcf().getDoctorName());
            map.put("address", req.getHcf().getAddress());
            map.put("contactPhone", req.getHcf().getContactPhone());
            map.put("agreementNumber", req.getAgreement().getAgreementNumber());
            map.put("fieldName", req.getFieldName());
            map.put("currentValue", req.getCurrentValue());
            map.put("requestedValue", req.getRequestedValue());
            map.put("reason", req.getReason());
            map.put("requestedAt", req.getRequestedAt());
            return map;
        }).toList());
    }

    @PostMapping("/corrections/{id}/approve")
    public ResponseEntity<?> approveCorrection(@PathVariable UUID id) {
        cbwtfHcfService.approveCorrectionRequest(id, requireTenantId(), requireUserId());
        return ResponseEntity.ok(Map.of("message", "Correction request approved and applied."));
    }

    @PostMapping("/corrections/{id}/reject")
    public ResponseEntity<?> rejectCorrection(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest payload) {
        cbwtfHcfService.rejectCorrectionRequest(id, requireTenantId(), cleanReason(payload.reason()), requireUserId());
        return ResponseEntity.ok(Map.of("message", "Correction request rejected."));
    }

    private UUID requireTenantId() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context is required");
        }
        return facilityId;
    }

    private UUID requireUserId() {
        UUID userId = TenantContext.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user context is required");
        }
        return userId;
    }

    private MediaType mediaTypeFor(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String extensionFor(Path filePath) {
        String filename = filePath.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private static String cleanReason(String reason) {
        String cleaned = reason == null ? "" : reason.trim().replaceAll("[\\r\\n\\t]+", " ");
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        return cleaned;
    }

    private static boolean isGlobalDuesRequest(DuesClearanceRequest request) {
        return request.getRequestMonth() == null && request.getRequestYear() == null;
    }

    public record RejectRequest(
            @NotBlank(message = "Reason is required")
            @Size(max = 500, message = "Reason must be 500 characters or less")
            String reason) {
    }
}
