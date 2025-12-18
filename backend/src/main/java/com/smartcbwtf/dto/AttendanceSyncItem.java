package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Single attendance event item within a sync request.
 */
public class AttendanceSyncItem {
    @NotNull
    private UUID clientEventId;

    @NotNull
    private UUID hcfId;

    @NotNull
    private Instant eventTs;

    @NotNull
    private Double gpsLat;

    @NotNull
    private Double gpsLon;

    private Double gpsAccuracyM;

    private String appDeviceId;

    // Getters and setters
    public UUID getClientEventId() { return clientEventId; }
    public void setClientEventId(UUID clientEventId) { this.clientEventId = clientEventId; }

    public UUID getHcfId() { return hcfId; }
    public void setHcfId(UUID hcfId) { this.hcfId = hcfId; }

    public Instant getEventTs() { return eventTs; }
    public void setEventTs(Instant eventTs) { this.eventTs = eventTs; }

    public Double getGpsLat() { return gpsLat; }
    public void setGpsLat(Double gpsLat) { this.gpsLat = gpsLat; }

    public Double getGpsLon() { return gpsLon; }
    public void setGpsLon(Double gpsLon) { this.gpsLon = gpsLon; }

    public Double getGpsAccuracyM() { return gpsAccuracyM; }
    public void setGpsAccuracyM(Double gpsAccuracyM) { this.gpsAccuracyM = gpsAccuracyM; }

    public String getAppDeviceId() { return appDeviceId; }
    public void setAppDeviceId(String appDeviceId) { this.appDeviceId = appDeviceId; }
}
