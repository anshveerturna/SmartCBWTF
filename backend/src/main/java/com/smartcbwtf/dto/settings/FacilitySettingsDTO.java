package com.smartcbwtf.dto.settings;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Complete facility settings response DTO.
 * Combines all sections for GET /api/cbwtf/settings response.
 */
public record FacilitySettingsDTO(
        Integer settingsVersion,

        // Section 1: Legal & Entity Profile
        LegalProfileDTO legal,

        // Section 2: Financial & Billing
        FinancialSettingsDTO financial,

        // Section 3: Payment & Reminders
        PaymentReminderDTO paymentReminders,

        // Section 4: Agreement Rules
        AgreementRulesDTO agreementRules,

        // Section 5: QR & Operational
        OperationalRulesDTO operational,

        // Section 6: Compliance & Reporting
        ComplianceSettingsDTO compliance,

        // Section 7: Email & Notification
        EmailSettingsDTO email,

        // Lock status
        LockedFieldsDTO lockedFields,

        // Timestamps
        Instant createdAt,
        Instant updatedAt) {
    public record LockedFieldsDTO(
            boolean gstLocked,
            boolean complianceLocked,
            boolean qrRulesLocked,
            Instant firstInvoiceAt,
            Instant firstQrGeneratedAt,
            Instant firstComplianceReportAt) {
    }
}
