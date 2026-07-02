package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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
        private static final int DEFAULT_HISTORY_LIMIT = 100;
        private static final int MAX_HISTORY_LIMIT = 250;
        private static final int MAX_REQUEST_NOTE_LENGTH = 1000;

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
        public ResponseEntity<?> requestReportAccess(@Valid @RequestBody(required = false) DuesRequestBody body) {
                UUID hcfId = TenantContext.getHcfId();
                UUID facilityId = TenantContext.getTenantId();
                UUID userId = TenantContext.getUserId();
                accessGuard.assertPortalAccess(hcfId, facilityId);

                boolean hasPending = clearanceRepo.existsByHcfIdAndFacilityIdAndManagementStatusIn(
                                hcfId, facilityId, List.of("PENDING", "SUBMITTED"));
                if (hasPending) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "REQUEST_EXISTS",
                                        "message", "A clearance request is already pending"));
                }

                Hcf hcf = hcfRepo.findByIdAndFacilityId(hcfId, facilityId).orElseThrow();
                Agreement agreement = agreementRepo.findActiveByHcfAndFacility(hcfId, facilityId).orElse(null);
                if (agreement == null) {
                        return ResponseEntity.badRequest().body(Map.of(
                                        "error", "NO_ACTIVE_AGREEMENT",
                                        "message", "No active agreement found for your facility"));
                }

                DuesClearanceRequest request = new DuesClearanceRequest();
                request.setHcf(hcf);
                request.setAgreement(agreement);
                request.setFacility(agreement.getFacility());
                request.setRequestedBy(userId);
                request.setRequestNotes(normalizeOptionalNote(body != null ? body.notes : null));

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
                UUID facilityId = TenantContext.getTenantId();
                accessGuard.assertPortalAccess(hcfId, facilityId);

                return clearanceRepo.findTopByHcfIdAndFacilityIdOrderByRequestedAtDesc(hcfId, facilityId)
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
        public ResponseEntity<?> getHistory(@RequestParam(name = "limit", defaultValue = "100") int limit) {
                UUID hcfId = TenantContext.getHcfId();
                UUID facilityId = TenantContext.getTenantId();
                accessGuard.assertPortalAccess(hcfId, facilityId);

                List<DuesClearanceRequest> requests = clearanceRepo.findByHcfIdAndFacilityIdOrderByRequestedAtDesc(
                                hcfId, facilityId, firstPage(limit));
                return privateResponse(Map.of(
                                "requests", requests.stream().map(req -> Map.of(
                                                "id", req.getId().toString(),
                                                "status", req.getManagementStatus(),
                                                "requestedAt", req.getRequestedAt().toString(),
                                                "hasReportAccess", req.hasReportAccess())).toList()));
        }

        private static <T> ResponseEntity<T> privateResponse(T body) {
                return ResponseEntity.ok()
                                .cacheControl(CacheControl.noStore())
                                .header(HttpHeaders.PRAGMA, "no-cache")
                                .body(body);
        }

        @GetMapping("/report-access")
        public ResponseEntity<?> checkReportAccess() {
                UUID hcfId = TenantContext.getHcfId();
                UUID facilityId = TenantContext.getTenantId();
                accessGuard.assertPortalAccess(hcfId, facilityId);

                return clearanceRepo.findTopByHcfIdAndFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                                hcfId, facilityId, DuesClearanceRequest.Status.APPROVED.name())
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
                @Size(max = MAX_REQUEST_NOTE_LENGTH, message = "Notes must be 1000 characters or fewer")
                public String notes;
        }

        private static PageRequest firstPage(int requestedLimit) {
                int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_HISTORY_LIMIT, MAX_HISTORY_LIMIT);
                return PageRequest.of(0, limit);
        }

        private static String normalizeOptionalNote(String notes) {
                if (notes == null) {
                        return null;
                }
                String normalized = notes.strip();
                if (normalized.isBlank()) {
                        return null;
                }
                if (normalized.length() > MAX_REQUEST_NOTE_LENGTH) {
                        throw new IllegalArgumentException(
                                        "Notes must be " + MAX_REQUEST_NOTE_LENGTH + " characters or fewer");
                }
                return normalized;
        }
}
