package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.AppUser;
import com.smartcbwtf.domain.SubscriptionAudit;
import com.smartcbwtf.dto.admin.UserManagementDTO;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.repository.HcfRepository;
import com.smartcbwtf.repository.SubscriptionAuditRepository;
import com.smartcbwtf.service.EmailService;
import com.smartcbwtf.service.PasswordPolicyValidator;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerValidationTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private HcfRepository hcfRepository;
    @Mock
    private SubscriptionAuditRepository auditRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private EmailService emailService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private UserManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new UserManagementController(userRepository, facilityRepository, hcfRepository,
                auditRepository, passwordEncoder, passwordPolicyValidator, emailService);
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, null, "SUPER_ADMIN", "root"));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void adminUserMutationEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedBody("disableUser", UUID.class, UserManagementController.DisableUserRequest.class);
        assertValidatedBody("changePassword", UUID.class,
                UserManagementController.ChangeManagedUserPasswordRequest.class);
    }

    @Test
    void requestBodiesBoundReasonAndPasswordFields() {
        assertFieldViolation(new UserManagementController.DisableUserRequest("x".repeat(1001)), "reason");
        assertFieldViolation(new UserManagementController.ChangeManagedUserPasswordRequest(""), "newPassword");
        assertFieldViolation(
                new UserManagementController.ChangeManagedUserPasswordRequest("x".repeat(257)),
                "newPassword");
        assertTrue(validator.validate(new UserManagementController.DisableUserRequest(null)).isEmpty());
    }

    @Test
    void disableUserNormalizesReasonBeforeAuditSave() {
        UUID userId = UUID.randomUUID();
        AppUser user = appUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<SubscriptionAudit> audit = ArgumentCaptor.forClass(SubscriptionAudit.class);

        ResponseEntity<?> response = controller.disableUser(userId,
                new UserManagementController.DisableUserRequest("  offboarding complete  "));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(false, user.isActive());
        verify(auditRepository).save(audit.capture());
        assertEquals("offboarding complete", audit.getValue().getNotes());
    }

    @Test
    void changePasswordRejectsMissingPasswordBeforePolicyOrRepositoryAccess() {
        ResponseEntity<?> response = controller.changePassword(UUID.randomUUID(), null);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(passwordPolicyValidator);
        verify(userRepository, never()).findById(any(UUID.class));
    }

    @Test
    void listUsersTrimsSearchBeforeRepositoryCall() {
        when(userRepository.searchUsers(eq("driver"), any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<UserManagementDTO>> response = controller.listUsers(null, null, null, "  driver  ", 0,
                20);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository).searchUsers(eq("driver"), any(Pageable.class));
    }

    @Test
    void listUsersTrimsRoleBeforeRepositoryCall() {
        when(userRepository.findByRole(eq("CBWTF_ADMIN"), any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<UserManagementDTO>> response = controller.listUsers(null, " CBWTF_ADMIN ", null, null, 0,
                20);

        assertEquals(200, response.getStatusCode().value());
        verify(userRepository).findByRole(eq("CBWTF_ADMIN"), any(Pageable.class));
    }

    @Test
    void listUsersRejectsOversizedSearchBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.listUsers(null, null, null, "x".repeat(121), 0, 20));

        verifyNoInteractions(userRepository);
    }

    private void assertValidatedBody(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = UserManagementController.class.getDeclaredMethod(methodName, parameterTypes);
        Parameter parameter = method.getParameters()[method.getParameterCount() - 1];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request body must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " must remain a request body");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private static AppUser appUser(UUID id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("managed-user");
        user.setFullName("Managed User");
        user.setRole("CBWTF_ADMIN");
        user.setActive(true);
        return user;
    }
}
