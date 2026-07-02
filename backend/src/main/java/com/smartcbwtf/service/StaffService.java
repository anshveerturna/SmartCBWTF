package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Attendance;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
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
    private static final String OPTIONAL_PHONE_PATTERN = "^$|^(?=(?:\\D*\\d){10,15}\\D*$)[+()\\-\\s0-9]+$";
    private static final String OPTIONAL_USERNAME_PATTERN = "^$|^[A-Za-z0-9._@-]+$";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PASSWORD_RANDOM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final AppUserRepository appUserRepository;
    private final FacilityRepository facilityRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public StaffService(
            AppUserRepository appUserRepository,
            FacilityRepository facilityRepository,
            AttendanceRepository attendanceRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            EmailService emailService,
            PasswordPolicyValidator passwordPolicyValidator) {
        this.appUserRepository = appUserRepository;
        this.facilityRepository = facilityRepository;
        this.attendanceRepository = attendanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /**
     * List all staff (DRIVER, PLANT_OPERATOR) for the current facility.
     */
    @Transactional(readOnly = true)
    public Page<StaffDTO> listStaff(Pageable pageable) {
        UUID facilityId = currentFacilityId();
        log.info("Listing staff for facility: {} (role: {})", facilityId, TenantContext.getRole());

        return appUserRepository.findByFacilityIdAndRoleIn(facilityId, STAFF_ROLES, pageable)
                .map(this::toStaffDTO);
    }

    /**
     * List staff filtered by role.
     */
    @Transactional(readOnly = true)
    public Page<StaffDTO> listStaffByRole(String role, Pageable pageable) {
        UUID facilityId = currentFacilityId();
        validateStaffRole(role);
        return appUserRepository.findByFacilityIdAndRole(facilityId, role, pageable)
                .map(this::toStaffDTO);
    }

    /**
     * Get staff detail by ID (with ownership check).
     */
    @Transactional(readOnly = true)
    public Optional<StaffDetailDTO> getStaffDetail(UUID staffId) {
        return findStaffForCurrentFacility(staffId).map(this::toStaffDetailDTO);
    }

    /**
     * Create new staff user with auto-generated username.
     * Username format: <CBWTF_CODE>-<ROLE_PREFIX>-<SEQUENCE>
     */
    public StaffDTO createStaff(CreateStaffRequest request) {
        UUID facilityId = TenantContext.getTenantId();
        String role = normalizeRole(request.role());
        validateStaffRole(role);
        String fullName = cleanLineRequired(request.fullName(), "Full name");
        String email = optionalCleanLine(request.email());
        String phone = optionalCleanLine(request.phone());

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility not found: " + facilityId));

        // Generate unique username
        String username = generateUsername(facility.getCode(), role);

        // Check username uniqueness (should never fail due to sequence logic, but
        // safety first)
        if (appUserRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username already exists: " + username);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role);
        user.setFacility(facility);
        user.setActive(true);
        user.setForcePasswordChange(true); // Force password change on first login

        // Generate temporary password or use provided
        String tempPassword = request.password() != null && !request.password().isBlank()
                ? request.password()
                : generateTempPassword();
        passwordPolicyValidator.validateOrThrow(tempPassword);
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
                        username, role, fullName));

        log.info("Created staff user: {} ({}) for facility: {}", username, role, facility.getCode());

        // Send staff credentials email
        if (email != null) {
            try {
                String html = emailService.getTemplates().staffCredentials(
                        fullName, role, username, tempPassword, facility.getName());
                emailService.sendHtmlEmail(email, "Your SmartCBWTF Staff Account", html);
                log.info("Staff credentials email sent to: {}", email);
            } catch (Exception e) {
                log.warn("Failed to send staff credentials email to {}: {}", email, e.getMessage());
            }
        }

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
     * Update staff profile (all profile fields).
     * Role changes require separate explicit action.
     */
    public StaffDTO updateStaff(UUID staffId, UpdateStaffRequest request) {
        AppUser user = findStaffForCurrentFacility(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        String profilePhotoUrl = UploadFileValidator.optionalProfilePhotoUrl(request.profilePhotoUrl());
        String fullName = cleanLineRequired(request.fullName(), "Full name");
        user.setFullName(fullName);
        user.setEmail(optionalCleanLine(request.email()));
        user.setPhone(optionalCleanLine(request.phone()));
        user.setGender(optionalCleanLine(request.gender()));
        user.setDob(request.dob());
        user.setProfilePhotoUrl(profilePhotoUrl);
        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_UPDATED", TenantContext.getUserId(),
                String.format("{\"fullName\":\"%s\"}", fullName));

        return toStaffDTO(user);
    }

    /**
     * Disable staff account (soft delete). Prevents login but preserves history.
     */
    public StaffDTO disableStaff(UUID staffId) {
        AppUser user = findStaffForCurrentFacility(staffId)
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
        AppUser user = findStaffForCurrentFacility(staffId)
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
        AppUser user = findStaffForCurrentFacility(staffId)
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
        AppUser user = findStaffForCurrentFacility(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        String newPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        user.setForcePasswordChange(true);
        appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_PASSWORD_RESET", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("Reset password for staff user: {}", user.getUsername());

        // Send password reset email
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                String html = emailService.getTemplates().passwordReset(user.getFullName(), newPassword);
                emailService.sendHtmlEmail(user.getEmail(), "Your SmartCBWTF Password Has Been Reset", html);
                log.info("Password reset email sent to staff: {}", user.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        return newPassword;
    }

    /**
     * Update staff login credentials (username and optionally password).
     * Username must be unique across the entire system.
     */
    public StaffDTO updateCredentials(UUID staffId, UpdateCredentialsRequest request) {
        AppUser user = findStaffForCurrentFacility(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        String oldUsername = user.getUsername();
        String requestedUsername = optionalCleanLine(request.username());
        boolean passwordProvided = request.password() != null && !request.password().isBlank();

        // Check if username is changing and validate uniqueness
        if (requestedUsername != null && !requestedUsername.equals(user.getUsername())) {
            if (!requestedUsername.matches("[A-Za-z0-9._@-]+")) {
                throw new IllegalArgumentException("Username contains invalid characters");
            }
            if (appUserRepository.existsByUsername(requestedUsername)) {
                throw new IllegalArgumentException("Username already exists: " + requestedUsername);
            }
            user.setUsername(requestedUsername);
        }

        // Update password if provided
        if (passwordProvided) {
            passwordPolicyValidator.validateOrThrow(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setMustChangePassword(request.forcePasswordChange() != null ? request.forcePasswordChange() : false);
            user.setForcePasswordChange(request.forcePasswordChange() != null ? request.forcePasswordChange() : false);
        }

        user = appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "STAFF_CREDENTIALS_UPDATED", TenantContext.getUserId(),
                String.format("{\"oldUsername\":\"%s\",\"newUsername\":\"%s\",\"passwordChanged\":%b}",
                        oldUsername, user.getUsername(), passwordProvided));

        log.info("Updated credentials for staff: {} -> {}", oldUsername, user.getUsername());
        return toStaffDTO(user);
    }

    /**
     * Request GPS refresh from staff's Android app.
     * Sets a timestamp that the Android app checks during sync and responds with
     * immediate location update.
     */
    public void requestGpsRefresh(UUID staffId) {
        AppUser user = findStaffForCurrentFacility(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found or access denied"));

        user.requestGpsRefresh();
        appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "GPS_REFRESH_REQUESTED", TenantContext.getUserId(),
                String.format("{\"username\":\"%s\"}", user.getUsername()));

        log.info("GPS refresh requested for staff: {}", user.getUsername());
    }

    /**
     * Upload staff profile photo.
     */
    public java.util.Map<String, String> uploadPhoto(UUID staffId,
            org.springframework.web.multipart.MultipartFile file) {
        AppUser user = findStaffForCurrentFacility(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        String ext = UploadFileValidator.publicImageExtension(file);

        try {
            String uploadDir = "uploads/profiles";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String filename = user.getId().toString() + "." + ext;
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String oldPhoto = user.getProfilePhotoUrl();
            String newPhoto = "/uploads/profiles/" + filename;
            deleteOldProfilePhoto(oldPhoto, newPhoto);
            user.setProfilePhotoUrl(newPhoto);
            user.setUpdatedAt(Instant.now());
            appUserRepository.save(user);

            auditLogService.log("STAFF", user.getId(), "PHOTO_UPDATED", TenantContext.getUserId(),
                    String.format("{\"old\":\"%s\",\"new\":\"%s\"}", oldPhoto, newPhoto));

            log.info("Photo uploaded for staff: {}", user.getUsername());
            return java.util.Map.of("photoUrl", newPhoto);

        } catch (java.io.IOException e) {
            log.error("Failed to upload photo", e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    /**
     * Remove staff profile photo.
     */
    public java.util.Map<String, String> removePhoto(UUID staffId) {
        AppUser user = findStaffForCurrentFacility(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        String oldPhoto = user.getProfilePhotoUrl();
        if (oldPhoto == null) {
            return java.util.Map.of("message", "No photo to remove");
        }

        try {
            UploadFileValidator.deleteProfilePhotoIfPresent("uploads/profiles", oldPhoto);
        } catch (java.io.IOException | IllegalArgumentException e) {
            log.warn("Failed to delete photo file: {}", e.getMessage());
        }

        user.setProfilePhotoUrl(null);
        user.setUpdatedAt(Instant.now());
        appUserRepository.save(user);

        auditLogService.log("STAFF", user.getId(), "PHOTO_REMOVED", TenantContext.getUserId(),
                String.format("{\"removed\":\"%s\"}", oldPhoto));

        log.info("Photo removed for staff: {}", user.getUsername());
        return java.util.Map.of("message", "Photo removed successfully");
    }

    // ============ Helper Methods ============

    private UUID currentFacilityId() {
        UUID facilityId = TenantContext.getTenantId();
        if (facilityId == null) {
            log.error("No facility ID in context. User: {}, Role: {}",
                    TenantContext.getUsername(), TenantContext.getRole());
            throw new IllegalStateException("Facility context not available. Please re-login.");
        }
        return facilityId;
    }

    private Optional<AppUser> findStaffForCurrentFacility(UUID staffId) {
        return appUserRepository.findByIdAndFacilityIdAndRoleIn(staffId, currentFacilityId(), STAFF_ROLES);
    }

    private void deleteOldProfilePhoto(String oldPhoto, String newPhoto) {
        if (oldPhoto == null || oldPhoto.equals(newPhoto)) {
            return;
        }
        try {
            UploadFileValidator.deleteProfilePhotoIfPresent("uploads/profiles", oldPhoto);
        } catch (java.io.IOException | IllegalArgumentException e) {
            log.warn("Failed to delete old photo file: {}", e.getMessage());
        }
    }

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
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            suffix.append(PASSWORD_RANDOM_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_RANDOM_CHARS.length())));
        }
        return "Tmp@" + SECURE_RANDOM.nextInt(10) + suffix;
    }

    private void validateStaffRole(String role) {
        if (!STAFF_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid staff role: " + role + ". Must be DRIVER or PLANT_OPERATOR");
        }
    }

    private String normalizeRole(String role) {
        return cleanLineRequired(role, "Role").toUpperCase(java.util.Locale.ROOT);
    }

    private String cleanLineRequired(String value, String fieldName) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    private String optionalCleanLine(String value) {
        String cleaned = cleanLine(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
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
                user.getGender(),
                user.getDob(),
                user.getProfilePhotoUrl(),
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
            String gender,
            LocalDate dob,
            String profilePhotoUrl,
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
            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be 120 characters or less")
            String fullName,
            @Email(message = "Invalid email format")
            @Size(max = 180, message = "Email must be 180 characters or less")
            String email,
            @Size(max = 20, message = "Phone must be 20 characters or less")
            @Pattern(regexp = OPTIONAL_PHONE_PATTERN, message = "Invalid phone number")
            String phone,
            @NotBlank(message = "Role is required")
            @Pattern(regexp = ROLE_DRIVER + "|" + ROLE_PLANT_OPERATOR, message = "Role must be DRIVER or PLANT_OPERATOR")
            String role,
            @Size(max = 256, message = "Password must be 256 characters or less")
            String password) {
    }

    public record UpdateStaffRequest(
            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be 120 characters or less")
            String fullName,
            @Email(message = "Invalid email format")
            @Size(max = 180, message = "Email must be 180 characters or less")
            String email,
            @Size(max = 20, message = "Phone must be 20 characters or less")
            @Pattern(regexp = OPTIONAL_PHONE_PATTERN, message = "Invalid phone number")
            String phone,
            @Pattern(regexp = "^$|MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE, or OTHER")
            String gender,
            @PastOrPresent(message = "Date of birth cannot be in the future")
            LocalDate dob,
            @Size(max = 512, message = "Profile photo URL must be 512 characters or less")
            String profilePhotoUrl) {
    }

    public record UpdateCredentialsRequest(
            @Size(max = 100, message = "Username must be 100 characters or less")
            @Pattern(regexp = OPTIONAL_USERNAME_PATTERN, message = "Username contains invalid characters")
            String username,
            @Size(max = 256, message = "Password must be 256 characters or less")
            String password,
            Boolean forcePasswordChange) {
    }
}
