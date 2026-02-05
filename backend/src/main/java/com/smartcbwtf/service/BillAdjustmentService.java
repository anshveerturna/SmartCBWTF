package com.smartcbwtf.service;

import com.smartcbwtf.domain.Bill;
import com.smartcbwtf.domain.BillVersion;
import com.smartcbwtf.repository.BillRepository;
import com.smartcbwtf.repository.BillVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for applying adjustments (concessions) to finalized bills.
 * 
 * Key rules:
 * - Only FINALIZED bills can be adjusted
 * - Adjustment amount must be negative (concession only)
 * - Adjustment cannot exceed base bill amount
 * - Creates immutable BillVersion audit record
 * - Triggers email alert to CBWTF admin
 * - Requires CBWTF_ADMIN role (enforced at controller level)
 */
@Service
public class BillAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(BillAdjustmentService.class);

    private final BillRepository billRepository;
    private final BillVersionRepository billVersionRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public BillAdjustmentService(
            BillRepository billRepository,
            BillVersionRepository billVersionRepository,
            AuditLogService auditLogService,
            EmailService emailService) {
        this.billRepository = billRepository;
        this.billVersionRepository = billVersionRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    /**
     * Apply an adjustment (concession) to a finalized bill.
     * 
     * @param billId           Bill UUID
     * @param adjustmentAmount Adjustment amount (must be negative)
     * @param reason           Mandatory reason for the adjustment
     * @param adjustedBy       User UUID who is applying the adjustment
     * @return Updated bill
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException    if bill is not in correct state
     */
    @Transactional
    public Bill applyAdjustment(UUID billId, BigDecimal adjustmentAmount, String reason, UUID adjustedBy) {
        log.info("Applying adjustment to bill {}: amount={}, reason={}, adjustedBy={}",
                billId, adjustmentAmount, reason, adjustedBy);

        // Fetch bill
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        // Validate bill state
        validateBillState(bill);

        // Validate adjustment
        validateAdjustment(bill, adjustmentAmount, reason);

        // Create audit version record BEFORE modifying the bill
        BillVersion version = BillVersion.fromAdjustment(bill, adjustmentAmount, reason, adjustedBy);

        // Update bill
        int newVersion = bill.getBillVersion() + 1;
        bill.setBillVersion(newVersion);
        bill.setAdjustmentAmount(adjustmentAmount);
        bill.setAdjustmentReason(reason);
        bill.setAdjustedBy(adjustedBy);
        bill.setAdjustedAt(Instant.now());
        bill.setFinalPayableAmount(bill.getTotalAmount().add(adjustmentAmount)); // adjustmentAmount is negative
        bill.setStatus(Bill.Status.FINALIZED_WITH_ADJUSTMENT.name());

        // Update version record with new version number
        version.setVersion(newVersion);

        // Persist
        billVersionRepository.save(version);
        Bill savedBill = billRepository.save(bill);

        // Audit log
        auditLogService.log(
                "BILL_ADJUSTMENT",
                savedBill.getId(),
                "BILL_ADJUSTED",
                adjustedBy,
                String.format(
                        "{\"billId\":\"%s\",\"originalTotal\":%s,\"adjustment\":%s,\"finalAmount\":%s,\"reason\":\"%s\"}",
                        billId,
                        bill.getTotalAmount(),
                        adjustmentAmount,
                        savedBill.getFinalPayableAmount(),
                        reason));

        // Send email alert
        sendAdjustmentAlertEmail(savedBill, version);

        log.info("Bill {} adjusted successfully. New version: {}, Final payable: {}",
                billId, newVersion, savedBill.getFinalPayableAmount());

        return savedBill;
    }

    /**
     * Get version history for a bill.
     */
    public List<BillVersion> getVersionHistory(UUID billId) {
        return billVersionRepository.findByBillIdOrderByVersionDesc(billId);
    }

    /**
     * Validate that the bill is in a state that allows adjustment.
     */
    private void validateBillState(Bill bill) {
        String status = bill.getStatus();

        // Only FINALIZED bills can be adjusted (not DRAFT, not already
        // FINALIZED_WITH_ADJUSTMENT)
        if (!Bill.Status.FINALIZED.name().equals(status)) {
            if (Bill.Status.FINALIZED_WITH_ADJUSTMENT.name().equals(status)) {
                throw new IllegalStateException(
                        "Bill has already been adjusted. Multiple adjustments are not allowed. " +
                                "Current adjustment: " + bill.getAdjustmentAmount());
            }
            throw new IllegalStateException(
                    "Only FINALIZED bills can be adjusted. Current status: " + status);
        }
    }

    /**
     * Validate the adjustment parameters.
     */
    private void validateAdjustment(Bill bill, BigDecimal adjustmentAmount, String reason) {
        // Reason is mandatory
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Adjustment reason is mandatory");
        }

        // Adjustment must be negative (concession only)
        if (adjustmentAmount == null) {
            throw new IllegalArgumentException("Adjustment amount is required");
        }
        if (adjustmentAmount.compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalArgumentException("Adjustment amount must be negative (concession only)");
        }

        // Adjustment cannot exceed total amount
        BigDecimal absoluteAdjustment = adjustmentAmount.abs();
        if (absoluteAdjustment.compareTo(bill.getTotalAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Adjustment amount (" + absoluteAdjustment +
                            ") cannot exceed bill total (" + bill.getTotalAmount() + ")");
        }
    }

    /**
     * Send email alert for bill adjustment.
     * 
     * CRITICAL: This alert goes to CBWTF admin notification email (not HCF).
     * This is a management safeguard for bill adjustments.
     */
    private void sendAdjustmentAlertEmail(Bill bill, BillVersion version) {
        try {
            // Get CBWTF admin notification email - this is the management safeguard
            String notificationEmail = bill.getFacility().getCbwtfNotificationEmail();
            if (notificationEmail == null || notificationEmail.isBlank()) {
                // Fallback to facility contact email if CBWTF notification email not configured
                notificationEmail = bill.getFacility().getContactEmail();
            }

            if (notificationEmail == null || notificationEmail.isBlank()) {
                log.warn("No CBWTF notification email or fallback contact email configured for facility {}. " +
                        "Skipping adjustment alert. This is a compliance issue!",
                        bill.getFacility().getId());
                return;
            }

            // Build email with professional template
            String subject = String.format("[BILL ADJUSTMENT ALERT] %s - %s",
                    bill.getAgreement().getHcf().getName(),
                    bill.getBillingMonth().toString());

            String html = emailService.getTemplates().billAdjustment(
                    "CBWTF Admin",
                    bill.getAgreement().getHcf().getName(),
                    bill.getBillingMonth().toString(),
                    "Concession",
                    version.getAdjustmentAmount().abs().toPlainString(),
                    version.getAdjustmentReason());

            // Send email
            emailService.sendHtmlEmail(notificationEmail, subject, html);

            log.info("CBWTF adjustment alert email sent to {} for bill {}", notificationEmail, bill.getId());

        } catch (Exception e) {
            // Log but don't fail the transaction - email is secondary to the adjustment
            // But log at ERROR level as this is a compliance concern
            log.error("COMPLIANCE WARNING: Failed to send adjustment alert email for bill {}: {}",
                    bill.getId(), e.getMessage(), e);
        }
    }
}
