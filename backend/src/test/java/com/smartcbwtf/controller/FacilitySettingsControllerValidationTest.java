package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacilitySettingsControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void updateAgreementTermsValidatesTypedRequestBody() throws NoSuchMethodException, NoSuchFieldException {
        Method method = FacilitySettingsController.class.getDeclaredMethod(
                "updateAgreementTerms",
                FacilitySettingsController.AgreementTermsRequest.class,
                jakarta.servlet.http.HttpServletRequest.class);

        assertTrue(method.getParameters()[0].isAnnotationPresent(Valid.class));
        assertTrue(method.getParameters()[0].isAnnotationPresent(RequestBody.class));

        Size size = FacilitySettingsController.AgreementTermsRequest.class
                .getDeclaredField("termsTemplate")
                .getAnnotation(Size.class);
        assertEquals(20_000, size.max());

        assertTrue(validator.validate(new FacilitySettingsController.AgreementTermsRequest("x".repeat(20_001)))
                .stream()
                .anyMatch(violation -> "termsTemplate".equals(violation.getPropertyPath().toString())));
    }
}
