package com.smartcbwtf.domain;

/**
 * HCF approval status.
 * 
 * Controls whether an HCF can participate in billing, pickups, and compliance.
 */
public enum ApprovalStatus {
    /**
     * Awaiting admin review.
     * - HCF cannot be billed
     * - HCF cannot be picked up
     * - Can be edited by admin
     */
    PENDING,

    /**
     * Approved by admin.
     * - HCF participates in billing
     * - HCF participates in pickups
     * - Billing model is LOCKED (immutable)
     * - Cannot be edited (create new version if changes needed)
     */
    APPROVED,

    /**
     * Rejected by admin.
     * - HCF cannot be billed
     * - HCF cannot be picked up
     * - Can be edited and re-submitted for approval
     */
    REJECTED
}
