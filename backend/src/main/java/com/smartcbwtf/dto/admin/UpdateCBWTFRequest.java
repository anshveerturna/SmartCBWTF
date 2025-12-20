package com.smartcbwtf.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating CBWTF details.
 */
public record UpdateCBWTFRequest(
                @NotBlank String name,
                @NotBlank String address,
                String ownerName,
                String contactEmail,
                String contactPhone,
                Double gpsLat,
                Double gpsLon,
                Integer geofenceRadiusM,
                String panNumber,
                String gstNumber,
                String aadharNumber) {
}
