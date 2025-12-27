package com.smartcbwtf.controller;

import com.smartcbwtf.service.StaffService;
import com.smartcbwtf.service.StaffService.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Staff Management Controller for CBWTF Admin Portal.
 * 
 * ACCESS: CBWTF_ADMIN only
 * All operations are automatically scoped to the logged-in admin's facility.
 */
@RestController
@RequestMapping("/api/cbwtf/staff")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class StaffController {

    private static final Logger log = LoggerFactory.getLogger(StaffController.class);

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * List all staff (DRIVER, PLANT_OPERATOR) for the current facility.
     */
    @GetMapping
    public ResponseEntity<Page<StaffDTO>> listStaff(
            @RequestParam(name = "role", required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {

        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(staffService.listStaffByRole(role, pageable));
        }
        return ResponseEntity.ok(staffService.listStaff(pageable));
    }

    /**
     * Get staff detail by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StaffDetailDTO> getStaffDetail(@PathVariable("id") UUID id) {
        return staffService.getStaffDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new staff user.
     * Username is auto-generated in format: <CBWTF_CODE>-<ROLE>-<SEQUENCE>
     */
    @PostMapping
    public ResponseEntity<StaffDTO> createStaff(@RequestBody CreateStaffRequest request) {
        StaffDTO created = staffService.createStaff(request);
        log.info("Staff created: {}", created.username());
        return ResponseEntity.ok(created);
    }

    /**
     * Update staff profile (name, email, phone only).
     */
    @PutMapping("/{id}")
    public ResponseEntity<StaffDTO> updateStaff(
            @PathVariable("id") UUID id,
            @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    /**
     * Disable staff account (soft delete).
     * Staff cannot login but history is preserved.
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<StaffDTO> disableStaff(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(staffService.disableStaff(id));
    }

    /**
     * Re-enable staff account.
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<StaffDTO> enableStaff(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(staffService.enableStaff(id));
    }

    /**
     * Unlock a locked staff account.
     * Used when staff has exceeded failed login attempts.
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<StaffDTO> unlockStaff(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(staffService.unlockStaff(id));
    }

    /**
     * Update staff credentials (username and/or password).
     */
    @PutMapping("/{id}/credentials")
    public ResponseEntity<StaffDTO> updateCredentials(
            @PathVariable("id") UUID id,
            @RequestBody UpdateCredentialsRequest request) {
        return ResponseEntity.ok(staffService.updateCredentials(id, request));
    }

    /**
     * Request GPS refresh from staff's Android app.
     * Sets a timestamp that the Android app checks and responds with current
     * location.
     */
    @PostMapping("/{id}/request-gps-refresh")
    public ResponseEntity<Void> requestGpsRefresh(@PathVariable("id") UUID id) {
        staffService.requestGpsRefresh(id);
        log.info("GPS refresh requested for staff: {}", id);
        return ResponseEntity.ok().build();
    }

    /**
     * Upload staff profile photo.
     */
    @PostMapping(value = "/{id}/photo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(
            @PathVariable("id") UUID id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(staffService.uploadPhoto(id, file));
    }

    /**
     * Remove staff profile photo.
     */
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<?> removePhoto(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(staffService.removePhoto(id));
    }
}
