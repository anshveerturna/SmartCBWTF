package com.smartcbwtf.dto.admin;

import java.time.Instant;
import java.util.UUID;
import com.smartcbwtf.domain.AppUser;

/**
 * DTO for user management in SuperAdmin portal.
 * Contains user details with associated CBWTF and HCF info.
 */
public record UserManagementDTO(
        UUID id,
        String username,
        String fullName,
        String email,
        String phone,
        String role,
        UUID cbwtfId,
        String cbwtfCode,
        String cbwtfName,
        UUID hcfId,
        String hcfName,
        boolean active,
        boolean forcePasswordChange,
        Instant createdAt,
        Instant updatedAt) {
    public static UserManagementDTO from(AppUser user) {
        return new UserManagementDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getFacility() != null ? user.getFacility().getId() : null,
                user.getFacility() != null ? user.getFacility().getCode() : null,
                user.getFacility() != null ? user.getFacility().getName() : null,
                user.getHcf() != null ? user.getHcf().getId() : null,
                user.getHcf() != null ? user.getHcf().getName() : null,
                user.isActive(),
                user.isForcePasswordChange(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
