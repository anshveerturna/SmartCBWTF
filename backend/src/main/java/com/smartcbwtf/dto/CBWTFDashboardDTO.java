package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * CBWTF Dashboard Metrics DTO.
 * All metrics are tenant-scoped to the requesting CBWTF's facility.
 */
public class CBWTFDashboardDTO {

    // === OVERVIEW METRICS ===
    private long activeAgreements;
    private long totalAgreements;
    private long activeHcfs;
    private long totalBagLabelsIssued;
    private long bagsProcessedToday;
    private long bagsProcessedThisWeek;
    private long bagsProcessedThisMonth;

    // === VEHICLE & STAFF METRICS ===
    private long vehiclesOnline; // GPS events < 15 min
    private long totalVehicles;
    private long staffPresentToday; // Attendance records for today
    private long totalStaff;

    // === FINANCIAL METRICS ===
    private BigDecimal pendingInvoiceAmount;
    private long pendingInvoiceCount;
    private BigDecimal paidInvoiceAmountThisMonth;
    private long paidInvoiceCountThisMonth;
    private BigDecimal totalRevenueAllTime;

    // === HEALTH METRICS ===
    private long agreementsExpiringSoon; // Within 30 days
    private long agreementsWithDuesPending;
    private long agreementsInDispute;
    private long anomalyBagsThisWeek; // MISMATCH or OUT_OF_GEOFENCE

    // === RECENT ACTIVITY ===
    private List<RecentBagEvent> recentBagEvents;
    private List<AgreementSummary> expiringAgreements;

    // === RISK ALERTS ===
    private List<RiskAlert> riskAlerts;

    // === FACILITY INFO ===
    private String facilityName;
    private String subscriptionPlan;
    private Instant subscriptionExpiresAt;
    private long subscriptionDaysLeft;

    // Getters and Setters
    public long getActiveAgreements() {
        return activeAgreements;
    }

    public void setActiveAgreements(long activeAgreements) {
        this.activeAgreements = activeAgreements;
    }

    public long getTotalAgreements() {
        return totalAgreements;
    }

    public void setTotalAgreements(long totalAgreements) {
        this.totalAgreements = totalAgreements;
    }

    public long getActiveHcfs() {
        return activeHcfs;
    }

    public void setActiveHcfs(long activeHcfs) {
        this.activeHcfs = activeHcfs;
    }

    public long getTotalBagLabelsIssued() {
        return totalBagLabelsIssued;
    }

    public void setTotalBagLabelsIssued(long totalBagLabelsIssued) {
        this.totalBagLabelsIssued = totalBagLabelsIssued;
    }

    public long getBagsProcessedToday() {
        return bagsProcessedToday;
    }

    public void setBagsProcessedToday(long bagsProcessedToday) {
        this.bagsProcessedToday = bagsProcessedToday;
    }

    public long getBagsProcessedThisWeek() {
        return bagsProcessedThisWeek;
    }

    public void setBagsProcessedThisWeek(long bagsProcessedThisWeek) {
        this.bagsProcessedThisWeek = bagsProcessedThisWeek;
    }

    public long getBagsProcessedThisMonth() {
        return bagsProcessedThisMonth;
    }

    public void setBagsProcessedThisMonth(long bagsProcessedThisMonth) {
        this.bagsProcessedThisMonth = bagsProcessedThisMonth;
    }

    public BigDecimal getPendingInvoiceAmount() {
        return pendingInvoiceAmount;
    }

    public void setPendingInvoiceAmount(BigDecimal pendingInvoiceAmount) {
        this.pendingInvoiceAmount = pendingInvoiceAmount;
    }

    public long getPendingInvoiceCount() {
        return pendingInvoiceCount;
    }

    public void setPendingInvoiceCount(long pendingInvoiceCount) {
        this.pendingInvoiceCount = pendingInvoiceCount;
    }

    public BigDecimal getPaidInvoiceAmountThisMonth() {
        return paidInvoiceAmountThisMonth;
    }

    public void setPaidInvoiceAmountThisMonth(BigDecimal paidInvoiceAmountThisMonth) {
        this.paidInvoiceAmountThisMonth = paidInvoiceAmountThisMonth;
    }

    public long getPaidInvoiceCountThisMonth() {
        return paidInvoiceCountThisMonth;
    }

    public void setPaidInvoiceCountThisMonth(long paidInvoiceCountThisMonth) {
        this.paidInvoiceCountThisMonth = paidInvoiceCountThisMonth;
    }

    public BigDecimal getTotalRevenueAllTime() {
        return totalRevenueAllTime;
    }

    public void setTotalRevenueAllTime(BigDecimal totalRevenueAllTime) {
        this.totalRevenueAllTime = totalRevenueAllTime;
    }

    public long getAgreementsExpiringSoon() {
        return agreementsExpiringSoon;
    }

    public void setAgreementsExpiringSoon(long agreementsExpiringSoon) {
        this.agreementsExpiringSoon = agreementsExpiringSoon;
    }

    public long getAgreementsWithDuesPending() {
        return agreementsWithDuesPending;
    }

    public void setAgreementsWithDuesPending(long agreementsWithDuesPending) {
        this.agreementsWithDuesPending = agreementsWithDuesPending;
    }

