package com.smartcbwtf.dto.admin;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request DTO for onboarding a new tenant (facility).
 */
public record OnboardTenantRequest(
        @NotBlank(message = "Code is required") @Size(min = 2, max = 50, message = "Code must be 2-50 characters") @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code must be uppercase alphanumeric with dashes/underscores") String code,

        @NotBlank(message = "Name is required") @Size(min = 2, max = 255, message = "Name must be 2-255 characters") String name,

        @NotBlank(message = "Address is required") String address,

        @Email(message = "Invalid email format") String contactEmail,

        @Pattern(regexp = "^[0-9+\\-\\s]+$", message = "Invalid phone format") String contactPhone,

        Double gpsLat,
        Double gpsLon,
        Integer geofenceRadiusM,

        @NotNull(message = "Subscription plan is required") String subscriptionPlan,

        @PositiveOrZero(message = "Trial days must be 0 or positive") Integer trialDays,

        // Initial admin user
        @NotBlank(message = "Admin email is required") @Email(message = "Invalid admin email format") String adminEmail,

        @NotBlank(message = "Admin name is required") String adminName) {
}
