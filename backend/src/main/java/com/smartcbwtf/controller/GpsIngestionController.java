package com.smartcbwtf.controller;

import com.smartcbwtf.service.GpsDeviceBindingService;
import com.smartcbwtf.service.GpsIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * GPS Ingestion Controller - Internal APIs for GPS data ingestion and device
 * management.
 * 
 * These endpoints are NOT for CBWTF admins - they're for:
 * - Vendor webhooks
 * - Support team operations
 * - System integrations
 */
@RestController
@RequestMapping("/api/internal/gps")
public class GpsIngestionController {

    private static final Logger log = LoggerFactory.getLogger(GpsIngestionController.class);

    private final GpsIngestionService ingestionService;
    private final GpsDeviceBindingService bindingService;

    public GpsIngestionController(
            GpsIngestionService ingestionService,
            GpsDeviceBindingService bindingService) {
        this.ingestionService = ingestionService;
        this.bindingService = bindingService;
    }

    /**
     * POST /api/internal/gps/ingest/{vendor} - Ingest GPS data from a vendor.
     * 
     * Used by:
     * - Wheelseye webhook
     * - Other vendor webhooks
     * - Polling jobs
     */
    @PostMapping("/ingest/{vendor}")
    public ResponseEntity<IngestionResponse> ingestGpsData(
            @PathVariable String vendor,
            @RequestBody Object payload) {
        try {
            int count = ingestionService.ingestFromVendor(vendor, payload);
            return ResponseEntity.ok(new IngestionResponse(true, count, null));
        } catch (IllegalArgumentException e) {
            log.warn("GPS ingestion failed for vendor {}: {}", vendor, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new IngestionResponse(false, 0, e.getMessage()));
        } catch (Exception e) {
            log.error("GPS ingestion error for vendor {}: {}", vendor, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new IngestionResponse(false, 0, "Internal error"));
        }
    }

    /**
     * POST /api/internal/gps/bind - Bind a GPS device to a vehicle.
     * Support team only.
     */
    @PostMapping("/bind")
    public ResponseEntity<BindResponse> bindDevice(@RequestBody BindRequest request) {
        try {
            bindingService.bindDevice(
                    request.deviceId(),
                    request.vehicleId(),
                    request.vendor(),
                    request.performedBy(),
                    request.notes());
            return ResponseEntity.ok(new BindResponse(true, "Device bound successfully"));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new BindResponse(false, e.getMessage()));
        }
    }

    /**
     * POST /api/internal/gps/unbind - Unbind a GPS device from a vehicle.
     */
    @PostMapping("/unbind")
    public ResponseEntity<BindResponse> unbindDevice(@RequestBody UnbindRequest request) {
        try {
            bindingService.unbindDevice(
                    request.deviceId(),
                    request.performedBy(),
                    request.notes());
            return ResponseEntity.ok(new BindResponse(true, "Device unbound successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new BindResponse(false, e.getMessage()));
        }
    }

    /**
     * GET /api/internal/gps/vendors - List registered GPS vendors/adapters.
     */
    @GetMapping("/vendors")
    public ResponseEntity<Set<String>> getRegisteredVendors() {
        return ResponseEntity.ok(ingestionService.getRegisteredVendors());
    }

    /**
     * POST /api/internal/gps/update-offline - Trigger offline detection.
     * Called by scheduled jobs.
     */
    @PostMapping("/update-offline")
    public ResponseEntity<Map<String, Integer>> updateOfflineVehicles() {
        int count = ingestionService.updateOfflineVehicles();
        return ResponseEntity.ok(Map.of("vehiclesMarkedOffline", count));
    }

    // Request/Response DTOs
    public record IngestionResponse(boolean success, int eventsIngested, String error) {
    }

    public record BindRequest(
            String deviceId,
            UUID vehicleId,
            String vendor,
            UUID performedBy,
            String notes) {
    }

    public record UnbindRequest(
            String deviceId,
            UUID performedBy,
            String notes) {
    }

    public record BindResponse(boolean success, String message) {
    }
}
