package com.smartcbwtf.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates passwords against configurable security policies.
 * All policy values are read from SystemConfigService.
 */
@Component
public class PasswordPolicyValidator {

    private final SystemConfigService configService;

    // Precompiled patterns for efficiency
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    public PasswordPolicyValidator(SystemConfigService configService) {
        this.configService = configService;
    }

    /**
     * Validate a password against all configured policies.
     * 
     * @param password The password to validate
     * @return ValidationResult containing success status and any violation messages
     */
    public ValidationResult validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            violations.add("Password cannot be empty");
            return new ValidationResult(false, violations);
        }

        // Get config values
        int minLength = configService.getInt("security.password_min_length", 8);
        int maxLength = configService.getInt("security.password_max_length", 12);
        boolean requireUppercase = configService.getBoolean("security.password_require_uppercase", true);
        boolean requireNumber = configService.getBoolean("security.password_require_number", true);
        boolean requireSpecial = configService.getBoolean("security.password_require_special", true);

        // Validate minimum length
        if (password.length() < minLength) {
            violations.add("Password must be at least " + minLength + " characters long");
        }

        // Validate maximum length
        if (password.length() > maxLength) {
            violations.add("Password must be at most " + maxLength + " characters long");
        }

        // Validate uppercase requirement
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one uppercase letter");
        }

        // Validate lowercase (always required for basic security)
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one lowercase letter");
        }

        // Validate number requirement
        if (requireNumber && !NUMBER_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one number");
        }

        // Validate special character requirement
        if (requireSpecial && !SPECIAL_PATTERN.matcher(password).find()) {
            violations.add("Password must contain at least one special character (!@#$%^&*...)");
        }

        return new ValidationResult(violations.isEmpty(), violations);
    }

    /**
     * Validate password and throw exception if invalid.
     * 
     * @param password The password to validate
     * @throws IllegalArgumentException if password doesn't meet policy
     */
    public void validateOrThrow(String password) {
        ValidationResult result = validate(password);
        if (!result.isValid()) {
            throw new IllegalArgumentException(
                    "Password policy violation: " + String.join("; ", result.getViolations()));
        }
    }

    /**
     * Get a human-readable description of the current password policy.
     */
    public String getPolicyDescription() {
        int minLength = configService.getInt("security.password_min_length", 8);
        boolean requireUppercase = configService.getBoolean("security.password_require_uppercase", true);
        boolean requireNumber = configService.getBoolean("security.password_require_number", true);
        boolean requireSpecial = configService.getBoolean("security.password_require_special", false);

        StringBuilder desc = new StringBuilder();
        desc.append("Minimum ").append(minLength).append(" characters");

        List<String> requirements = new ArrayList<>();
        requirements.add("lowercase letter");
        if (requireUppercase)
            requirements.add("uppercase letter");
        if (requireNumber)
            requirements.add("number");
        if (requireSpecial)
            requirements.add("special character");

        if (!requirements.isEmpty()) {
            desc.append(", must contain: ").append(String.join(", ", requirements));
        }

        return desc.toString();
    }

    /**
     * Result of password validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> violations;

        public ValidationResult(boolean valid, List<String> violations) {
            this.valid = valid;
            this.violations = violations;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getViolations() {
            return violations;
        }
    }
}
