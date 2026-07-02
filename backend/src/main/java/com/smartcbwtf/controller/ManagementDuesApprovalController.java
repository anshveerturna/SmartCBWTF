package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Management Dues Approval Controller - Top Management endpoints for final
 * approval.
 * 
 * Hard Isolation:
 * - Only TOP_MANAGEMENT_ADMIN role can access
 * - No CBWTF/HCF navigation bleed
 * - Minimal JWT claims (role + userId only)
 * 
 * Management can:
 * - View pending approvals for their assigned CBWTF
 * - Approve requests (grants report access)
 * - Reject requests (with reason)
 * - Bulk approve
 */
@RestController
@RequestMapping("/api/management/dues-approvals")
@PreAuthorize("hasRole('TOP_MANAGEMENT')")
public class ManagementDuesApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ManagementDuesApprovalController.class);
    private static final int DEFAULT_REQUEST_LIST_LIMIT = 100;
    private static final int MAX_REQUEST_LIST_LIMIT = 250;
    private static final int MAX_REASON_LENGTH = 1000;
    private static final int MAX_BULK_APPROVAL_IDS = 100;

    private final DuesClearanceRequestRepository clearanceRepo;
    private final AuditLogService auditLogService;

    private final com.smartcbwtf.repository.HcfRepository hcfRepository;

    public ManagementDuesApprovalController(
            DuesClearanceRequestRepository clearanceRepo,
            AuditLogService auditLogService,
            com.smartcbwtf.repository.HcfRepository hcfRepository) {
        this.clearanceRepo = clearanceRepo;
        this.auditLogService = auditLogService;
        this.hcfRepository = hcfRepository;
    }

    /**
     * List pending approvals - all requests waiting for management decision.
     */
    @GetMapping
    public ResponseEntity<?> listPending(
            @RequestParam(name = "status", required = false, defaultValue = "SUBMITTED") String status,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {

        log.debug("Management listPending called with status: {}", status);

        UUID facilityId = requireTenantId();
        PageRequest pageable = firstPage(limit);
        List<DuesClearanceRequest> requests = status.equals("ALL")
                ? clearanceRepo.findByFacilityIdOrderByRequestedAtDesc(facilityId, pageable)
                : clearanceRepo.findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(facilityId, status,
                        pageable);
        long total = status.equals("ALL")
                ? clearanceRepo.countByFacilityId(facilityId)
                : clearanceRepo.countByFacilityIdAndManagementStatus(facilityId, status);

        log.debug("Found {} of {} requests for status {}", requests.size(), total, status);
        requests.forEach(r -> log.trace("Found request {} with status {}", r.getId(), r.getManagementStatus()));

        return ResponseEntity.ok(Map.of(
                "requests", requests.stream().map(req -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", req.getId().toString());
                    item.put("hcfId", req.getHcf().getId().toString());
                    item.put("hcfName", req.getHcf().getName());
                    item.put("hcfCode", req.getHcf().getCode());
                    item.put("facilityId", req.getFacility().getId().toString());
                    item.put("facilityName", req.getFacility().getName());
                    item.put("status", req.getManagementStatus());
                    item.put("requestedAt", req.getRequestedAt().toString());
                    item.put("amountCleared", req.getAmountCleared());
                    item.put("outstandingDues", req.getOutstandingDues());
                    if (req.getCbwtfSubmittedAt() != null) {
                        item.put("submittedAt", req.getCbwtfSubmittedAt().toString());
                    }
                    if (req.getAgreement() != null) {
                        item.put("agreementNumber", req.getAgreement().getAgreementNumber());
                    } else {
                        item.put("agreementNumber", null);
                    }
                    item.put("cbwtfNotes", req.getCbwtfNotes());
                    item.put("requestMonth", req.getRequestMonth());
                    item.put("requestYear", req.getRequestYear());
                    return item;
                }).toList(),
                "total", total));
    }

    /**
     * Get request details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetails(@PathVariable("id") UUID id) {
        UUID facilityId = requireTenantId();
        return clearanceRepo.findByIdAndFacilityId(id, facilityId)
                .map(req -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", req.getId().toString());
                    result.put("hcfId", req.getHcf().getId().toString());
                    result.put("hcfName", req.getHcf().getName());
                    result.put("hcfCode", req.getHcf().getCode());
                    result.put("facilityId", req.getFacility().getId().toString());
                    result.put("facilityName", req.getFacility().getName());
                    result.put("agreementId", req.getAgreement().getId().toString());
                    result.put("status", req.getManagementStatus());
                    result.put("requestedAt", req.getRequestedAt().toString());
                    result.put("requestNotes", req.getRequestNotes());
                    result.put("amountCleared", req.getAmountCleared());
                    result.put("outstandingDues", req.getOutstandingDues());
                    result.put("cbwtfNotes", req.getCbwtfNotes());
                    result.put("requestMonth", req.getRequestMonth());
                    result.put("requestYear", req.getRequestYear());
                    if (req.getCbwtfSubmittedAt() != null) {
                        result.put("submittedAt", req.getCbwtfSubmittedAt().toString());
                    }
                    if (req.getApprovedAt() != null) {
                        result.put("approvedAt", req.getApprovedAt().toString());
                    }
                    result.put("rejectionReason", req.getRejectionReason());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Approve a dues clearance request.
     * This grants the HCF access to monthly/yearly reports.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable("id") UUID id) {
        UUID userId = TenantContext.getUserId();
        UUID facilityId = requireTenantId();

        DuesClearanceRequest request = clearanceRepo.findByIdAndFacilityId(id, facilityId).orElse(null);

        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!DuesClearanceRequest.Status.SUBMITTED.name().equals(request.getManagementStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_STATUS",
                    "message", "Request must be in SUBMITTED status to approve"));
        }

        // Approve and grant access
        request.setManagementStatusEnum(DuesClearanceRequest.Status.APPROVED);
        request.setApprovedBy(userId);
        request.setApprovedAt(Instant.now());
        request.grantReportAccess();

        clearanceRepo.save(request);

        if (isGlobalDuesRequest(request)) {
            com.smartcbwtf.domain.Hcf hcf = request.getHcf();
            hcf.setDuesClearStatus(com.smartcbwtf.domain.DuesClearStatus.CLEARED);
            hcfRepository.save(hcf);
        }

        auditLogService.log("DUES_CLEARANCE", request.getId(), "APPROVED_BY_MANAGEMENT",
                userId, "Report access granted");

        log.info("Management approved dues clearance {} for HCF {}", id, request.getHcf().getId());

        return ResponseEntity.ok(Map.of(
                "id", request.getId().toString(),
                "status", request.getManagementStatus(),
                "message", "Request approved. Report access granted to HCF."));
    }

    /**
     * Reject a dues clearance request.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectRequest body) {

        UUID userId = TenantContext.getUserId();
        UUID facilityId = requireTenantId();

        DuesClearanceRequest request = clearanceRepo.findByIdAndFacilityId(id, facilityId).orElse(null);

        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!DuesClearanceRequest.Status.SUBMITTED.name().equals(request.getManagementStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_STATUS",
                    "message", "Request must be in SUBMITTED status to reject"));
        }

        String reason = normalizeReason(body != null ? body.reason() : null);
        if (reason == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "REASON_REQUIRED",
                    "message", "Rejection reason is required"));
        }

        request.setManagementStatusEnum(DuesClearanceRequest.Status.REJECTED);
        request.setApprovedBy(userId);
        request.setApprovedAt(Instant.now());
        request.setRejectionReason(reason);

        clearanceRepo.save(request);

        auditLogService.log("DUES_CLEARANCE", request.getId(), "REJECTED_BY_MANAGEMENT",
                userId, "Reason: " + reason);

        log.info("Management rejected dues clearance {} for HCF {}: {}",
                id, request.getHcf().getId(), reason);

        return ResponseEntity.ok(Map.of(
                "id", request.getId().toString(),
                "status", request.getManagementStatus(),
                "message", "Request rejected."));
    }

    /**
     * Bulk approve multiple requests.
     */
    @PostMapping("/bulk-approve")
    public ResponseEntity<?> bulkApprove(@Valid @RequestBody BulkApproveRequest body) {
        UUID userId = TenantContext.getUserId();
        UUID facilityId = requireTenantId();

        if (body == null || body.ids() == null || body.ids().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "NO_IDS",
                    "message", "Request IDs are required"));
        }
        if (body.ids().size() > MAX_BULK_APPROVAL_IDS) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "TOO_MANY_IDS",
                    "message", "Bulk approval is limited to " + MAX_BULK_APPROVAL_IDS + " requests"));
        }

        int approved = 0;
        int failed = 0;
        Map<UUID, DuesClearanceRequest> requestsById = new HashMap<>();
        clearanceRepo.findByFacilityIdAndIdIn(facilityId, body.ids())
                .forEach(request -> requestsById.put(request.getId(), request));

        for (UUID id : body.ids()) {
            DuesClearanceRequest request = requestsById.get(id);
            if (request != null &&
                    DuesClearanceRequest.Status.SUBMITTED.name().equals(request.getManagementStatus())) {

                request.setManagementStatusEnum(DuesClearanceRequest.Status.APPROVED);
                request.setApprovedBy(userId);
                request.setApprovedAt(Instant.now());
                request.grantReportAccess();
                clearanceRepo.save(request);

                if (isGlobalDuesRequest(request)) {
                    com.smartcbwtf.domain.Hcf hcf = request.getHcf();
                    hcf.setDuesClearStatus(com.smartcbwtf.domain.DuesClearStatus.CLEARED);
                    hcfRepository.save(hcf);
                }

                auditLogService.log("DUES_CLEARANCE", request.getId(),
                        "BULK_APPROVED_BY_MANAGEMENT", userId, null);
                approved++;
            } else {
                failed++;
            }
        }

        log.info("Management bulk approved {} requests, {} failed", approved, failed);

        return ResponseEntity.ok(Map.of(
                "approved", approved,
                "failed", failed,
                "message", "Bulk approval completed"));
    }

    public record RejectRequest(
            @NotBlank(message = "Rejection reason is required")
            @Size(max = MAX_REASON_LENGTH, message = "Rejection reason must be 1000 characters or fewer")
            String reason) {
    }

    public record BulkApproveRequest(
            @NotEmpty(message = "Request IDs are required")
            @Size(max = MAX_BULK_APPROVAL_IDS, message = "Bulk approval is limited to 100 requests")
            List<@NotNull(message = "Request ID is required") UUID> ids) {
    }

    private UUID requireTenantId() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context is required");
        }
        return facilityId;
    }

    private static PageRequest firstPage(int requestedLimit) {
        int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_REQUEST_LIST_LIMIT, MAX_REQUEST_LIST_LIMIT);
        return PageRequest.of(0, limit);
    }

    private static String normalizeReason(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rejection reason must be " + MAX_REASON_LENGTH + " characters or fewer");
        }
        return normalized;
    }

    private static boolean isGlobalDuesRequest(DuesClearanceRequest request) {
        return request.getRequestMonth() == null && request.getRequestYear() == null;
    }
}
