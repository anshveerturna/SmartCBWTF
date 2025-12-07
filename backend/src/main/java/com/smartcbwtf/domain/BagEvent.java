package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class BagEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private BagLabel bagLabel;

    @ManyToOne(optional = false)
    private Facility facility;

    @ManyToOne(optional = false)
    private Hcf hcf;

    @Column(nullable = false)
    private String eventType; // HCF_COLLECTION / CBWTF_VERIFICATION

    @Column(nullable = false)
    private Instant eventTs;

    @Column(nullable = false)
    private Double gpsLat;

    @Column(nullable = false)
    private Double gpsLon;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private UUID collectedByUserId;

    private String appDeviceId;
    private String anomalyState; // OK / OUT_OF_GEOFENCE / MISMATCH
    private String notes;
    private Instant createdAt = Instant.now();

    // getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BagLabel getBagLabel() { return bagLabel; }
    public void setBagLabel(BagLabel bagLabel) { this.bagLabel = bagLabel; }
    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }
    public Hcf getHcf() { return hcf; }
    public void setHcf(Hcf hcf) { this.hcf = hcf; }
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
    public String getAppDeviceId() { return appDeviceId; }
    public void setAppDeviceId(String appDeviceId) { this.appDeviceId = appDeviceId; }
    public String getAnomalyState() { return anomalyState; }
    public void setAnomalyState(String anomalyState) { this.anomalyState = anomalyState; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
