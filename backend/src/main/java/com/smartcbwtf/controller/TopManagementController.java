package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/top-mgmt/approvals")
@PreAuthorize("hasRole('TOP_MANAGEMENT')")
public class TopManagementController {

    private final DuesClearanceRequestRepository requestRepository;
    private final HcfRepository hcfRepository;

    public TopManagementController(
            DuesClearanceRequestRepository requestRepository,
            HcfRepository hcfRepository) {
        this.requestRepository = requestRepository;
        this.hcfRepository = hcfRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listPendingApprovals() {
        // List only those submitted by CBWTF Admin
        List<DuesClearanceRequest> requests = requestRepository
                .findByManagementStatusOrderByRequestedAtDesc("SUBMITTED");
        return ResponseEntity.ok(requests.stream().map(this::toDTO).toList());
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approveRequest(@PathVariable UUID id) {
        return processApproval(id, true, null);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> rejectRequest(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        return processApproval(id, false, payload.get("reason"));
    }

    @PostMapping("/approve-all")
    @Transactional
    public ResponseEntity<?> approveAll() {
        List<DuesClearanceRequest> pending = requestRepository
                .findByManagementStatusOrderByRequestedAtDesc("SUBMITTED");
        UUID userId = TenantContext.get().userId();
        Instant now = Instant.now();

        int count = 0;
        for (DuesClearanceRequest req : pending) {
            approveSingle(req, userId, now);
            count++;
        }

        return ResponseEntity.ok(Map.of("message", "Approved " + count + " requests"));
    }

    private ResponseEntity<?> processApproval(UUID id, boolean approve, String reason) {
        UUID userId = TenantContext.get().userId();
        DuesClearanceRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

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

            // Reset HCF
            Hcf hcf = request.getHcf();
            hcf.setDuesClearStatus(DuesClearStatus.PENDING);
            hcfRepository.save(hcf);
        }

        return ResponseEntity.ok(toDTO(request));
    }

    private void approveSingle(DuesClearanceRequest req, UUID userId, Instant now) {
        req.setManagementStatusEnum(DuesClearanceRequest.Status.APPROVED);
        req.setApprovedBy(userId);
        req.setApprovedAt(now);
        req.grantReportAccess(); // Sets access timestamps
        requestRepository.save(req);

        // Update HCF Status to CLEARED
        Hcf hcf = req.getHcf();
        hcf.setDuesClearStatus(DuesClearStatus.CLEARED);
        hcfRepository.save(hcf);
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
}
