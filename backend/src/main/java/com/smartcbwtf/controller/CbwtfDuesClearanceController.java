package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
// Using for basic checks if needed, or removing
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/cbwtf/dues-clearance")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfDuesClearanceController {

        private final DuesClearanceRequestRepository requestRepository;
        private final HcfRepository hcfRepository;

        public CbwtfDuesClearanceController(
                        DuesClearanceRequestRepository requestRepository,
                        HcfRepository hcfRepository) {
                this.requestRepository = requestRepository;
                this.hcfRepository = hcfRepository;
        }

        // List all requests for this CBWTF
        @GetMapping
        public ResponseEntity<List<Map<String, Object>>> listRequests(
                        @RequestParam(name = "status", required = false) String status) {

                UUID facilityId = TenantContext.getTenantId();

                List<DuesClearanceRequest> requests;
                if (status != null && !status.isBlank()) {
                        requests = requestRepository
                                        .findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(facilityId, status);
                } else {
                        requests = requestRepository.findByFacilityIdOrderByRequestedAtDesc(facilityId);
                }

                return ResponseEntity.ok(requests.stream().map(this::toDTO).toList());
        }

        // Submit for Top Management Approval
        @PostMapping("/{id}/submit")
        @Transactional
        public ResponseEntity<?> submitForApproval(
                        @PathVariable("id") UUID id,
                        @RequestBody Map<String, Object> payload) {

                UUID facilityId = TenantContext.getTenantId();
                UUID userId = TenantContext.get().userId();

                DuesClearanceRequest request = requestRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

                if (!request.getFacility().getId().equals(facilityId)) {
                        return ResponseEntity.status(403).body("Access denied");
                }

                // Use enum name for comparison
                if (!"PENDING".equals(request.getManagementStatus())) {
                        return ResponseEntity.badRequest().body("Request is not in PENDING state");
                }

                // Amount is MANDATORY for clearance submission
                if (!payload.containsKey("amount") || payload.get("amount") == null) {
                        return ResponseEntity.badRequest().body("Clearance amount is required.");
                }

                BigDecimal amount = new BigDecimal(payload.get("amount").toString());
                request.setAmountCleared(amount);

                String notes = payload.get("notes") != null ? payload.get("notes").toString() : null;
                request.setCbwtfNotes(notes);
                request.setCbwtfSubmittedBy(userId);
                request.setCbwtfSubmittedAt(Instant.now());
                request.setManagementStatusEnum(DuesClearanceRequest.Status.SUBMITTED);

                requestRepository.save(request);

                return ResponseEntity.ok(toDTO(request));
        }

        // Reject Request (Directly reject without sending to Top Mgmt)
        @PostMapping("/{id}/reject")
        @Transactional
        public ResponseEntity<?> rejectRequest(
                        @PathVariable("id") UUID id,
                        @RequestBody Map<String, Object> payload) {

                UUID facilityId = TenantContext.getTenantId();

                DuesClearanceRequest request = requestRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

                if (!request.getFacility().getId().equals(facilityId)) {
                        return ResponseEntity.status(403).body("Access denied");
                }

                String reason = (String) payload.get("reason");
                if (reason == null || reason.isBlank()) {
                        return ResponseEntity.badRequest().body("Rejection reason is required");
                }

                // Amount is MANDATORY for rejection (Outstanding Dues)
                if (!payload.containsKey("amount") || payload.get("amount") == null) {
                        return ResponseEntity.badRequest().body("Outstanding dues amount is required for rejection.");
                }

                BigDecimal outstandingAmount = new BigDecimal(payload.get("amount").toString());
                request.setOutstandingDues(outstandingAmount);

                request.setManagementStatusEnum(DuesClearanceRequest.Status.REJECTED);
                request.setRejectionReason(reason);
                // We set approved/rejected by as the CBWTF admin in this case,
                // effectively acting as management for rejection
                request.setApprovedBy(TenantContext.get().userId());
                request.setApprovedAt(Instant.now());

                requestRepository.save(request);

                // Reset HCF Status so they can request again
                Hcf hcf = request.getHcf();
                hcf.setDuesClearStatus(DuesClearStatus.PENDING);
                hcfRepository.save(hcf);

                return ResponseEntity.ok(toDTO(request));
        }

        private Map<String, Object> toDTO(DuesClearanceRequest req) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", req.getId());
                map.put("hcfName", req.getHcf().getName());
                map.put("hcfCode", req.getHcf().getCode());
                map.put("agreementNumber", req.getAgreement().getAgreementNumber());
                map.put("requestedAt", req.getRequestedAt());
                map.put("status", req.getManagementStatus());
                map.put("amountCleared", req.getAmountCleared());
                map.put("outstandingDues", req.getOutstandingDues());
                map.put("cbwtfNotes", req.getCbwtfNotes());
                map.put("rejectionReason", req.getRejectionReason());
                return map;
        }
}
