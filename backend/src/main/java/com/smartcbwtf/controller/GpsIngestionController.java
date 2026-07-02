package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.service.GpsDeviceBindingService;
import com.smartcbwtf.service.GpsIngestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
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
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GpsIngestionController {

    private static final Logger log = LoggerFactory.getLogger(GpsIngestionController.class);
    private static final int MAX_VENDOR_LENGTH = 50;
    private static final String VENDOR_PATTERN = "^[A-Za-z0-9_-]+$";

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
            String normalizedVendor = cleanVendor(vendor);
            int count = ingestionService.ingestFromVendor(normalizedVendor, payload);
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
    public ResponseEntity<BindResponse> bindDevice(@Valid @RequestBody BindRequest request) {
        UUID performedBy = authenticatedUserId();
        if (performedBy == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BindResponse(false, "Authenticated user not available"));
        }
        try {
            bindingService.bindDevice(
                    cleanLine(request.deviceId()),
                    request.vehicleId(),
                    cleanLine(request.vendor()).toUpperCase(Locale.ROOT),
                    performedBy,
                    trimToNull(request.notes()));
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
    public ResponseEntity<BindResponse> unbindDevice(@Valid @RequestBody UnbindRequest request) {
        UUID performedBy = authenticatedUserId();
        if (performedBy == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new BindResponse(false, "Authenticated user not available"));
        }
        try {
            bindingService.unbindDevice(
                    cleanLine(request.deviceId()),
                    performedBy,
                    trimToNull(request.notes()));
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

    private UUID authenticatedUserId() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null ? info.userId() : null;
    }

    private static String cleanLine(String value) {
        return value == null ? "" : value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = cleanLine(value);
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String cleanVendor(String vendor) {
        String cleaned = cleanLine(vendor);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Vendor is required");
        }
        if (cleaned.length() > MAX_VENDOR_LENGTH) {
            throw new IllegalArgumentException("Vendor must be 50 characters or less");
        }
        if (!cleaned.matches(VENDOR_PATTERN)) {
            throw new IllegalArgumentException("Vendor contains invalid characters");
        }
        return cleaned.toUpperCase(Locale.ROOT);
    }

    // Request/Response DTOs
    public record IngestionResponse(boolean success, int eventsIngested, String error) {
    }

    public record BindRequest(
            @NotBlank(message = "Device ID is required")
            @Size(max = 100, message = "Device ID must be 100 characters or less")
            @Pattern(regexp = "^[A-Za-z0-9._:-]+$", message = "Device ID contains invalid characters")
            String deviceId,
            @NotNull(message = "Vehicle ID is required")
            UUID vehicleId,
            @NotBlank(message = "Vendor is required")
            @Size(max = 50, message = "Vendor must be 50 characters or less")
            @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Vendor contains invalid characters")
            String vendor,
            UUID performedBy,
            @Size(max = 2000, message = "Notes must be 2000 characters or less")
            String notes) {
    }

    public record UnbindRequest(
            @NotBlank(message = "Device ID is required")
            @Size(max = 100, message = "Device ID must be 100 characters or less")
            @Pattern(regexp = "^[A-Za-z0-9._:-]+$", message = "Device ID contains invalid characters")
            String deviceId,
            UUID performedBy,
            @Size(max = 2000, message = "Notes must be 2000 characters or less")
            String notes) {
    }

    public record BindResponse(boolean success, String message) {
    }
}
