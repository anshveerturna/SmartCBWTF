package com.smartcbwtf.gps.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Normalized GPS Event DTO - internal format used after vendor-specific
 * parsing.
 * All adapters must convert their payload to this format.
 */
public record GpsEventDTO(
        String deviceId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speed,
        BigDecimal heading,
        BigDecimal altitude,
        BigDecimal accuracy,
        Instant recordedAt,
        String source,
        Map<String, Object> rawPayload) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String deviceId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal speed;
        private BigDecimal heading;
        private BigDecimal altitude;
        private BigDecimal accuracy;
        private Instant recordedAt;
        private String source;
        private Map<String, Object> rawPayload;

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder latitude(BigDecimal latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(BigDecimal longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder speed(BigDecimal speed) {
            this.speed = speed;
            return this;
        }

        public Builder heading(BigDecimal heading) {
            this.heading = heading;
            return this;
        }

        public Builder altitude(BigDecimal altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder accuracy(BigDecimal accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public Builder recordedAt(Instant recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder rawPayload(Map<String, Object> rawPayload) {
            this.rawPayload = rawPayload;
            return this;
        }

        public GpsEventDTO build() {
            return new GpsEventDTO(deviceId, latitude, longitude, speed, heading,
                    altitude, accuracy, recordedAt, source, rawPayload);
        }
    }
}
