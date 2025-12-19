package com.smartcbwtf.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Request DTO for creating a new user via SuperAdmin portal.
 */
public record CreateUserRequest(
        @NotBlank(message = "Username is required") String username,

        @NotBlank(message = "Full name is required") String fullName,

        @Email(message = "Invalid email format") String email,

        String phone,

        @NotBlank(message = "Role is required") String role,

        UUID cbwtfId,

        UUID hcfId) {
}
