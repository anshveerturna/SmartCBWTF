package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void tenantLifecycleEndpointsValidateTypedRequestBodies() throws NoSuchMethodException {
        assertValidRequestBody(AdminController.class.getDeclaredMethod(
                "suspendCBWTF", UUID.class, AdminController.SuspendTenantRequest.class), 1);
        assertValidRequestBody(AdminController.class.getDeclaredMethod(
                "reactivateCBWTF", UUID.class, AdminController.ReactivateTenantRequest.class), 1);
        assertValidRequestBody(AdminController.class.getDeclaredMethod(
                "grantTemporaryAccess", UUID.class, AdminController.TemporaryAccessRequest.class), 1);
        assertValidRequestBody(AdminController.class.getDeclaredMethod(
                "sendTestEmail", AdminController.TestEmailRequest.class), 0);
    }

    @Test
    void featureUpdateMapIsBoundedRequestBody() throws NoSuchMethodException {
        Method method = AdminController.class.getDeclaredMethod("updateFeatures", UUID.class, Map.class);

        assertTrue(method.getParameters()[1].isAnnotationPresent(RequestBody.class));
        Size size = method.getParameters()[1].getAnnotation(Size.class);
        assertEquals(20, size.max());
    }

    @Test
    void tenantLifecycleRequestsBoundDaysAndReasons() throws NoSuchFieldException {
        assertSize(AdminController.SuspendTenantRequest.class, "reason", 1_000);
        assertRange(AdminController.ReactivateTenantRequest.class, "days", 1, 3_650);
        assertSize(AdminController.ReactivateTenantRequest.class, "notes", 1_000);
        assertRange(AdminController.TemporaryAccessRequest.class, "days", 1, 365);
        assertSize(AdminController.TemporaryAccessRequest.class, "reason", 1_000);

        assertViolation(new AdminController.SuspendTenantRequest("x".repeat(1_001)), "reason");
        assertViolation(new AdminController.ReactivateTenantRequest(0, null), "days");
        assertViolation(new AdminController.TemporaryAccessRequest(366, null), "days");
    }

    @Test
    void testEmailRequestRequiresValidEmail() throws NoSuchFieldException {
        assertTrue(AdminController.TestEmailRequest.class.getDeclaredField("email").isAnnotationPresent(NotBlank.class));
        assertTrue(AdminController.TestEmailRequest.class.getDeclaredField("email").isAnnotationPresent(Email.class));
        assertSize(AdminController.TestEmailRequest.class, "email", 255);

        assertViolation(new AdminController.TestEmailRequest("not-an-email"), "email");
    }

    private void assertValidRequestBody(Method method, int parameterIndex) {
        assertTrue(method.getParameters()[parameterIndex].isAnnotationPresent(Valid.class));
        assertTrue(method.getParameters()[parameterIndex].isAnnotationPresent(RequestBody.class));
    }

    private void assertRange(Class<?> requestClass, String field, int min, int max) throws NoSuchFieldException {
        Min minAnnotation = requestClass.getDeclaredField(field).getAnnotation(Min.class);
        Max maxAnnotation = requestClass.getDeclaredField(field).getAnnotation(Max.class);

        assertEquals(min, minAnnotation.value(), field + " min");
        assertEquals(max, maxAnnotation.value(), field + " max");
    }

    private void assertSize(Class<?> requestClass, String field, int max) throws NoSuchFieldException {
        Size size = requestClass.getDeclaredField(field).getAnnotation(Size.class);
        assertEquals(max, size.max(), field + " max size");
    }

    private void assertViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field);
    }
}
