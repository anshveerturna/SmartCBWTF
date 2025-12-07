package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BagEventSyncItem {
    @NotBlank
    private String qrCode;
    @NotBlank
    private String eventType; // HCF_COLLECTION / CBWTF_VERIFICATION
    @NotNull
    private Instant eventTs;
    @NotNull
    private Double gpsLat;
    @NotNull
    private Double gpsLon;
    @NotNull
    private BigDecimal weightKg;
    @NotNull
    private UUID collectedByUserId;
    private UUID facilityId;
    private Double gpsAccuracyM;
    private String appDeviceId;
    private String notes;

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getEventTs() { return eventTs; }
    public void setEventTs(Instant eventTs) { this.eventTs = eventTs; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLon() { return gpsLon; }
    public void setGpsLon(Double gpsLon) { this.gpsLon = gpsLon; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public UUID getCollectedByUserId() { return collectedByUserId; }
    public void setCollectedByUserId(UUID collectedByUserId) { this.collectedByUserId = collectedByUserId; }
    public UUID getFacilityId() { return facilityId; }
    public void setFacilityId(UUID facilityId) { this.facilityId = facilityId; }
    public Double getGpsAccuracyM() { return gpsAccuracyM; }
    public void setGpsAccuracyM(Double gpsAccuracyM) { this.gpsAccuracyM = gpsAccuracyM; }
    public String getAppDeviceId() { return appDeviceId; }
    public void setAppDeviceId(String appDeviceId) { this.appDeviceId = appDeviceId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
