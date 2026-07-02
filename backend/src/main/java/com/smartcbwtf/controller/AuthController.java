package com.smartcbwtf.controller;

import com.smartcbwtf.config.JwtService;
import com.smartcbwtf.domain.Agreement;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.dto.AuthLoginRequest;
import com.smartcbwtf.dto.AuthLoginResponse;
import com.smartcbwtf.repository.AgreementRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.SystemConfigService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;
    private final SystemConfigService systemConfigService;
    private final HcfAccessGuard hcfAccessGuard;
    private final AgreementRepository agreementRepository;
    private final com.smartcbwtf.service.EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            AppUserRepository appUserRepository, AuditLogService auditLogService,
            SystemConfigService systemConfigService, HcfAccessGuard hcfAccessGuard,
            AgreementRepository agreementRepository, com.smartcbwtf.service.EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.auditLogService = auditLogService;
        this.systemConfigService = systemConfigService;
        this.hcfAccessGuard = hcfAccessGuard;
        this.agreementRepository = agreementRepository;
        this.emailService = emailService;
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

            UUID tenantId = resolveTenantId(user);
            UUID hcfId = user.getHcf() != null ? user.getHcf().getId() : null;

            // Build JWT claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("user_id", user.getId().toString());
            claims.put("role", user.getRole());
            claims.put("full_name", user.getFullName());
            claims.put("profile_photo_url", user.getProfilePhotoUrl());
            claims.put("tenant_id", tenantId != null ? tenantId.toString() : null);
            claims.put("hcf_id", hcfId != null ? hcfId.toString() : null);
            claims.put("must_change_password", mustChangePassword);

            String token = jwtService.generateToken(user.getUsername(), claims);

            // Audit log
            auditLogService.log("APP_USER", user.getId(), "LOGIN", user.getId(), null);

            log.info("Successful login: {} (role: {})", user.getUsername(), user.getRole());

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .body(new AuthLoginResponse(
                            token,
                            user.getRole(),
                            mustChangePassword,
                            user.getFullName(),
                            tenantId != null ? tenantId.toString() : null,
                            hcfId != null ? hcfId.toString() : null));

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

                    // Send account locked email notification
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        try {
                            String html = emailService.getTemplates().accountLocked(
                                    user.getFullName(),
                                    "Multiple failed login attempts (" + maxLoginAttempts + " attempts). Account locked for " + LOCKOUT_DURATION_MINUTES + " minutes.");
                            emailService.sendHtmlEmail(user.getEmail(), "Account Security Alert - SmartCBWTF", html);
                            log.info("Account locked email sent to: {}", user.getEmail());
                        } catch (Exception emailEx) {
                            log.warn("Failed to send account locked email to {}: {}", user.getEmail(), emailEx.getMessage());
                        }
                    }

                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", "ACCOUNT_LOCKED",
                            "message", "Account locked due to too many failed login attempts. Try again in "
                                    + LOCKOUT_DURATION_MINUTES + " minutes."));
                }

                appUserRepository.save(user);
                int attemptsRemaining = maxLoginAttempts - user.getFailedLoginAttempts();

                log.warn("Failed login for user {} ({} attempts remaining)", user.getUsername(), attemptsRemaining);

                return invalidCredentialsResponse();
            }

            // User doesn't exist - return generic error
            return invalidCredentialsResponse();
        }
    }

    private ResponseEntity<Map<String, String>> invalidCredentialsResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", INVALID_CREDENTIALS_MESSAGE));
    }

    private UUID resolveTenantId(AppUser user) {
        if (user.getFacility() != null) {
            return user.getFacility().getId();
        }

        if (!"HCF_ADMIN".equals(user.getRole()) || user.getHcf() == null) {
            return null;
        }

        return agreementRepository.findFirstByHcfIdAndStatusOrderByStartDateDesc(
                        user.getHcf().getId(), Agreement.Status.ACTIVE.name())
                .filter(agreement -> agreement.getEndDate() == null
                        || !agreement.getEndDate().isBefore(LocalDate.now()))
                .map(Agreement::getFacility)
                .filter(facility -> facility != null)
                .map(facility -> facility.getId())
                .orElse(null);
    }

    /**
     * Unlock a user account (SuperAdmin only).
     */
    @PostMapping("/unlock/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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
