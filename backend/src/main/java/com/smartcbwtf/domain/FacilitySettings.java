package com.smartcbwtf.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Unified facility settings for all CBWTF configuration.
 * This is the configuration backbone of the platform.
 * All downstream systems (billing, compliance, payments, alerts, reports)
 * derive behavior from these settings.
 */
@Entity
@Table(name = "facility_settings")
public class FacilitySettings {

    @Id
    @Column(name = "facility_id")
    private UUID facilityId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "facility_id")
    private Facility facility;

    // Schema versioning for evolution & rollback
    @Column(name = "settings_version", nullable = false)
    private Integer settingsVersion = 1;

    // ==================== Section 1: Legal & Entity Profile ====================
    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "authorization_number", length = 100)
    private String authorizationNumber;

    @Column(name = "spcb_name")
    private String spcbName;

    @Column(name = "spcb_state", length = 100)
    private String spcbState;

    @Column(length = 20)
    private String gstin;

    @Column(length = 20)
    private String pan;

    @Column(name = "registered_address", columnDefinition = "TEXT")
    private String registeredAddress;

    @Column(name = "registered_state", length = 100)
    private String registeredState;

    @Column(name = "registered_pincode", length = 10)
    private String registeredPincode;

    @Column(name = "official_email")
    private String officialEmail;

    @Column(name = "official_phone", length = 20)
    private String officialPhone;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "logo_checksum", length = 64)
    private String logoChecksum;

    @Column(name = "signature_url", length = 512)
    private String signatureUrl;

    @Column(name = "signature_checksum", length = 64)
    private String signatureChecksum;

    // ==================== Section 2: Financial & Billing ====================
    @Column(name = "cgst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstPercent = new BigDecimal("9.00");

    @Column(name = "sgst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstPercent = new BigDecimal("9.00");

    @Column(name = "igst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal igstPercent = new BigDecimal("18.00");

    @Column(name = "gst_enabled", nullable = false)
    private Boolean gstEnabled = true;

    // ==================== Section 3: Payment & Reminders ====================
    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 7;

    @Column(name = "auto_alert_escalation", nullable = false)
    private Boolean autoAlertEscalation = true;

    // ==================== Section 4: Agreement Rules ====================
    @Column(name = "default_agreement_validity_months", nullable = false)
    private Integer defaultAgreementValidityMonths = 12;

    @Column(name = "agreement_renewal_window_days", nullable = false)
    private Integer agreementRenewalWindowDays = 30;

    @Column(name = "block_overlapping_agreements", nullable = false)
    private Boolean blockOverlappingAgreements = true;

    // Agreement Number Format Settings
    @Column(name = "agreement_number_prefix", nullable = false, length = 20)
    private String agreementNumberPrefix = "HCF";

    @Column(name = "agreement_number_separator", nullable = false, length = 5)
    private String agreementNumberSeparator = "-";

    @Column(name = "agreement_number_sequence_digits", nullable = false)
    private Integer agreementNumberSequenceDigits = 5;

    @Column(name = "agreement_number_include_facility_code", nullable = false)
    private Boolean agreementNumberIncludeFacilityCode = true;

    @Column(name = "agreement_number_include_year", nullable = false)
    private Boolean agreementNumberIncludeYear = true;

    @Column(name = "agreement_terms_template", columnDefinition = "TEXT")
    private String agreementTermsTemplate;

    // ==================== Section 5: QR & Operational Rules ====================
    @Column(name = "qr_validity_days", nullable = false)
    private Integer qrValidityDays = 30;

    @Column(name = "allow_multiple_active_qrs", nullable = false)
    private Boolean allowMultipleActiveQrs = false;

    @Column(name = "require_cbwtf_verification", nullable = false)
    private Boolean requireCbwtfVerification = true;

    @Column(name = "gps_geofence_radius_m", nullable = false)
    private Integer gpsGeofenceRadiusM = 100;

    @Column(name = "max_unverified_bags", nullable = false)
    private Integer maxUnverifiedBags = 50;

    @Column(name = "blue_waste_min_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal blueWasteMinPercent = new BigDecimal("5.00");

    // ==================== Section 6: Compliance & Reporting ====================
    @Column(name = "daily_report_time", nullable = false)
    private LocalTime dailyReportTime = LocalTime.of(23, 0);

    @Column(name = "monthly_report_day", nullable = false)
    private Integer monthlyReportDay = 1;

    @Column(name = "annual_form_iv_date")
    private LocalDate annualFormIvDate;

    @Column(name = "enforce_checksum", nullable = false)
    private Boolean enforceChecksum = true;

    // ==================== Section 7: Email & Notification ====================
    // System-controlled sender identity (IMMUTABLE after first email)
    @Column(name = "sender_slug", length = 50)
    private String senderSlug;

    @Column(name = "use_generic_sender", nullable = false)
    private Boolean useGenericSender = false;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Column(name = "first_email_sent_at")
    private Instant firstEmailSentAt;

    // Legacy fields kept for backward compatibility (no longer used)
    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "cc_admin_on_hcf_emails", nullable = false)
    private Boolean ccAdminOnHcfEmails = true;

    @Column(name = "email_notifications_enabled", nullable = false)
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "in_app_alerts_enabled", nullable = false)
    private Boolean inAppAlertsEnabled = true;

    // ==================== First-use tracking for soft-locks ====================
    @Column(name = "first_invoice_at")
    private Instant firstInvoiceAt;

    @Column(name = "first_qr_generated_at")
    private Instant firstQrGeneratedAt;

    @Column(name = "first_compliance_report_at")
    private Instant firstComplianceReportAt;

    // ==================== Timestamps ====================
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // ==================== Helper Methods ====================

    /**
     * Get the name to use for invoices/reports (always legal name).
     */
    public String getNameForInvoice() {
        return legalName;
    }

    /**
     * Get the name to use for dashboard/UI (trade name if available, else legal
     * name).
     */
    public String getNameForDisplay() {
        return tradeName != null && !tradeName.isBlank() ? tradeName : legalName;
    }

    /**
     * Check if GST fields are locked (after first invoice).
     */
    public boolean isGstLocked() {
        return firstInvoiceAt != null;
    }

    /**
     * Check if authorization/SPCB fields are locked (after first compliance
     * report).
     */
    public boolean isComplianceLocked() {
        return firstComplianceReportAt != null;
    }

    /**
     * Get effective total GST rate.
     */
    public BigDecimal getEffectiveGstRate() {
        if (!gstEnabled) {
            return BigDecimal.ZERO;
        }
        return cgstPercent.add(sgstPercent);
    }

    // ==================== Sender Identity Methods ====================

    /**
     * Get the resolved sender email address.
     * Returns generic sender if useGenericSender is true, else facility-specific.
     */
    public String getResolvedSenderEmail() {
        if (Boolean.TRUE.equals(useGenericSender) || senderSlug == null || senderSlug.isBlank()) {
            return "no-reply@smartcbwtf.com";
        }
        return senderSlug + "@smartcbwtf.com";
    }

    /**
     * Get the resolved sender display name.
     * Format: "SmartCBWTF – {Facility Trade Name}"
     */
    public String getResolvedSenderName() {
        String displayName = getNameForDisplay();
        if (displayName == null || displayName.isBlank()) {
            return "SmartCBWTF";
        }
        return "SmartCBWTF – " + displayName;
    }

    /**
     * Check if sender_slug is locked (after first email sent).
     */
    public boolean isSenderSlugLocked() {
        return firstEmailSentAt != null;
    }

    /**
     * Generate sender slug from trade name or code.
     * Lowercase, alphanumeric + hyphens only, max 50 chars.
     */
    public static String generateSenderSlug(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug.length() > 50 ? slug.substring(0, 50) : slug;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        this.settingsVersion++;
    }

    // ==================== Getters and Setters ====================
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

    public Integer getSettingsVersion() {
        return settingsVersion;
    }

    public void setSettingsVersion(Integer settingsVersion) {
        this.settingsVersion = settingsVersion;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public String getAuthorizationNumber() {
        return authorizationNumber;
    }

    public void setAuthorizationNumber(String authorizationNumber) {
        this.authorizationNumber = authorizationNumber;
    }

    public String getSpcbName() {
        return spcbName;
    }

    public void setSpcbName(String spcbName) {
        this.spcbName = spcbName;
    }

    public String getSpcbState() {
        return spcbState;
    }

    public void setSpcbState(String spcbState) {
        this.spcbState = spcbState;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public String getRegisteredState() {
        return registeredState;
    }

    public void setRegisteredState(String registeredState) {
        this.registeredState = registeredState;
    }

    public String getRegisteredPincode() {
        return registeredPincode;
    }

    public void setRegisteredPincode(String registeredPincode) {
        this.registeredPincode = registeredPincode;
    }

    public String getOfficialEmail() {
        return officialEmail;
    }

    public void setOfficialEmail(String officialEmail) {
        this.officialEmail = officialEmail;
    }

    public String getOfficialPhone() {
        return officialPhone;
    }

    public void setOfficialPhone(String officialPhone) {
        this.officialPhone = officialPhone;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLogoChecksum() {
        return logoChecksum;
    }

    public void setLogoChecksum(String logoChecksum) {
        this.logoChecksum = logoChecksum;
    }

    public String getSignatureUrl() {
        return signatureUrl;
    }

    public void setSignatureUrl(String signatureUrl) {
        this.signatureUrl = signatureUrl;
    }

    public String getSignatureChecksum() {
        return signatureChecksum;
    }

    public void setSignatureChecksum(String signatureChecksum) {
        this.signatureChecksum = signatureChecksum;
    }

    public BigDecimal getCgstPercent() {
        return cgstPercent;
    }

    public void setCgstPercent(BigDecimal cgstPercent) {
        this.cgstPercent = cgstPercent;
    }

    public BigDecimal getSgstPercent() {
        return sgstPercent;
    }

    public void setSgstPercent(BigDecimal sgstPercent) {
        this.sgstPercent = sgstPercent;
    }

    public BigDecimal getIgstPercent() {
        return igstPercent;
    }

    public void setIgstPercent(BigDecimal igstPercent) {
        this.igstPercent = igstPercent;
    }

    public Boolean getGstEnabled() {
        return gstEnabled;
    }

    public void setGstEnabled(Boolean gstEnabled) {
        this.gstEnabled = gstEnabled;
    }

    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public Boolean getAutoAlertEscalation() {
        return autoAlertEscalation;
    }

    public void setAutoAlertEscalation(Boolean autoAlertEscalation) {
        this.autoAlertEscalation = autoAlertEscalation;
    }

    public Integer getDefaultAgreementValidityMonths() {
        return defaultAgreementValidityMonths;
    }

    public void setDefaultAgreementValidityMonths(Integer defaultAgreementValidityMonths) {
        this.defaultAgreementValidityMonths = defaultAgreementValidityMonths;
    }

    public Integer getAgreementRenewalWindowDays() {
        return agreementRenewalWindowDays;
    }

    public void setAgreementRenewalWindowDays(Integer agreementRenewalWindowDays) {
        this.agreementRenewalWindowDays = agreementRenewalWindowDays;
    }

    public Boolean getBlockOverlappingAgreements() {
        return blockOverlappingAgreements;
    }

    public void setBlockOverlappingAgreements(Boolean blockOverlappingAgreements) {
        this.blockOverlappingAgreements = blockOverlappingAgreements;
    }

    public String getAgreementNumberPrefix() {
        return agreementNumberPrefix;
    }

    public void setAgreementNumberPrefix(String agreementNumberPrefix) {
        this.agreementNumberPrefix = agreementNumberPrefix;
    }

    public String getAgreementNumberSeparator() {
        return agreementNumberSeparator;
    }

    public void setAgreementNumberSeparator(String agreementNumberSeparator) {
        this.agreementNumberSeparator = agreementNumberSeparator;
    }

    public Integer getAgreementNumberSequenceDigits() {
        return agreementNumberSequenceDigits;
    }

    public void setAgreementNumberSequenceDigits(Integer agreementNumberSequenceDigits) {
        this.agreementNumberSequenceDigits = agreementNumberSequenceDigits;
    }

    public Boolean getAgreementNumberIncludeFacilityCode() {
        return agreementNumberIncludeFacilityCode;
    }

    public void setAgreementNumberIncludeFacilityCode(Boolean agreementNumberIncludeFacilityCode) {
        this.agreementNumberIncludeFacilityCode = agreementNumberIncludeFacilityCode;
    }

    public Boolean getAgreementNumberIncludeYear() {
        return agreementNumberIncludeYear;
    }

    public void setAgreementNumberIncludeYear(Boolean agreementNumberIncludeYear) {
        this.agreementNumberIncludeYear = agreementNumberIncludeYear;
    }

    public String getAgreementTermsTemplate() {
        return agreementTermsTemplate;
    }

    public void setAgreementTermsTemplate(String agreementTermsTemplate) {
        this.agreementTermsTemplate = agreementTermsTemplate;
    }

    public Integer getQrValidityDays() {
        return qrValidityDays;
    }

    public void setQrValidityDays(Integer qrValidityDays) {
        this.qrValidityDays = qrValidityDays;
    }

    public Boolean getAllowMultipleActiveQrs() {
        return allowMultipleActiveQrs;
    }

    public void setAllowMultipleActiveQrs(Boolean allowMultipleActiveQrs) {
        this.allowMultipleActiveQrs = allowMultipleActiveQrs;
    }

    public Boolean getRequireCbwtfVerification() {
        return requireCbwtfVerification;
    }

    public void setRequireCbwtfVerification(Boolean requireCbwtfVerification) {
        this.requireCbwtfVerification = requireCbwtfVerification;
    }

    public Integer getGpsGeofenceRadiusM() {
        return gpsGeofenceRadiusM;
    }

    public void setGpsGeofenceRadiusM(Integer gpsGeofenceRadiusM) {
        this.gpsGeofenceRadiusM = gpsGeofenceRadiusM;
    }

    public Integer getMaxUnverifiedBags() {
        return maxUnverifiedBags;
    }

    public void setMaxUnverifiedBags(Integer maxUnverifiedBags) {
        this.maxUnverifiedBags = maxUnverifiedBags;
    }

    public BigDecimal getBlueWasteMinPercent() {
        return blueWasteMinPercent;
    }

    public void setBlueWasteMinPercent(BigDecimal blueWasteMinPercent) {
        this.blueWasteMinPercent = blueWasteMinPercent;
    }

    public LocalTime getDailyReportTime() {
        return dailyReportTime;
    }

    public void setDailyReportTime(LocalTime dailyReportTime) {
        this.dailyReportTime = dailyReportTime;
    }

    public Integer getMonthlyReportDay() {
        return monthlyReportDay;
    }

    public void setMonthlyReportDay(Integer monthlyReportDay) {
        this.monthlyReportDay = monthlyReportDay;
    }

    public LocalDate getAnnualFormIvDate() {
        return annualFormIvDate;
    }

    public void setAnnualFormIvDate(LocalDate annualFormIvDate) {
        this.annualFormIvDate = annualFormIvDate;
    }

    public Boolean getEnforceChecksum() {
        return enforceChecksum;
    }

    public void setEnforceChecksum(Boolean enforceChecksum) {
        this.enforceChecksum = enforceChecksum;
    }

    // New sender identity fields
    public String getSenderSlug() {
        return senderSlug;
    }

    public void setSenderSlug(String senderSlug) {
        this.senderSlug = senderSlug;
    }

    public Boolean getUseGenericSender() {
        return useGenericSender;
    }

    public void setUseGenericSender(Boolean useGenericSender) {
        this.useGenericSender = useGenericSender;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public Instant getFirstEmailSentAt() {
        return firstEmailSentAt;
    }

    public void setFirstEmailSentAt(Instant firstEmailSentAt) {
        this.firstEmailSentAt = firstEmailSentAt;
    }

    // Legacy sender fields (for backward compatibility)
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public Boolean getCcAdminOnHcfEmails() {
        return ccAdminOnHcfEmails;
    }

    public void setCcAdminOnHcfEmails(Boolean ccAdminOnHcfEmails) {
        this.ccAdminOnHcfEmails = ccAdminOnHcfEmails;
    }

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public Boolean getInAppAlertsEnabled() {
        return inAppAlertsEnabled;
    }

    public void setInAppAlertsEnabled(Boolean inAppAlertsEnabled) {
        this.inAppAlertsEnabled = inAppAlertsEnabled;
    }

    public Instant getFirstInvoiceAt() {
        return firstInvoiceAt;
    }

    public void setFirstInvoiceAt(Instant firstInvoiceAt) {
        this.firstInvoiceAt = firstInvoiceAt;
    }

    public Instant getFirstQrGeneratedAt() {
        return firstQrGeneratedAt;
    }

    public void setFirstQrGeneratedAt(Instant firstQrGeneratedAt) {
        this.firstQrGeneratedAt = firstQrGeneratedAt;
    }

    public Instant getFirstComplianceReportAt() {
        return firstComplianceReportAt;
    }

    public void setFirstComplianceReportAt(Instant firstComplianceReportAt) {
        this.firstComplianceReportAt = firstComplianceReportAt;
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
}
