package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Facility notification settings.
 * CBWTF admins can configure reminder behavior.
 */
@Entity
@Table(name = "facility_notification_settings")
public class FacilityNotificationSettings {

    @Id
    @Column(name = "facility_id")
    private UUID facilityId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(name = "payment_reminder_start_days", nullable = false)
    private Integer paymentReminderStartDays = 7;

    @Column(name = "payment_reminder_frequency_days", nullable = false)
    private Integer paymentReminderFrequencyDays = 3;

    @Column(name = "max_overdue_reminders", nullable = false)
    private Integer maxOverdueReminders = 5;

    @Column(name = "agreement_expiry_warning_days", nullable = false)
    private Integer agreementExpiryWarningDays = 30;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Getters and setters
    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Integer getPaymentReminderStartDays() {
        return paymentReminderStartDays;
    }

    public void setPaymentReminderStartDays(Integer paymentReminderStartDays) {
        this.paymentReminderStartDays = paymentReminderStartDays;
    }

    public Integer getPaymentReminderFrequencyDays() {
        return paymentReminderFrequencyDays;
    }

    public void setPaymentReminderFrequencyDays(Integer paymentReminderFrequencyDays) {
        this.paymentReminderFrequencyDays = paymentReminderFrequencyDays;
    }

    public Integer getMaxOverdueReminders() {
        return maxOverdueReminders;
    }

    public void setMaxOverdueReminders(Integer maxOverdueReminders) {
        this.maxOverdueReminders = maxOverdueReminders;
    }

    public Integer getAgreementExpiryWarningDays() {
        return agreementExpiryWarningDays;
    }

    public void setAgreementExpiryWarningDays(Integer agreementExpiryWarningDays) {
        this.agreementExpiryWarningDays = agreementExpiryWarningDays;
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

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