    public long getAgreementsInDispute() {
        return agreementsInDispute;
    }

    public void setAgreementsInDispute(long agreementsInDispute) {
        this.agreementsInDispute = agreementsInDispute;
    }

    public long getAnomalyBagsThisWeek() {
        return anomalyBagsThisWeek;
    }

    public void setAnomalyBagsThisWeek(long anomalyBagsThisWeek) {
        this.anomalyBagsThisWeek = anomalyBagsThisWeek;
    }

    public List<RecentBagEvent> getRecentBagEvents() {
        return recentBagEvents;
    }

    public void setRecentBagEvents(List<RecentBagEvent> recentBagEvents) {
        this.recentBagEvents = recentBagEvents;
    }

    public List<AgreementSummary> getExpiringAgreements() {
        return expiringAgreements;
    }

    public void setExpiringAgreements(List<AgreementSummary> expiringAgreements) {
        this.expiringAgreements = expiringAgreements;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public Instant getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }

    public void setSubscriptionExpiresAt(Instant subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }

    public long getSubscriptionDaysLeft() {
        return subscriptionDaysLeft;
    }

    public void setSubscriptionDaysLeft(long subscriptionDaysLeft) {
        this.subscriptionDaysLeft = subscriptionDaysLeft;
    }

    public long getVehiclesOnline() {
        return vehiclesOnline;
    }

    public void setVehiclesOnline(long vehiclesOnline) {
        this.vehiclesOnline = vehiclesOnline;
    }

    public long getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(long totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public long getStaffPresentToday() {
        return staffPresentToday;
    }

    public void setStaffPresentToday(long staffPresentToday) {
        this.staffPresentToday = staffPresentToday;
    }

    public long getTotalStaff() {
        return totalStaff;
    }

    public void setTotalStaff(long totalStaff) {
        this.totalStaff = totalStaff;
    }

    public List<RiskAlert> getRiskAlerts() {
        return riskAlerts;
    }

    public void setRiskAlerts(List<RiskAlert> riskAlerts) {
        this.riskAlerts = riskAlerts;
    }

    // === NESTED CLASSES ===

    public static class RecentBagEvent {
        private String qrCode;
        private String hcfName;
        private String eventType;
        private String anomalyState;
        private String wasteCategory;
        private Instant eventTs;

        public RecentBagEvent() {
        }

        public RecentBagEvent(String qrCode, String hcfName, String eventType, String anomalyState,
                String wasteCategory, Instant eventTs) {
            this.qrCode = qrCode;
            this.hcfName = hcfName;
            this.eventType = eventType;
            this.anomalyState = anomalyState;
            this.wasteCategory = wasteCategory;
            this.eventTs = eventTs;
        }

        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }

        public String getHcfName() {
            return hcfName;
        }

        public void setHcfName(String hcfName) {
            this.hcfName = hcfName;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getAnomalyState() {
            return anomalyState;
        }

        public void setAnomalyState(String anomalyState) {
            this.anomalyState = anomalyState;
        }

        public String getWasteCategory() {
            return wasteCategory;
        }

        public void setWasteCategory(String wasteCategory) {
            this.wasteCategory = wasteCategory;
        }

        public Instant getEventTs() {
            return eventTs;
        }

        public void setEventTs(Instant eventTs) {
            this.eventTs = eventTs;
        }
    }

    public static class AgreementSummary {
        private String agreementNumber;
        private String hcfName;
        private String status;
        private String duesStatus;
        private java.time.LocalDate endDate;
        private int daysUntilExpiry;

        public AgreementSummary() {
        }

        public String getAgreementNumber() {
            return agreementNumber;
        }

        public void setAgreementNumber(String agreementNumber) {
            this.agreementNumber = agreementNumber;
        }

        public String getHcfName() {
            return hcfName;
        }

        public void setHcfName(String hcfName) {
            this.hcfName = hcfName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDuesStatus() {
            return duesStatus;
        }

        public void setDuesStatus(String duesStatus) {
            this.duesStatus = duesStatus;
        }

        public java.time.LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(java.time.LocalDate endDate) {
            this.endDate = endDate;
        }

        public int getDaysUntilExpiry() {
            return daysUntilExpiry;
        }

        public void setDaysUntilExpiry(int daysUntilExpiry) {
            this.daysUntilExpiry = daysUntilExpiry;
        }
    }

    /**
     * Risk Alert for dashboard notifications.
     * Severity levels: CRITICAL, HIGH, MEDIUM
     */
    public static class RiskAlert {
        private String severity; // CRITICAL, HIGH, MEDIUM
        private String type; // SUBSCRIPTION_EXPIRY, CPCB_OVERDUE, INVOICE_OVERDUE, VEHICLE_OFFLINE,
                             // AGREEMENT_EXPIRY
        private String title;
        private String description;
        private String entityId; // Optional: ID of related entity (agreement, vehicle, invoice)

        public RiskAlert() {
        }

        public RiskAlert(String severity, String type, String title, String description, String entityId) {
            this.severity = severity;
            this.type = type;
            this.title = title;
            this.description = description;
            this.entityId = entityId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }
    }
}
