package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.security.FileValidator;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * CBWTF Admin self-profile management.
 * 
 * Endpoints:
 * - GET /me - Get current CBWTF Admin profile
 * - PUT /me - Update profile details
 * - POST /me/photo - Upload profile photo
 * - POST /me/password - Change password
 */
@RestController
@RequestMapping("/api/cbwtf/profile")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class CbwtfProfileController {

    private static final Logger log = LoggerFactory.getLogger(CbwtfProfileController.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditLogService auditLogService;

    @Value("${app.upload.profile-photos:uploads/profiles}")
    private String uploadDir;

    public CbwtfProfileController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.auditLogService = auditLogService;
    }

    /**
     * Get current CBWTF Admin's profile.
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<CbwtfProfileDTO> getMyProfile() {
        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(CbwtfProfileDTO.from(user));
    }

    /**
     * Update current CBWTF Admin's profile.
     */
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMyProfile(@RequestBody ProfileUpdateRequest request) {
        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldValues = String.format("name=%s,email=%s,phone=%s",
                user.getFullName(), user.getEmail(), user.getPhone());

        // Update allowed fields
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String newValues = String.format("name=%s,email=%s,phone=%s",
                user.getFullName(), user.getEmail(), user.getPhone());

        // Audit log
        auditLogService.log("USER", user.getId(), "PROFILE_UPDATED", user.getId(),
                String.format("{\"old\":\"%s\",\"new\":\"%s\"}", oldValues, newValues));

        log.info("CBWTF Admin profile updated: user={}", user.getUsername());
        return ResponseEntity.ok(CbwtfProfileDTO.from(user));
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

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        String ext;
        try {
            // Validate file type
            String contentType = FileValidator.validateImage(file);

            // Determine extension
            ext = switch (contentType) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                default -> "jpg";
            };
        } catch (Exception e) {
             return ResponseEntity.badRequest().body(Map.of("error", "Invalid image file"));
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
            user.setProfilePhotoUrl(newPhoto);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);

            // Audit log
            auditLogService.log("USER", user.getId(), "PHOTO_UPDATED", user.getId(),
                    String.format("{\"old\":\"%s\",\"new\":\"%s\"}", oldPhoto, newPhoto));

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
            Path filePath = Paths.get(uploadDir, oldPhoto.replace("/uploads/profiles/", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete photo file: {}", e.getMessage());
        }

        // Update user
        user.setProfilePhotoUrl(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Audit log
        auditLogService.log("USER", user.getId(), "PHOTO_REMOVED", user.getId(),
                String.format("{\"removed\":\"%s\"}", oldPhoto));

        log.info("Photo removed: user={}", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Photo removed successfully"));
    }

    /**
     * Change password.
     */
    @PostMapping("/me/password")
    @Transactional
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request) {
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
        auditLogService.log("USER", user.getId(), "PASSWORD_CHANGED", user.getId(),
                "{\"action\":\"self_password_change\"}");

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
    public record CbwtfProfileDTO(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone,
            String profilePhotoUrl,
            String role,
            String facilityName,
            boolean active,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt) {

        public static CbwtfProfileDTO from(AppUser user) {
            return new CbwtfProfileDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getProfilePhotoUrl(),
                    user.getRole(),
                    user.getFacility() != null ? user.getFacility().getName() : null,
                    user.isActive(),
                    user.getLastLoginAt(),
                    user.getCreatedAt(),
                    user.getUpdatedAt());
        }
    }

    public record ProfileUpdateRequest(String fullName, String email, String phone) {
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {
    }
}
