package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.util.PaginationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
// Using for basic checks if needed, or removing
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/cbwtf/dues-clearance")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfDuesClearanceController {
        private static final int DEFAULT_REQUEST_LIST_LIMIT = 100;
        private static final int MAX_REQUEST_LIST_LIMIT = 250;
        private static final int MAX_CLEARANCE_NOTE_LENGTH = 1000;
        private static final int MONEY_INTEGER_DIGITS = 14;

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
                        @RequestParam(name = "status", required = false) String status,
                        @RequestParam(name = "limit", defaultValue = "100") int limit) {

                UUID facilityId = TenantContext.getTenantId();
                PageRequest pageable = firstPage(limit);

                List<DuesClearanceRequest> requests;
                if (status != null && !status.isBlank()) {
                        requests = requestRepository
                                        .findByFacilityIdAndManagementStatusOrderByRequestedAtDesc(
                                                        facilityId, status, pageable);
                } else {
                        requests = requestRepository.findByFacilityIdOrderByRequestedAtDesc(facilityId, pageable);
                }

                return ResponseEntity.ok(requests.stream().map(this::toDTO).toList());
        }

        // Submit for Top Management Approval
        @PostMapping("/{id}/submit")
        @Transactional
        public ResponseEntity<?> submitForApproval(
                        @PathVariable("id") UUID id,
                        @Valid @RequestBody SubmitForApprovalRequest payload) {

                UUID facilityId = TenantContext.getTenantId();
                UUID userId = TenantContext.get().userId();

                DuesClearanceRequest request = requestRepository.findByIdAndFacilityId(id, facilityId)
                                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

                // Use enum name for comparison
                if (!"PENDING".equals(request.getManagementStatus())) {
                        return ResponseEntity.badRequest().body("Request is not in PENDING state");
                }

                if (payload == null || payload.amount() == null) {
                        return ResponseEntity.badRequest().body("Clearance amount is required.");
                }
                BigDecimal amount = requireNonNegativeMoney(payload.amount(), "Clearance amount");

                request.setAmountCleared(amount);

                String notes = normalizeOptionalText(payload.notes(), MAX_CLEARANCE_NOTE_LENGTH, "Notes");
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
                        @Valid @RequestBody RejectClearanceRequest payload) {

                UUID facilityId = TenantContext.getTenantId();

                DuesClearanceRequest request = requestRepository.findByIdAndFacilityId(id, facilityId)
                                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

                String reason = payload != null ? normalizeRequiredText(payload.reason(), MAX_CLEARANCE_NOTE_LENGTH,
                                "Rejection reason") : null;
                if (reason == null || reason.isBlank()) {
                        return ResponseEntity.badRequest().body("Rejection reason is required");
                }

                if (payload.amount() == null) {
                        return ResponseEntity.badRequest().body("Outstanding dues amount is required for rejection.");
                }
                BigDecimal outstandingAmount = requireNonNegativeMoney(payload.amount(), "Outstanding dues amount");

                request.setOutstandingDues(outstandingAmount);

                request.setManagementStatusEnum(DuesClearanceRequest.Status.REJECTED);
                request.setRejectionReason(reason);
                // We set approved/rejected by as the CBWTF admin in this case,
                // effectively acting as management for rejection
                request.setApprovedBy(TenantContext.get().userId());
                request.setApprovedAt(Instant.now());

                requestRepository.save(request);

                if (isGlobalDuesRequest(request)) {
                        Hcf hcf = request.getHcf();
                        hcf.setDuesClearStatus(DuesClearStatus.PENDING);
                        hcfRepository.save(hcf);
                }

                return ResponseEntity.ok(toDTO(request));
        }

        public record SubmitForApprovalRequest(
                        @NotNull(message = "Amount is required") @DecimalMin(value = "0.00", message = "Amount cannot be negative") @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2, message = "Amount must have at most 2 decimal places") BigDecimal amount,
                        @Size(max = MAX_CLEARANCE_NOTE_LENGTH, message = "Notes must be 1000 characters or fewer") String notes) {
        }

        public record RejectClearanceRequest(
                        @NotBlank(message = "Rejection reason is required") @Size(max = MAX_CLEARANCE_NOTE_LENGTH, message = "Rejection reason must be 1000 characters or fewer") String reason,
                        @NotNull(message = "Amount is required") @DecimalMin(value = "0.00", message = "Amount cannot be negative") @Digits(integer = MONEY_INTEGER_DIGITS, fraction = 2, message = "Amount must have at most 2 decimal places") BigDecimal amount) {
        }

        private Map<String, Object> toDTO(DuesClearanceRequest req) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", req.getId());
                map.put("hcfName", req.getHcf().getName());
                map.put("hcfCode", req.getHcf().getCode());
                if (req.getAgreement() != null) {
                        map.put("agreementNumber", req.getAgreement().getAgreementNumber());
                } else {
                        map.put("agreementNumber", null);
                }
                map.put("requestedAt", req.getRequestedAt());
                map.put("status", req.getManagementStatus());
                map.put("amountCleared", req.getAmountCleared());
                map.put("outstandingDues", req.getOutstandingDues());
                map.put("cbwtfNotes", req.getCbwtfNotes());
                map.put("rejectionReason", req.getRejectionReason());
                return map;
        }

        private static PageRequest firstPage(int requestedLimit) {
                int limit = PaginationUtils.normalizeSize(requestedLimit, DEFAULT_REQUEST_LIST_LIMIT,
                                MAX_REQUEST_LIST_LIMIT);
                return PageRequest.of(0, limit);
        }

        private static BigDecimal requireNonNegativeMoney(BigDecimal amount, String fieldName) {
                if (amount.signum() < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot be negative");
                }
                if (amount.stripTrailingZeros().scale() > 2) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        fieldName + " must have at most 2 decimal places");
                }
                if (amount.precision() - Math.max(amount.scale(), 0) > MONEY_INTEGER_DIGITS) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is too large");
                }
                return amount;
        }

        private static String normalizeOptionalText(String value, int maxLength, String fieldName) {
                if (value == null) {
                        return null;
                }
                String normalized = value.strip();
                if (normalized.isBlank()) {
                        return null;
                }
                if (normalized.length() > maxLength) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        fieldName + " must be " + maxLength + " characters or fewer");
                }
                return normalized;
        }

        private static String normalizeRequiredText(String value, int maxLength, String fieldName) {
                String normalized = normalizeOptionalText(value, maxLength, fieldName);
                if (normalized == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
                }
                return normalized;
        }

        private static boolean isGlobalDuesRequest(DuesClearanceRequest request) {
                return request.getRequestMonth() == null && request.getRequestYear() == null;
        }
}
