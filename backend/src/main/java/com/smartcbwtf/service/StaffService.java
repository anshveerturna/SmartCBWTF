package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Attendance;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Staff Management Service for CBWTF Admin Portal.
 * 
 * SECURITY INVARIANTS:
 * - All operations are scoped to TenantContext.getTenantId()
 * - Staff roles: DRIVER, PLANT_OPERATOR only (not CBWTF_ADMIN)
 * - Username format: <CBWTF_CODE>-<ROLE>-<SEQUENCE>
 * - No hard deletes - soft disable only
 * - All actions are audited
 */
@Service
@Transactional
public class StaffService {

    private static final Logger log = LoggerFactory.getLogger(StaffService.class);
    private static final int ONLINE_THRESHOLD_MINUTES = 15;

    // Staff roles managed through this service
    public static final String ROLE_DRIVER = "DRIVER";
    public static final String ROLE_PLANT_OPERATOR = "PLANT_OPERATOR";
    private static final List<String> STAFF_ROLES = List.of(ROLE_DRIVER, ROLE_PLANT_OPERATOR);

    private final AppUserRepository appUserRepository;
    private final FacilityRepository facilityRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public StaffService(
            AppUserRepository appUserRepository,
            FacilityRepository facilityRepository,
            AttendanceRepository attendanceRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.facilityRepository = facilityRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * List all staff (DRIVER, PLANT_OPERATOR) for the current facility.
     */
    @Transactional(readOnly = true)
    public Page<StaffDTO> listStaff(Pageable pageable) {
        UUID facilityId = TenantContext.getTenantId();
        log.info("Listing staff for facility: {} (role: {})", facilityId, TenantContext.getRole());

        if (facilityId == null) {
            log.error("No facility ID in context. User: {}, Role: {}",
                    TenantContext.getUsername(), TenantContext.getRole());
            throw new IllegalStateException("Facility context not available. Please re-login.");
        }

        return appUserRepository.findByFacilityIdAndRoleIn(facilityId, STAFF_ROLES, pageable)
                .map(this::toStaffDTO);
    }

    /**
     * List staff filtered by role.
     */
    @Transactional(readOnly = true)
    public Page<StaffDTO> listStaffByRole(String role, Pageable pageable) {
        UUID facilityId = TenantContext.getTenantId();
        validateStaffRole(role);
        return appUserRepository.findByFacilityIdAndRole(facilityId, role, pageable)
                .map(this::toStaffDTO);
    }

    /**
     * Get staff detail by ID (with ownership check).
     */
    @Transactional(readOnly = true)
    public Optional<StaffDetailDTO> getStaffDetail(UUID staffId) {
        UUID facilityId = TenantContext.getTenantId();
        return appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .map(this::toStaffDetailDTO);
    }

    /**
     * Create new staff user with auto-generated username.
     * Username format: <CBWTF_CODE>-<ROLE_PREFIX>-<SEQUENCE>
     */
    public StaffDTO createStaff(CreateStaffRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        validateStaffRole(request.role());

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility not found: " + facilityId));

        // Generate unique username
        String username = generateUsername(facility.getCode(), request.role());

        // Check username uniqueness (should never fail due to sequence logic, but
        // safety first)
        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username already exists: " + username);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setFacility(facility);
        user.setActive(true);
        user.setForcePasswordChange(true); // Force password change on first login

        // Generate temporary password or use provided
        String tempPassword = request.password() != null ? request.password() : generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);

        user = appUserRepository.save(user);

        // Audit
        auditLogService.log(
                "STAFF",
                user.getId(),
                "STAFF_CREATED",
                TenantContext.getUserId(),
                String.format("{\"username\":\"%s\",\"role\":\"%s\",\"fullName\":\"%s\"}",
                        username, request.role(), request.fullName()));

        log.info("Created staff user: {} ({}) for facility: {}", username, request.role(), facility.getCode());

        // Return DTO with temporary password (only time it's visible)
        return new StaffDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                getGpsStatus(user),
                user.getLastGpsAt(),
                user.getCreatedAt(),
                tempPassword);
    }

    /**
     * Update staff profile (name, email, phone only).
     * Role changes require separate explicit action.
     */
    public StaffDTO updateStaff(UUID staffId, UpdateStaffRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_UPDATED", TenantContext.getUserId(),
                String.format("{\"fullName\":\"%s\"}", request.fullName()));

        return toStaffDTO(user);
    }

    /**
     * Disable staff account (soft delete). Prevents login but preserves history.
     */
    public StaffDTO disableStaff(UUID staffId) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        user.setActive(false);
        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_DISABLED", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("Disabled staff user: {}", user.getUsername());
        return toStaffDTO(user);
    }

    /**
     * Re-enable staff account.
     */
    public StaffDTO enableStaff(UUID staffId) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        user.setActive(true);
        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_ENABLED", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("Enabled staff user: {}", user.getUsername());
        return toStaffDTO(user);
    }

    /**
     * Unlock a locked staff account (admin action).
     * Called when staff has been locked due to failed login attempts.
     */
    public StaffDTO unlockStaff(UUID staffId) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        user.unlockAccount();
        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_UNLOCKED", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("Unlocked staff user: {}", user.getUsername());
        return toStaffDTO(user);
    }

    /**
     * Reset staff password (admin action).
     */
    public String resetPassword(UUID staffId) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        String newPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        user.setForcePasswordChange(true);
        appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_PASSWORD_RESET", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("Reset password for staff user: {}", user.getUsername());
        return newPassword;
    }

    /**
     * Update staff login credentials (username and optionally password).
     * Username must be unique across the entire system.
     */
    public StaffDTO updateCredentials(UUID staffId, UpdateCredentialsRequest request) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        String oldUsername = user.getUsername();

        // Check if username is changing and validate uniqueness
        if (request.username() != null && !request.username().isBlank()
                && !request.username().equals(user.getUsername())) {
            if (appUserRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username already exists: " + request.username());
            }
            user.setUsername(request.username());
        }

        // Update password if provided
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setMustChangePassword(request.forcePasswordChange() != null ? request.forcePasswordChange() : false);
            user.setForcePasswordChange(request.forcePasswordChange() != null ? request.forcePasswordChange() : false);
        }

        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_CREDENTIALS_UPDATED", TenantContext.getUserId(),
                String.format("{\"oldUsername\":\"%s\",\"newUsername\":\"%s\",\"passwordChanged\":%b}",
                        oldUsername, user.getUsername(), request.password() != null));

        log.info("Updated credentials for staff: {} -> {}", oldUsername, user.getUsername());
        return toStaffDTO(user);
    }

    /**
     * Request GPS refresh from staff's Android app.
     * Sets a timestamp that the Android app checks during sync and responds with
     * immediate location update.
     */
    public void requestGpsRefresh(UUID staffId) {
        UUID facilityId = TenantContext.getTenantId();

        AppUser user = appUserRepository.findById(staffId)
                .filter(u -> u.getFacility() != null && u.getFacility().getId().equals(facilityId))
                .filter(u -> STAFF_ROLES.contains(u.getRole()))
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        user.requestGpsRefresh();
        appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "GPS_REFRESH_REQUESTED", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("GPS refresh requested for staff: {}", user.getUsername());
    }

    // ============ Helper Methods ============

    private String generateUsername(String cbwtfCode, String role) {
        String prefix = switch (role) {
            case ROLE_DRIVER -> "DRV";
            case ROLE_PLANT_OPERATOR -> "PLANT";
            default -> "STAFF";
        };

        // Find next sequence number
        String pattern = cbwtfCode + "-" + prefix + "-%";
        int maxSequence = 0;

        // Query for existing usernames with this pattern
        List<AppUser> existingUsers = appUserRepository.findByFacilityIdAndRole(
                TenantContext.getTenantId(), role);

        for (AppUser u : existingUsers) {
            String username = u.getUsername();
            if (username.startsWith(cbwtfCode + "-" + prefix + "-")) {
                try {
                    String seqPart = username.substring((cbwtfCode + "-" + prefix + "-").length());
                    int seq = Integer.parseInt(seqPart);
                    maxSequence = Math.max(maxSequence, seq);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return String.format("%s-%s-%04d", cbwtfCode, prefix, maxSequence + 1);
    }

    private String generateTempPassword() {
        // Generate a secure random password
        return "Temp@" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void validateStaffRole(String role) {
        if (!STAFF_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid staff role: " + role + ". Must be DRIVER or PLANT_OPERATOR");
        }
    }

    private String getGpsStatus(AppUser user) {
        if (user.getLastGpsAt() == null) {
            return "NEVER";
        }
        Instant threshold = Instant.now().minus(ONLINE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        return user.getLastGpsAt().isAfter(threshold) ? "ONLINE" : "OFFLINE";
    }

    private StaffDTO toStaffDTO(AppUser user) {
        return new StaffDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                getGpsStatus(user),
                user.getLastGpsAt(),
                user.getCreatedAt(),
                null);
    }

    private StaffDetailDTO toStaffDetailDTO(AppUser user) {
        // Get last attendance
        Optional<Attendance> lastAttendance = attendanceRepository.findLatestByDriverId(user.getId());

        return new StaffDetailDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                getGpsStatus(user),
                user.getLastGpsAt(),
                user.getLastGpsLat(),
                user.getLastGpsLon(),
                lastAttendance.map(a -> a.getHcf().getName()).orElse(null),
                lastAttendance.map(Attendance::getEventTs).orElse(null),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    // ============ DTOs ============

    public record StaffDTO(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone,
            String role,
            boolean active,
            String gpsStatus,
            Instant lastGpsAt,
            Instant createdAt,
            String tempPassword) {
    }

    public record StaffDetailDTO(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone,
            String role,
            boolean active,
            String gpsStatus,
            Instant lastGpsAt,
            java.math.BigDecimal lastGpsLat,
            java.math.BigDecimal lastGpsLon,
            String lastAttendanceHcf,
            Instant lastAttendanceAt,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record CreateStaffRequest(
            String fullName,
            String email,
            String phone,
            String role,
            String password) {
    }

    public record UpdateStaffRequest(
            String fullName,
            String email,
            String phone) {
    }

    public record UpdateCredentialsRequest(
            String username,
            String password,
            Boolean forcePasswordChange) {
    }
}
