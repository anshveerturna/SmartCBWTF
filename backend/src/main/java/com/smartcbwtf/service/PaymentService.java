package com.smartcbwtf.service;

import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

/**
 * Payment Service - Core payment recording and allocation.
 * 
 * INVARIANTS:
 * - Payments are IMMUTABLE (no updates, use reversals)
 * - FIFO allocation: oldest invoices paid first
 * - Overpayments go to advance ledger
 * - All amounts are BigDecimal (never double)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentReversalRepository reversalRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final HcfAdvanceLedgerRepository advanceLedgerRepository;
    private final InvoiceRepository invoiceRepository;
    private final HcfRepository hcfRepository;
    private final FacilityRepository facilityRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AlertService alertService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentReversalRepository reversalRepository,
            InvoicePaymentRepository invoicePaymentRepository,
            HcfAdvanceLedgerRepository advanceLedgerRepository,
            InvoiceRepository invoiceRepository,
            HcfRepository hcfRepository,
            FacilityRepository facilityRepository,
            BankAccountRepository bankAccountRepository,
            AlertService alertService) {
        this.paymentRepository = paymentRepository;
        this.reversalRepository = reversalRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.advanceLedgerRepository = advanceLedgerRepository;
        this.invoiceRepository = invoiceRepository;
        this.hcfRepository = hcfRepository;
        this.facilityRepository = facilityRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.alertService = alertService;
    }

    /**
     * Record a new payment and allocate to invoices (FIFO).
     */
    @Transactional
    public PaymentResult recordPayment(RecordPaymentRequest request) {
        // Validate HCF exists
        Hcf hcf = hcfRepository.findById(request.hcfId()).orElse(null);
        if (hcf == null) {
            return PaymentResult.error("HCF not found");
        }

        // Validate facility
        Facility facility = facilityRepository.findById(request.facilityId()).orElse(null);
        if (facility == null) {
            return PaymentResult.error("Facility not found");
        }

        // Validate bank account if provided
        BankAccount bankAccount = null;
        if (request.bankAccountId() != null) {
            bankAccount = bankAccountRepository.findById(request.bankAccountId()).orElse(null);
            if (bankAccount == null || bankAccount.getStatus() != BankAccount.Status.ACTIVE) {
                return PaymentResult.error("Bank account not found or disabled");
            }
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setFacility(facility);
        payment.setHcf(hcf);
        payment.setBankAccount(bankAccount);
        payment.setPaymentDate(request.paymentDate());
        payment.setAmount(request.amount());
        payment.setMode(request.mode());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setPayerName(request.payerName());
        payment.setNotes(request.notes());
        payment.setCreatedBy(request.userId());
        payment.setChecksum(calculateChecksum(payment));

        payment = paymentRepository.save(payment);
        log.info("Recorded payment {} for HCF {} amount {}", payment.getId(), hcf.getId(), request.amount());

        // Allocate to invoices (FIFO)
        AllocationResult allocation = allocateToInvoices(payment, hcf.getId());

        // Create alert
        UUID eventId = UUID.randomUUID();
        if (allocation.totalAllocated.compareTo(request.amount()) < 0) {
            alertService.createAlert(eventId, facility.getId(), AlertType.PAYMENT_RECEIVED,
                    AlertSeverity.INFO, "Payment Received (with advance)",
                    String.format("₹%.2f received from %s. ₹%.2f credited to advance.",
                            request.amount(), hcf.getName(), allocation.advanceAmount),
                    "Payment", payment.getId());
        } else {
            alertService.createAlert(eventId, facility.getId(), AlertType.PAYMENT_RECEIVED,
                    AlertSeverity.INFO, "Payment Received",
                    String.format("₹%.2f received from %s.", request.amount(), hcf.getName()),
                    "Payment", payment.getId());
        }

        return PaymentResult.success(payment.getId(), allocation);
    }

    /**
     * Allocate payment to invoices using FIFO (oldest first).
     */
    private AllocationResult allocateToInvoices(Payment payment, UUID hcfId) {
        BigDecimal remainingAmount = payment.getAmount();
        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<AllocationEntry> allocations = new ArrayList<>();

        // Get unpaid invoices for this HCF, sorted by date (FIFO)
        List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidByHcfIdOrderByDateAsc(hcfId);

        for (Invoice invoice : unpaidInvoices) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0)
                break;

            // Calculate outstanding for this invoice
            BigDecimal totalPaid = invoicePaymentRepository.getTotalPaidForInvoice(invoice.getId());
            BigDecimal outstanding = invoice.getTotalAmount().subtract(totalPaid);

            if (outstanding.compareTo(BigDecimal.ZERO) <= 0)
                continue;

            // Allocate
            BigDecimal toAllocate = remainingAmount.min(outstanding);

            InvoicePayment ip = new InvoicePayment();
            ip.setInvoice(invoice);
            ip.setPayment(payment);
            ip.setAllocatedAmount(toAllocate);
            invoicePaymentRepository.save(ip);

            allocations.add(new AllocationEntry(invoice.getId(), invoice.getInvoiceNumber(), toAllocate));
            totalAllocated = totalAllocated.add(toAllocate);
            remainingAmount = remainingAmount.subtract(toAllocate);

            log.debug("Allocated {} to invoice {}", toAllocate, invoice.getInvoiceNumber());
        }

        // Handle overpayment -> advance ledger
        BigDecimal advanceAmount = BigDecimal.ZERO;
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            HcfAdvanceLedger advance = new HcfAdvanceLedger();
            advance.setHcf(payment.getHcf());
            advance.setSourcePayment(payment);
            advance.setAmount(remainingAmount);
            advance.setChecksum(calculateAdvanceChecksum(advance));
            advanceLedgerRepository.save(advance);

            advanceAmount = remainingAmount;
            log.info("Created advance balance {} for HCF {}", remainingAmount, hcfId);
        }

        return new AllocationResult(totalAllocated, advanceAmount, allocations);
    }

    /**
     * Get payments for facility.
     */
    public Page<Payment> getPayments(UUID facilityId, Pageable pageable) {
        return paymentRepository.findByFacilityId(facilityId, pageable);
    }

    /**
     * Get payments for HCF.
     */
    public Page<Payment> getPaymentsForHcf(UUID hcfId, Pageable pageable) {
        return paymentRepository.findByHcfId(hcfId, pageable);
    }

    /**
     * Check if payment is reversed.
     */
    public boolean isReversed(UUID paymentId) {
        return reversalRepository.existsByOriginalPaymentId(paymentId);
    }

    /**
     * Get advance balance for HCF.
     */
    public BigDecimal getAdvanceBalance(UUID hcfId) {
        return advanceLedgerRepository.getAdvanceBalance(hcfId);
    }

    /**
     * Get total outstanding for facility.
     */
    public BigDecimal getTotalOutstanding(UUID facilityId) {
        // Sum of all invoice totals minus sum of all allocations
        // This should be computed via query for performance
        return BigDecimal.ZERO; // TODO: implement query
    }

    /**
     * Get total collected MTD.
     */
    public BigDecimal getTotalCollectedMTD(UUID facilityId) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate today = LocalDate.now();
        BigDecimal total = paymentRepository.getTotalCollected(facilityId, startOfMonth, today);
        return total != null ? total : BigDecimal.ZERO;
    }

    private String calculateChecksum(Payment p) {
        try {
            String data = p.getHcf().getId() + "|" + p.getAmount() + "|" + p.getPaymentDate() + "|" + p.getMode();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private String calculateAdvanceChecksum(HcfAdvanceLedger a) {
        try {
            String data = a.getHcf().getId() + "|" + a.getAmount() + "|" + a.getSourcePayment().getId();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Request/Response records
    public record RecordPaymentRequest(
            UUID facilityId,
            UUID hcfId,
            UUID bankAccountId,
            LocalDate paymentDate,
            BigDecimal amount,
            PaymentMode mode,
            String referenceNumber,
            String payerName,
            String notes,
            UUID userId) {
    }

    public record PaymentResult(
            boolean success,
            String error,
            UUID paymentId,
            AllocationResult allocation) {
        public static PaymentResult success(UUID paymentId, AllocationResult allocation) {
            return new PaymentResult(true, null, paymentId, allocation);
        }

        public static PaymentResult error(String error) {
            return new PaymentResult(false, error, null, null);
        }
    }

    public record AllocationResult(
            BigDecimal totalAllocated,
            BigDecimal advanceAmount,
            List<AllocationEntry> allocations) {
    }

    public record AllocationEntry(
            UUID invoiceId,
            String invoiceNumber,
            BigDecimal amount) {
    }
}
