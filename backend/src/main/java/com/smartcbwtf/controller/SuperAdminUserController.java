package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.SubscriptionAudit;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * SuperAdmin user management.
 * Manage ALL SuperAdmin users in the system.
 * 
 * Security rules:
 * - Cannot disable the LAST active SuperAdmin
 * - Cannot disable yourself
 */
@RestController
@RequestMapping("/api/superadmin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminUserController {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminUserController.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final SubscriptionAuditRepository auditRepository;

    @Value("${app.upload.profile-photos:uploads/profiles}")
    private String uploadDir;

    public SuperAdminUserController(
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
     * List all SuperAdmin users.
     */
    @GetMapping
    public ResponseEntity<Page<SuperAdminUserDTO>> listSuperAdmins(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AppUser> users;

        if (search != null && !search.isBlank()) {
            users = userRepository.searchByRoleAndUsernameOrEmail("SUPER_ADMIN", search, pageable);
        } else if ("ACTIVE".equalsIgnoreCase(status)) {
            users = userRepository.findByRoleAndActive("SUPER_ADMIN", true, pageable);
        } else if ("DISABLED".equalsIgnoreCase(status)) {
            users = userRepository.findByRoleAndActive("SUPER_ADMIN", false, pageable);
        } else {
            users = userRepository.findByRole("SUPER_ADMIN", pageable);
        }

        return ResponseEntity.ok(users.map(SuperAdminUserDTO::from));
    }

    /**
     * Get single SuperAdmin by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SuperAdminUserDTO> getSuperAdmin(@PathVariable UUID id) {
        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(SuperAdminUserDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new SuperAdmin.
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> createSuperAdmin(@RequestBody CreateSuperAdminRequest request) {
        // Validate username uniqueness
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "USERNAME_EXISTS",
                    "message", "Username already exists"));
        }

        // Validate password
        var validation = passwordPolicyValidator.validate(request.password());
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "PASSWORD_POLICY_VIOLATION",
                    "violations", validation.getViolations()));
        }

        AppUser newUser = new AppUser();
        newUser.setUsername(request.username());
        newUser.setFullName(request.fullName());
        newUser.setEmail(request.email());
        newUser.setPhone(request.phone());
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setRole("SUPER_ADMIN");
        newUser.setActive(true);
        newUser.setMustChangePassword(true); // Force password change on first login
        newUser.setForcePasswordChange(true);
        newUser.setCreatedAt(Instant.now());
        newUser.setUpdatedAt(Instant.now());

        newUser = userRepository.save(newUser);

        // Audit log
        AppUser currentUser = getCurrentUser();
        auditRepository.save(SubscriptionAudit.create(
                "USER", newUser.getId(),
                SubscriptionAudit.Action.SUPERADMIN_CREATED,
                null, newUser.getUsername(),
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getUsername() : "SYSTEM",
                "SUPER_ADMIN",
                "New SuperAdmin created"));

        log.info("SuperAdmin created: {} by {}", newUser.getUsername(),
                currentUser != null ? currentUser.getUsername() : "SYSTEM");

        return ResponseEntity.ok(SuperAdminUserDTO.from(newUser));
    }

    /**
     * Update SuperAdmin.
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateSuperAdmin(@PathVariable UUID id,
            @RequestBody UpdateSuperAdminRequest request) {

        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(user -> {
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

                    // Audit
                    AppUser currentUser = getCurrentUser();
                    auditRepository.save(SubscriptionAudit.create(
                            "USER", user.getId(),
                            SubscriptionAudit.Action.SUPERADMIN_UPDATED,
                            oldValues, newValues,
                            currentUser != null ? currentUser.getId() : null,
                            currentUser != null ? currentUser.getUsername() : "SYSTEM",
                            "SUPER_ADMIN",
                            "SuperAdmin updated"));

                    log.info("SuperAdmin updated: {}", user.getUsername());
                    return ResponseEntity.ok(SuperAdminUserDTO.from(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Upload photo for a SuperAdmin.
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadUserPhoto(@PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {

        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(user -> {
                    if (file.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
                    }

                    String contentType = file.getContentType();
                    if (contentType == null || !contentType.startsWith("image/")) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Only images allowed"));
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

                        // Audit
                        AppUser currentUser = getCurrentUser();
                        auditRepository.save(SubscriptionAudit.create(
                                "USER", user.getId(),
                                SubscriptionAudit.Action.PHOTO_UPDATED,
                                oldPhoto, newPhoto,
                                currentUser != null ? currentUser.getId() : null,
                                currentUser != null ? currentUser.getUsername() : "SYSTEM",
                                "SUPER_ADMIN",
                                "Photo updated by admin"));

                        return ResponseEntity.ok(Map.of("photoUrl", newPhoto));
                    } catch (IOException e) {
                        log.error("Failed to upload photo", e);
                        return ResponseEntity.internalServerError()
                                .body(Map.of("error", "Failed to save file"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Disable SuperAdmin.
     */
    @PutMapping("/{id}/disable")
    @Transactional
    public ResponseEntity<?> disableSuperAdmin(@PathVariable UUID id) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        // Cannot disable yourself
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "CANNOT_DISABLE_SELF",
                    "message", "You cannot disable your own account"));
        }

        // Check if this is the last active SuperAdmin
        long activeCount = userRepository.countByRoleAndActive("SUPER_ADMIN", true);
        if (activeCount <= 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "LAST_SUPERADMIN",
                    "message", "Cannot disable the last active SuperAdmin"));
        }

        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(user -> {
                    if (!user.isActive()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "ALREADY_DISABLED",
                                "message", "User is already disabled"));
                    }

                    user.setActive(false);
                    user.setUpdatedAt(Instant.now());
                    userRepository.save(user);

                    // Audit
                    auditRepository.save(SubscriptionAudit.create(
                            "USER", user.getId(),
                            SubscriptionAudit.Action.SUPERADMIN_DISABLED,
                            "active=true", "active=false",
                            currentUser.getId(), currentUser.getUsername(), "SUPER_ADMIN",
                            "SuperAdmin disabled"));

                    log.info("SuperAdmin disabled: {} by {}", user.getUsername(), currentUser.getUsername());
                    return ResponseEntity.ok(Map.of("message", "User disabled successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Enable SuperAdmin.
     */
    @PutMapping("/{id}/enable")
    @Transactional
    public ResponseEntity<?> enableSuperAdmin(@PathVariable UUID id) {
        AppUser currentUser = getCurrentUser();

        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(user -> {
                    if (user.isActive()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "ALREADY_ENABLED",
                                "message", "User is already enabled"));
                    }

                    user.setActive(true);
                    user.setUpdatedAt(Instant.now());
                    userRepository.save(user);

                    // Audit
                    auditRepository.save(SubscriptionAudit.create(
                            "USER", user.getId(),
                            SubscriptionAudit.Action.SUPERADMIN_ENABLED,
                            "active=false", "active=true",
                            currentUser != null ? currentUser.getId() : null,
                            currentUser != null ? currentUser.getUsername() : "SYSTEM",
                            "SUPER_ADMIN",
                            "SuperAdmin enabled"));

                    log.info("SuperAdmin enabled: {}", user.getUsername());
                    return ResponseEntity.ok(Map.of("message", "User enabled successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Force password reset for SuperAdmin.
     */
    @PostMapping("/{id}/reset-password")
    @Transactional
    public ResponseEntity<?> forcePasswordReset(@PathVariable UUID id,
            @RequestBody(required = false) PasswordResetRequest request) {

        AppUser currentUser = getCurrentUser();

        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(user -> {
                    String newPassword = request != null && request.newPassword() != null
                            ? request.newPassword()
                            : generateTemporaryPassword();

                    // Validate password
                    var validation = passwordPolicyValidator.validate(newPassword);
                    if (!validation.isValid()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "PASSWORD_POLICY_VIOLATION",
                                "violations", validation.getViolations()));
                    }

                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                    user.setMustChangePassword(true);
                    user.setForcePasswordChange(true);
                    user.setPasswordChangedAt(Instant.now());
                    user.setUpdatedAt(Instant.now());
                    userRepository.save(user);

                    // Audit
                    auditRepository.save(SubscriptionAudit.create(
                            "USER", user.getId(),
                            SubscriptionAudit.Action.PASSWORD_RESET_FORCED,
                            null, "password_reset",
                            currentUser != null ? currentUser.getId() : null,
                            currentUser != null ? currentUser.getUsername() : "SYSTEM",
                            "SUPER_ADMIN",
                            "Password reset by admin"));

                    log.info("Password reset for: {} by {}", user.getUsername(),
                            currentUser != null ? currentUser.getUsername() : "SYSTEM");

                    return ResponseEntity.ok(Map.of(
                            "message", "Password reset successfully",
                            "temporaryPassword", newPassword,
                            "mustChangeOnLogin", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String generateTemporaryPassword() {
        // Generate a secure temporary password
        return "Temp" + UUID.randomUUID().toString().substring(0, 8) + "!";
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
    public record SuperAdminUserDTO(
            UUID id,
            String username,
            String fullName,
            String email,
            String phone,
            String profilePhotoUrl,
            boolean active,
            Instant lastLoginAt,
            Instant createdAt) {

        public static SuperAdminUserDTO from(AppUser user) {
            return new SuperAdminUserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getProfilePhotoUrl(),
                    user.isActive(),
                    user.getLastLoginAt(),
                    user.getCreatedAt());
        }
    }

    public record CreateSuperAdminRequest(
            String username,
            String fullName,
            String email,
            String phone,
            String password) {
    }

    public record UpdateSuperAdminRequest(String fullName, String email, String phone) {
    }

    public record PasswordResetRequest(String newPassword) {
    }
}
