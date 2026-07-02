package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.PaymentService;
import com.smartcbwtf.service.ReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Payment Controller.
 * Record and view payments, download receipts, reverse payments.
 */
@RestController
@RequestMapping("/api/cbwtf/payments")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class PaymentController {

        private static final int MAX_PAYMENT_REFERENCE_LENGTH = 100;
        private static final int MAX_PAYER_NAME_LENGTH = 255;
        private static final int MAX_PAYMENT_NOTES_LENGTH = 2000;
        private static final int MAX_REVERSAL_REASON_LENGTH = 1000;

        private final PaymentService paymentService;
        private final PaymentRepository paymentRepository;
        private final InvoicePaymentRepository invoicePaymentRepository;
        private final PaymentReversalRepository reversalRepository;
        private final ReceiptService receiptService;

        public PaymentController(
                        PaymentService paymentService,
                        PaymentRepository paymentRepository,
                        InvoicePaymentRepository invoicePaymentRepository,
                        PaymentReversalRepository reversalRepository,
                        ReceiptService receiptService) {
                this.paymentService = paymentService;
                this.paymentRepository = paymentRepository;
                this.invoicePaymentRepository = invoicePaymentRepository;
                this.reversalRepository = reversalRepository;
                this.receiptService = receiptService;
        }

        /**
         * Record a new payment.
         */
        @PostMapping
        public ResponseEntity<?> recordPayment(
                        @Valid @RequestBody RecordPaymentRequest request) {

                UUID facilityId = TenantContext.getTenantId();
                if (facilityId == null) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No facility context"));
                }

                UUID userId = TenantContext.getUserId();

                var serviceRequest = new PaymentService.RecordPaymentRequest(
                                facilityId,
                                request.hcfId(),
                                request.bankAccountId(),
                                request.paymentDate(),
                                request.amount(),
                                request.mode(),
                                trimToNull(request.referenceNumber()),
                                trimToNull(request.payerName()),
                                trimToNull(request.notes()),
                                userId);

                var result = paymentService.recordPayment(serviceRequest);

                if (!result.success()) {
                        return ResponseEntity.badRequest().body(Map.of("error", result.error()));
                }

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "paymentId", result.paymentId(),
                                "receiptNumber", result.receiptNumber(),
                                "allocation", result.allocation()));
        }

        /**
         * Reverse a payment.
         * Creates counter-entries, never deletes.
         */
        @PostMapping("/{id}/reverse")
        public ResponseEntity<?> reversePayment(
                        @PathVariable UUID id,
                        @Valid @RequestBody ReversePaymentRequest request) {

                UUID facilityId = TenantContext.getTenantId();

                // Verify payment belongs to facility
                if (findTenantPayment(id).isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                UUID userId = TenantContext.getUserId();

                try {
                        String reason = trimRequired(request.reason());
                        var result = paymentService.reversePayment(facilityId, id, reason, userId);
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "reversalPaymentId", result.reversalPaymentId(),
                                        "reversalId", result.reversalId()));
                } catch (IllegalArgumentException | IllegalStateException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                }
        }

        /**
         * Download receipt PDF.
         */
        @GetMapping("/{id}/receipt")
        public ResponseEntity<?> downloadReceipt(@PathVariable UUID id) {
                // Verify payment belongs to facility
                if (findTenantPayment(id).isEmpty()) {
                        return ResponseEntity.notFound().build();
                }

                try {
                        var receipt = receiptService.getReceipt(id);
                        return ResponseEntity.ok()
                                        .cacheControl(CacheControl.noStore())
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=\""
                                                                        + receipt.getReceiptNumber().replace("/", "-")
                                                                        + ".pdf\"")
                                        .contentType(MediaType.APPLICATION_PDF)
                                        .body(receipt.getPdfBytes());
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.notFound().build();
                }
        }

        /**
         * List payments for facility.
         */
        @GetMapping
        public ResponseEntity<Page<PaymentDTO>> listPayments(
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "20") int size,
                        @RequestParam(name = "hcfId", required = false) UUID hcfId) {

                UUID facilityId = TenantContext.getTenantId();
                var pageable = pageRequest(page, size, 20, Sort.by(Sort.Direction.DESC, "paymentDate"));

                Page<Payment> payments;
                if (hcfId != null) {
                        payments = paymentService.getPaymentsForHcf(facilityId, hcfId, pageable);
                } else {
                        payments = paymentService.getPayments(facilityId, pageable);
                }

                Set<UUID> reversedPaymentIds = reversedPaymentIds(payments.getContent());
                Set<UUID> reversalEntryIds = reversalEntryIds(payments.getContent());
                return ResponseEntity.ok(payments.map(payment -> toDTO(payment, reversedPaymentIds, reversalEntryIds)));
        }

        /**
         * Get payment detail.
         */
        @GetMapping("/{id}")
        public ResponseEntity<PaymentDetailDTO> getPayment(@PathVariable UUID id) {
                UUID facilityId = TenantContext.getTenantId();

                return paymentRepository.findByIdAndFacilityId(id, facilityId)
                                .map(payment -> {
                                        var allocations = invoicePaymentRepository.findByPaymentId(payment.getId());
                                        boolean isReversed = paymentService.isReversed(payment.getId());

                                        return ResponseEntity.ok(new PaymentDetailDTO(
                                                        toDTO(payment),
                                                        allocations.stream().map(ip -> new AllocationDTO(
                                                                        ip.getInvoice().getId(),
                                                                        ip.getInvoice().getInvoiceNumber(),
                                                                        ip.getAllocatedAmount(),
                                                                        ip.getAllocatedAt().toString())).toList(),
                                                        isReversed));
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * Get reconciliation summary.
         */
        @GetMapping("/summary")
        public ResponseEntity<ReconciliationSummaryDTO> getSummary() {
                UUID facilityId = TenantContext.getTenantId();

                BigDecimal collectedMTD = paymentService.getTotalCollectedMTD(facilityId);
                BigDecimal outstanding = paymentService.getTotalOutstanding(facilityId);
                BigDecimal totalAdvance = paymentService.getTotalAdvanceBalance(facilityId);

                return ResponseEntity.ok(new ReconciliationSummaryDTO(
                                outstanding,
                                collectedMTD,
                                totalAdvance));
        }

        /**
         * Get HCF advance balance.
         */
        @GetMapping("/hcf/{hcfId}/advance")
        public ResponseEntity<Map<String, BigDecimal>> getHcfAdvance(@PathVariable UUID hcfId) {
                UUID facilityId = TenantContext.getTenantId();
                BigDecimal balance = paymentService.getAdvanceBalance(facilityId, hcfId);
                return ResponseEntity.ok(Map.of("advanceBalance", balance));
        }

        // ========== DTOs ==========

        private Optional<Payment> findTenantPayment(UUID paymentId) {
                return paymentRepository.findByIdAndFacilityId(paymentId, TenantContext.getTenantId());
        }

        private PaymentDTO toDTO(Payment p) {
                boolean isReversed = reversalRepository.existsByOriginalPaymentId(p.getId());
                boolean isReversalEntry = reversalRepository.existsByReversalPaymentId(p.getId());
                return toDTO(p, isReversed, isReversalEntry);
        }

        private PaymentDTO toDTO(Payment p, Set<UUID> reversedPaymentIds, Set<UUID> reversalEntryIds) {
                return toDTO(p, reversedPaymentIds.contains(p.getId()), reversalEntryIds.contains(p.getId()));
        }

        private PaymentDTO toDTO(Payment p, boolean isReversed, boolean isReversalEntry) {
                String hcfName = p.getHcf() != null ? p.getHcf().getName() : "Unknown";
                String bankName = p.getBankAccount() != null ? p.getBankAccount().getBankName() : null;

                return new PaymentDTO(
                                p.getId(),
                                p.getHcf().getId(),
                                hcfName,
                                p.getPaymentDate().toString(),
                                p.getAmount(),
                                p.getMode().name(),
                                p.getReferenceNumber(),
                                p.getPayerName(),
                                bankName,
                                p.getCreatedAt().toString(),
                                isReversed,
                                isReversalEntry);
        }

        private Set<UUID> reversedPaymentIds(List<Payment> payments) {
                List<UUID> paymentIds = paymentIds(payments);
                if (paymentIds.isEmpty()) {
                        return Set.of();
                }
                return reversalRepository.findOriginalPaymentIdsIn(paymentIds).stream().collect(Collectors.toSet());
        }

        private Set<UUID> reversalEntryIds(List<Payment> payments) {
                List<UUID> paymentIds = paymentIds(payments);
                if (paymentIds.isEmpty()) {
                        return Set.of();
                }
                return reversalRepository.findReversalPaymentIdsIn(paymentIds).stream().collect(Collectors.toSet());
        }

        private List<UUID> paymentIds(List<Payment> payments) {
                return payments.stream().map(Payment::getId).toList();
        }

        public record RecordPaymentRequest(
                        @NotNull UUID hcfId,
                        UUID bankAccountId,
                        @NotNull @PastOrPresent LocalDate paymentDate,
                        @NotNull @Positive @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
                        @NotNull PaymentMode mode,
                        @Size(max = MAX_PAYMENT_REFERENCE_LENGTH) String referenceNumber,
                        @Size(max = MAX_PAYER_NAME_LENGTH) String payerName,
                        @Size(max = MAX_PAYMENT_NOTES_LENGTH) String notes) {
        }

        public record ReversePaymentRequest(
                        @NotBlank @Size(max = MAX_REVERSAL_REASON_LENGTH) String reason) {
        }

        public record PaymentDTO(
                        UUID id,
                        UUID hcfId,
                        String hcfName,
                        String paymentDate,
                        BigDecimal amount,
                        String mode,
                        String referenceNumber,
                        String payerName,
                        String bankName,
                        String createdAt,
                        boolean isReversed,
                        boolean isReversalEntry) {
        }

        public record PaymentDetailDTO(
                        PaymentDTO payment,
                        java.util.List<AllocationDTO> allocations,
                        boolean isReversed) {
        }

        public record AllocationDTO(
                        UUID invoiceId,
                        String invoiceNumber,
                        BigDecimal amount,
                        String allocatedAt) {
        }

        public record ReconciliationSummaryDTO(
                        BigDecimal totalOutstanding,
                        BigDecimal collectedMTD,
                        BigDecimal totalAdvance) {
        }

        private static String trimRequired(String value) {
                if (value == null || value.isBlank()) {
                        throw new IllegalArgumentException("Required value is blank");
                }
                return value.trim();
        }

        private static String trimToNull(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }
                return value.trim();
        }
}
