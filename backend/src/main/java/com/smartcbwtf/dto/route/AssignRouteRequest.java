package com.smartcbwtf.dto.route;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for assigning a route to staff.
 */
public record AssignRouteRequest(
        @NotNull(message = "Staff ID is required") UUID staffId,

        UUID vehicleId // Optional
) {
}
