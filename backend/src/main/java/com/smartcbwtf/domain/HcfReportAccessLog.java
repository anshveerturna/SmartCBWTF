package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * HCF Report Access Log - Audit trail for report access.
 * 
 * Every view or download of HCF reports is logged for regulatory compliance.
 * This is critical during CPCB inspections.
 */
@Entity
@Table(name = "hcf_report_access_log", indexes = {
        @Index(name = "idx_report_access_hcf", columnList = "hcf_id"),
        @Index(name = "idx_report_access_date", columnList = "accessed_at")
})
public class HcfReportAccessLog {

    public enum ReportType {
        MONTHLY,
        YEARLY
    }

    public enum Action {
        VIEW,
        DOWNLOAD
    }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hcf_id")
    private Hcf hcf;

    @ManyToOne
    @JoinColumn(name = "clearance_request_id")
    private DuesClearanceRequest clearanceRequest;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "report_period", nullable = false)
    private String reportPeriod;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt = Instant.now();

    @Column(name = "accessed_by", nullable = false)
    private UUID accessedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "action", nullable = false)
    private String action = Action.VIEW.name();

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Hcf getHcf() {
        return hcf;
    }

    public void setHcf(Hcf hcf) {
        this.hcf = hcf;
    }

    public DuesClearanceRequest getClearanceRequest() {
        return clearanceRequest;
    }

    public void setClearanceRequest(DuesClearanceRequest clearanceRequest) {
        this.clearanceRequest = clearanceRequest;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public ReportType getReportTypeEnum() {
        return reportType != null ? ReportType.valueOf(reportType) : null;
    }

    public void setReportTypeEnum(ReportType type) {
        this.reportType = type != null ? type.name() : null;
    }

    public String getReportPeriod() {
        return reportPeriod;
    }

    public void setReportPeriod(String reportPeriod) {
        this.reportPeriod = reportPeriod;
    }

    public Instant getAccessedAt() {
        return accessedAt;
    }

    public void setAccessedAt(Instant accessedAt) {
        this.accessedAt = accessedAt;
    }

    public UUID getAccessedBy() {
        return accessedBy;
    }

    public void setAccessedBy(UUID accessedBy) {
        this.accessedBy = accessedBy;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Action getActionEnum() {
        return action != null ? Action.valueOf(action) : null;
    }

    public void setActionEnum(Action action) {
        this.action = action != null ? action.name() : null;
    }
}
