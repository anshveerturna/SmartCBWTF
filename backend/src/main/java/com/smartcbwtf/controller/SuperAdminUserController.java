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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9._@-]+$";
    private static final String OPTIONAL_PHONE_PATTERN = "^$|^[0-9+()\\-\\s]{7,20}$";
    private static final String PASSWORD_RANDOM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int MAX_QUERY_FILTER_LENGTH = 80;
    private static final int MAX_SEARCH_LENGTH = 120;

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

        Pageable pageable = pageRequest(page, size, 20, Sort.by("createdAt").descending());
        Page<AppUser> users;
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeQueryFilter(status, "status");

        if (normalizedSearch != null) {
            users = userRepository.searchByRoleAndUsernameOrEmail("SUPER_ADMIN", normalizedSearch, pageable);
        } else if ("ACTIVE".equalsIgnoreCase(normalizedStatus)) {
            users = userRepository.findByRoleAndActive("SUPER_ADMIN", true, pageable);
        } else if ("DISABLED".equalsIgnoreCase(normalizedStatus)) {
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
    public ResponseEntity<?> createSuperAdmin(@Valid @RequestBody CreateSuperAdminRequest request) {
        String username = cleanLineRequired(request.username(), "Username");
        String fullName = cleanLineRequired(request.fullName(), "Full name");
        String email = optionalCleanLine(request.email());
        String phone = optionalCleanLine(request.phone());

        // Validate username uniqueness
        if (userRepository.findByUsername(username).isPresent()) {
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
        newUser.setUsername(username);
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);
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
            @Valid @RequestBody UpdateSuperAdminRequest request) {

        return userRepository.findById(id)
                .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                .map(user -> {
                    String oldValues = String.format("name=%s,email=%s,phone=%s",
                            user.getFullName(), user.getEmail(), user.getPhone());

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
                    String ext;
                    try {
                        ext = UploadFileValidator.publicImageExtension(file);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                    }

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
                        deleteOldProfilePhoto(oldPhoto, newPhoto);
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
    public ResponseEntity<?> forcePasswordReset(@PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) PasswordResetRequest request) {

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

                    return privateCredentialResponse(Map.of(
                            "message", "Password reset successfully",
                            "temporaryPassword", newPassword,
                            "mustChangeOnLogin", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<Map<String, Object>> privateCredentialResponse(Map<String, Object> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    private String generateTemporaryPassword() {
        int[] preferredLengths = { 12, 11, 10, 9, 8, 13, 14, 15, 16, 20, 24, 32 };
        for (int length : preferredLengths) {
            String candidate = generateTemporaryPassword(length);
            if (passwordPolicyValidator.validate(candidate).isValid()) {
                return candidate;
            }
        }
        return generateTemporaryPassword(12);
    }

    private String generateTemporaryPassword(int length) {
        StringBuilder password = new StringBuilder(length);
        password.append("Tmp@").append(SECURE_RANDOM.nextInt(10));
        while (password.length() < length) {
            password.append(PASSWORD_RANDOM_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_RANDOM_CHARS.length())));
        }
        return password.toString();
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

    private static String normalizeSearch(String search) {
        return normalizeQueryText(search, MAX_SEARCH_LENGTH, "search");
    }

    private static String normalizeQueryFilter(String value, String label) {
        return normalizeQueryText(value, MAX_QUERY_FILTER_LENGTH, label);
    }

    private static String normalizeQueryText(String value, int maxLength, String label) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " must be " + maxLength + " characters or fewer");
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) {
                throw new IllegalArgumentException(label + " contains unsupported control characters");
            }
        }
        return normalized;
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
            @NotBlank(message = "Username is required")
            @Size(max = 100, message = "Username must be 100 characters or less")
            @Pattern(regexp = USERNAME_PATTERN, message = "Username contains invalid characters")
            String username,
            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name must be 120 characters or less")
            String fullName,
            @Email(message = "Invalid email format")
            @Size(max = 180, message = "Email must be 180 characters or less")
            String email,
            @Size(max = 20, message = "Phone must be 20 characters or less")
            @Pattern(regexp = OPTIONAL_PHONE_PATTERN, message = "Invalid phone number")
            String phone,
            @NotBlank(message = "Password is required")
            @Size(max = 256, message = "Password must be 256 characters or less")
            String password) {
    }

    public record UpdateSuperAdminRequest(
            @Size(max = 120, message = "Full name must be 120 characters or less")
            String fullName,
            @Email(message = "Invalid email format")
            @Size(max = 180, message = "Email must be 180 characters or less")
            String email,
            @Size(max = 20, message = "Phone must be 20 characters or less")
            @Pattern(regexp = OPTIONAL_PHONE_PATTERN, message = "Invalid phone number")
            String phone) {
    }

    public record PasswordResetRequest(
            @Size(max = 256, message = "Password must be 256 characters or less")
            String newPassword) {
    }
}
