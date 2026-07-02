package com.smartcbwtf.controller;

import com.smartcbwtf.dto.UserProfileResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * User profile controller.
 * 
 * Provides READ-ONLY access to user profile data EXCEPT for password change.
 * Profile data is centrally managed at the backend database level.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public UserController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /**
     * Get current authenticated user's profile.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getPrincipal().toString();

        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .body(UserProfileResponse.fromUser(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Change current user's password.
     * 
     * SECURITY: This is the ONLY mutation endpoint for users on mobile.
     * Used to enforce security.force_password_reset_first_login.
     * After successful change, mustChangePassword flag is cleared.
     */
    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getPrincipal().toString();

        return userRepository.findByUsername(username)
                .map(user -> {
                    // Verify current password
                    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "INVALID_CURRENT_PASSWORD",
                                "message", "Current password is incorrect"));
                    }

                    // Validate new password against policy
                    PasswordPolicyValidator.ValidationResult validation = passwordPolicyValidator
                            .validate(request.newPassword());
                    if (!validation.isValid()) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "PASSWORD_POLICY_VIOLATION",
                                "message", "Password does not meet security requirements",
                                "violations", validation.getViolations(),
                                "policy", passwordPolicyValidator.getPolicyDescription()));
                    }

                    // Don't allow same password
                    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "SAME_PASSWORD",
                                "message", "New password must be different from current password"));
                    }

                    // Update password
                    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    user.setPasswordChangedAt(Instant.now());
                    user.setMustChangePassword(false);
                    user.setForcePasswordChange(false);
                    userRepository.save(user);

                    return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request body for password change.
     */
    public record ChangePasswordRequest(
            @NotBlank @Size(max = 256) String currentPassword,
            @NotBlank @Size(max = 256) String newPassword) {
    }
}
