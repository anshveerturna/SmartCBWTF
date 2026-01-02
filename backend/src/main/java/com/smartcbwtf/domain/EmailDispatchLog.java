package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Email dispatch log - IMMUTABLE audit trail.
 * Idempotency enforced via UNIQUE(event_id, template_code).
 */
@Entity
@Table(name = "email_dispatch_log")
public class EmailDispatchLog {

    public enum Status {
        SENT, FAILED, SKIPPED
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(name = "template_version", nullable = false)
    private Integer templateVersion = 1;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "cc_email")
    private String ccEmail;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "reminder_sequence", nullable = false)
    private Integer reminderSequence = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "body_snapshot", columnDefinition = "TEXT")
    private String bodySnapshot;

    @Column(name = "placeholders_snapshot", columnDefinition = "JSONB")
    private String placeholdersSnapshot;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(Integer templateVersion) {
        this.templateVersion = templateVersion;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getCcEmail() {
        return ccEmail;
    }

    public void setCcEmail(String ccEmail) {
        this.ccEmail = ccEmail;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
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

    public Integer getReminderSequence() {
        return reminderSequence;
    }

    public void setReminderSequence(Integer reminderSequence) {
        this.reminderSequence = reminderSequence;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getBodySnapshot() {
        return bodySnapshot;
    }

    public void setBodySnapshot(String bodySnapshot) {
        this.bodySnapshot = bodySnapshot;
    }

    public String getPlaceholdersSnapshot() {
        return placeholdersSnapshot;
    }

    public void setPlaceholdersSnapshot(String placeholdersSnapshot) {
        this.placeholdersSnapshot = placeholdersSnapshot;
    }
}
