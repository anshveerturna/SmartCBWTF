package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AttendanceRepository;
import com.smartcbwtf.repository.FacilityRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffServiceSecurityTest {

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final FacilityRepository facilityRepository = mock(FacilityRepository.class);
    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final PasswordPolicyValidator passwordPolicyValidator = mock(PasswordPolicyValidator.class);
    private final StaffService service = new StaffService(
            appUserRepository,
            facilityRepository,
            attendanceRepository,
            passwordEncoder,
            auditLogService,
            emailService,
            passwordPolicyValidator);

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void updateStaffRejectsUntrustedProfilePhotoUrl() {
        UUID facilityId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        AppUser user = staffUser(staffId, facilityId);
        when(appUserRepository.findByIdAndFacilityIdAndRoleIn(eq(staffId), eq(facilityId), any()))
                .thenReturn(Optional.of(user));

        StaffService.UpdateStaffRequest request = new StaffService.UpdateStaffRequest(
                "Driver One",
                "driver@example.com",
                "9999999999",
                "MALE",
                null,
                "https://evil.example/avatar.png");

        assertThrows(IllegalArgumentException.class, () -> service.updateStaff(staffId, request));

        verify(appUserRepository, never()).save(user);
    }

    @Test
    void updateCredentialsRejectsPasswordPolicyViolationBeforeEncodingOrSaving() {
        UUID facilityId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        AppUser user = staffUser(staffId, facilityId);
        when(appUserRepository.findByIdAndFacilityIdAndRoleIn(eq(staffId), eq(facilityId), any()))
                .thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("too weak"))
                .when(passwordPolicyValidator).validateOrThrow("weak");

        StaffService.UpdateCredentialsRequest request = new StaffService.UpdateCredentialsRequest(
                null,
                "weak",
                false);

        assertThrows(IllegalArgumentException.class, () -> service.updateCredentials(staffId, request));

        verify(passwordEncoder, never()).encode(any(String.class));
        verify(appUserRepository, never()).save(user);
    }

    @Test
    void updateCredentialsNormalizesUsernameBeforeUniquenessCheckAndSave() {
        UUID facilityId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        AppUser user = staffUser(staffId, facilityId);
        when(appUserRepository.findByIdAndFacilityIdAndRoleIn(eq(staffId), eq(facilityId), any()))
                .thenReturn(Optional.of(user));
        when(appUserRepository.existsByUsername("driver.new")).thenReturn(false);
        when(appUserRepository.save(user)).thenReturn(user);

        StaffService.UpdateCredentialsRequest request = new StaffService.UpdateCredentialsRequest(
                " driver.new\t",
                null,
                null);

        service.updateCredentials(staffId, request);

        verify(appUserRepository).existsByUsername("driver.new");
        verify(appUserRepository).save(user);
        assertEquals("driver.new", user.getUsername());
    }

    @Test
    void requestGpsRefreshRejectsStaffOutsideCurrentFacility() {
        UUID facilityId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, null, "CBWTF_ADMIN", "admin"));
        when(appUserRepository.findByIdAndFacilityIdAndRoleIn(eq(staffId), eq(facilityId), any()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.requestGpsRefresh(staffId));

        verify(appUserRepository).findByIdAndFacilityIdAndRoleIn(eq(staffId), eq(facilityId), any());
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void staffRequestValidationRejectsUnsafeCreatePayloads() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(new StaffService.CreateStaffRequest(
                "",
                "not-an-email",
                "abc",
                "CBWTF_ADMIN",
                "x".repeat(257)));

        assertTrue(violations.stream().anyMatch(v -> "fullName".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "email".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "phone".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "role".contentEquals(v.getPropertyPath().toString())));
        assertTrue(violations.stream().anyMatch(v -> "password".contentEquals(v.getPropertyPath().toString())));
    }

    private AppUser staffUser(UUID staffId, UUID facilityId) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        AppUser user = new AppUser();
        user.setId(staffId);
        user.setFacility(facility);
        user.setRole(StaffService.ROLE_DRIVER);
        user.setUsername("driver");
        user.setFullName("Driver One");
        return user;
    }
}
