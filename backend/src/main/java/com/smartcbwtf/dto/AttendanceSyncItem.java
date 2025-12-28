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

    /**
     * Event timestamp in epoch milliseconds (from Android
     * System.currentTimeMillis()).
     */
    @NotNull
    private Long eventTsMillis;

    @NotNull
    private Double gpsLat;

    @NotNull
    private Double gpsLon;

    private Double gpsAccuracyM;

    private String appDeviceId;

    // Getters and setters
    public UUID getClientEventId() {
        return clientEventId;
    }

    public void setClientEventId(UUID clientEventId) {
        this.clientEventId = clientEventId;
    }

    public UUID getHcfId() {
        return hcfId;
    }

    public void setHcfId(UUID hcfId) {
        this.hcfId = hcfId;
    }

    /**
     * Returns eventTs as a proper Instant, converting from epoch milliseconds.
     */
    public Instant getEventTs() {
        return eventTsMillis != null ? Instant.ofEpochMilli(eventTsMillis) : null;
    }

    public Long getEventTsMillis() {
        return eventTsMillis;
    }

    public void setEventTsMillis(Long eventTsMillis) {
        this.eventTsMillis = eventTsMillis;
    }

    // For backward compatibility with existing JSON that uses "eventTs" field name
    public void setEventTs(Long eventTs) {
        this.eventTsMillis = eventTs;
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

    public Double getGpsAccuracyM() {
        return gpsAccuracyM;
    }

    public void setGpsAccuracyM(Double gpsAccuracyM) {
        this.gpsAccuracyM = gpsAccuracyM;
    }

    public String getAppDeviceId() {
        return appDeviceId;
    }

    public void setAppDeviceId(String appDeviceId) {
        this.appDeviceId = appDeviceId;
    }
}
