package com.smartcbwtf.dto;

/**
 * Login response containing JWT token and user information.
 */
public class AuthLoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String role;
    private boolean mustChangePassword;
    private String fullName;
    private String tenantId;
    private String hcfId;

    // Simple constructor for backward compatibility
    public AuthLoginResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    // Full constructor with all security and context fields
    public AuthLoginResponse(String accessToken, String role, boolean mustChangePassword,
            String fullName, String tenantId, String hcfId) {
        this.accessToken = accessToken;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
        this.fullName = fullName;
        this.tenantId = tenantId;
        this.hcfId = hcfId;
    }

    // Getters and setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHcfId() {
        return hcfId;
    }

    public void setHcfId(String hcfId) {
        this.hcfId = hcfId;
    }
}
