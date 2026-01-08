package com.smartcbwtf.dto.route;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for setting route waypoints.
 * HCF IDs should be in the desired order (1, 2, 3, ...).
 */
public record SetWaypointsRequest(
        @NotEmpty(message = "At least one HCF must be specified") List<UUID> hcfIds) {
}
