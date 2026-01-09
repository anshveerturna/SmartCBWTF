package com.smartcbwtf.controller;

import com.smartcbwtf.config.JwtService;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.dto.AuthLoginRequest;
import com.smartcbwtf.dto.AuthLoginResponse;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;
    private final HcfAccessGuard hcfAccessGuard;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            AppUserRepository appUserRepository, AuditLogService auditLogService,
            SystemConfigService systemConfigService, HcfAccessGuard hcfAccessGuard) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.systemConfigService = systemConfigService;
        this.hcfAccessGuard = hcfAccessGuard;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequest request) {
        // Get security config values
        int maxLoginAttempts = systemConfigService.getInt("security.max_login_attempts", 5);
        boolean forcePasswordResetFirstLogin = systemConfigService
                .getBoolean("security.force_password_reset_first_login", false);

        // Check if user exists first (for lockout tracking)
        Optional<AppUser> userOpt = appUserRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();

            // Check if account is locked
            if (user.isLocked()) {
                long minutesRemaining = Duration.between(Instant.now(), user.getLockedUntil()).toMinutes();
                log.warn("Login attempt on locked account: {}", user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "ACCOUNT_LOCKED",
                        "message", "Account is locked. Try again in " + minutesRemaining + " minutes.",
                        "lockedUntil", user.getLockedUntil().toString()));
            }

            // Check if account is active
            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "ACCOUNT_DISABLED",
                        "message", "Account is disabled. Contact administrator."));
            }
        }

        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            AppUser user = appUserRepository.findByUsername(authentication.getName()).orElseThrow();

            // HCF_ADMIN: Validate bed count and approval status BEFORE completing login
            if ("HCF_ADMIN".equals(user.getRole())) {
                HcfAccessGuard.AccessCheckResult accessResult = hcfAccessGuard.checkPortalAccess(
                        user.getHcf() != null ? user.getHcf().getId() : null);
                if (!accessResult.isAllowed()) {
                    log.warn("HCF Admin login denied: user={}, errorCode={}",
                            user.getUsername(), accessResult.getErrorCode());
                    auditLogService.log("APP_USER", user.getId(), "LOGIN_DENIED_HCF_ACCESS",
                            user.getId(), accessResult.getErrorCode());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", accessResult.getErrorCode(),
                            "message", accessResult.getMessage()));
                }
            }

            // Successful login - reset failed attempts and record login
            user.recordSuccessfulLogin();
            appUserRepository.save(user);

            // Check if password change is required
            boolean mustChangePassword = user.isMustChangePassword() || user.isForcePasswordChange();

            // Check first login password reset requirement
            if (forcePasswordResetFirstLogin && user.getPasswordChangedAt() == null) {
                mustChangePassword = true;
            }

            // Build JWT claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("user_id", user.getId().toString());
            claims.put("role", user.getRole());
            claims.put("full_name", user.getFullName());
            claims.put("profile_photo_url", user.getProfilePhotoUrl());
            claims.put("tenant_id", user.getFacility() != null ? user.getFacility().getId().toString() : null);
            claims.put("hcf_id", user.getHcf() != null ? user.getHcf().getId().toString() : null);
            claims.put("must_change_password", mustChangePassword);

            String token = jwtService.generateToken(user.getUsername(), claims);

            // Audit log
            auditLogService.log("APP_USER", user.getId(), "LOGIN", user.getId(), null);

            log.info("Successful login: {} (role: {})", user.getUsername(), user.getRole());

            return ResponseEntity.ok(new AuthLoginResponse(
                    token,
                    user.getRole(),
                    mustChangePassword,
                    user.getFullName(),
                    user.getFacility() != null ? user.getFacility().getId().toString() : null,
                    user.getHcf() != null ? user.getHcf().getId().toString() : null));

        } catch (BadCredentialsException e) {
            // Failed login - increment counter and potentially lock account
            if (userOpt.isPresent()) {
                AppUser user = userOpt.get();
                user.incrementFailedAttempts();

                if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                    user.lockAccount(LOCKOUT_DURATION_MINUTES);
                    appUserRepository.save(user);

                    // Audit log account lockout
                    auditLogService.log("APP_USER", user.getId(), "ACCOUNT_LOCKED", user.getId(),
                            "Locked due to " + maxLoginAttempts + " failed login attempts");

                    log.warn("Account locked after {} failed attempts: {}", maxLoginAttempts, user.getUsername());

                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", "ACCOUNT_LOCKED",
                            "message", "Account locked due to too many failed login attempts. Try again in "
                                    + LOCKOUT_DURATION_MINUTES + " minutes."));
                }

                appUserRepository.save(user);
                int attemptsRemaining = maxLoginAttempts - user.getFailedLoginAttempts();

                log.warn("Failed login for user {} ({} attempts remaining)", user.getUsername(), attemptsRemaining);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "INVALID_CREDENTIALS",
                        "message",
                        "Invalid username or password. " + attemptsRemaining + " attempts remaining before lockout."));
            }

            // User doesn't exist - return generic error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "INVALID_CREDENTIALS",
                    "message", "Invalid username or password."));
        }
    }

    /**
     * Unlock a user account (SuperAdmin only).
     */
    @PostMapping("/unlock/{username}")
    public ResponseEntity<?> unlockAccount(@PathVariable("username") String username) {
        Optional<AppUser> userOpt = appUserRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AppUser user = userOpt.get();
        user.unlockAccount();
        appUserRepository.save(user);

        auditLogService.log("APP_USER", user.getId(), "ACCOUNT_UNLOCKED", null, "Unlocked by administrator");
        log.info("Account unlocked: {}", username);

        return ResponseEntity.ok(Map.of("message", "Account unlocked successfully"));
    }
}
