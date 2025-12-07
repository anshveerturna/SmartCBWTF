package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AlertMissingBagDto {
    private UUID bagLabelId;
    private String qrCode;
    private String category;
    private String hcfName;
    private BigDecimal weightKg;
    private Instant eventTs;
    private Double gpsLat;
    private Double gpsLon;
    private UUID driverId;

    public AlertMissingBagDto(UUID bagLabelId, String qrCode, String category, String hcfName, BigDecimal weightKg, Instant eventTs, Double gpsLat, Double gpsLon, UUID driverId) {
        this.bagLabelId = bagLabelId;
        this.qrCode = qrCode;
        this.category = category;
        this.hcfName = hcfName;
        this.weightKg = weightKg;
        this.eventTs = eventTs;
        this.gpsLat = gpsLat;
        this.gpsLon = gpsLon;
        this.driverId = driverId;
    }

    public UUID getBagLabelId() { return bagLabelId; }
    public void setBagLabelId(UUID bagLabelId) { this.bagLabelId = bagLabelId; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getHcfName() { return hcfName; }
    public void setHcfName(String hcfName) { this.hcfName = hcfName; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public Instant getEventTs() { return eventTs; }
    public void setEventTs(Instant eventTs) { this.eventTs = eventTs; }
    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }
    public Double getGpsLon() { return gpsLon; }
    public void setGpsLon(Double gpsLon) { this.gpsLon = gpsLon; }
    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }
}
