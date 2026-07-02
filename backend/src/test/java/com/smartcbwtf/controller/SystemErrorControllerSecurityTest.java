package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.SystemError;
import com.smartcbwtf.repository.SystemErrorRepository;
import com.smartcbwtf.service.SystemErrorService;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemErrorControllerSecurityTest {

    @Mock
    private SystemErrorService errorService;
    @Mock
    private SystemErrorRepository errorRepository;

    private SystemErrorController controller;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        controller = new SystemErrorController(errorService, errorRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void hcfAdminReportsAgainstAuthenticatedHcf() {
        UUID userId = UUID.randomUUID();
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(userId, null, hcfId, "HCF_ADMIN", "hcf"));
        SystemError saved = savedError();
        when(errorService.reportError(
                eq("Portal issue"),
                eq("Screen failed"),
                eq("PORTAL"),
                eq("WARNING"),
                eq(userId),
                isNull(),
                eq(hcfId))).thenReturn(saved);

        controller.reportError(new SystemErrorController.ReportErrorRequest(
                "Portal issue",
                "Screen failed",
                "PORTAL",
                "WARNING",
                null));

        verify(errorService).reportError(
                "Portal issue",
                "Screen failed",
                "PORTAL",
                "WARNING",
                userId,
                null,
                hcfId);
    }

    @Test
    void hcfAdminCannotReportAgainstAnotherHcf() {
        UUID hcfId = UUID.randomUUID();
        TenantContext.set(new TenantContext.TenantInfo(UUID.randomUUID(), null, hcfId, "HCF_ADMIN", "hcf"));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.reportError(new SystemErrorController.ReportErrorRequest(
                        "Portal issue",
                        "Screen failed",
                        "PORTAL",
                        "WARNING",
                        UUID.randomUUID())));

        assertEquals(403, thrown.getStatusCode().value());
        verifyNoInteractions(errorService);
    }

    @Test
    void errorReportRequestsHaveBoundedValidation() {
        assertFieldViolation(new SystemErrorController.ReportErrorRequest(
                "",
                "Screen failed",
                "PORTAL",
                "WARNING",
                null), "title");
        assertFieldViolation(new SystemErrorController.ReportErrorRequest(
                "x".repeat(256),
                "Screen failed",
                "PORTAL",
                "WARNING",
                null), "title");
        assertFieldViolation(new SystemErrorController.ReportErrorRequest(
                "Portal issue",
                "x".repeat(5001),
                "PORTAL",
                "WARNING",
                null), "description");
        assertFieldViolation(new SystemErrorController.ReportErrorRequest(
                "Portal issue",
                "Screen failed",
                "x".repeat(51),
                "WARNING",
                null), "component");
        assertFieldViolation(new SystemErrorController.ReportErrorRequest(
                "Portal issue",
                "Screen failed",
                "PORTAL",
                "EMERGENCY",
                null), "severity");
    }

    @Test
    void mobileErrorRequestsHaveBoundedValidation() {
        assertFieldViolation(new SystemErrorController.MobileErrorRequest("", "stack", "device"), "title");
        assertFieldViolation(new SystemErrorController.MobileErrorRequest("x".repeat(256), "stack", "device"),
                "title");
        assertFieldViolation(new SystemErrorController.MobileErrorRequest("Crash", "x".repeat(20001), "device"),
                "stackTrace");
        assertFieldViolation(new SystemErrorController.MobileErrorRequest("Crash", "stack", "x".repeat(2001)),
                "deviceInfo");
    }

    @Test
    void adminStatusAndResolutionRequestsHaveBoundedValidation() {
        assertFieldViolation(new SystemErrorController.UpdateStatusRequest(""), "status");
        assertFieldViolation(new SystemErrorController.UpdateStatusRequest("DONE"), "status");
        assertTrue(validator.validate(new SystemErrorController.UpdateStatusRequest("resolved")).isEmpty());
        assertFieldViolation(new SystemErrorController.ResolveErrorRequest("x".repeat(4001)), "notes");
    }

    @Test
    void mutatingErrorManagementEndpointsValidateRequestBodies() throws NoSuchMethodException {
        assertValidatedRequestBody(SystemErrorController.class, "updateStatus", UUID.class,
                SystemErrorController.UpdateStatusRequest.class);
        assertValidatedRequestBody(SystemErrorController.class, "resolveError", UUID.class,
                SystemErrorController.ResolveErrorRequest.class);
    }

    @Test
    void listErrorsTrimsAndNormalizesFiltersBeforeRepositoryCall() {
        when(errorRepository.findByStatusAndSeverityOrderByCreatedAtDesc(eq("OPEN"), eq("WARNING"),
                any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<SystemErrorController.SystemErrorDTO>> response = controller.listErrors(
                " open ", " warning ", null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        verify(errorRepository).findByStatusAndSeverityOrderByCreatedAtDesc(eq("OPEN"), eq("WARNING"),
                any(Pageable.class));
    }

    @Test
    void listErrorsTrimsSearchBeforeRepositoryCall() {
        when(errorRepository.searchByTitleOrDescription(eq("portal"), any(Pageable.class))).thenReturn(Page.empty());

        ResponseEntity<Page<SystemErrorController.SystemErrorDTO>> response = controller.listErrors(
                null, null, "  portal  ", 0, 20);

        assertEquals(200, response.getStatusCode().value());
        verify(errorRepository).searchByTitleOrDescription(eq("portal"), any(Pageable.class));
    }

    @Test
    void listErrorsRejectsUnsupportedFilterBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.listErrors("DONE", null, null, 0, 20));

        verifyNoInteractions(errorRepository);
    }

    @Test
    void listErrorsRejectsOversizedSearchBeforeRepositoryCall() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.listErrors(null, null, "x".repeat(121), 0, 20));

        verifyNoInteractions(errorRepository);
    }

    private SystemError savedError() {
        SystemError error = new SystemError();
        error.setId(UUID.randomUUID());
        error.setTitle("Portal issue");
        error.setStatus("OPEN");
        return error;
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private void assertValidatedRequestBody(Class<?> controllerClass, String methodName,
            Class<?> idType, Class<?> requestType) throws NoSuchMethodException {
        Method method = controllerClass.getDeclaredMethod(methodName, idType, requestType);
        Parameter parameter = method.getParameters()[1];

        assertTrue(parameter.isAnnotationPresent(Valid.class), methodName + " request must be validated");
        assertTrue(parameter.isAnnotationPresent(RequestBody.class), methodName + " request must remain a body");
    }
}
