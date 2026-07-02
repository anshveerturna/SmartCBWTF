package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordChangeValidationContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void selfServicePasswordChangeEndpointsValidateRequestBodies() throws NoSuchMethodException {
        for (PasswordRequestCase requestCase : passwordRequestCases()) {
            Method method = requestCase.controllerClass()
                    .getDeclaredMethod("changePassword", requestCase.requestClass());
            Parameter parameter = method.getParameters()[0];

            assertTrue(parameter.isAnnotationPresent(Valid.class),
                    requestCase.requestClass().getSimpleName() + " must be validated");
            assertTrue(parameter.isAnnotationPresent(RequestBody.class),
                    requestCase.requestClass().getSimpleName() + " must remain a request body");
        }
    }

    @Test
    void passwordChangeRequestsRequireNonBlankBoundedFields() throws NoSuchFieldException {
        for (PasswordRequestCase requestCase : passwordRequestCases()) {
            assertConstrained(requestCase.requestClass(), "currentPassword");
            assertConstrained(requestCase.requestClass(), "newPassword");
            assertFieldViolation(requestCase.blankRequest(), "currentPassword");
            assertFieldViolation(requestCase.blankRequest(), "newPassword");
            assertFieldViolation(requestCase.oversizedRequest(), "newPassword");
        }
    }

    private void assertConstrained(Class<?> requestClass, String componentName) throws NoSuchFieldException {
        var field = requestClass.getDeclaredField(componentName);

        assertTrue(field.isAnnotationPresent(NotBlank.class), componentName + " must be nonblank");
        Size size = field.getAnnotation(Size.class);
        assertEquals(256, size.max(), componentName + " max size");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private List<PasswordRequestCase> passwordRequestCases() {
        String oversized = "x".repeat(257);
        return List.of(
                new PasswordRequestCase(
                        UserController.class,
                        UserController.ChangePasswordRequest.class,
                        new UserController.ChangePasswordRequest("", ""),
                        new UserController.ChangePasswordRequest("current-ok", oversized)),
                new PasswordRequestCase(
                        CbwtfProfileController.class,
                        CbwtfProfileController.PasswordChangeRequest.class,
                        new CbwtfProfileController.PasswordChangeRequest("", ""),
                        new CbwtfProfileController.PasswordChangeRequest("current-ok", oversized)),
                new PasswordRequestCase(
                        HcfProfileController.class,
                        HcfProfileController.PasswordChangeRequest.class,
                        new HcfProfileController.PasswordChangeRequest("", ""),
                        new HcfProfileController.PasswordChangeRequest("current-ok", oversized)),
                new PasswordRequestCase(
                        SuperAdminProfileController.class,
                        SuperAdminProfileController.PasswordChangeRequest.class,
                        new SuperAdminProfileController.PasswordChangeRequest("", ""),
                        new SuperAdminProfileController.PasswordChangeRequest("current-ok", oversized)));
    }

    private record PasswordRequestCase(
            Class<?> controllerClass,
            Class<?> requestClass,
            Object blankRequest,
            Object oversizedRequest) {
    }
}
