package com.smartcbwtf.domain;

/**
 * HCF Facility Type enum.
 * 
 * New categories for specialized healthcare facilities.
 * All non-hospital types default to BEDS_0_TO_30 bed access category.
 */
public enum HcfType {

    HOSPITAL("Hospital"),
    DENTAL("Dental"),
    CLINIC("Clinic"),
    PATHOLOGY_COLLECTION("Pathology Lab (Collection)"),
    PATHOLOGY_STORAGE("Pathology Lab (Storage)");

    private final String displayName;

    HcfType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this HCF type supports seat count input.
     */
    public boolean supportsSeatCount() {
        return this == DENTAL || this == CLINIC;
    }

    /**
     * Returns true if this HCF type supports bed count input.
     * Only hospitals have beds.
     */
    public boolean supportsBedCount() {
        return this == HOSPITAL;
    }
}
