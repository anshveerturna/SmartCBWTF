package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.Facility;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.AuditLogService;
import com.smartcbwtf.service.PasswordPolicyValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CbwtfProfileControllerTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private AuditLogService auditLogService;

    private CbwtfProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new CbwtfProfileController(
                userRepository,
                passwordEncoder,
                passwordPolicyValidator,
                auditLogService);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("cbwtf-admin", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfileIsNotCacheable() {
        AppUser user = user();
        when(userRepository.findByUsername("cbwtf-admin")).thenReturn(Optional.of(user));

        var response = controller.getMyProfile();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals(user.getId(), response.getBody().id());
    }

    @Test
    void updateMyProfileIsNotCacheable() {
        AppUser user = user();
        when(userRepository.findByUsername("cbwtf-admin")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var response = controller.updateMyProfile(new CbwtfProfileController.ProfileUpdateRequest(
                "Updated Admin",
                "updated@example.com",
                "+91 98765 43210"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    private static AppUser user() {
        Facility facility = new Facility();
        facility.setId(UUID.randomUUID());
        facility.setCode("CBWTF");
        facility.setName("CBWTF Facility");

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("cbwtf-admin");
        user.setRole("CBWTF_ADMIN");
        user.setFullName("CBWTF Admin");
        user.setEmail("admin@example.com");
        user.setPhone("+91 90000 00000");
        user.setFacility(facility);
        return user;
    }
}
