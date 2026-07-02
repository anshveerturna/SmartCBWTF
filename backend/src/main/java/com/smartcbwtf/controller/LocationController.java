package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.UserLocation;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.UserLocationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Location tracking controller.
 * POST /api/location/update - Record location (DRIVER, PLANT_OPERATOR)
 * GET /api/admin/users/{id}/location-history - View history (SUPER_ADMIN)
 */
@RestController
@RequestMapping("/api")
public class LocationController {

    private static final Logger log = LoggerFactory.getLogger(LocationController.class);

    // Roles that should track location
    private static final Set<String> TRACKABLE_ROLES = Set.of("DRIVER", "PLANT_OPERATOR");

    private final UserLocationRepository locationRepository;
    private final AppUserRepository userRepository;

    public LocationController(UserLocationRepository locationRepository, AppUserRepository userRepository) {
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record current user's location.
     * Only DRIVER and PLANT_OPERATOR roles can record.
     */
    @PostMapping("/location/update")
    @PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR')")
    public ResponseEntity<Map<String, Object>> updateLocation(@Valid @RequestBody LocationUpdateRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        if (info == null || info.userId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        UUID userId = info.userId();

        if (!isFiniteInRange(request.latitude, -90, 90)
                || !isFiniteInRange(request.longitude, -180, 180)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid coordinates"));
        }

        if (request.accuracy != null && !isFiniteInRange(request.accuracy, 0, 10_000)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid accuracy"));
        }

        var previousLocation = locationRepository.findFirstByUserIdOrderByRecordedAtDesc(userId);
        Instant throttleCutoff = Instant.now().minus(5, java.time.temporal.ChronoUnit.MINUTES);
        boolean shouldUpdateUser = previousLocation.isEmpty()
                || previousLocation.get().getRecordedAt().isBefore(throttleCutoff);

        // Save location
        UserLocation location = UserLocation.create(
                userId,
                request.latitude,
                request.longitude,
                request.accuracy);
        locationRepository.save(location);

        // Update user's last GPS position, throttled to avoid write amplification.
        if (shouldUpdateUser) {
            userRepository.findById(userId).ifPresent(user -> {
                user.updateGpsPosition(
                        java.math.BigDecimal.valueOf(request.latitude),
                        java.math.BigDecimal.valueOf(request.longitude));
                userRepository.save(user);
            });
        }

        log.debug("Recorded location for user {} with accuracy {}m", userId, request.accuracy);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "locationId", location.getId(),
                "recordedAt", location.getRecordedAt().toString()));
    }

    /**
     * Get current user's latest location.
     */
    @GetMapping("/location/current")
    @PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR')")
    public ResponseEntity<Map<String, Object>> getCurrentLocation() {
        TenantContext.TenantInfo info = TenantContext.get();
        if (info == null || info.userId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return locationRepository.findFirstByUserIdOrderByRecordedAtDesc(info.userId())
                .map(loc -> privateResponse(Map.<String, Object>of(
                        "latitude", loc.getLatitude(),
                        "longitude", loc.getLongitude(),
                        "accuracy", loc.getAccuracy() != null ? loc.getAccuracy() : 0,
                        "recordedAt", loc.getRecordedAt().toString())))
                .orElse(privateResponse(Map.of("error", "No location recorded")));
    }

    /**
     * SuperAdmin: Get location history for any user.
     */
    @GetMapping("/admin/users/{userId}/location-history")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getLocationHistory(
            @PathVariable("userId") UUID userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = pageRequest(page, size, 50);
        Page<UserLocation> locations = locationRepository.findByUserIdOrderByRecordedAtDesc(userId, pageable);

        Page<Map<String, Object>> result = locations.map(loc -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", loc.getId());
            map.put("latitude", loc.getLatitude());
            map.put("longitude", loc.getLongitude());
            map.put("accuracy", loc.getAccuracy());
            map.put("recordedAt", loc.getRecordedAt());
            return map;
        });

        return privateResponse(result);
    }

    /**
     * SuperAdmin: Get latest location for a user.
     */
    @GetMapping("/admin/users/{userId}/location")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserLocation(@PathVariable("userId") UUID userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return locationRepository.findFirstByUserIdOrderByRecordedAtDesc(userId)
                .map(loc -> privateResponse(Map.<String, Object>of(
                        "userId", userId,
                        "username", user.get().getUsername(),
                        "latitude", loc.getLatitude(),
                        "longitude", loc.getLongitude(),
                        "accuracy", loc.getAccuracy() != null ? loc.getAccuracy() : 0,
                        "recordedAt", loc.getRecordedAt().toString())))
                .orElse(privateResponse(Map.of(
                        "userId", userId,
                        "username", user.get().getUsername(),
                        "error", "No location recorded")));
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private boolean isFiniteInRange(Double value, double min, double max) {
        return value != null && Double.isFinite(value) && value >= min && value <= max;
    }

    // Request DTO
    public static class LocationUpdateRequest {
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
        public Double latitude;

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
        public Double longitude;

        @DecimalMin(value = "0.0", message = "Accuracy must be zero or greater")
        @DecimalMax(value = "10000.0", message = "Accuracy must be 10000 meters or less")
        public Double accuracy;
    }
}
