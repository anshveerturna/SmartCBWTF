package com.smartcbwtf.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Monthly Compliance Report - Immutable snapshot of monthly operations.
 * Generated automatically on 1st of each month at 01:30 AM IST.
 */
@Entity
@Table(name = "monthly_compliance_report")
public class MonthlyComplianceReport {

    public enum Status {
        READY, FLAGGED
    }

    public enum DataCompleteness {
        COMPLETE, PARTIAL
    }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth; // First day of month

    @Column(name = "report_version", nullable = false)
    private Integer reportVersion = 1;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.READY;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_completeness", nullable = false)
    private DataCompleteness dataCompleteness = DataCompleteness.COMPLETE;

    @Column(name = "source_window_from", nullable = false)
    private Instant sourceWindowFrom;

    @Column(name = "source_window_to", nullable = false)
    private Instant sourceWindowTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_json", nullable = false, columnDefinition = "jsonb")
    private String dataJson;

    @Column(name = "pdf_bytes")
    private byte[] pdfBytes;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "created_by", nullable = false)
    private String createdBy = "SYSTEM";

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public LocalDate getReportMonth() {
        return reportMonth;
    }

    public void setReportMonth(LocalDate reportMonth) {
        this.reportMonth = reportMonth;
    }

    public Integer getReportVersion() {
        return reportVersion;
    }

    public void setReportVersion(Integer reportVersion) {
        this.reportVersion = reportVersion;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DataCompleteness getDataCompleteness() {
        return dataCompleteness;
    }

    public void setDataCompleteness(DataCompleteness dataCompleteness) {
        this.dataCompleteness = dataCompleteness;
    }

    public Instant getSourceWindowFrom() {
        return sourceWindowFrom;
    }

    public void setSourceWindowFrom(Instant sourceWindowFrom) {
        this.sourceWindowFrom = sourceWindowFrom;
    }

    public Instant getSourceWindowTo() {
        return sourceWindowTo;
    }

    public void setSourceWindowTo(Instant sourceWindowTo) {
        this.sourceWindowTo = sourceWindowTo;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public void setPdfBytes(byte[] pdfBytes) {
        this.pdfBytes = pdfBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
