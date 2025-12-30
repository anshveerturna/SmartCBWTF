package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Report Generation Lock - Prevents duplicate report generation.
 * Similar to billing_lock pattern.
 */
@Entity
@Table(name = "report_generation_lock")
@IdClass(ReportGenerationLockId.class)
public class ReportGenerationLock {

    @Id
    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Id
    @Column(name = "period_key", nullable = false, length = 20)
    private String periodKey;

    @Id
    @Column(name = "facility_id", nullable = false)
    private UUID facilityId;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt = Instant.now();

    // Getters and setters
    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(UUID facilityId) {
        this.facilityId = facilityId;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }
}
