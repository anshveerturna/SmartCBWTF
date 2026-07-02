package com.smartcbwtf.config;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class SuperAdminBootstrapTest {

    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final PasswordPolicyValidator passwordPolicyValidator = mock(PasswordPolicyValidator.class);
    private final Environment environment = mock(Environment.class);

    @Test
    void prodWithoutActiveSuperAdminOrBootstrapPasswordFailsFast() {
        when(environment.getActiveProfiles()).thenReturn(new String[] { "prod" });
        when(userRepository.countByRoleAndActive("SUPER_ADMIN", true)).thenReturn(0L);

        SuperAdminBootstrap bootstrap = bootstrap("", "", "", "");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
    }

    @Test
    void nonProdWithoutActiveSuperAdminOnlyWarns() throws Exception {
        when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
        when(userRepository.countByRoleAndActive("SUPER_ADMIN", true)).thenReturn(0L);

        bootstrap("", "", "", "").run(null);

        verify(userRepository).countByRoleAndActive("SUPER_ADMIN", true);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void configuredPasswordCreatesDefaultSuperAdminAndForcesPasswordChange() throws Exception {
        when(userRepository.countByRoleAndActive("SUPER_ADMIN", true)).thenReturn(0L);
        when(passwordEncoder.encode("long-initial-password")).thenReturn("encoded");
        when(userRepository.findByUsername("super_admin")).thenReturn(Optional.empty());

        bootstrap("", "long-initial-password", "ops@example.com", "Ops Admin").run(null);

        org.mockito.ArgumentCaptor<AppUser> captor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertEquals("super_admin", saved.getUsername());
        assertEquals("SUPER_ADMIN", saved.getRole());
        assertEquals("encoded", saved.getPasswordHash());
        assertEquals("ops@example.com", saved.getEmail());
        assertEquals("Ops Admin", saved.getFullName());
        assertTrue(saved.isActive());
        assertTrue(saved.isForcePasswordChange());
        assertTrue(saved.isMustChangePassword());
        verify(passwordPolicyValidator).validateOrThrow("long-initial-password");
    }

    @Test
    void configuredInitialPasswordDoesNotResetExistingActiveSuperAdmin() throws Exception {
        when(userRepository.countByRoleAndActive("SUPER_ADMIN", true)).thenReturn(1L);

        bootstrap("super_admin", "long-initial-password", "ops@example.com", "Ops Admin").run(null);

        verify(userRepository).countByRoleAndActive("SUPER_ADMIN", true);
        verify(userRepository, never()).findByUsername("super_admin");
        verifyNoInteractions(passwordPolicyValidator);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void configuredPasswordRejectsKnownDemoPassword() {
        when(userRepository.countByRoleAndActive("SUPER_ADMIN", true)).thenReturn(0L);
        SuperAdminBootstrap bootstrap = bootstrap("super_admin", "demo123", "", "");

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void configuredPasswordRejectsPasswordPolicyViolationBeforeEncoding() {
        when(userRepository.countByRoleAndActive("SUPER_ADMIN", true)).thenReturn(0L);
        doThrow(new IllegalArgumentException("Password policy violation: too weak"))
                .when(passwordPolicyValidator).validateOrThrow("password123456");

        SuperAdminBootstrap bootstrap = bootstrap("super_admin", "password123456", "", "");

        assertThrows(IllegalArgumentException.class, () -> bootstrap.run(null));
        verifyNoInteractions(passwordEncoder);
    }

    private SuperAdminBootstrap bootstrap(String username, String password, String email, String fullName) {
        return new SuperAdminBootstrap(userRepository, passwordEncoder, passwordPolicyValidator, environment,
                username, password, email, fullName);
    }
}
