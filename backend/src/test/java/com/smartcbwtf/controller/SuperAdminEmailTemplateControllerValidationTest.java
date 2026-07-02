package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperAdminEmailTemplateControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void templateMutationEndpointsValidateRequestBodies() throws NoSuchMethodException {
        Method update = SuperAdminEmailTemplateController.class.getDeclaredMethod(
                "updateTemplate",
                String.class,
                SuperAdminEmailTemplateController.UpdateTemplateRequest.class,
                java.util.UUID.class);
        assertValidRequestBody(update.getParameters()[1]);

        Method validate = SuperAdminEmailTemplateController.class.getDeclaredMethod(
                "validateTemplate",
                SuperAdminEmailTemplateController.ValidateTemplateRequest.class);
        assertValidRequestBody(validate.getParameters()[0]);
    }

    @Test
    void updateTemplateRequestBoundsSubjectBodyAndPlaceholders() throws NoSuchFieldException {
        Class<?> requestClass = SuperAdminEmailTemplateController.UpdateTemplateRequest.class;

        assertTrue(requestClass.getDeclaredField("subject").isAnnotationPresent(NotBlank.class));
        assertSize(requestClass, "subject", 200);
        assertTrue(requestClass.getDeclaredField("bodyHtml").isAnnotationPresent(NotBlank.class));
        assertSize(requestClass, "bodyHtml", 100_000);
        assertSize(requestClass, "requiredPlaceholders", 50);
        assertSize(requestClass, "optionalPlaceholders", 50);

        assertViolation(new SuperAdminEmailTemplateController.UpdateTemplateRequest(
                "", "<p>{{hcfName}}</p>", List.of("hcfName"), List.of()), "subject");
        assertViolation(new SuperAdminEmailTemplateController.UpdateTemplateRequest(
                "Subject", "", List.of("hcfName"), List.of()), "bodyHtml");
        assertViolation(new SuperAdminEmailTemplateController.UpdateTemplateRequest(
                "Subject", "<p>{{hcfName}}</p>", List.of("bad key"), List.of()), "requiredPlaceholders[0].<list element>");
    }

    @Test
    void validateTemplateRequestBoundsTemplateCode() throws NoSuchFieldException {
        Class<?> requestClass = SuperAdminEmailTemplateController.ValidateTemplateRequest.class;

        assertTrue(requestClass.getDeclaredField("templateCode").isAnnotationPresent(NotBlank.class));
        assertSize(requestClass, "templateCode", 50);
        assertTrue(requestClass.getDeclaredField("templateCode").isAnnotationPresent(Pattern.class));

        assertViolation(new SuperAdminEmailTemplateController.ValidateTemplateRequest(
                "bad-code", "Subject", "<p>{{hcfName}}</p>", List.of("hcfName"), List.of()), "templateCode");
    }

    private void assertValidRequestBody(Parameter parameter) {
        assertTrue(parameter.isAnnotationPresent(Valid.class));
        assertTrue(parameter.isAnnotationPresent(RequestBody.class));
    }

    private void assertSize(Class<?> requestClass, String componentName, int max) throws NoSuchFieldException {
        Size size = requestClass.getDeclaredField(componentName).getAnnotation(Size.class);
        assertEquals(max, size.max(), componentName + " max size");
    }

    private void assertViolation(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field);
    }
}
