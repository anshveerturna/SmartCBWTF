package com.smartcbwtf.domain;

/**
 * Bed Access Category enum for HCF regulatory classification.
 * 
 * The 30-bed threshold is FIXED per regulatory norms.
 * HCFs with 0-30 beds are CBWTF-managed only (no portal access).
 * HCFs with above 30 beds are eligible for HCF Admin Portal access.
 */
public enum HcfBedAccessCategory {

    BEDS_0_TO_30("0–30 Beds"),
    ABOVE_30_BEDS("Above 30 Beds");

    private static final int PORTAL_ELIGIBILITY_THRESHOLD = 30;

    private final String displayName;

    HcfBedAccessCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this category is eligible for HCF Admin Portal access.
     */
    public boolean isPortalEligible() {
        return this == ABOVE_30_BEDS;
    }

    /**
     * Calculate the bed access category from bed count and bedded status.
     * 
     * Rules:
     * - Non-bedded HCFs (isBedded = false or null) → BEDS_0_TO_30
     * - Null bed count → BEDS_0_TO_30
     * - Bed count <= 30 → BEDS_0_TO_30
     * - Bed count > 30 → ABOVE_30_BEDS
     */
    public static HcfBedAccessCategory calculate(Integer numberOfBeds, Boolean isBedded) {
        // Non-bedded facilities are always 0-30 category
        if (isBedded == null || !isBedded) {
            return BEDS_0_TO_30;
        }

        // Null or <= 30 beds → 0-30 category
        if (numberOfBeds == null || numberOfBeds <= PORTAL_ELIGIBILITY_THRESHOLD) {
            return BEDS_0_TO_30;
        }

        // Above 30 beds
        return ABOVE_30_BEDS;
    }
}
