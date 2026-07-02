package com.smartcbwtf.controller;

import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.SubscriptionAudit;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.service.PasswordPolicyValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SuperAdminUserControllerTest {

    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final PasswordPolicyValidator passwordPolicyValidator = mock(PasswordPolicyValidator.class);
    private final SubscriptionAuditRepository auditRepository = mock(SubscriptionAuditRepository.class);
    private final SuperAdminUserController controller = new SuperAdminUserController(
            userRepository,
            passwordEncoder,
            passwordPolicyValidator,
            auditRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSuperAdminNormalizesProfileFieldsBeforePersisting() {
        when(userRepository.findByUsername("root.admin")).thenReturn(Optional.empty());
        when(passwordPolicyValidator.validate("Str0ng@1"))
                .thenReturn(new PasswordPolicyValidator.ValidationResult(true, List.of()));
        when(passwordEncoder.encode("Str0ng@1")).thenReturn("encoded-password");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ResponseEntity<?> response = controller.createSuperAdmin(new SuperAdminUserController.CreateSuperAdminRequest(
                "root.admin",
                "  Root\nAdmin\tOne  ",
                "",
                "",
                "Str0ng@1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        AppUser saved = userCaptor.getValue();
        assertEquals("root.admin", saved.getUsername());
        assertEquals("Root Admin One", saved.getFullName());
        assertNull(saved.getEmail());
        assertNull(saved.getPhone());
        assertEquals("encoded-password", saved.getPasswordHash());
        assertEquals("SUPER_ADMIN", saved.getRole());
        assertTrue(saved.isMustChangePassword());
        assertTrue(saved.isForcePasswordChange());

        verify(auditRepository).save(any(SubscriptionAudit.class));
    }

    @Test
    void createSuperAdminRejectsWeakPasswordBeforeEncodingOrSaving() {
        when(userRepository.findByUsername("root.admin")).thenReturn(Optional.empty());
        when(passwordPolicyValidator.validate("weak"))
                .thenReturn(new PasswordPolicyValidator.ValidationResult(false, List.of("too weak")));

        ResponseEntity<?> response = controller.createSuperAdmin(new SuperAdminUserController.CreateSuperAdminRequest(
                "root.admin",
                "Root Admin",
                "root@example.com",
                "9999999999",
                "weak"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void listSuperAdminsTrimsSearchBeforeRepositoryCall() {
        when(userRepository.searchByRoleAndUsernameOrEmail(eq("SUPER_ADMIN"), eq("root"), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<Page<SuperAdminUserController.SuperAdminUserDTO>> response = controller.listSuperAdmins(
                "  root  ", null, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRepository).searchByRoleAndUsernameOrEmail(eq("SUPER_ADMIN"), eq("root"), any(Pageable.class));
    }

    @Test
    void listSuperAdminsTrimsStatusBeforeRepositoryCall() {
        when(userRepository.findByRoleAndActive(eq("SUPER_ADMIN"), eq(true), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<Page<SuperAdminUserController.SuperAdminUserDTO>> response = controller.listSuperAdmins(null,
                " ACTIVE ", 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRepository).findByRoleAndActive(eq("SUPER_ADMIN"), eq(true), any(Pageable.class));
    }

    @Test
    void listSuperAdminsRejectsOversizedSearchBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class, () -> controller.listSuperAdmins("x".repeat(121), null, 0, 20));

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateSuperAdminNormalizesOptionalProfileFields() {
        UUID userId = UUID.randomUUID();
        AppUser user = superAdmin(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        ResponseEntity<?> response = controller.updateSuperAdmin(userId,
                new SuperAdminUserController.UpdateSuperAdminRequest(
                        "  Root\nAdmin\tTwo  ",
                        "",
                        ""));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Root Admin Two", user.getFullName());
        assertNull(user.getEmail());
        assertNull(user.getPhone());
        verify(userRepository).save(user);
        verify(auditRepository).save(any(SubscriptionAudit.class));
    }

    @Test
    void forcePasswordResetWithoutProvidedPasswordGeneratesPolicyCompliantTemporaryPassword() {
        UUID userId = UUID.randomUUID();
        AppUser user = superAdmin(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordPolicyValidator.validate(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            List<String> violations = defaultPasswordViolations(password);
            return new PasswordPolicyValidator.ValidationResult(violations.isEmpty(), violations);
        });
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);

        ResponseEntity<?> response = controller.forcePasswordReset(userId, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst("Pragma"));
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        assertTrue(defaultPasswordViolations(generatedPassword).isEmpty());
        assertTrue(generatedPassword.length() <= 12);
        assertTrue(user.isMustChangePassword());
        assertTrue(user.isForcePasswordChange());
        assertNotNull(user.getPasswordChangedAt());
    }

    @Test
    void requestValidationRejectsUnsafeSuperAdminPayloads() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var createViolations = validator.validate(new SuperAdminUserController.CreateSuperAdminRequest(
                "bad username",
                "",
                "not-an-email",
                "abc",
                ""));

        assertTrue(createViolations.stream().anyMatch(v -> "username".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "fullName".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "email".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "phone".contentEquals(v.getPropertyPath().toString())));
        assertTrue(createViolations.stream().anyMatch(v -> "password".contentEquals(v.getPropertyPath().toString())));

        var resetViolations = validator.validate(new SuperAdminUserController.PasswordResetRequest("x".repeat(257)));

        assertTrue(resetViolations.stream().anyMatch(v -> "newPassword".contentEquals(v.getPropertyPath().toString())));
    }

    private static AppUser superAdmin(UUID id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("root.admin");
        user.setFullName("Root Admin");
        user.setEmail("root@example.com");
        user.setPhone("9999999999");
        user.setRole("SUPER_ADMIN");
        user.setActive(true);
        return user;
    }

    private static List<String> defaultPasswordViolations(String password) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            violations.add("Password cannot be empty");
            return violations;
        }
        if (password.length() < 8) {
            violations.add("Password must be at least 8 characters long");
        }
        if (password.length() > 12) {
            violations.add("Password must be at most 12 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            violations.add("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            violations.add("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            violations.add("Password must contain at least one number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            violations.add("Password must contain at least one special character");
        }
        return violations;
    }
}
