package com.smartcbwtf.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing CBWTF admin credentials.
 */
public record ChangeCBWTFCredentialsRequest(
        @NotBlank String newUsername,
        @NotBlank String newPassword) {
}
