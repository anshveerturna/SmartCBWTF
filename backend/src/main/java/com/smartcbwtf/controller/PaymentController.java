package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
 * Record and view payments for CBWTF.
 */
@RestController
@RequestMapping("/api/cbwtf/payments")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final HcfAdvanceLedgerRepository advanceLedgerRepository;
    private final HcfRepository hcfRepository;

    public PaymentController(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            InvoicePaymentRepository invoicePaymentRepository,
            HcfAdvanceLedgerRepository advanceLedgerRepository,
            HcfRepository hcfRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.advanceLedgerRepository = advanceLedgerRepository;
        this.hcfRepository = hcfRepository;
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

        // Get user ID (simplified - you may need to adapt to your auth setup)
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
                "allocation", result.allocation()));
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

        // Get total advance balance across all HCFs
        BigDecimal totalAdvance = BigDecimal.ZERO;
        // TODO: sum advance for all HCFs in facility

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
                p.getCreatedAt().toString());
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
            String createdAt) {
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
