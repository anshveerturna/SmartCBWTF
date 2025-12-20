package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.admin.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.SubscriptionService;
import com.smartcbwtf.service.SystemConfigService;
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

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Admin API for SuperAdmin CBWTF management.
 * All endpoints require SUPER_ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final FacilityRepository facilityRepository;
    private final AppUserRepository userRepository;
    private final HcfRepository hcfRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionAuditRepository auditRepository;
    private final InvoiceRepository invoiceRepository;
    private final SystemErrorRepository systemErrorRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;

    public AdminController(
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            HcfRepository hcfRepository,
            SubscriptionService subscriptionService,
            SubscriptionAuditRepository auditRepository,
            InvoiceRepository invoiceRepository,
            SystemErrorRepository systemErrorRepository,
            PasswordEncoder passwordEncoder,
            SystemConfigService systemConfigService) {
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.hcfRepository = hcfRepository;
        this.subscriptionService = subscriptionService;
        this.auditRepository = auditRepository;
        this.invoiceRepository = invoiceRepository;
        this.systemErrorRepository = systemErrorRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemConfigService = systemConfigService;
    }

    // ========== CBWTF LISTING ==========

    @GetMapping("/cbwtfs")
    public ResponseEntity<Page<TenantDTO>> listTenants(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Facility> facilities;

        if (search != null && !search.isBlank()) {
            facilities = facilityRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                    search, search, pageable);
        } else if (status != null && !status.isBlank()) {
            facilities = facilityRepository.findBySubscriptionStatus(status, pageable);
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
                    admin.setForcePasswordChange(true);
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
                            "CBWTF admin credentials changed, password reset required"));

                    log.info("Changed credentials for CBWTF {} admin", facility.getCode());

                    return ResponseEntity.ok(Map.of(
                            "message", "Credentials updated",
                            "username", request.newUsername(),
                            "forcePasswordChange", "true"));
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

        // TODO: Send email with temp password to admin
        // For now, log it (remove in production!)
        log.warn("TEMP PASSWORD for {}: {} (remove this log in production)", request.adminEmail(), tempPassword);

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
            @RequestBody Map<String, String> body) {

        String reason = body.getOrDefault("reason", "Suspended by admin");

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
            @RequestBody Map<String, Object> body) {

        int days = (Integer) body.getOrDefault("days", 365);
        String notes = (String) body.getOrDefault("notes", "Reactivated by admin");
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
            @RequestBody Map<String, Object> body) {

        int days = (Integer) body.getOrDefault("days", 7);
        String reason = (String) body.getOrDefault("reason", "Temporary access granted");

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
            @RequestBody Map<String, Boolean> features) {

        if (!facilityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        for (Map.Entry<String, Boolean> entry : features.entrySet()) {
            subscriptionService.setFeatureEnabled(
                    id,
                    entry.getKey(),
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

        Pageable pageable = PageRequest.of(page, size);
        Page<SubscriptionAudit> audits = auditRepository.findByFacilityId(id, pageable);
        Page<TenantAuditDTO> result = audits.map(TenantAuditDTO::from);

        return ResponseEntity.ok(result);
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

        return ResponseEntity.ok(new PlatformStatsDTO(
                totalCBWTFs,
                activeCBWTFs,
                trialCBWTFs,
                expiredCBWTFs,
                suspendedCBWTFs,
                totalHcfs,
                totalUsers,
                0L, // TODO: Add bags processed count
                totalRevenue,
                pendingErrors,
                recentErrors,
                LocalDate.now()));
    }

    private List<PlatformStatsDTO.SystemErrorDTO> getRecentSystemErrors() {
        // Get real system errors from the error table
        return systemErrorRepository.findTop10OpenOrderedBySeverity()
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
        // TODO: Add proper query when HCF has facility relation
        return 0;
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
}
