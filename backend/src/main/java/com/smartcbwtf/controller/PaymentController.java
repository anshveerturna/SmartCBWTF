package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.PaymentService;
import com.smartcbwtf.service.ReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Controller.
 * Record and view payments, download receipts, reverse payments.
 */
@RestController
@RequestMapping("/api/cbwtf/payments")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class PaymentController {

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
                        @Valid @RequestBody RecordPaymentRequest request,
                        @AuthenticationPrincipal UserDetails user) {

                UUID facilityId = TenantContext.getTenantId();
                if (facilityId == null) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No facility context"));
                }

                UUID userId = null; // TODO: extract from user principal

                var serviceRequest = new PaymentService.RecordPaymentRequest(
                                facilityId,
                                request.hcfId(),
                                request.bankAccountId(),
                                request.paymentDate(),
                                request.amount(),
                                request.mode(),
                                request.referenceNumber(),
                                request.payerName(),
                                request.notes(),
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
                        @Valid @RequestBody ReversePaymentRequest request,
                        @AuthenticationPrincipal UserDetails user) {

                UUID facilityId = TenantContext.getTenantId();

                // Verify payment belongs to facility
                var payment = paymentRepository.findById(id).orElse(null);
                if (payment == null || !payment.getFacility().getId().equals(facilityId)) {
                        return ResponseEntity.notFound().build();
                }

                UUID userId = null; // TODO: extract from user principal

                try {
                        var result = paymentService.reversePayment(id, request.reason(), userId);
                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "reversalPaymentId", result.reversalPaymentId(),
                                        "reversalId", result.reversalId()));
                } catch (IllegalStateException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                }
        }

        /**
         * Download receipt PDF.
         */
        @GetMapping("/{id}/receipt")
        public ResponseEntity<?> downloadReceipt(@PathVariable UUID id) {
                UUID facilityId = TenantContext.getTenantId();

                // Verify payment belongs to facility
                var payment = paymentRepository.findById(id).orElse(null);
                if (payment == null || !payment.getFacility().getId().equals(facilityId)) {
                        return ResponseEntity.notFound().build();
                }

                try {
                        var receipt = receiptService.getReceipt(id);
                        return ResponseEntity.ok()
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
                var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentDate"));

                Page<Payment> payments;
                if (hcfId != null) {
                        payments = paymentService.getPaymentsForHcf(hcfId, pageable);
                } else {
                        payments = paymentService.getPayments(facilityId, pageable);
                }

                return ResponseEntity.ok(payments.map(this::toDTO));
        }

        /**
         * Get payment detail.
         */
        @GetMapping("/{id}")
        public ResponseEntity<PaymentDetailDTO> getPayment(@PathVariable UUID id) {
                UUID facilityId = TenantContext.getTenantId();

                return paymentRepository.findById(id)
                                .filter(p -> p.getFacility().getId().equals(facilityId))
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
                BigDecimal totalAdvance = BigDecimal.ZERO; // TODO: sum advance for all HCFs

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
                BigDecimal balance = paymentService.getAdvanceBalance(hcfId);
                return ResponseEntity.ok(Map.of("advanceBalance", balance));
        }

        // ========== DTOs ==========

        private PaymentDTO toDTO(Payment p) {
                String hcfName = p.getHcf() != null ? p.getHcf().getName() : "Unknown";
                String bankName = p.getBankAccount() != null ? p.getBankAccount().getBankName() : null;
                boolean isReversed = reversalRepository.existsByOriginalPaymentId(p.getId());

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
                                isReversed);
        }

        public record RecordPaymentRequest(
                        @NotNull UUID hcfId,
                        UUID bankAccountId,
                        @NotNull LocalDate paymentDate,
                        @NotNull @Positive BigDecimal amount,
                        @NotNull PaymentMode mode,
                        String referenceNumber,
                        String payerName,
                        String notes) {
        }

        public record ReversePaymentRequest(
                        @NotBlank String reason) {
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
                        boolean isReversed) {
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
}
