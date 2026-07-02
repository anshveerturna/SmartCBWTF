package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.admin.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.EmailService;
import com.smartcbwtf.service.SubscriptionService;
import com.smartcbwtf.service.SystemConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Admin API for SuperAdmin CBWTF management.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final int MAX_ADMIN_REASON_LENGTH = 1_000;
    private static final int MAX_FEATURE_FLAGS_PER_REQUEST = 20;
    private static final int MAX_REACTIVATION_DAYS = 3_650;
    private static final int MAX_TEMPORARY_ACCESS_DAYS = 365;
    private static final int MAX_QUERY_FILTER_LENGTH = 80;
    private static final int MAX_SEARCH_LENGTH = 120;
    private static final String FEATURE_KEY_PATTERN = "^[A-Z0-9_]+$";
    private static final Set<String> SUPPORTED_FEATURE_KEYS = Set.of(
            TenantFeatureFlag.ADVANCED_ANALYTICS,
            TenantFeatureFlag.ROUTE_OPTIMIZATION,
            TenantFeatureFlag.CPCB_REPORTING,
            TenantFeatureFlag.INVOICE_AUTO_SEND,
            TenantFeatureFlag.PAYMENT_GATEWAY,
            TenantFeatureFlag.ATTENDANCE_ENFORCEMENT,
            TenantFeatureFlag.VEHICLE_TRACKING,
            TenantFeatureFlag.AI_INSIGHTS,
            TenantFeatureFlag.MULTI_VEHICLE,
            TenantFeatureFlag.HCF_SELF_SERVICE);

    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final HcfRepository hcfRepository;
    private final AgreementRepository agreementRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionAuditRepository auditRepository;
    private final InvoiceRepository invoiceRepository;
    private final BagEventRepository bagEventRepository;
    private final SystemErrorRepository systemErrorRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;
    private final EmailService emailService;

    @Value("${app.portal.url:https://portal.smartcbwtf.com}")
    private String portalUrl;

    public AdminController(
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            HcfRepository hcfRepository,
            AgreementRepository agreementRepository,
            SubscriptionService subscriptionService,
            SubscriptionAuditRepository auditRepository,
            InvoiceRepository invoiceRepository,
            BagEventRepository bagEventRepository,
            SystemErrorRepository systemErrorRepository,
            PasswordEncoder passwordEncoder,
            SystemConfigService systemConfigService,
            EmailService emailService) {
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.hcfRepository = hcfRepository;
        this.agreementRepository = agreementRepository;
        this.subscriptionService = subscriptionService;
        this.auditRepository = auditRepository;
        this.invoiceRepository = invoiceRepository;
        this.bagEventRepository = bagEventRepository;
        this.systemErrorRepository = systemErrorRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemConfigService = systemConfigService;
        this.emailService = emailService;
    }

    // ========== CBWTF LISTING ==========

    @GetMapping("/cbwtfs")
    public ResponseEntity<Page<TenantDTO>> listTenants(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20, Sort.by("createdAt").descending());
        Page<Facility> facilities;
        String normalizedStatus = normalizeQueryFilter(status, "status");
        String normalizedSearch = normalizeSearch(search);

        if (normalizedSearch != null) {
            facilities = facilityRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    normalizedSearch, normalizedSearch, pageable);
        } else if (normalizedStatus != null) {
            facilities = facilityRepository.findBySubscriptionStatus(normalizedStatus, pageable);
        } else {
            facilities = facilityRepository.findAll(pageable);
        }

        Page<TenantDTO> result = facilities.map(f -> TenantDTO.from(
                f,
                countHcfsForFacility(f.getId()),
                countActiveUsersForFacility(f.getId()),
                subscriptionService.getEnabledFeatures(f.getId())));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/cbwtfs/{id}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable("id") UUID id) {
        return facilityRepository.findById(id)
                .map(f -> TenantDTO.from(
                        f,
                        countHcfsForFacility(f.getId()),
                        countActiveUsersForFacility(f.getId()),
                        subscriptionService.getEnabledFeatures(f.getId())))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get CBWTF admin user info.
     * Note: Password cannot be returned as it's hashed.
     */
    @GetMapping("/cbwtfs/{id}/admin")
    public ResponseEntity<Map<String, Object>> getCBWTFAdmin(@PathVariable("id") UUID id) {
        return facilityRepository.findById(id)
                .map(facility -> {
                    AppUser admin = userRepository.findByFacilityIdAndRole(id, "CBWTF_ADMIN")
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (admin == null) {
                        return ResponseEntity.<Map<String, Object>>ok(Map.of(
                                "hasAdmin", false,
                                "message", "No CBWTF_ADMIN found for this facility"));
                    }

                    return ResponseEntity.ok(Map.<String, Object>of(
                            "hasAdmin", true,
                            "id", admin.getId().toString(),
                            "username", admin.getUsername(),
                            "email", admin.getEmail() != null ? admin.getEmail() : "",
                            "fullName", admin.getFullName() != null ? admin.getFullName() : "",
                            "active", admin.isActive(),
                            "lastLoginAt", admin.getLastLoginAt() != null ? admin.getLastLoginAt().toString() : ""));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== CBWTF UPDATE ==========

    @PutMapping("/cbwtfs/{id}")
    @Transactional
    public ResponseEntity<TenantDTO> updateCBWTF(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCBWTFRequest request) {

        return facilityRepository.findById(id)
                .map(facility -> {
                    String oldName = facility.getName();

                    facility.setName(request.name());
                    facility.setAddress(request.address());
                    facility.setOwnerName(request.ownerName());
                    facility.setContactEmail(request.contactEmail());
                    facility.setContactPhone(request.contactPhone());
                    if (request.gpsLat() != null)
                        facility.setGpsLat(request.gpsLat());
                    if (request.gpsLon() != null)
                        facility.setGpsLon(request.gpsLon());
                    if (request.geofenceRadiusM() != null)
                        facility.setGeofenceRadiusM(request.geofenceRadiusM());
                    if (request.panNumber() != null)
                        facility.setPanNumber(request.panNumber());
                    if (request.gstNumber() != null)
                        facility.setGstNumber(request.gstNumber());
                    if (request.aadharNumber() != null)
                        facility.setAadharNumber(request.aadharNumber());

                    facility = facilityRepository.save(facility);

                    // Audit
                    auditRepository.save(SubscriptionAudit.forFacility(
                            facility.getId(),
                            SubscriptionAudit.Action.STATUS_CHANGED,
                            oldName,
                            request.name(),
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "CBWTF details updated"));

                    log.info("Updated CBWTF {} details", facility.getCode());

                    return ResponseEntity.ok(TenantDTO.from(
                            facility,
                            countHcfsForFacility(id),
                            countActiveUsersForFacility(id),
                            subscriptionService.getEnabledFeatures(id)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cbwtfs/{id}/change-credentials")
    @Transactional
    public ResponseEntity<Map<String, String>> changeCBWTFCredentials(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ChangeCBWTFCredentialsRequest request) {

        // Find the CBWTF_ADMIN user for this facility
        return facilityRepository.findById(id)
                .map(facility -> {
                    AppUser admin = userRepository.findByFacilityIdAndRole(id, "CBWTF_ADMIN")
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (admin == null) {
                        return ResponseEntity.badRequest()
                                .<Map<String, String>>body(Map.of("error", "No CBWTF_ADMIN found for this facility"));
                    }

                    String oldUsername = admin.getUsername();
                    admin.setUsername(request.newUsername());
                    admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    // Don't force password change when SuperAdmin sets a new password
                    // User can use "Force Password Reset" action separately if needed
                    admin.setForcePasswordChange(false);
                    admin.setMustChangePassword(false);
                    userRepository.save(admin);

                    // Audit
                    auditRepository.save(SubscriptionAudit.forFacility(
                            id,
                            SubscriptionAudit.Action.USER_UPDATED,
                            oldUsername,
                            request.newUsername(),
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "CBWTF admin credentials changed by SuperAdmin"));

                    log.info("Changed credentials for CBWTF {} admin", facility.getCode());

                    return ResponseEntity.ok(Map.of(
                            "message", "Credentials updated successfully",
                            "username", request.newUsername()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Force password reset for CBWTF admin.
     * This marks the user to change password on next login.
     */
    @PostMapping("/cbwtfs/{id}/force-password-reset")
    @Transactional
    public ResponseEntity<Map<String, String>> forceCBWTFPasswordReset(@PathVariable("id") UUID id) {
        return facilityRepository.findById(id)
                .map(facility -> {
                    AppUser admin = userRepository.findByFacilityIdAndRole(id, "CBWTF_ADMIN")
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (admin == null) {
                        return ResponseEntity.badRequest()
                                .<Map<String, String>>body(Map.of("error", "No CBWTF_ADMIN found for this facility"));
                    }

                    admin.setForcePasswordChange(true);
                    admin.setMustChangePassword(true);
                    userRepository.save(admin);

                    // Audit
                    auditRepository.save(SubscriptionAudit.forFacility(
                            id,
                            SubscriptionAudit.Action.PASSWORD_RESET_FORCED,
                            null,
                            admin.getUsername(),
                            getCurrentUserId(),
                            getCurrentUsername(),
                            "SUPER_ADMIN",
                            "CBWTF admin forced to reset password on next login"));

                    log.info("Forced password reset for CBWTF {} admin", facility.getCode());

                    return ResponseEntity.ok(Map.of(
                            "message", "Password reset required on next login",
                            "username", admin.getUsername()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== CBWTF ONBOARDING ==========

    @PostMapping("/cbwtfs")
    @Transactional
    public ResponseEntity<TenantDTO> onboardTenant(@Valid @RequestBody OnboardTenantRequest request) {
        // Validate code uniqueness
        if (facilityRepository.existsByCode(request.code())) {
            return ResponseEntity.badRequest().build();
        }

        UUID performerId = getCurrentUserId();
        String performerUsername = getCurrentUsername();

        // Create facility
        Facility facility = new Facility();
        facility.setCode(request.code());
        facility.setName(request.name());
        facility.setAddress(request.address());
        facility.setContactEmail(request.contactEmail());
        facility.setContactPhone(request.contactPhone());
        facility.setGpsLat(request.gpsLat());
        facility.setGpsLon(request.gpsLon());
        facility.setGeofenceRadiusM(request.geofenceRadiusM());

        // Set subscription
        Facility.Plan plan = Facility.Plan.valueOf(request.subscriptionPlan());
        facility.setSubscriptionPlanEnum(plan);

        if (request.trialDays() != null && request.trialDays() > 0) {
            facility.setSubscriptionStatusEnum(Facility.Status.TRIAL);
            facility.setSubscriptionExpiresAt(Instant.now().plus(request.trialDays(), ChronoUnit.DAYS));
        } else {
            facility.setSubscriptionStatusEnum(Facility.Status.ACTIVE);
            facility.setSubscriptionExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        }

        facility.setOnboardedAt(Instant.now());
        facility.setOnboardedBy(performerId);

        facility = facilityRepository.save(facility);

        // Create initial admin user
        String tempPassword = generateTempPassword();
        AppUser adminUser = new AppUser();
        adminUser.setUsername(request.adminEmail());
        adminUser.setEmail(request.adminEmail());
        adminUser.setName(request.adminName());
        adminUser.setPasswordHash(passwordEncoder.encode(tempPassword));
        adminUser.setRole("CBWTF_ADMIN");
        adminUser.setFacility(facility);
        adminUser.setActive(true);
        adminUser.setForcePasswordChange(true);
        userRepository.save(adminUser);

        // Enable all feature flags by default
        enableAllDefaultFeatures(facility);

        // Audit logs
        auditRepository.save(SubscriptionAudit.forFacility(
                facility.getId(),
                SubscriptionAudit.Action.CREATED,
                null,
                plan.name(),
                performerId,
                performerUsername,
                "SUPER_ADMIN",
                "CBWTF onboarded with " + facility.getSubscriptionStatus() + " status"));

        auditRepository.save(SubscriptionAudit.forFacility(
                facility.getId(),
                SubscriptionAudit.Action.USER_CREATED,
                null,
                request.adminEmail(),
                performerId,
                performerUsername,
                "SUPER_ADMIN",
                "Initial CBWTF_ADMIN user created"));

        log.info("Onboarded CBWTF {} with admin user {}", facility.getCode(), request.adminEmail());

        // Send welcome email with credentials via Brevo
        try {
            String html = emailService.getTemplates().cbwtfWelcome(
                    facility.getName(),
                    request.adminName(),
                    request.adminEmail(),
                    tempPassword,
                    portalUrl);
            emailService.sendHtmlEmail(request.adminEmail(), "Welcome to SmartCBWTF – Your Admin Credentials", html);
            log.info("Sent CBWTF welcome email to {}", request.adminEmail());
        } catch (Exception e) {
            log.error("Failed to send CBWTF welcome email to {}: {}", request.adminEmail(), e.getMessage());
        }

        return ResponseEntity.ok(TenantDTO.from(
                facility,
                0,
                1,
                Map.of()));
    }

    // ========== SUBSCRIPTION MANAGEMENT ==========

    @PutMapping("/cbwtfs/{id}/subscription")
    public ResponseEntity<TenantDTO> updateSubscription(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateSubscriptionRequest request) {

        Facility.Plan plan = Facility.Plan.valueOf(request.plan());
        Instant expiresAt = request.expiresAt().atStartOfDay(ZoneId.systemDefault()).toInstant();

        Facility facility = subscriptionService.updateSubscriptionPlan(
                id,
                plan,
                expiresAt,
                getCurrentUserId(),
                getCurrentUsername(),
                request.notes());

        return ResponseEntity.ok(TenantDTO.from(
                facility,
                countHcfsForFacility(id),
                countActiveUsersForFacility(id),
                subscriptionService.getEnabledFeatures(id)));
    }

    @PostMapping("/cbwtfs/{id}/suspend")
    public ResponseEntity<TenantDTO> suspendCBWTF(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) SuspendTenantRequest body) {

        String reason = defaultCleanLine(body != null ? body.reason() : null, "Suspended by admin");

        Facility facility = subscriptionService.suspendTenant(
                id,
                getCurrentUserId(),
                getCurrentUsername(),
                reason);

        return ResponseEntity.ok(TenantDTO.from(
                facility,
                countHcfsForFacility(id),
                countActiveUsersForFacility(id),
                subscriptionService.getEnabledFeatures(id)));
    }

    @PostMapping("/cbwtfs/{id}/reactivate")
    public ResponseEntity<TenantDTO> reactivateCBWTF(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) ReactivateTenantRequest body) {

        int days = body != null && body.days() != null ? body.days() : 365;
        String notes = defaultCleanLine(body != null ? body.notes() : null, "Reactivated by admin");
        Instant expiresAt = Instant.now().plus(days, ChronoUnit.DAYS);

        Facility facility = subscriptionService.reactivateTenant(
                id,
                expiresAt,
                getCurrentUserId(),
                getCurrentUsername(),
                notes);

        return ResponseEntity.ok(TenantDTO.from(
                facility,
                countHcfsForFacility(id),
                countActiveUsersForFacility(id),
                subscriptionService.getEnabledFeatures(id)));
    }

    @PostMapping("/cbwtfs/{id}/temporary-access")
    public ResponseEntity<TenantDTO> grantTemporaryAccess(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) TemporaryAccessRequest body) {

        int days = body != null && body.days() != null ? body.days() : 7;
        String reason = defaultCleanLine(body != null ? body.reason() : null, "Temporary access granted");

        Facility facility = subscriptionService.grantTemporaryAccess(
                id,
                days,
                getCurrentUserId(),
                getCurrentUsername(),
                reason);

        return ResponseEntity.ok(TenantDTO.from(
                facility,
                countHcfsForFacility(id),
                countActiveUsersForFacility(id),
                subscriptionService.getEnabledFeatures(id)));
    }

    // ========== FEATURE FLAGS ==========

    @GetMapping("/cbwtfs/{id}/features")
    public ResponseEntity<Map<String, Boolean>> getFeatures(@PathVariable("id") UUID id) {
        if (!facilityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(subscriptionService.getEnabledFeatures(id));
    }

    @PutMapping("/cbwtfs/{id}/features")
    public ResponseEntity<Map<String, Boolean>> updateFeatures(
            @PathVariable("id") UUID id,
            @Valid @RequestBody
            @Size(max = MAX_FEATURE_FLAGS_PER_REQUEST, message = "Too many feature flags in one request")
            Map<@NotBlank(message = "Feature key is required")
                    @Size(max = 100, message = "Feature key must be 100 characters or less")
                    @Pattern(regexp = FEATURE_KEY_PATTERN, message = "Feature key contains invalid characters") String,
                    @NotNull(message = "Feature enabled value is required") Boolean> features) {

        if (!facilityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (features == null) {
            throw new IllegalArgumentException("Feature update request is required");
        }

        for (Map.Entry<String, Boolean> entry : features.entrySet()) {
            String featureKey = cleanFeatureKey(entry.getKey());
            subscriptionService.setFeatureEnabled(
                    id,
                    featureKey,
                    entry.getValue(),
                    getCurrentUserId(),
                    getCurrentUsername());
        }

        return ResponseEntity.ok(subscriptionService.getEnabledFeatures(id));
    }

    // ========== AUDIT HISTORY ==========

    @GetMapping("/cbwtfs/{id}/audit")
    public ResponseEntity<Page<TenantAuditDTO>> getAuditHistory(
            @PathVariable("id") UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = pageRequest(page, size, 20);
        Page<SubscriptionAudit> audits = auditRepository.findByFacilityId(id, pageable);
        Page<TenantAuditDTO> result = audits.map(TenantAuditDTO::from);

        return privateResponse(result);
    }

    private static <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    // ========== EMAIL TEST ==========

    /**
     * Test email sending via Brevo.
     * SuperAdmin only - for verifying email configuration.
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@Valid @RequestBody TestEmailRequest body) {
        if (body == null) {
            throw new IllegalArgumentException("Test email request is required");
        }
        String toEmail = body.email().trim();

        try {
            String subject = "SmartCBWTF Test Email";
            String messageBody = "Hello!\n\nThis is a test email from SmartCBWTF to verify your Brevo email integration is working correctly.\n\nIf you received this email, your configuration is correct!\n\nTimestamp: "
                    + java.time.Instant.now();

            emailService.sendEmail(toEmail, subject, messageBody);

            log.info("Test email sent to {} by {}", toEmail, getCurrentUsername());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Test email sent to " + toEmail,
                    "timestamp", java.time.Instant.now().toString()));
        } catch (Exception e) {
            log.error("Failed to send test email to {}: {}", toEmail, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    // ========== PLATFORM STATS ==========

    @GetMapping("/platform/stats")
    public ResponseEntity<PlatformStatsDTO> getPlatformStats() {
        int totalCBWTFs = (int) facilityRepository.count();
        int activeCBWTFs = (int) facilityRepository.countBySubscriptionStatus("ACTIVE");
        int trialCBWTFs = (int) facilityRepository.countBySubscriptionStatus("TRIAL");
        int expiredCBWTFs = (int) facilityRepository.countBySubscriptionStatus("EXPIRED");
        int suspendedCBWTFs = (int) facilityRepository.countBySubscriptionStatus("SUSPENDED");
        int totalHcfs = (int) hcfRepository.count();
        int totalUsers = (int) userRepository.count();

        // Calculate total revenue from invoices
        java.math.BigDecimal totalRevenue = invoiceRepository.sumPaidAmount()
                .orElse(java.math.BigDecimal.ZERO);

        // Get recent system errors (from audit log with ERROR action)
        List<PlatformStatsDTO.SystemErrorDTO> recentErrors = getRecentSystemErrors();
        int pendingErrors = (int) recentErrors.stream().filter(e -> !e.resolved()).count();
        long totalBagsProcessed = bagEventRepository.countByEventType("CBWTF_VERIFICATION");

        return ResponseEntity.ok(new PlatformStatsDTO(
                totalCBWTFs,
                activeCBWTFs,
                trialCBWTFs,
                expiredCBWTFs,
                suspendedCBWTFs,
                totalHcfs,
                totalUsers,
                totalBagsProcessed,
                totalRevenue,
                pendingErrors,
                recentErrors,
                LocalDate.now()));
    }

    private List<PlatformStatsDTO.SystemErrorDTO> getRecentSystemErrors() {
        // Get real system errors from the error table
        return systemErrorRepository.findTop10OpenOrderedBySeverity(pageRequest(0, 10, 10))
                .stream()
                .map(error -> new PlatformStatsDTO.SystemErrorDTO(
                        error.getId().toString(),
                        error.getCreatedAt().toString(),
                        error.getSeverity(),
                        error.getComponent(),
                        error.getTitle(),
                        error.getFacility() != null ? error.getFacility().getCode() : "N/A",
                        "RESOLVED".equals(error.getStatus())))
                .toList();
    }

    // ========== HELPER METHODS ==========

    private int countHcfsForFacility(UUID facilityId) {
        return Math.toIntExact(agreementRepository.countDistinctActiveHcfsByFacilityId(facilityId));
    }

    private int countActiveUsersForFacility(UUID facilityId) {
        return userRepository.countByFacilityIdAndActive(facilityId, true);
    }

    private UUID getCurrentUserId() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null ? info.userId() : null;
    }

    private String getCurrentUsername() {
        TenantContext.TenantInfo info = TenantContext.get();
        return info != null ? info.username() : "SYSTEM";
    }

    private static String cleanFeatureKey(String value) {
        String cleaned = cleanLine(value).toUpperCase(Locale.ROOT);
        if (!cleaned.matches(FEATURE_KEY_PATTERN) || !SUPPORTED_FEATURE_KEYS.contains(cleaned)) {
            throw new IllegalArgumentException("Unsupported feature key: " + cleaned);
        }
        return cleaned;
    }

    private static String defaultCleanLine(String value, String fallback) {
        String cleaned = cleanLine(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static String cleanLine(String value) {
        return value == null ? "" : value.trim().replaceAll("[\\r\\n\\t]+", " ");
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

    private String generateTempPassword() {
        // Generate a secure random password
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        StringBuilder sb = new StringBuilder(12);
        Random random = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void enableAllDefaultFeatures(Facility facility) {
        // Feature flags to check from system config
        Map<String, String> featureConfigMap = Map.of(
                TenantFeatureFlag.ADVANCED_ANALYTICS, "feature.default_advanced_analytics",
                TenantFeatureFlag.ROUTE_OPTIMIZATION, "feature.default_route_optimization",
                TenantFeatureFlag.CPCB_REPORTING, "feature.default_cpcb_reporting",
                TenantFeatureFlag.INVOICE_AUTO_SEND, "feature.default_invoice_auto_send",
                TenantFeatureFlag.PAYMENT_GATEWAY, "feature.default_payment_gateway",
                TenantFeatureFlag.ATTENDANCE_ENFORCEMENT, "feature.default_attendance_enforcement",
                TenantFeatureFlag.VEHICLE_TRACKING, "feature.default_vehicle_tracking",
                TenantFeatureFlag.AI_INSIGHTS, "feature.default_ai_insights",
                TenantFeatureFlag.MULTI_VEHICLE, "feature.default_multi_vehicle",
                TenantFeatureFlag.HCF_SELF_SERVICE, "feature.default_hcf_self_service");

        StringBuilder enabledFeatures = new StringBuilder();
        for (Map.Entry<String, String> entry : featureConfigMap.entrySet()) {
            String featureKey = entry.getKey();
            String configKey = entry.getValue();
            boolean enabled = systemConfigService.getBoolean(configKey, true); // Default true for backward compat

            subscriptionService.setFeatureEnabled(
                    facility.getId(),
                    featureKey,
                    enabled,
                    getCurrentUserId(),
                    getCurrentUsername());

            if (enabled) {
                if (enabledFeatures.length() > 0)
                    enabledFeatures.append(", ");
                enabledFeatures.append(featureKey);
            }
        }

        log.info("Feature defaults applied for CBWTF {}: [{}]", facility.getCode(), enabledFeatures);
    }

    public record SuspendTenantRequest(
            @Size(max = MAX_ADMIN_REASON_LENGTH, message = "Reason must be 1000 characters or less")
            String reason) {
    }

    public record ReactivateTenantRequest(
            @Min(value = 1, message = "Reactivation days must be at least 1")
            @Max(value = MAX_REACTIVATION_DAYS, message = "Reactivation days must be 3650 or less")
            Integer days,
            @Size(max = MAX_ADMIN_REASON_LENGTH, message = "Notes must be 1000 characters or less")
            String notes) {
    }

    public record TemporaryAccessRequest(
            @Min(value = 1, message = "Temporary access days must be at least 1")
            @Max(value = MAX_TEMPORARY_ACCESS_DAYS, message = "Temporary access days must be 365 or less")
            Integer days,
            @Size(max = MAX_ADMIN_REASON_LENGTH, message = "Reason must be 1000 characters or less")
            String reason) {
    }

    public record TestEmailRequest(
            @NotBlank(message = "Email address is required")
            @Email(message = "Invalid email address")
            @Size(max = 255, message = "Email must be 255 characters or less")
            String email) {
    }
}
