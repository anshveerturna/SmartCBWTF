package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for bag verification at CBWTF facility.
 * This endpoint is used by the mobile app when verifying bags received at the plant.
 */
public class BagVerifyRequest {

    private UUID bagLabelId;

    @NotBlank(message = "QR code is required")
    private String qrCode;

    private String eventType = "CBWTF_VERIFICATION";

    @NotNull(message = "GPS latitude is required")
    private Double gpsLat;

    @NotNull(message = "GPS longitude is required")
    private Double gpsLon;

    @NotNull(message = "GPS accuracy is required")
    private Float gpsAccuracy;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private BigDecimal weightKg;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Verified by user ID is required")
    private UUID verifiedByUserId;

    private String notes;

    private Instant eventTs;

    // Getters and Setters
    public UUID getBagLabelId() {
        return bagLabelId;
    }

    public void setBagLabelId(UUID bagLabelId) {
        this.bagLabelId = bagLabelId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Double getGpsLat() {
        return gpsLat;
    }

    public void setGpsLat(Double gpsLat) {
        this.gpsLat = gpsLat;
    }

    public Double getGpsLon() {
        return gpsLon;
    }

    public void setGpsLon(Double gpsLon) {
        this.gpsLon = gpsLon;
    }

    public Float getGpsAccuracy() {
        return gpsAccuracy;
    }

    public void setGpsAccuracy(Float gpsAccuracy) {
        this.gpsAccuracy = gpsAccuracy;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(UUID verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getEventTs() {
        return eventTs;
    }

    public void setEventTs(Instant eventTs) {
        this.eventTs = eventTs;
    }
}
