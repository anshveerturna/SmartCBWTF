package com.smartcbwtf.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordPolicyValidatorTest {

    @Test
    void acceptsPasswordLongerThanLegacyTwelveCharacterLimitByDefault() {
        PasswordPolicyValidator validator = new PasswordPolicyValidator(configService(8, 128, true, true, true));

        PasswordPolicyValidator.ValidationResult result = validator.validate("LongerPassword123!");

        assertTrue(result.isValid(), () -> String.join("; ", result.getViolations()));
    }

    @Test
    void rejectsPasswordLongerThanConfiguredMaximum() {
        PasswordPolicyValidator validator = new PasswordPolicyValidator(configService(8, 16, true, true, true));

        PasswordPolicyValidator.ValidationResult result = validator.validate("LongerPassword123!");

        assertFalse(result.isValid());
        assertTrue(result.getViolations().contains("Password must be at most 16 characters long"));
    }

    @Test
    void policyDescriptionIncludesMaximumLengthAndSpecialRequirement() {
        PasswordPolicyValidator validator = new PasswordPolicyValidator(configService(10, 128, true, true, true));

        String description = validator.getPolicyDescription();

        assertTrue(description.contains("Minimum 10 characters"));
        assertTrue(description.contains("maximum 128 characters"));
        assertTrue(description.contains("special character"));
    }

    private static SystemConfigService configService(
            int minLength,
            int maxLength,
            boolean requireUppercase,
            boolean requireNumber,
            boolean requireSpecial) {
        SystemConfigService configService = mock(SystemConfigService.class);
        when(configService.getInt(eq("security.password_min_length"), anyInt())).thenReturn(minLength);
        when(configService.getInt(eq("security.password_max_length"), anyInt())).thenReturn(maxLength);
        when(configService.getBoolean(eq("security.password_require_uppercase"), anyBoolean()))
                .thenReturn(requireUppercase);
        when(configService.getBoolean(eq("security.password_require_number"), anyBoolean()))
                .thenReturn(requireNumber);
        when(configService.getBoolean(eq("security.password_require_special"), anyBoolean()))
                .thenReturn(requireSpecial);
        return configService;
    }
}
