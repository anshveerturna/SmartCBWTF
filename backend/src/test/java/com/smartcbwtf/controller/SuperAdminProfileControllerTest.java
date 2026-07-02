package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminProfileControllerTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private SubscriptionAuditRepository auditRepository;

    private SuperAdminProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new SuperAdminProfileController(
                userRepository,
                passwordEncoder,
                passwordPolicyValidator,
                auditRepository);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("super-admin", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfileIsNotCacheable() {
        AppUser user = user();
        when(userRepository.findByUsername("super-admin")).thenReturn(Optional.of(user));

        var response = controller.getMyProfile();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals(user.getId(), response.getBody().id());
    }

    @Test
    void updateMyProfileIsNotCacheable() {
        when(userRepository.findByUsername("super-admin")).thenReturn(Optional.of(user()));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.updateMyProfile(new SuperAdminProfileController.ProfileUpdateRequest(
                "Updated Super Admin",
                "updated@example.com",
                "+91 98765 43210"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
    }

    private static AppUser user() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername("super-admin");
        user.setRole("SUPER_ADMIN");
        user.setFullName("Super Admin");
        user.setEmail("super@example.com");
        user.setPhone("+91 90000 00000");
        return user;
    }
}
