package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.dto.AttendanceSyncRequest;
import com.smartcbwtf.dto.AttendanceSyncResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AppUserRepository appUserRepository;

    public AttendanceController(AttendanceService attendanceService, AppUserRepository appUserRepository) {
        this.attendanceService = attendanceService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Sync attendance events from mobile device.
     * Supports offline queue: multiple events can be submitted in batch.
     * Each event is processed independently with geofence and cooldown validation.
     * 
     * Returns partial success: some events may succeed while others fail.
     */
    @PostMapping("/sync")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<AttendanceSyncResponse> sync(
            @Valid @RequestBody AttendanceSyncRequest request,
            Authentication authentication) {

        // Get driver user ID from authentication
        String username = authentication.getName();
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        UUID driverId = user.getId();
        UUID facilityId = user.getFacility() != null ? user.getFacility().getId() : null;

        AttendanceSyncResponse response = attendanceService.sync(request, driverId, facilityId);
        return ResponseEntity.ok(response);
    }
}
