package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.DuesClearanceRequest;
import com.smartcbwtf.repository.DuesClearanceRequestRepository;
import com.smartcbwtf.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CBWTF Dues Clearance Controller - CBWTF Admin endpoints for dues
 * verification.
 */
@RestController
@RequestMapping("/api/cbwtf/dues-clearance")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfDuesClearanceController {

        private static final Logger log = LoggerFactory.getLogger(CbwtfDuesClearanceController.class);

        private final DuesClearanceRequestRepository clearanceRepo;
        private final AuditLogService auditLogService;

        public CbwtfDuesClearanceController(
                        DuesClearanceRequestRepository clearanceRepo,
                        AuditLogService auditLogService) {
                this.clearanceRepo = clearanceRepo;
                this.auditLogService = auditLogService;
        }

        @GetMapping
        public ResponseEntity<?> listPending(
                        @RequestParam(required = false, defaultValue = "PENDING") String status) {

                UUID facilityId = TenantContext.getTenantId();

                List<DuesClearanceRequest> requests = status.equals("ALL")
                                ? clearanceRepo.findByFacilityIdOrderByRequestedAtDesc(facilityId)
                                : clearanceRepo.findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                                                facilityId, status);

                return ResponseEntity.ok(Map.of(
                                "requests", requests.stream().map(req -> {
                                        Map<String, Object> item = new HashMap<>();
                                        item.put("id", req.getId().toString());
                                        item.put("hcfId", req.getHcf().getId().toString());
                                        item.put("hcfName", req.getHcf().getName());
                                        item.put("hcfCode", req.getHcf().getCode());
                                        item.put("status", req.getManagementStatus());
                                        item.put("requestedAt", req.getRequestedAt().toString());
                                        if (req.getCbwtfSubmittedAt() != null) {
                                                item.put("submittedAt", req.getCbwtfSubmittedAt().toString());
                                        }
                                        item.put("amountCleared", req.getAmountCleared());
                                        return item;
                                }).toList(),
                                "total", requests.size()));
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> getDetails(@PathVariable UUID id) {
                UUID facilityId = TenantContext.getTenantId();

                return clearanceRepo.findById(id)
                                .filter(req -> req.getFacility().getId().equals(facilityId))
                                .map(req -> {
                                        Map<String, Object> result = new HashMap<>();
                                        result.put("id", req.getId().toString());
                                        result.put("hcfId", req.getHcf().getId().toString());
                                        result.put("hcfName", req.getHcf().getName());
                                        result.put("hcfCode", req.getHcf().getCode());
                                        result.put("agreementId", req.getAgreement().getId().toString());
                                        result.put("status", req.getManagementStatus());
                                        result.put("requestedAt", req.getRequestedAt().toString());
                                        result.put("requestNotes", req.getRequestNotes());
                                        if (req.getCbwtfSubmittedAt() != null) {
                                                result.put("submittedAt", req.getCbwtfSubmittedAt().toString());
                                        }
                                        result.put("amountCleared", req.getAmountCleared());
                                        result.put("cbwtfNotes", req.getCbwtfNotes());
                                        if (req.getApprovedAt() != null) {
                                                result.put("approvedAt", req.getApprovedAt().toString());
                                        }
                                        result.put("rejectionReason", req.getRejectionReason());
                                        return ResponseEntity.ok(result);
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        @PostMapping("/{id}/submit")
        public ResponseEntity<?> submitToManagement(
                        @PathVariable UUID id,
                        @RequestBody SubmitRequest body) {

                UUID facilityId = TenantContext.getTenantId();
                UUID userId = TenantContext.getUserId();

                DuesClearanceRequest request = clearanceRepo.findById(id)
                                .filter(req -> req.getFacility().getId().equals(facilityId))
                                .orElse(null);

                if (request == null) {
                        return ResponseEntity.notFound().build();
                }

                if (!DuesClearanceRequest.Status.PENDING.name().equals(request.getManagementStatus())) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "INVALID_STATUS",
                                        "message", "Request must be in PENDING status to submit"));
                }

                if (body.amountCleared == null || body.amountCleared.compareTo(BigDecimal.ZERO) < 0) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "INVALID_AMOUNT",
                                        "message", "Valid cleared amount is required"));
                }

                request.setCbwtfSubmittedAt(Instant.now());
                request.setCbwtfSubmittedBy(userId);
                request.setAmountCleared(body.amountCleared);
                request.setCbwtfNotes(body.notes);
                request.setManagementStatusEnum(DuesClearanceRequest.Status.SUBMITTED);

                clearanceRepo.save(request);

                auditLogService.log("DUES_CLEARANCE", request.getId(), "SUBMITTED_TO_MANAGEMENT",
                                userId, "Amount: " + body.amountCleared);

                log.info("CBWTF {} submitted clearance request {} to management", facilityId, id);

                return ResponseEntity.ok(Map.of(
                                "id", request.getId().toString(),
                                "status", request.getManagementStatus(),
                                "message", "Request submitted to Top Management for approval"));
        }

        public static class SubmitRequest {
                public BigDecimal amountCleared;
                public String notes;
        }
}
