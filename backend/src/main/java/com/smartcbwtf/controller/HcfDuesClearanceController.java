package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HCF Dues Clearance Controller - HCF Admin endpoints for report access
 * requests.
 */
@RestController
@RequestMapping("/api/hcf/dues")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfDuesClearanceController {

        private static final Logger log = LoggerFactory.getLogger(HcfDuesClearanceController.class);

        private final DuesClearanceRequestRepository clearanceRepo;
        private final HcfRepository hcfRepo;
        private final AgreementRepository agreementRepo;
        private final HcfAccessGuard accessGuard;

        public HcfDuesClearanceController(
                        DuesClearanceRequestRepository clearanceRepo,
                        HcfRepository hcfRepo,
                        AgreementRepository agreementRepo,
                        HcfAccessGuard accessGuard) {
                this.clearanceRepo = clearanceRepo;
                this.hcfRepo = hcfRepo;
                this.agreementRepo = agreementRepo;
                this.accessGuard = accessGuard;
        }

        @PostMapping("/request")
        public ResponseEntity<?> requestReportAccess(@RequestBody(required = false) DuesRequestBody body) {
                UUID hcfId = TenantContext.getHcfId();
                UUID userId = TenantContext.getUserId();
                accessGuard.assertPortalAccess(hcfId);

                boolean hasPending = clearanceRepo.existsByHcfIdAndManagementStatusIn(
                                hcfId, List.of("PENDING", "SUBMITTED"));
                if (hasPending) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "REQUEST_EXISTS",
                                        "message", "A clearance request is already pending"));
                }

                Hcf hcf = hcfRepo.findById(hcfId).orElseThrow();
                List<Agreement> agreements = agreementRepo.findByHcfIdAndStatus(hcfId, Agreement.Status.ACTIVE.name());

                if (agreements.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "NO_ACTIVE_AGREEMENT",
                                        "message", "No active agreement found for your facility"));
                }

                Agreement agreement = agreements.get(0);
                DuesClearanceRequest request = new DuesClearanceRequest();
                request.setHcf(hcf);
                request.setAgreement(agreement);
                request.setFacility(agreement.getFacility());
                request.setRequestedBy(userId);
                request.setRequestNotes(body != null ? body.notes : null);

                clearanceRepo.save(request);
                log.info("HCF {} created dues clearance request {}", hcfId, request.getId());

                return ResponseEntity.ok(Map.of(
                                "id", request.getId().toString(),
                                "status", request.getManagementStatus(),
                                "message", "Report access request submitted successfully"));
        }

        @GetMapping("/status")
        public ResponseEntity<?> getStatus() {
                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                return clearanceRepo.findTopByHcfIdOrderByRequestedAtDesc(hcfId)
                                .map(req -> {
                                        Map<String, Object> result = new HashMap<>();
                                        result.put("id", req.getId().toString());
                                        result.put("status", req.getManagementStatus());
                                        result.put("requestedAt", req.getRequestedAt().toString());
                                        result.put("hasReportAccess", req.hasReportAccess());
                                        if (req.getCbwtfSubmittedAt() != null) {
                                                result.put("submittedAt", req.getCbwtfSubmittedAt().toString());
                                        }
                                        if (req.getApprovedAt() != null) {
                                                result.put("approvedAt", req.getApprovedAt().toString());
                                        }
                                        if (req.getRejectionReason() != null) {
                                                result.put("rejectionReason", req.getRejectionReason());
                                        }
                                        return ResponseEntity.ok(result);
                                })
                                .orElse(ResponseEntity.ok(Map.of(
                                                "status", "NONE",
                                                "hasReportAccess", false,
                                                "message", "No clearance request found")));
        }

        @GetMapping("/history")
        public ResponseEntity<?> getHistory() {
                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                List<DuesClearanceRequest> requests = clearanceRepo.findByHcfIdOrderByRequestedAtDesc(hcfId);
                return ResponseEntity.ok(Map.of(
                                "requests", requests.stream().map(req -> Map.of(
                                                "id", req.getId().toString(),
                                                "status", req.getManagementStatus(),
                                                "requestedAt", req.getRequestedAt().toString(),
                                                "hasReportAccess", req.hasReportAccess())).toList()));
        }

        @GetMapping("/report-access")
        public ResponseEntity<?> checkReportAccess() {
                UUID hcfId = TenantContext.getHcfId();
                accessGuard.assertPortalAccess(hcfId);

                return clearanceRepo.findTopByHcfIdAndManagementStatusOrderByRequestedAtDesc(
                                hcfId, DuesClearanceRequest.Status.APPROVED.name())
                                .filter(DuesClearanceRequest::hasReportAccess)
                                .map(req -> ResponseEntity.ok(Map.of(
                                                "hasAccess", true,
                                                "grantedAt", req.getReportsAccessGrantedAt().toString(),
                                                "clearanceId", req.getId().toString())))
                                .orElse(ResponseEntity.ok(Map.of(
                                                "hasAccess", false,
                                                "message", "Access Restricted — Dues Pending")));
        }

        public static class DuesRequestBody {
                public String notes;
        }
}
