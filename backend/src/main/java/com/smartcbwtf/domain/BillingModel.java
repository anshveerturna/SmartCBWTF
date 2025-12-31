package com.smartcbwtf.domain;

/**
 * Billing model for HCF.
 * 
 * This determines how an HCF is billed:
 * - BEDDED: Bill = beds × rate_per_bed × days_in_month (+ GST)
 * - FIXED_MONTHLY: Bill = flat_monthly_charge (+ GST), pickup weight ignored
 *
 * IMMUTABLE after HCF approval. Selected at registration time.
 */
public enum BillingModel {
    /**
     * Bed-based billing.
     * Requires: number_of_beds > 0
     * Calculation: beds × rate_per_bed × days_in_month
     * Excess waste charges apply based on pickup weight.
     */
    BEDDED,

    /**
     * Fixed monthly charge.
     * Requires: monthly_charges > 0
     * Calculation: flat monthly_charge (pickup weight is ignored for billing)
     * Pickup data is still recorded for compliance purposes.
     */
    FIXED_MONTHLY
}
