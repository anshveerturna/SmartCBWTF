package com.smartcbwtf.gps.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wheelseye GPS Vendor Adapter.
 * 
 * Parses GPS data from Wheelseye API/webhook format.
 * Expected payload structure:
 * {
 * "data": [
 * {
 * "imei": "123456789",
 * "lat": 28.6139,
 * "lng": 77.2090,
 * "speed": 45.5,
 * "heading": 180,
 * "timestamp": "2025-01-01T10:00:00Z"
 * }
 * ]
 * }
 */
@Component
public class WheelseyeAdapter implements GpsVendorAdapter {

    private static final Logger log = LoggerFactory.getLogger(WheelseyeAdapter.class);
    private static final String VENDOR_NAME = "WHEELSEYE";
    private static final String SOURCE = "VENDOR_API";

    private final ObjectMapper objectMapper;

    public WheelseyeAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getVendorName() {
        return VENDOR_NAME;
    }

    @Override
    public boolean validatePayload(Object payload) {
        try {
            JsonNode node = objectMapper.valueToTree(payload);
            return node.has("data") && node.get("data").isArray();
        } catch (Exception e) {
            log.warn("Wheelseye payload validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<GpsEventDTO> parsePayload(Object payload) {
        List<GpsEventDTO> events = new ArrayList<>();

        try {
            JsonNode root = objectMapper.valueToTree(payload);
            JsonNode dataArray = root.get("data");

            if (dataArray == null || !dataArray.isArray()) {
                throw new IllegalArgumentException("Invalid Wheelseye payload: missing 'data' array");
            }

            for (JsonNode item : dataArray) {
                try {
                    GpsEventDTO event = parseItem(item);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse Wheelseye GPS item: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Wheelseye payload: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to parse Wheelseye payload", e);
        }

        return events;
    }

    private GpsEventDTO parseItem(JsonNode item) {
        String deviceId = getStringField(item, "imei");
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }

        BigDecimal lat = getDecimalField(item, "lat");
        BigDecimal lng = getDecimalField(item, "lng", "lon", "longitude");

        if (lat == null || lng == null) {
            log.warn("Missing lat/lng for device: {}", deviceId);
            return null;
        }

        Instant recordedAt = parseTimestamp(item);
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }

        return GpsEventDTO.builder()
                .deviceId(deviceId)
                .latitude(lat)
                .longitude(lng)
                .speed(getDecimalField(item, "speed"))
                .heading(getDecimalField(item, "heading", "course", "bearing"))
                .altitude(getDecimalField(item, "altitude", "alt"))
                .accuracy(getDecimalField(item, "accuracy"))
                .recordedAt(recordedAt)
                .source(SOURCE)
                .rawPayload(item.toString())
                .build();
    }

    private String getStringField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        }
        return null;
    }

    private BigDecimal getDecimalField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && node.get(key).isNumber()) {
                return new BigDecimal(node.get(key).asText());
            }
        }
        return null;
    }

    private Instant parseTimestamp(JsonNode item) {
        String timestamp = getStringField(item, "timestamp", "time", "recorded_at", "gpsTime");
        if (timestamp != null) {
            try {
                return Instant.parse(timestamp);
            } catch (DateTimeParseException e) {
                // Try epoch seconds
                try {
                    long epoch = Long.parseLong(timestamp);
                    return epoch > 1_000_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }
}
