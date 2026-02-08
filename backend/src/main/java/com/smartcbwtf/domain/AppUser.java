package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;
    private String email;
    private String phone;
    private String gender; // MALE, FEMALE, OTHER
    private LocalDate dob;
    private String profilePhotoUrl;

    @Column(nullable = false)
    private String role; // SUPER_ADMIN / CBWTF_ADMIN / HCF_ADMIN / DRIVER / PLANT_OPERATOR

    @ManyToOne
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @ManyToOne
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "force_password_change")
    private boolean forcePasswordChange = false;

    // Login security tracking
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "must_change_password")
    private boolean mustChangePassword = false;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // GPS tracking fields for quick "online" status
    @Column(name = "last_gps_at")
    private Instant lastGpsAt;

    @Column(name = "last_gps_lat", precision = 10, scale = 7)
    private java.math.BigDecimal lastGpsLat;

    @Column(name = "last_gps_lon", precision = 10, scale = 7)
    private java.math.BigDecimal lastGpsLon;

    // Admin can request a GPS refresh - Android app checks this timestamp
    @Column(name = "gps_refresh_requested_at")
    private Instant gpsRefreshRequestedAt;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    // getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Aliases for convenience
    public String getName() {
        return fullName;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    // Login security getters/setters
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public void setLastFailedLoginAt(Instant lastFailedLoginAt) {
        this.lastFailedLoginAt = lastFailedLoginAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(Instant passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    // Helper methods for login security
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = Instant.now();
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
    }

    public void lockAccount(int lockoutMinutes) {
        int effectiveMinutes = Math.max(1, lockoutMinutes);
        this.lockedUntil = Instant.now().plusSeconds(effectiveMinutes * 60L);
    }

    public void unlockAccount() {
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = Instant.now();
        resetFailedAttempts();
    }

    // GPS tracking getters/setters
    public Instant getLastGpsAt() {
        return lastGpsAt;
    }

    public void setLastGpsAt(Instant lastGpsAt) {
        this.lastGpsAt = lastGpsAt;
    }

    public java.math.BigDecimal getLastGpsLat() {
        return lastGpsLat;
    }

    public void setLastGpsLat(java.math.BigDecimal lastGpsLat) {
        this.lastGpsLat = lastGpsLat;
    }

    public java.math.BigDecimal getLastGpsLon() {
        return lastGpsLon;
    }

    public void setLastGpsLon(java.math.BigDecimal lastGpsLon) {
        this.lastGpsLon = lastGpsLon;
    }

    public void updateGpsPosition(java.math.BigDecimal lat, java.math.BigDecimal lon) {
        this.lastGpsLat = lat;
        this.lastGpsLon = lon;
        this.lastGpsAt = Instant.now();
    }

    // GPS refresh request getters/setters
    public Instant getGpsRefreshRequestedAt() {
        return gpsRefreshRequestedAt;
    }

    public void setGpsRefreshRequestedAt(Instant gpsRefreshRequestedAt) {
        this.gpsRefreshRequestedAt = gpsRefreshRequestedAt;
    }

    public void requestGpsRefresh() {
        this.gpsRefreshRequestedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
