package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.AuditLogRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.HcfAccessGuard;
import com.smartcbwtf.service.PasswordPolicyValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcfProfileControllerTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private HcfAccessGuard accessGuard;

    private HcfProfileController controller;
    private UUID hcfId;
    private UUID facilityId;

    @BeforeEach
    void setUp() {
        controller = new HcfProfileController(
                userRepository,
                hcfRepository,
                auditLogRepository,
                passwordEncoder,
                passwordPolicyValidator,
                auditLogService,
                accessGuard);
        hcfId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), facilityId, hcfId, "HCF_ADMIN", "hcf-admin"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("hcf-admin", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void getMyProfileIsNotCacheable() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("hcf-admin");
        user.setRole("HCF_ADMIN");
        user.setFullName("HCF Admin");
        when(userRepository.findByUsername("hcf-admin")).thenReturn(Optional.of(user));
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.empty());

        var response = controller.getMyProfile();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }

    @Test
    void activityLogsDefaultInvalidLimitInsteadOfThrowing() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("hcf-admin");
        when(userRepository.findByUsername("hcf-admin")).thenReturn(Optional.of(user));
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.empty());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(auditLogRepository.findByActorUserIdOrderByTsDesc(eq(user.getId()), pageable.capture()))
                .thenReturn(List.of());

        var response = controller.getActivityLogs(0);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals(20, pageable.getValue().getPageSize());
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }

    @Test
    void updateMyProfileNormalizesIdentityFieldsAndEscapesAuditJson() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("hcf-admin");
        user.setRole("HCF_ADMIN");
        user.setFullName("Old \"Name\"\nX");
        user.setEmail("old@example.com");
        user.setPhone("1234567");
        when(userRepository.findByUsername("hcf-admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hcfRepository.findByIdAndFacilityId(hcfId, facilityId)).thenReturn(Optional.empty());

        var response = controller.updateMyProfile(new HcfProfileController.ProfileUpdateRequest(
                "  Dr.\nAsha\tRao  ",
                "",
                "  +91 98765-43210  "));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));

        ArgumentCaptor<AppUser> savedUser = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("Dr. Asha Rao", savedUser.getValue().getFullName());
        assertNull(savedUser.getValue().getEmail());
        assertEquals("+91 98765-43210", savedUser.getValue().getPhone());

        ArgumentCaptor<String> auditDetails = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(eq("USER"), eq(user.getId()), eq("PROFILE_UPDATED"), eq(user.getId()),
                auditDetails.capture());
        assertTrue(auditDetails.getValue().contains("Old \\\"Name\\\"\\nX"));
        assertTrue(auditDetails.getValue().contains("\"new\":\"name=Dr. Asha Rao,email=null,phone=+91 98765-43210\""));
        verify(accessGuard).assertPortalAccess(hcfId, facilityId);
    }
}
