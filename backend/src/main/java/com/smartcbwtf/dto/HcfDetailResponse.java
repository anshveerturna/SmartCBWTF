package com.smartcbwtf.dto;

import com.smartcbwtf.domain.ApprovalStatus;
import com.smartcbwtf.domain.BillingModel;
import com.smartcbwtf.domain.Hcf;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for HCF details including all editable fields.
 */
public record HcfDetailResponse(
        UUID id,
        String code,
        String name,
        String doctorName,
        String address,
        String pincode,
        String state,
        String contactEmail,
        String contactPhone,
        String panNo,
        String gstNo,
        String aadharNo,
        Integer numberOfBeds,
        BigDecimal monthlyCharges,
        BillingModel billingModel,
        String otherNotes,
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
                hcf.getDoctorName(),
                hcf.getAddress(),
                hcf.getPincode(),
                hcf.getState(),
                hcf.getContactEmail(),
                hcf.getContactPhone(),
                hcf.getPanNo(),
                hcf.getGstNo(),
                hcf.getAadharNo(),
                hcf.getNumberOfBeds(),
                hcf.getMonthlyCharges(),
                hcf.getBillingModel(),
                hcf.getOtherNotes(),
                hcf.getApprovalStatus(),
                hcf.getRejectionReason(),
                hcf.getApprovedBy(),
                hcf.getApprovedAt(),
                hcf.getStatus(),
                hcf.getCreatedAt(),
                hcf.getUpdatedAt());
    }
}
