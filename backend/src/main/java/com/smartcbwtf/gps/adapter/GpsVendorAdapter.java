package com.smartcbwtf.gps.adapter;

import java.util.List;

/**
 * GPS Vendor Adapter Interface - vendor-agnostic GPS data ingestion.
 * 
 * ARCHITECTURE NOTES:
 * - Each GPS vendor (Wheelseye, Fleetx, etc.) implements this interface
 * - Adapters ONLY parse and normalize data - they don't write to DB
 * - Core services handle persistence and business logic
 * 
 * TO ADD A NEW VENDOR:
 * 1. Create a new class implementing this interface
 * 2. Register it as a Spring @Component
 * 3. It will be auto-discovered by GpsIngestionService
 */
public interface GpsVendorAdapter {

    /**
     * Returns the vendor name (e.g., "WHEELSEYE", "FLEETX", "GENERIC")
     * Used for routing webhook payloads to the correct adapter.
     */
    String getVendorName();

    /**
     * Validates the incoming payload structure.
     * Returns true if the payload can be processed by this adapter.
     */
    boolean validatePayload(Object payload);

    /**
     * Parses vendor-specific payload into normalized GpsEventDTO list.
     * 
     * @param payload Raw payload from webhook or API
     * @return List of normalized GPS events
     * @throws IllegalArgumentException if payload is invalid
     */
    List<GpsEventDTO> parsePayload(Object payload);

    /**
     * Optional: Returns whether this adapter supports batch payloads.
     */
    default boolean supportsBatch() {
        return true;
    }
}
