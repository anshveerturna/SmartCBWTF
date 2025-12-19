package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.*;
import com.smartcbwtf.dto.admin.*;
import com.smartcbwtf.repository.*;
import com.smartcbwtf.service.SubscriptionService;
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
 * Admin API for SuperAdmin tenant management.
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
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            FacilityRepository facilityRepository,
            AppUserRepository userRepository,
            HcfRepository hcfRepository,
            SubscriptionService subscriptionService,
            SubscriptionAuditRepository auditRepository,
            PasswordEncoder passwordEncoder) {
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.hcfRepository = hcfRepository;
        this.subscriptionService = subscriptionService;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== TENANT LISTING ==========

    @GetMapping("/tenants")
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

    @GetMapping("/tenants/{id}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable UUID id) {
        return facilityRepository.findById(id)
                .map(f -> TenantDTO.from(
                        f,
                        countHcfsForFacility(f.getId()),
                        countActiveUsersForFacility(f.getId()),
                        subscriptionService.getEnabledFeatures(f.getId())))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== TENANT ONBOARDING ==========

    @PostMapping("/tenants")
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

        // Audit logs
        auditRepository.save(SubscriptionAudit.forFacility(
                facility.getId(),
                SubscriptionAudit.Action.CREATED,
                null,
                plan.name(),
                performerId,
                performerUsername,
                "SUPER_ADMIN",
                "Tenant onboarded with " + facility.getSubscriptionStatus() + " status"));

        auditRepository.save(SubscriptionAudit.forFacility(
                facility.getId(),
                SubscriptionAudit.Action.USER_CREATED,
                null,
                request.adminEmail(),
                performerId,
                performerUsername,
                "SUPER_ADMIN",
                "Initial CBWTF_ADMIN user created"));

        log.info("Onboarded tenant {} with admin user {}", facility.getCode(), request.adminEmail());

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

    @PutMapping("/tenants/{id}/subscription")
    public ResponseEntity<TenantDTO> updateSubscription(
            @PathVariable UUID id,
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

    @PostMapping("/tenants/{id}/suspend")
    public ResponseEntity<TenantDTO> suspendTenant(
            @PathVariable UUID id,
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

    @PostMapping("/tenants/{id}/reactivate")
    public ResponseEntity<TenantDTO> reactivateTenant(
            @PathVariable UUID id,
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

    @PostMapping("/tenants/{id}/temporary-access")
    public ResponseEntity<TenantDTO> grantTemporaryAccess(
            @PathVariable UUID id,
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

    @GetMapping("/tenants/{id}/features")
    public ResponseEntity<Map<String, Boolean>> getFeatures(@PathVariable UUID id) {
        if (!facilityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(subscriptionService.getEnabledFeatures(id));
    }

    @PutMapping("/tenants/{id}/features")
    public ResponseEntity<Map<String, Boolean>> updateFeatures(
            @PathVariable UUID id,
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

    @GetMapping("/tenants/{id}/audit")
    public ResponseEntity<Page<TenantAuditDTO>> getAuditHistory(
            @PathVariable UUID id,
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
        int totalTenants = (int) facilityRepository.count();
        int activeTenants = (int) facilityRepository.countBySubscriptionStatus("ACTIVE");
        int trialTenants = (int) facilityRepository.countBySubscriptionStatus("TRIAL");
        int expiredTenants = (int) facilityRepository.countBySubscriptionStatus("EXPIRED");
        int suspendedTenants = (int) facilityRepository.countBySubscriptionStatus("SUSPENDED");
        int totalHcfs = (int) hcfRepository.count();
        int totalUsers = (int) userRepository.count();

        return ResponseEntity.ok(new PlatformStatsDTO(
                totalTenants,
                activeTenants,
                trialTenants,
                expiredTenants,
                suspendedTenants,
                totalHcfs,
                totalUsers,
                0, // TODO: Add bags processed count
                LocalDate.now()));
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
}
