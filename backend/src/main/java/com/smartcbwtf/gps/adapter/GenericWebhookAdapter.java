package com.smartcbwtf.gps.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Webhook GPS Adapter.
 * 
 * Fallback adapter for custom/generic GPS payloads.
 * Supports multiple common field naming conventions.
 * 
 * Expected payload structure (flexible):
 * {
 * "events": [
 * {
 * "device_id": "...",
 * "latitude": 28.6139,
 * "longitude": 77.2090,
 * "speed": 45.5,
 * "timestamp": "2025-01-01T10:00:00Z"
 * }
 * ]
 * }
 * 
 * OR single event:
 * {
 * "device_id": "...",
 * "latitude": 28.6139,
 * ...
 * }
 */
@Component
public class GenericWebhookAdapter implements GpsVendorAdapter {

    private static final Logger log = LoggerFactory.getLogger(GenericWebhookAdapter.class);
    private static final String VENDOR_NAME = "GENERIC";
    private static final String SOURCE = "IOT_DEVICE";

    private final ObjectMapper objectMapper;

    public GenericWebhookAdapter(ObjectMapper objectMapper) {
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
            // Accept arrays or single objects
            return node.isArray() || node.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<GpsEventDTO> parsePayload(Object payload) {
        List<GpsEventDTO> events = new ArrayList<>();

        try {
            JsonNode root = objectMapper.valueToTree(payload);

            // Handle array at root
            if (root.isArray()) {
                for (JsonNode item : root) {
                    parseAndAdd(item, events);
                }
            }
            // Handle "events" or "data" array
            else if (root.has("events") && root.get("events").isArray()) {
                for (JsonNode item : root.get("events")) {
                    parseAndAdd(item, events);
                }
            } else if (root.has("data") && root.get("data").isArray()) {
                for (JsonNode item : root.get("data")) {
                    parseAndAdd(item, events);
                }
            }
            // Handle single object
            else if (root.isObject()) {
                parseAndAdd(root, events);
            }
        } catch (Exception e) {
            log.error("Error parsing generic webhook payload: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to parse generic webhook payload", e);
        }

        return events;
    }

    private void parseAndAdd(JsonNode item, List<GpsEventDTO> events) {
        try {
            GpsEventDTO event = parseItem(item);
            if (event != null) {
                events.add(event);
            }
        } catch (Exception e) {
            log.warn("Failed to parse generic GPS item: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private GpsEventDTO parseItem(JsonNode item) {
        String deviceId = findStringField(item, "device_id", "deviceId", "imei", "id", "tracker_id");
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }

        BigDecimal lat = findDecimalField(item, "latitude", "lat");
        BigDecimal lng = findDecimalField(item, "longitude", "lng", "lon");

        if (lat == null || lng == null) {
            return null;
        }

        Instant recordedAt = parseTimestamp(item);
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }

        // Convert JsonNode to Map for JSONB storage
        java.util.Map<String, Object> rawPayload = objectMapper.convertValue(item, java.util.Map.class);

        return GpsEventDTO.builder()
                .deviceId(deviceId)
                .latitude(lat)
                .longitude(lng)
                .speed(findDecimalField(item, "speed", "velocity"))
                .heading(findDecimalField(item, "heading", "course", "bearing", "direction"))
                .altitude(findDecimalField(item, "altitude", "alt", "elevation"))
                .accuracy(findDecimalField(item, "accuracy", "hdop"))
                .recordedAt(recordedAt)
                .source(SOURCE)
                .rawPayload(rawPayload)
                .build();
    }

    private String findStringField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && !node.get(key).isNull()) {
                return node.get(key).asText();
            }
        }
        return null;
    }

    private BigDecimal findDecimalField(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key) && node.get(key).isNumber()) {
                return new BigDecimal(node.get(key).asText());
            }
        }
        return null;
    }

    private Instant parseTimestamp(JsonNode item) {
        String timestamp = findStringField(item, "timestamp", "time", "recorded_at",
                "gps_time", "created_at", "datetime");
        if (timestamp != null) {
            try {
                return Instant.parse(timestamp);
            } catch (Exception e) {
                try {
                    long epoch = Long.parseLong(timestamp);
                    return epoch > 1_000_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Check for numeric timestamp field
        if (item.has("timestamp") && item.get("timestamp").isNumber()) {
            long epoch = item.get("timestamp").asLong();
            return epoch > 1_000_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
        }

        return null;
    }
}
