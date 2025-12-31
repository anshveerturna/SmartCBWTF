package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.repository.HcfAuditLogRepository;
import com.smartcbwtf.repository.HcfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for HCF approval workflow.
 * Handles admin review, edit, approve, and reject operations.
 */
@Service
@Transactional
public class HcfApprovalService {

    private static final Logger log = LoggerFactory.getLogger(HcfApprovalService.class);

    private final HcfRepository hcfRepository;
    private final HcfAuditLogRepository auditLogRepository;

    public HcfApprovalService(HcfRepository hcfRepository, HcfAuditLogRepository auditLogRepository) {
        this.hcfRepository = hcfRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Update HCF before approval.
     * Only allowed if status is PENDING or REJECTED.
     * All changes are logged to audit table.
     */
    public Hcf updatePendingHcf(UUID hcfId, BillingModel billingModel, Integer beds, BigDecimal monthlyCharge) {
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found: " + hcfId));

        if (hcf.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Cannot edit approved HCF. Billing model is locked.");
        }

        UUID userId = TenantContext.getUserId();

        // Log billing model change
        if (!Objects.equals(hcf.getBillingModel(), billingModel)) {
            logChange(hcfId, "billingModel",
                    hcf.getBillingModel() != null ? hcf.getBillingModel().name() : null,
                    billingModel.name(), userId);
            hcf.setBillingModel(billingModel);
        }

        // Log beds change
        if (!Objects.equals(hcf.getNumberOfBeds(), beds)) {
            logChange(hcfId, "numberOfBeds",
                    hcf.getNumberOfBeds() != null ? hcf.getNumberOfBeds().toString() : null,
                    beds != null ? beds.toString() : null, userId);
            hcf.setNumberOfBeds(beds);
        }

        // Log monthly charge change
        if (!Objects.equals(hcf.getMonthlyCharges(), monthlyCharge)) {
            logChange(hcfId, "monthlyCharges",
                    hcf.getMonthlyCharges() != null ? hcf.getMonthlyCharges().toString() : null,
                    monthlyCharge != null ? monthlyCharge.toString() : null, userId);
            hcf.setMonthlyCharges(monthlyCharge);
        }

        // Validate billing model constraints
        validateBillingModelConstraints(hcf);

        hcf.setUpdatedAt(Instant.now());
        Hcf saved = hcfRepository.save(hcf);
        log.info("Updated pending HCF {} by admin {}", hcfId, userId);
        return saved;
    }

    /**
     * Approve an HCF.
     * After approval, billing model is LOCKED and cannot be changed.
     */
    public Hcf approve(UUID hcfId) {
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found: " + hcfId));

        if (hcf.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("HCF is already approved");
        }

        // Validate billing model is set and constraints are met
        if (hcf.getBillingModel() == null) {
            throw new IllegalStateException("Billing model must be set before approval");
        }
        validateBillingModelConstraints(hcf);

        UUID userId = TenantContext.getUserId();

        logChange(hcfId, "approvalStatus", hcf.getApprovalStatus().name(), ApprovalStatus.APPROVED.name(), userId);

        hcf.setApprovalStatus(ApprovalStatus.APPROVED);
        hcf.setApprovedBy(userId);
        hcf.setApprovedAt(Instant.now());
        hcf.setRejectionReason(null);
        hcf.setStatus("ACTIVE"); // Keep legacy status in sync
        hcf.setUpdatedAt(Instant.now());

        Hcf saved = hcfRepository.save(hcf);
        log.info("Approved HCF {} by admin {}. Billing model {} is now LOCKED.", hcfId, userId, hcf.getBillingModel());
        return saved;
    }

    /**
     * Reject an HCF with reason.
     * HCF can be edited and resubmitted after rejection.
     */
    public Hcf reject(UUID hcfId, String reason) {
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found: " + hcfId));

        if (hcf.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Cannot reject an approved HCF");
        }

        UUID userId = TenantContext.getUserId();

        logChange(hcfId, "approvalStatus", hcf.getApprovalStatus().name(), ApprovalStatus.REJECTED.name(), userId);
        logChange(hcfId, "rejectionReason", hcf.getRejectionReason(), reason, userId);

        hcf.setApprovalStatus(ApprovalStatus.REJECTED);
        hcf.setRejectionReason(reason);
        hcf.setStatus("REJECTED"); // Keep legacy status in sync
        hcf.setUpdatedAt(Instant.now());

        Hcf saved = hcfRepository.save(hcf);
        log.info("Rejected HCF {} by admin {} with reason: {}", hcfId, userId, reason);
        return saved;
    }

    /**
     * Resubmit a rejected HCF for approval.
     */
    public Hcf resubmit(UUID hcfId) {
        Hcf hcf = hcfRepository.findById(hcfId)
                .orElseThrow(() -> new IllegalArgumentException("HCF not found: " + hcfId));

        if (hcf.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Only rejected HCFs can be resubmitted");
        }

        UUID userId = TenantContext.getUserId();

        logChange(hcfId, "approvalStatus", ApprovalStatus.REJECTED.name(), ApprovalStatus.PENDING.name(), userId);

        hcf.setApprovalStatus(ApprovalStatus.PENDING);
        hcf.setRejectionReason(null);
        hcf.setStatus("PENDING_APPROVAL"); // Keep legacy status in sync
        hcf.setUpdatedAt(Instant.now());

        Hcf saved = hcfRepository.save(hcf);
        log.info("Resubmitted HCF {} for approval by {}", hcfId, userId);
        return saved;
    }

    /**
     * Validate billing model constraints.
     * BEDDED: needs beds > 0
     * FIXED_MONTHLY: needs monthlyCharges > 0
     */
    private void validateBillingModelConstraints(Hcf hcf) {
        if (hcf.getBillingModel() == BillingModel.BEDDED) {
            if (hcf.getNumberOfBeds() == null || hcf.getNumberOfBeds() <= 0) {
                throw new IllegalStateException("BEDDED billing model requires number_of_beds > 0");
            }
        } else if (hcf.getBillingModel() == BillingModel.FIXED_MONTHLY) {
            if (hcf.getMonthlyCharges() == null || hcf.getMonthlyCharges().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("FIXED_MONTHLY billing model requires monthly_charges > 0");
            }
        }
    }

    private void logChange(UUID hcfId, String fieldName, String oldValue, String newValue, UUID userId) {
        if (!Objects.equals(oldValue, newValue)) {
            HcfAuditLog log = new HcfAuditLog(hcfId, fieldName, oldValue, newValue, userId);
            auditLogRepository.save(log);
        }
    }
}
