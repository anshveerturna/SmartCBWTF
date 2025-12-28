package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request for scanning QR code (pickup or verification).
 */
public class QrScanRequest {

    @NotBlank(message = "QR payload is required")
    private String qrPayloadJson; // Scanned JSON from QR code

    // For pickup: weight and location
    private BigDecimal weightKg;
    private Double gpsLat;
    private Double gpsLon;

    // For pickup: optional notes
    private String notes;

    // For pickup: bag event ID to link (set by mobile app after creating bag event)
    private UUID pickupEventId;

    // Getters and Setters
    public String getQrPayloadJson() {
        return qrPayloadJson;
    }

    public void setQrPayloadJson(String qrPayloadJson) {
        this.qrPayloadJson = qrPayloadJson;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getPickupEventId() {
        return pickupEventId;
    }

    public void setPickupEventId(UUID pickupEventId) {
        this.pickupEventId = pickupEventId;
    }
}
