package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for subscription and tenant lifecycle changes.
 * Extensible design: entity_type allows future auditing of USER, CONFIG, etc.
 */
@Entity
@Table(name = "subscription_audit", indexes = {
        @Index(name = "idx_audit_entity_time", columnList = "entity_type, entity_id, created_at DESC")
})
public class SubscriptionAudit {

    // Audit action types
    public enum Action {
        CREATED, // Tenant onboarded
        PLAN_CHANGED, // Subscription plan changed
        RENEWED, // Subscription renewed
        EXPIRED, // Subscription expired (auto or manual)
        SUSPENDED, // Tenant suspended by admin
        REACTIVATED, // Tenant reactivated
        CANCELLED, // Subscription cancelled
        TEMP_ACCESS_GRANTED, // Temporary access granted
        FEATURE_CHANGED, // Feature flag toggled
        USER_CREATED, // Admin user created for tenant
        USER_UPDATED, // User details updated
        USER_DISABLED, // User disabled
        USER_ENABLED, // User enabled
        PASSWORD_RESET_FORCED, // Password reset required
        PASSWORD_CHANGED, // Password change completed
        ACCESS_REVOKED, // All access revoked immediately
        STATUS_CHANGED, // General status change
        ACCOUNT_LOCKED, // Account locked due to failed login attempts
        ACCOUNT_UNLOCKED, // Account unlocked by admin
        FEATURE_DEFAULTS_APPLIED, // Default feature flags applied on creation
        // SuperAdmin profile management
        LOGIN_SUCCESS, // Successful login
        LOGIN_FAILURE, // Failed login attempt
        PROFILE_UPDATED, // Profile details updated
        PHOTO_UPDATED, // Profile photo changed
        SUPERADMIN_CREATED, // New SuperAdmin created
        SUPERADMIN_UPDATED, // SuperAdmin updated
        SUPERADMIN_DISABLED, // SuperAdmin disabled
        SUPERADMIN_ENABLED, // SuperAdmin enabled
        LOGOUT // User logout
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType = "FACILITY";

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    // Legacy column for V8 migration compatibility - must be set for FACILITY
    // entities
    @Column(name = "facility_id")
    private UUID facilityId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "performed_by_username", length = 100)
    private String performedByUsername;

    @Column(name = "performed_by_role", length = 30)
    private String performedByRole;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Constructors
    public SubscriptionAudit() {
    }

    public static SubscriptionAudit create(
            String entityType,
            UUID entityId,
            Action action,
            String oldValue,
            String newValue,
            UUID performedBy,
            String performedByUsername,
            String performedByRole,
            String notes) {
        SubscriptionAudit audit = new SubscriptionAudit();
        audit.entityType = entityType;
        audit.entityId = entityId;
        audit.action = action.name();
        audit.oldValue = oldValue;
        audit.newValue = newValue;
        audit.performedBy = performedBy;
        audit.performedByUsername = performedByUsername;
        audit.performedByRole = performedByRole;
        audit.notes = notes;
        return audit;
    }

    public static SubscriptionAudit forFacility(
            UUID facilityId,
            Action action,
            String oldValue,
            String newValue,
            UUID performedBy,
            String performedByUsername,
            String performedByRole,
            String notes) {
        SubscriptionAudit audit = create("FACILITY", facilityId, action, oldValue, newValue,
                performedBy, performedByUsername, performedByRole, notes);
        // Also set facilityId for V8 migration schema compatibility
        audit.facilityId = facilityId;
        return audit;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Action getActionEnum() {
        return Action.valueOf(action);
    }

    public void setActionEnum(Action action) {
        this.action = action.name();
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(UUID performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByUsername() {
        return performedByUsername;
    }

    public void setPerformedByUsername(String performedByUsername) {
        this.performedByUsername = performedByUsername;
    }

    public String getPerformedByRole() {
        return performedByRole;
    }

    public void setPerformedByRole(String performedByRole) {
        this.performedByRole = performedByRole;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
