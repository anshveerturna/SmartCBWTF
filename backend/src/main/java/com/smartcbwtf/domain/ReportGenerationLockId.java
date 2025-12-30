package com.smartcbwtf.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite key for ReportGenerationLock.
 */
public class ReportGenerationLockId implements Serializable {
    private String reportType;
    private String periodKey;
    private UUID facilityId;

    public ReportGenerationLockId() {
    }

    public ReportGenerationLockId(String reportType, String periodKey, UUID facilityId) {
        this.reportType = reportType;
        this.periodKey = periodKey;
        this.facilityId = facilityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReportGenerationLockId that = (ReportGenerationLockId) o;
        return Objects.equals(reportType, that.reportType) &&
                Objects.equals(periodKey, that.periodKey) &&
                Objects.equals(facilityId, that.facilityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportType, periodKey, facilityId);
    }

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
}
