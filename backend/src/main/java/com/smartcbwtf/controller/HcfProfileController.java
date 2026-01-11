package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.AuditLog;
import com.smartcbwtf.domain.Hcf;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AuditLogRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HCF Admin self-profile management.
 */
@RestController
@RequestMapping("/api/hcf/profile")
@PreAuthorize("hasRole('HCF_ADMIN')")
public class HcfProfileController {

    private static final Logger log = LoggerFactory.getLogger(HcfProfileController.class);

    private final AppUserRepository userRepository;
    private final HcfRepository hcfRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditLogService auditLogService;
    private final HcfAccessGuard accessGuard;

    @Value("${app.upload.profile-photos:uploads/profiles}")
    private String uploadDir;

    public HcfProfileController(
            AppUserRepository userRepository,
            HcfRepository hcfRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            AuditLogService auditLogService,
            HcfAccessGuard accessGuard) {
        this.userRepository = userRepository;
        this.hcfRepository = hcfRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.auditLogService = auditLogService;
        this.accessGuard = accessGuard;
    }

    /**
     * Get current HCF Admin's profile.
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<HcfProfileDTO> getMyProfile() {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Hcf hcf = hcfRepository.findById(hcfId).orElse(null);
        return ResponseEntity.ok(HcfProfileDTO.from(user, hcf));
    }

    /**
     * Update current HCF Admin's profile.
     */
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMyProfile(@RequestBody ProfileUpdateRequest request) {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldValues = String.format("name=%s,email=%s,phone=%s",
                user.getFullName(), user.getEmail(), user.getPhone());

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

        auditLogService.log("USER", user.getId(), "PROFILE_UPDATED", user.getId(),
                String.format("{\"old\":\"%s\",\"new\":\"%s\"}", oldValues, newValues));

        log.info("HCF Admin profile updated: user={}", user.getUsername());
        Hcf hcf = hcfRepository.findById(hcfId).orElse(null);
        return ResponseEntity.ok(HcfProfileDTO.from(user, hcf));
    }

    /**
     * Upload profile photo.
     */
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file) {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files allowed"));
        }

        String ext = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = user.getId().toString() + "." + ext;
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String oldPhoto = user.getProfilePhotoUrl();
            String newPhoto = "/uploads/profiles/" + filename;
            user.setProfilePhotoUrl(newPhoto);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);

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
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String oldPhoto = user.getProfilePhotoUrl();
        if (oldPhoto == null) {
            return ResponseEntity.ok(Map.of("message", "No photo to remove"));
        }

        try {
            Path filePath = Paths.get(uploadDir, oldPhoto.replace("/uploads/profiles/", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete photo file: {}", e.getMessage());
        }

        user.setProfilePhotoUrl(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

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
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_CURRENT_PASSWORD",
                    "message", "Current password is incorrect"));
        }

        var validation = passwordPolicyValidator.validate(request.newPassword());
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "PASSWORD_POLICY_VIOLATION",
                    "message", "Password does not meet requirements",
                    "violations", validation.getViolations()));
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "SAME_PASSWORD",
                    "message", "New password must be different"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setMustChangePassword(false);
        user.setForcePasswordChange(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditLogService.log("USER", user.getId(), "PASSWORD_CHANGED", user.getId(),
                "{\"action\":\"self_password_change\"}");

        log.info("Password changed: user={}", user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Get activity logs for current user.
     */
    @GetMapping("/me/logs")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getActivityLogs(@RequestParam(defaultValue = "20") int limit) {
        UUID hcfId = TenantContext.getHcfId();
        accessGuard.assertPortalAccess(hcfId);

        AppUser user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<AuditLog> logs = auditLogRepository.findByActorUserIdOrderByTsDesc(
                user.getId(), PageRequest.of(0, Math.min(limit, 50)));

        List<ActivityLogDTO> dtos = logs.stream()
                .map(ActivityLogDTO::from)
                .toList();

        return ResponseEntity.ok(Map.of("logs", dtos, "total", dtos.size()));
    }

    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        String username = auth.getPrincipal().toString();
        return userRepository.findByUsername(username).orElse(null);
    }

    // DTOs
    public record HcfProfileDTO(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone,
            String profilePhotoUrl,
            String role,
            String hcfName,
            String hcfCode,
            boolean active,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt) {

        public static HcfProfileDTO from(AppUser user, Hcf hcf) {
            return new HcfProfileDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getProfilePhotoUrl(),
                    user.getRole(),
                    hcf != null ? hcf.getName() : null,
                    hcf != null ? hcf.getCode() : null,
                    user.isActive(),
                    user.getLastLoginAt(),
                    user.getCreatedAt(),
                    user.getUpdatedAt());
        }
    }

    public record ActivityLogDTO(
            UUID id,
            String action,
            String entityType,
            UUID entityId,
            String details,
            Instant timestamp) {

        public static ActivityLogDTO from(AuditLog log) {
            return new ActivityLogDTO(
                    log.getId(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getDataJson(),
                    log.getTs());
        }
    }

    public record ProfileUpdateRequest(String fullName, String email, String phone) {
    }

    public record PasswordChangeRequest(String currentPassword, String newPassword) {
    }
}
