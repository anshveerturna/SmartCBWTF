package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.SubscriptionAudit;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import com.smartcbwtf.service.UploadFileValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * SuperAdmin self-profile management.
 * 
 * Endpoints:
 * - GET /me - Get current SuperAdmin profile
 * - PUT /me - Update profile details
 * - POST /me/photo - Upload profile photo
 * - POST /me/password - Change password
 */
@RestController
@RequestMapping("/api/superadmin/profile")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminProfileController {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminProfileController.class);
    private static final String OPTIONAL_PHONE_PATTERN = "^\\s*$|^[0-9+()\\-\\s]{7,20}$";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final SubscriptionAuditRepository auditRepository;

    @Value("${app.upload.profile-photos:uploads/profiles}")
    private String uploadDir;

    public SuperAdminProfileController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            SubscriptionAuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.auditRepository = auditRepository;
    }

    /**
     * Get current SuperAdmin's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<SuperAdminProfileDTO> getMyProfile() {
        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return privateProfileResponse(SuperAdminProfileDTO.from(user));
    }

    /**
     * Update current SuperAdmin's profile.
     */
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMyProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Profile update request is required");
        }

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldValues = String.format("name=%s,email=%s,phone=%s",
                user.getFullName(), user.getEmail(), user.getPhone());

        // Update allowed fields
        if (request.fullName() != null) {
            user.setFullName(cleanLineRequired(request.fullName(), "Full name"));
        }
        if (request.email() != null) {
            user.setEmail(optionalCleanLine(request.email()));
        }
        if (request.phone() != null) {
            user.setPhone(optionalCleanLine(request.phone()));
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String newValues = String.format("name=%s,email=%s,phone=%s",
                user.getFullName(), user.getEmail(), user.getPhone());

        // Audit log
        auditRepository.save(SubscriptionAudit.create(
                "USER", user.getId(),
                SubscriptionAudit.Action.PROFILE_UPDATED,
                oldValues, newValues,
                user.getId(), user.getUsername(), user.getRole(),
                "Self profile update"));

        log.info("Profile updated: user={}", user.getUsername());
        return privateProfileResponse(SuperAdminProfileDTO.from(user));
    }

    /**
     * Upload profile photo.
     */
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file) {
        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String ext;
        try {
            ext = UploadFileValidator.publicImageExtension(file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file
            String filename = user.getId().toString() + "." + ext;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user profile
            String oldPhoto = user.getProfilePhotoUrl();
            String newPhoto = "/uploads/profiles/" + filename;
            deleteOldProfilePhoto(oldPhoto, newPhoto);
            user.setProfilePhotoUrl(newPhoto);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);

            // Audit log
            auditRepository.save(SubscriptionAudit.create(
                    "USER", user.getId(),
                    SubscriptionAudit.Action.PHOTO_UPDATED,
                    oldPhoto, newPhoto,
                    user.getId(), user.getUsername(), user.getRole(),
                    "Profile photo updated"));

            log.info("Photo uploaded: user={}, file={}", user.getUsername(), filename);
            return ResponseEntity.ok(Map.of(
                    "message", "Photo uploaded successfully",
                    "photoUrl", newPhoto));

        } catch (IOException e) {
            log.error("Failed to upload photo", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save file"));
        }
    }

    /**
     * Remove profile photo.
     */
    @DeleteMapping("/me/photo")
    @Transactional
    public ResponseEntity<?> removePhoto() {
        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldPhoto = user.getProfilePhotoUrl();
        if (oldPhoto == null) {
            return ResponseEntity.ok(Map.of("message", "No photo to remove"));
        }

        // Delete the file
        try {
            UploadFileValidator.deleteProfilePhotoIfPresent(uploadDir, oldPhoto);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to delete photo file: {}", e.getMessage());
        }

        // Update user
        user.setProfilePhotoUrl(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Audit log
        auditRepository.save(SubscriptionAudit.create(
                "USER", user.getId(),
                SubscriptionAudit.Action.PHOTO_UPDATED,
                oldPhoto, null,
                user.getId(), user.getUsername(), user.getRole(),
                "Profile photo removed"));

        log.info("Photo removed: user={}", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Photo removed successfully"));
    }

    private void deleteOldProfilePhoto(String oldPhoto, String newPhoto) {
        if (oldPhoto == null || oldPhoto.equals(newPhoto)) {
            return;
        }
        try {
            UploadFileValidator.deleteProfilePhotoIfPresent(uploadDir, oldPhoto);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to delete old photo file: {}", e.getMessage());
        }
    }

    private static String cleanLineRequired(String value, String fieldName) {
        String cleaned = cleanLine(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    private static String optionalCleanLine(String value) {
        String cleaned = cleanLine(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String cleanLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private static ResponseEntity<SuperAdminProfileDTO> privateProfileResponse(SuperAdminProfileDTO body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    /**
     * Change password.
     */
    @PostMapping("/me/password")
    @Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_CURRENT_PASSWORD",
                    "message", "Current password is incorrect"));
        }

        // Validate new password
        var validation = passwordPolicyValidator.validate(request.newPassword());
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "PASSWORD_POLICY_VIOLATION",
                    "message", "Password does not meet requirements",
                    "violations", validation.getViolations()));
        }

        // Prevent same password
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "SAME_PASSWORD",
                    "message", "New password must be different"));
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        user.setForcePasswordChange(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Audit log
        auditRepository.save(SubscriptionAudit.create(
                "USER", user.getId(),
                SubscriptionAudit.Action.PASSWORD_CHANGED,
                null, null,
                user.getId(), user.getUsername(), user.getRole(),
                "Self password change"));

        log.info("Password changed: user={}", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // Helper to get current authenticated user
    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        String username = auth.getPrincipal().toString();
        return userRepository.findByUsername(username).orElse(null);
    }

    // DTOs
    public record SuperAdminProfileDTO(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone,
            String profilePhotoUrl,
            String role,
            boolean active,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt) {

        public static SuperAdminProfileDTO from(AppUser user) {
            return new SuperAdminProfileDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getProfilePhotoUrl(),
                    user.getRole(),
                    user.isActive(),
                    user.getLastLoginAt(),
                    user.getCreatedAt(),
                    user.getUpdatedAt());
        }
    }

    public record ProfileUpdateRequest(
            @Size(max = 120, message = "Full name must be 120 characters or less")
            String fullName,
            @Email(message = "Invalid email format")
            @Size(max = 180, message = "Email must be 180 characters or less")
            String email,
            @Size(max = 20, message = "Phone must be 20 characters or less")
            @Pattern(regexp = OPTIONAL_PHONE_PATTERN, message = "Invalid phone number")
            String phone) {
    }

    public record PasswordChangeRequest(
            @NotBlank @Size(max = 256) String currentPassword,
            @NotBlank @Size(max = 256) String newPassword) {
    }
}
