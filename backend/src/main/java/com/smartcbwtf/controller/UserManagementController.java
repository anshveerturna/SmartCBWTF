package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.admin.*;
import com.smartcbwtf.repository.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

/**
 * Global User Management API for SuperAdmin.
 * Provides full control over all users across all CBWTFs.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserManagementController {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    private final AppUserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final HcfRepository hcfRepository;
    private final SubscriptionAuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(
            AppUserRepository userRepository,
            FacilityRepository facilityRepository,
            HcfRepository hcfRepository,
            SubscriptionAuditRepository auditRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
        this.hcfRepository = hcfRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== LIST ALL USERS ==========

    @GetMapping
    public ResponseEntity<Page<UserManagementDTO>> listUsers(
            @RequestParam(name = "cbwtfId", required = false) UUID cbwtfId,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AppUser> users;

        // Handle search
        if (search != null && !search.isBlank()) {
            if (cbwtfId != null) {
                users = userRepository.searchUsersByFacility(cbwtfId, search, pageable);
            } else {
                users = userRepository.searchUsers(search, pageable);
            }
        }
        // Handle filters
        else if (cbwtfId != null && role != null && active != null) {
            users = userRepository.findByFacilityIdAndRoleAndActive(cbwtfId, role, active, pageable);
        } else if (cbwtfId != null && role != null) {
            users = userRepository.findByFacilityIdAndRole(cbwtfId, role, pageable);
        } else if (cbwtfId != null && active != null) {
            users = userRepository.findByFacilityIdAndActive(cbwtfId, active, pageable);
        } else if (role != null && active != null) {
            users = userRepository.findByRoleAndActive(role, active, pageable);
        } else if (cbwtfId != null) {
            users = userRepository.findByFacilityId(cbwtfId, pageable);
        } else if (role != null) {
            users = userRepository.findByRole(role, pageable);
        } else if (active != null) {
            users = userRepository.findByActive(active, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        Page<UserManagementDTO> result = users.map(UserManagementDTO::from);
        return ResponseEntity.ok(result);
    }

    // ========== GET SINGLE USER ==========

    @GetMapping("/{id}")
    public ResponseEntity<UserManagementDTO> getUser(@PathVariable("id") UUID id) {
        return userRepository.findById(id)
                .map(UserManagementDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== CREATE USER ==========

    @PostMapping
    @Transactional
    public ResponseEntity<UserManagementDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Validate username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest().build();
        }

        // Validate CBWTF exists if specified
        Facility facility = null;
        if (request.cbwtfId() != null) {
            facility = facilityRepository.findById(request.cbwtfId()).orElse(null);
            if (facility == null) {
                return ResponseEntity.badRequest().build();
            }
        }

        // Validate HCF exists if specified
        Hcf hcf = null;
        if (request.hcfId() != null) {
            hcf = hcfRepository.findById(request.hcfId()).orElse(null);
            if (hcf == null) {
                return ResponseEntity.badRequest().build();
            }
        }

        // Generate temporary password
        String tempPassword = generateTempPassword();

        // Create user
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setFacility(facility);
        user.setHcf(hcf);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setActive(true);
        user.setForcePasswordChange(true);

        user = userRepository.save(user);

        // Audit log
        auditRepository.save(SubscriptionAudit.forFacility(
                facility != null ? facility.getId() : null,
                SubscriptionAudit.Action.USER_CREATED,
                null,
                request.username(),
                getCurrentUserId(),
                getCurrentUsername(),
                "SUPER_ADMIN",
                "User created by SuperAdmin"));

        log.info("SuperAdmin created user {} with role {}", request.username(), request.role());
        log.warn("TEMP PASSWORD for {}: {} (remove this log in production)", request.username(), tempPassword);

        return ResponseEntity.ok(UserManagementDTO.from(user));
    }

    // ========== UPDATE USER ==========

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<UserManagementDTO> updateUser(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        return userRepository.findById(id)
                .map(user -> {
                    String oldValues = String.format("name=%s,email=%s,role=%s,active=%s",
                            user.getFullName(), user.getEmail(), user.getRole(), user.isActive());

                    if (request.fullName() != null)
                        user.setFullName(request.fullName());
                    if (request.email() != null)
                        user.setEmail(request.email());
                    if (request.phone() != null)
                        user.setPhone(request.phone());
                    if (request.role() != null)
                        user.setRole(request.role());
                    if (request.active() != null)
                        user.setActive(request.active());

                    if (request.cbwtfId() != null) {
                        facilityRepository.findById(request.cbwtfId())
                                .ifPresent(user::setFacility);
                    }
                    if (request.hcfId() != null) {
                        hcfRepository.findById(request.hcfId())
                                .ifPresent(user::setHcf);
                    }

                    user = userRepository.save(user);

                    String newValues = String.format("name=%s,email=%s,role=%s,active=%s",
                            user.getFullName(), user.getEmail(), user.getRole(), user.isActive());

                    // Audit log
                    auditRepository.save(SubscriptionAudit.forFacility(
                            user.getFacility() != null ? user.getFacility().getId() : null,
                            SubscriptionAudit.Action.USER_UPDATED,
                            oldValues,
                            newValues,
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "User updated by SuperAdmin"));

                    return ResponseEntity.ok(UserManagementDTO.from(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== DISABLE USER ==========

    @PostMapping("/{id}/disable")
    @Transactional
    public ResponseEntity<UserManagementDTO> disableUser(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> body) {

        String reason = body.getOrDefault("reason", "Disabled by admin");

        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    user = userRepository.save(user);

                    auditRepository.save(SubscriptionAudit.forFacility(
                            user.getFacility() != null ? user.getFacility().getId() : null,
                            SubscriptionAudit.Action.USER_DISABLED,
                            "active=true",
                            "active=false",
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            reason));

                    log.info("User {} disabled by SuperAdmin: {}", user.getUsername(), reason);
                    return ResponseEntity.ok(UserManagementDTO.from(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== ENABLE USER ==========

    @PostMapping("/{id}/enable")
    @Transactional
    public ResponseEntity<UserManagementDTO> enableUser(@PathVariable("id") UUID id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(true);
                    user = userRepository.save(user);

                    auditRepository.save(SubscriptionAudit.forFacility(
                            user.getFacility() != null ? user.getFacility().getId() : null,
                            SubscriptionAudit.Action.USER_ENABLED,
                            "active=false",
                            "active=true",
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "User enabled by SuperAdmin"));

                    log.info("User {} enabled by SuperAdmin", user.getUsername());
                    return ResponseEntity.ok(UserManagementDTO.from(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== FORCE PASSWORD RESET ==========

    @PostMapping("/{id}/force-password-reset")
    @Transactional
    public ResponseEntity<UserManagementDTO> forcePasswordReset(@PathVariable("id") UUID id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setForcePasswordChange(true);
                    user = userRepository.save(user);

                    auditRepository.save(SubscriptionAudit.forFacility(
                            user.getFacility() != null ? user.getFacility().getId() : null,
                            SubscriptionAudit.Action.PASSWORD_RESET_FORCED,
                            null,
                            null,
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "Password reset forced by SuperAdmin"));

                    log.info("Password reset forced for user {} by SuperAdmin", user.getUsername());
                    return ResponseEntity.ok(UserManagementDTO.from(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== REVOKE ACCESS ==========

    @DeleteMapping("/{id}/revoke")
    @Transactional
    public ResponseEntity<Void> revokeAccess(@PathVariable("id") UUID id) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(false);
                    user.setForcePasswordChange(true);
                    userRepository.save(user);

                    auditRepository.save(SubscriptionAudit.forFacility(
                            user.getFacility() != null ? user.getFacility().getId() : null,
                            SubscriptionAudit.Action.ACCESS_REVOKED,
                            null,
                            null,
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "All access revoked immediately by SuperAdmin"));

                    log.warn("All access revoked for user {} by SuperAdmin", user.getUsername());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== HELPER METHODS ==========

    private UUID getCurrentUserId() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null ? info.userId() : null;
    }

    private String getCurrentUsername() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null ? info.username() : "SYSTEM";
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        StringBuilder sb = new StringBuilder(12);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
