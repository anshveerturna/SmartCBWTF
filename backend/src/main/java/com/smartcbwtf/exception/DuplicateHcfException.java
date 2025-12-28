package com.smartcbwtf.exception;

/**
 * Exception thrown when attempting to register an HCF that duplicates
 * an existing HCF with an active agreement.
 * 
 * This exception provides detailed information about:
 * - Which field caused the duplicate detection
 * - The code of the existing HCF
 * - Whether it was a proximity-based detection
 */
public class DuplicateHcfException extends RuntimeException {

    /**
     * The field that triggered duplicate detection.
     * Values: PAN, GST, AADHAR, PHONE, GPS_LOCATION
     */
    private final String duplicateField;

    /**
     * The HCF code of the existing registration that conflicts.
     */
    private final String existingHcfCode;

    /**
     * For GPS proximity duplicates, the distance in meters.
     */
    private final Double distanceMeters;

    public DuplicateHcfException(String message, String duplicateField, String existingHcfCode) {
        super(message);
        this.duplicateField = duplicateField;
        this.existingHcfCode = existingHcfCode;
        this.distanceMeters = null;
    }

    public DuplicateHcfException(String message, String duplicateField, String existingHcfCode, Double distanceMeters) {
        super(message);
        this.duplicateField = duplicateField;
        this.existingHcfCode = existingHcfCode;
        this.distanceMeters = distanceMeters;
    }

    public String getDuplicateField() {
        return duplicateField;
    }

    public String getExistingHcfCode() {
        return existingHcfCode;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    /**
     * Returns a structured error response suitable for API responses.
     */
    public DuplicateHcfErrorResponse toErrorResponse() {
        return new DuplicateHcfErrorResponse(
                getMessage(),
                duplicateField,
                existingHcfCode,
                distanceMeters);
    }

    /**
     * Error response DTO for API responses.
     */
    public record DuplicateHcfErrorResponse(
            String message,
            String duplicateField,
            String existingHcfCode,
            Double distanceMeters) {
    }
}
