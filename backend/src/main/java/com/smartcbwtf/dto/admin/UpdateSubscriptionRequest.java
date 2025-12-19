package com.smartcbwtf.dto.admin;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request DTO for updating a tenant's subscription.
 */
public record UpdateSubscriptionRequest(
        @NotNull(message = "Plan is required") String plan,

        @NotNull(message = "Expiry date is required") @Future(message = "Expiry date must be in the future") LocalDate expiresAt,

        String notes) {
}
