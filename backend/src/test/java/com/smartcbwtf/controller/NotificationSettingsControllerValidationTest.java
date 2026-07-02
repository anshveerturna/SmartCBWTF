package com.smartcbwtf.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationSettingsControllerValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void updateSettingsValidatesRequestBody() throws NoSuchMethodException {
        Method method = NotificationSettingsController.class
                .getDeclaredMethod("updateSettings", NotificationSettingsController.UpdateSettingsRequest.class);

        assertTrue(method.getParameters()[0].isAnnotationPresent(Valid.class));
        assertTrue(method.getParameters()[0].isAnnotationPresent(RequestBody.class));
    }

    @Test
    void updateSettingsRequestMatchesUiNumericBounds() throws NoSuchFieldException {
        assertRange("paymentReminderStartDays", 1, 30);
        assertRange("paymentReminderFrequencyDays", 1, 14);
        assertRange("maxOverdueReminders", 1, 10);
        assertRange("agreementExpiryWarningDays", 7, 90);

        assertViolation(new NotificationSettingsController.UpdateSettingsRequest(0, 3, 5, 30),
                "paymentReminderStartDays");
        assertViolation(new NotificationSettingsController.UpdateSettingsRequest(7, 15, 5, 30),
                "paymentReminderFrequencyDays");
        assertViolation(new NotificationSettingsController.UpdateSettingsRequest(7, 3, 11, 30),
                "maxOverdueReminders");
        assertViolation(new NotificationSettingsController.UpdateSettingsRequest(7, 3, 5, 6),
                "agreementExpiryWarningDays");
    }

    private void assertRange(String field, int min, int max) throws NoSuchFieldException {
        Min minAnnotation = NotificationSettingsController.UpdateSettingsRequest.class
                .getDeclaredField(field)
                .getAnnotation(Min.class);
        Max maxAnnotation = NotificationSettingsController.UpdateSettingsRequest.class
                .getDeclaredField(field)
                .getAnnotation(Max.class);

        assertEquals(min, minAnnotation.value(), field + " min");
        assertEquals(max, maxAnnotation.value(), field + " max");
    }

    private void assertViolation(NotificationSettingsController.UpdateSettingsRequest request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> field.equals(violation.getPropertyPath().toString())),
                () -> "Expected validation violation for " + field);
    }
}
