package com.smartcbwtf.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only user profile response DTO.
 * 
 * This DTO is intentionally designed for READ-ONLY operations.
 * Profile data is centrally managed at the backend level and
 * cannot be modified through the mobile application.
 */
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dob;
    private String role;
    private UUID facilityId;
    private String facilityName;
    private String profilePhotoUrl;

    // Default constructor
    public UserProfileResponse() {
    }

    // All-args constructor for convenience
    public UserProfileResponse(UUID id, String username, String fullName, String email,
            String phone, String gender, LocalDate dob, String role,
            UUID facilityId, String facilityName, String profilePhotoUrl) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.dob = dob;
        this.role = role;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    // Getters only - intentionally no setters to emphasize read-only nature
    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public String getRole() {
        return role;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    // Builder-style factory method
    public static UserProfileResponse fromUser(com.smartcbwtf.domain.AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getDob(),
                user.getRole(),
                user.getFacility() != null ? user.getFacility().getId() : null,
                user.getFacility() != null ? user.getFacility().getName() : null,
                user.getProfilePhotoUrl());
    }
}
