package com.smartcbwtf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_client")
public class OAuthClient {
    @Id
    @Column(name = "client_id", length = 120)
    private String clientId;

    @Column(name = "client_secret_hash")
    private String clientSecretHash;

    @Column(nullable = false)
    private String name;

    @Column(name = "redirect_uris", columnDefinition = "TEXT")
    private String redirectUris;

    @Column(name = "allowed_scopes", columnDefinition = "TEXT")
    private String allowedScopes;

    @Column(name = "allowed_grant_types", columnDefinition = "TEXT")
    private String allowedGrantTypes;

    @ManyToOne
    @JoinColumn(name = "service_account_user_id")
    private AppUser serviceAccountUser;

    private boolean active = true;
    private boolean confidential = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getAllowedScopes() {
        return allowedScopes;
    }

    public void setAllowedScopes(String allowedScopes) {
        this.allowedScopes = allowedScopes;
    }

    public String getAllowedGrantTypes() {
        return allowedGrantTypes;
    }

    public void setAllowedGrantTypes(String allowedGrantTypes) {
        this.allowedGrantTypes = allowedGrantTypes;
    }

    public AppUser getServiceAccountUser() {
        return serviceAccountUser;
    }

    public void setServiceAccountUser(AppUser serviceAccountUser) {
        this.serviceAccountUser = serviceAccountUser;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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
}
