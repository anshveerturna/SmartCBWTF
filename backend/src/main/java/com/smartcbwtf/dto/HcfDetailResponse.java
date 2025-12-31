package com.smartcbwtf.dto;

import com.smartcbwtf.domain.ApprovalStatus;
import com.smartcbwtf.domain.BillingModel;
import com.smartcbwtf.domain.Hcf;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for HCF details including billing model and approval status.
 */
public record HcfDetailResponse(
        UUID id,
        String code,
        String name,
        String address,
        String contactEmail,
        String contactPhone,
        Integer numberOfBeds,
        BigDecimal monthlyCharges,
        BillingModel billingModel,
        ApprovalStatus approvalStatus,
        String rejectionReason,
        UUID approvedBy,
        Instant approvedAt,
        String status,
        Instant createdAt,
        Instant updatedAt) {
    public static HcfDetailResponse from(Hcf hcf) {
        return new HcfDetailResponse(
                hcf.getId(),
                hcf.getCode(),
                hcf.getName(),
                hcf.getAddress(),
                hcf.getContactEmail(),
                hcf.getContactPhone(),
                hcf.getNumberOfBeds(),
                hcf.getMonthlyCharges(),
                hcf.getBillingModel(),
                hcf.getApprovalStatus(),
                hcf.getRejectionReason(),
                hcf.getApprovedBy(),
                hcf.getApprovedAt(),
                hcf.getStatus(),
                hcf.getCreatedAt(),
                hcf.getUpdatedAt());
    }
}
