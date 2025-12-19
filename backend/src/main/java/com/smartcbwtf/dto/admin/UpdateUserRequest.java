package com.smartcbwtf.dto.admin;

import jakarta.validation.constraints.Email;
import java.util.UUID;

/**
 * Request DTO for updating a user via SuperAdmin portal.
 */
public record UpdateUserRequest(
        String fullName,

        @Email(message = "Invalid email format") String email,

        String phone,

        String role,

        UUID cbwtfId,

        UUID hcfId,

        Boolean active) {
}
