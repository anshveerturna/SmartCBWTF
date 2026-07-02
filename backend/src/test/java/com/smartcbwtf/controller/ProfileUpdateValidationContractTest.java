package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileUpdateValidationContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void selfServiceProfileUpdateEndpointsValidateRequestBodies() throws NoSuchMethodException {
        for (ProfileRequestCase requestCase : profileRequestCases()) {
            Method method = requestCase.controllerClass()
                    .getDeclaredMethod("updateMyProfile", requestCase.requestClass());
            Parameter parameter = method.getParameters()[0];

            assertTrue(parameter.isAnnotationPresent(Valid.class),
                    requestCase.requestClass().getSimpleName() + " must be validated");
            assertTrue(parameter.isAnnotationPresent(RequestBody.class),
                    requestCase.requestClass().getSimpleName() + " must remain a request body");
        }
    }

    @Test
    void profileUpdateRequestsBoundIdentityFields() throws NoSuchFieldException {
        for (ProfileRequestCase requestCase : profileRequestCases()) {
            assertSize(requestCase.requestClass(), "fullName", 120);
            assertSize(requestCase.requestClass(), "email", 180);
            assertSize(requestCase.requestClass(), "phone", 20);
            assertTrue(requestCase.requestClass().getDeclaredField("email").isAnnotationPresent(Email.class));
            assertTrue(requestCase.requestClass().getDeclaredField("phone").isAnnotationPresent(Pattern.class));

            assertFieldViolation(requestCase.oversizedNameRequest(), "fullName");
            assertFieldViolation(requestCase.invalidEmailRequest(), "email");
            assertFieldViolation(requestCase.invalidPhoneRequest(), "phone");
            assertFalse(validator.validate(requestCase.validBlankPhoneRequest()).stream()
                    .anyMatch(violation -> "phone".equals(violation.getPropertyPath().toString())));
        }
    }

    private void assertSize(Class<?> requestClass, String componentName, int max) throws NoSuchFieldException {
        Size size = requestClass.getDeclaredField(componentName).getAnnotation(Size.class);
        assertEquals(max, size.max(), componentName + " max size");
    }

    private void assertFieldViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field + " on " + request.getClass().getSimpleName());
    }

    private List<ProfileRequestCase> profileRequestCases() {
        String oversizedName = "x".repeat(121);
        return List.of(
                new ProfileRequestCase(
                        CbwtfProfileController.class,
                        CbwtfProfileController.ProfileUpdateRequest.class,
                        new CbwtfProfileController.ProfileUpdateRequest(oversizedName, null, null),
                        new CbwtfProfileController.ProfileUpdateRequest(null, "not-an-email", null),
                        new CbwtfProfileController.ProfileUpdateRequest(null, null, "DROP TABLE"),
                        new CbwtfProfileController.ProfileUpdateRequest(null, null, "   ")),
                new ProfileRequestCase(
                        HcfProfileController.class,
                        HcfProfileController.ProfileUpdateRequest.class,
                        new HcfProfileController.ProfileUpdateRequest(oversizedName, null, null),
                        new HcfProfileController.ProfileUpdateRequest(null, "not-an-email", null),
                        new HcfProfileController.ProfileUpdateRequest(null, null, "DROP TABLE"),
                        new HcfProfileController.ProfileUpdateRequest(null, null, "   ")),
                new ProfileRequestCase(
                        SuperAdminProfileController.class,
                        SuperAdminProfileController.ProfileUpdateRequest.class,
                        new SuperAdminProfileController.ProfileUpdateRequest(oversizedName, null, null),
                        new SuperAdminProfileController.ProfileUpdateRequest(null, "not-an-email", null),
                        new SuperAdminProfileController.ProfileUpdateRequest(null, null, "DROP TABLE"),
                        new SuperAdminProfileController.ProfileUpdateRequest(null, null, "   ")));
    }

    private record ProfileRequestCase(
            Class<?> controllerClass,
            Class<?> requestClass,
            Object oversizedNameRequest,
            Object invalidEmailRequest,
            Object invalidPhoneRequest,
            Object validBlankPhoneRequest) {
    }
}
