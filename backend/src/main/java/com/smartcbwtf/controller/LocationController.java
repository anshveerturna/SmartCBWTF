package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.UserLocation;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.UserLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

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
    public ResponseEntity<Map<String, Object>> updateLocation(@RequestBody LocationUpdateRequest request) {
        TenantContext.TenantInfo info = TenantContext.get();
        if (info == null || info.userId() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        UUID userId = info.userId();

        // Validate coordinates
        if (request.latitude == null || request.longitude == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Latitude and longitude required"));
        }

        if (request.latitude < -90 || request.latitude > 90 ||
                request.longitude < -180 || request.longitude > 180) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid coordinates"));
        }

        // Save location
        UserLocation location = UserLocation.create(
                userId,
                request.latitude,
                request.longitude,
                request.accuracy);
        locationRepository.save(location);

        log.debug("Recorded location for user {}: ({}, {})", userId, request.latitude, request.longitude);

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
                .map(loc -> ResponseEntity.ok(Map.<String, Object>of(
                        "latitude", loc.getLatitude(),
                        "longitude", loc.getLongitude(),
                        "accuracy", loc.getAccuracy() != null ? loc.getAccuracy() : 0,
                        "recordedAt", loc.getRecordedAt().toString())))
                .orElse(ResponseEntity.ok(Map.of("error", "No location recorded")));
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

        Pageable pageable = PageRequest.of(page, size);
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

        return ResponseEntity.ok(result);
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
                .map(loc -> ResponseEntity.ok(Map.<String, Object>of(
                        "userId", userId,
                        "username", user.get().getUsername(),
                        "latitude", loc.getLatitude(),
                        "longitude", loc.getLongitude(),
                        "accuracy", loc.getAccuracy() != null ? loc.getAccuracy() : 0,
                        "recordedAt", loc.getRecordedAt().toString())))
                .orElse(ResponseEntity.ok(Map.of(
                        "userId", userId,
                        "username", user.get().getUsername(),
                        "error", "No location recorded")));
    }

    // Request DTO
    public static class LocationUpdateRequest {
        public Double latitude;
        public Double longitude;
        public Double accuracy;
    }
}
