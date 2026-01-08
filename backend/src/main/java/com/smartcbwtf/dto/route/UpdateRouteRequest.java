package com.smartcbwtf.dto.route;

import com.smartcbwtf.domain.enums.RouteStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing route.
 */
public record UpdateRouteRequest(
                @Size(max = 100, message = "Route name must be 100 characters or less") String name,

                String description,

                @Size(max = 7, message = "Color must be a valid hex code") String color,

                RouteStatus status,

                @Min(value = 1, message = "Completion days must be at least 1") Integer completionDays) {
}
