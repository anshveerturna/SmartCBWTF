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
 * Payment Service - Core payment recording, allocation, and reversal.
 * 
 * INVARIANTS:
 * - Payments are IMMUTABLE (no updates, use reversals)
 * - FIFO allocation: oldest invoices paid first
 * - Overpayments go to advance ledger
 * - Reversals use COUNTER-ENTRIES, never deletion
 * - Receipt is generated SYNCHRONOUSLY (fail-closed)
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
    private final AgreementRepository agreementRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AlertService alertService;
    private final ReceiptService receiptService;
    private final EmailService emailService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentReversalRepository reversalRepository,
            InvoicePaymentRepository invoicePaymentRepository,
            HcfAdvanceLedgerRepository advanceLedgerRepository,
            InvoiceRepository invoiceRepository,
            HcfRepository hcfRepository,
            FacilityRepository facilityRepository,
            AgreementRepository agreementRepository,
            BankAccountRepository bankAccountRepository,
            AlertService alertService,
            ReceiptService receiptService,
            EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.reversalRepository = reversalRepository;
        this.invoicePaymentRepository = invoicePaymentRepository;
        this.advanceLedgerRepository = advanceLedgerRepository;
        this.invoiceRepository = invoiceRepository;
        this.hcfRepository = hcfRepository;
        this.facilityRepository = facilityRepository;
        this.agreementRepository = agreementRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.alertService = alertService;
        this.receiptService = receiptService;
        this.emailService = emailService;
    }

    /**
     * Record a new payment with FIFO allocation and synchronous receipt generation.
     * If receipt generation fails, the entire transaction is rolled back.
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

        Agreement agreement = agreementRepository.findActiveByHcfAndFacility(hcf.getId(), facility.getId())
                .orElse(null);
        if (agreement == null) {
            return PaymentResult.error("HCF is not active under this facility");
        }

        // Validate bank account if provided
        BankAccount bankAccount = null;
        if (request.bankAccountId() != null) {
            bankAccount = bankAccountRepository.findById(request.bankAccountId()).orElse(null);
            if (bankAccount == null || bankAccount.getStatus() != BankAccount.Status.ACTIVE
                    || bankAccount.getFacility() == null
                    || !bankAccount.getFacility().getId().equals(facility.getId())) {
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
        AllocationResult allocation = allocateToInvoices(payment, facility.getId(), hcf.getId());

        // Generate receipt SYNCHRONOUSLY (fail-closed)
        // If this fails, the entire transaction is rolled back
        List<InvoicePayment> allocations = invoicePaymentRepository.findByPaymentId(payment.getId());
        PaymentReceipt receipt = receiptService.generateReceipt(payment, allocations);
        log.info("Generated receipt {} for payment {}", receipt.getReceiptNumber(), payment.getId());

        // Create alert
        UUID eventId = UUID.randomUUID();
        if (allocation.advanceAmount.compareTo(BigDecimal.ZERO) > 0) {
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

        // Send payment received email to HCF
        if (hcf.getContactEmail() != null && !hcf.getContactEmail().isBlank()) {
            try {
                String html = emailService.getTemplates().paymentReceived(
                        hcf.getName(),
                        receipt.getReceiptNumber(),
                        String.format("%.2f", request.amount()),
                        request.mode() != null ? request.mode().name() : "N/A",
                        request.referenceNumber() != null ? request.referenceNumber() : receipt.getReceiptNumber());
                emailService.sendHtmlEmail(hcf.getContactEmail(),
                        "Payment Received - " + receipt.getReceiptNumber(), html);
                log.info("Payment received email sent to HCF: {}", hcf.getContactEmail());
            } catch (Exception e) {
                log.warn("Failed to send payment received email to {}: {}", hcf.getContactEmail(), e.getMessage());
            }
        }

        return PaymentResult.success(payment.getId(), receipt.getReceiptNumber(), allocation);
    }

    /**
     * Reverse a payment using COUNTER-ENTRIES (never deletion).
     * 
     * This creates:
     * 1. A new reversal payment record
     * 2. A payment_reversal link
     * 3. Negative invoice_payment entries (counter-entries)
     * 4. Negative advance ledger entry (if applicable)
     */
    @Transactional
    public ReversalResult reversePayment(UUID facilityId, UUID paymentId, String reason, UUID reversedBy) {
        // Find original payment
        Payment original = paymentRepository.findByIdAndFacilityId(paymentId, facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (original.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Reversal counter-entries cannot be reversed");
        }

        // Check not already reversed
        if (reversalRepository.existsByOriginalPaymentId(paymentId)) {
            throw new IllegalStateException("Payment already reversed: " + paymentId);
        }

        // 1. Create reversal payment as a counter-entry, linked to original
        Payment reversalPayment = new Payment();
        reversalPayment.setFacility(original.getFacility());
        reversalPayment.setHcf(original.getHcf());
        reversalPayment.setBankAccount(original.getBankAccount());
        reversalPayment.setPaymentDate(LocalDate.now());
        reversalPayment.setAmount(original.getAmount().negate());
        reversalPayment.setMode(original.getMode());
        reversalPayment.setReferenceNumber("REV-" + original.getId().toString().substring(0, 8));
        reversalPayment.setNotes("Reversal of payment " + original.getId() + ": " + reason);
        reversalPayment.setCreatedBy(reversedBy);
        reversalPayment.setChecksum(calculateChecksum(reversalPayment));
        reversalPayment = paymentRepository.save(reversalPayment);

        // 2. Create payment_reversal link
        PaymentReversal reversal = new PaymentReversal();
        reversal.setOriginalPayment(original);
        reversal.setReversalPayment(reversalPayment);
        reversal.setReason(reason);
        reversal.setCreatedBy(reversedBy);
        reversalRepository.save(reversal);

        // 3. Create NEGATIVE allocations (counter-entries, NOT deletion!)
        List<InvoicePayment> originalAllocations = invoicePaymentRepository.findByPaymentId(paymentId);
        for (InvoicePayment alloc : originalAllocations) {
            InvoicePayment counterEntry = new InvoicePayment();
            counterEntry.setInvoice(alloc.getInvoice());
            counterEntry.setPayment(reversalPayment);
            counterEntry.setAllocatedAmount(alloc.getAllocatedAmount().negate()); // NEGATIVE
            invoicePaymentRepository.save(counterEntry);
            log.debug("Created counter-entry for invoice {}: {}",
                    alloc.getInvoice().getInvoiceNumber(),
                    alloc.getAllocatedAmount().negate());
        }

        // 4. Reverse advance ledger entries (create negative entry)
        BigDecimal advanceAmount = advanceLedgerRepository.sumByPaymentId(paymentId);
        if (advanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            HcfAdvanceLedger reversalLedger = new HcfAdvanceLedger();
            reversalLedger.setHcf(original.getHcf());
            reversalLedger.setSourcePayment(reversalPayment);
            reversalLedger.setAmount(advanceAmount.negate()); // NEGATIVE
            reversalLedger.setChecksum(calculateAdvanceChecksum(reversalLedger));
            advanceLedgerRepository.save(reversalLedger);
            log.info("Created advance ledger counter-entry: {}", advanceAmount.negate());
        }

        // Create alert
        UUID eventId = UUID.randomUUID();
        alertService.createAlert(eventId, original.getFacility().getId(), AlertType.PAYMENT_RECEIVED,
                AlertSeverity.WARN, "Payment Reversed",
                String.format("Payment of ₹%.2f from %s has been reversed. Reason: %s",
                        original.getAmount(), original.getHcf().getName(), reason),
                "Payment", reversalPayment.getId());

        log.info("Reversed payment {} with reversal {}", paymentId, reversalPayment.getId());

        return new ReversalResult(reversalPayment.getId(), reversal.getId());
    }

    /**
     * Allocate payment to invoices using FIFO (oldest first).
     */
    private AllocationResult allocateToInvoices(Payment payment, UUID facilityId, UUID hcfId) {
        BigDecimal remainingAmount = payment.getAmount();
        BigDecimal totalAllocated = BigDecimal.ZERO;
        List<AllocationEntry> allocations = new ArrayList<>();

        // Get unpaid invoices for this HCF, sorted by date (FIFO)
        List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidByFacilityAndHcfOrderByDateAsc(facilityId, hcfId);

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
    public Page<Payment> getPaymentsForHcf(UUID facilityId, UUID hcfId, Pageable pageable) {
        return paymentRepository.findByFacilityIdAndHcfId(facilityId, hcfId, pageable);
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
    public BigDecimal getAdvanceBalance(UUID facilityId, UUID hcfId) {
        return advanceLedgerRepository.getAdvanceBalanceForFacility(facilityId, hcfId);
    }

    /**
     * Get total advance balance for facility.
     */
    public BigDecimal getTotalAdvanceBalance(UUID facilityId) {
        return zeroIfNull(advanceLedgerRepository.getTotalAdvanceBalanceForFacility(facilityId));
    }

    /**
     * Get total outstanding for facility.
     */
    public BigDecimal getTotalOutstanding(UUID facilityId) {
        BigDecimal invoiceTotal = zeroIfNull(invoiceRepository.sumTotalAmountByFacilityId(facilityId));
        BigDecimal allocatedTotal = zeroIfNull(invoicePaymentRepository.getTotalAllocatedForFacility(facilityId));
        return invoiceTotal.subtract(allocatedTotal).max(BigDecimal.ZERO);
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

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
            String receiptNumber,
            AllocationResult allocation) {
        public static PaymentResult success(UUID paymentId, String receiptNumber, AllocationResult allocation) {
            return new PaymentResult(true, null, paymentId, receiptNumber, allocation);
        }

        public static PaymentResult error(String error) {
            return new PaymentResult(false, error, null, null, null);
        }
    }

    public record ReversalResult(UUID reversalPaymentId, UUID reversalId) {
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
