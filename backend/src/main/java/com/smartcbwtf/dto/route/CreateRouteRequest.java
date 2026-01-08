package com.smartcbwtf.dto.route;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new route.
 */
public record CreateRouteRequest(
                @NotBlank(message = "Route name is required") @Size(max = 100, message = "Route name must be 100 characters or less") String name,

                String description,

                @Size(max = 7, message = "Color must be a valid hex code") String color,

                @Min(value = 1, message = "Completion days must be at least 1") Integer completionDays) {
}
