package com.smartcbwtf.domain;

/**
 * Alert types for portal-visible notifications.
 */
public enum AlertType {
    // Financial Alerts
    BILL_GENERATED,
    PAYMENT_RECEIVED,
    PAYMENT_DUE,
    PAYMENT_REMINDER_SENT,
    PAYMENT_OVERDUE,

    // Operational Alerts
    UNVERIFIED_BAG_DETECTED,
    WEIGHT_MISMATCH_DETECTED,
    GPS_ANOMALY_DETECTED,
    MISSED_PICKUP_DETECTED,
    BLUE_WASTE_BELOW_THRESHOLD,
    QR_VERIFICATION_SLA_BREACHED,

    // Compliance Alerts
    AGREEMENT_EXPIRING,
    AGREEMENT_EXPIRED,
    REPORT_GENERATED,
    REPORT_FLAGGED,

    // System Alerts
    EMAIL_FAILED,
    HCF_REGISTERED
}
