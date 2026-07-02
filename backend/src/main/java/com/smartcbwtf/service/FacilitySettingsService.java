package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.domain.SettingsAuditLog;
import com.smartcbwtf.dto.settings.*;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.repository.SettingsAuditLogRepository;
import com.smartcbwtf.repository.AppUserRepository;
import com.smartcbwtf.repository.BankAccountRepository;
import com.smartcbwtf.repository.FacilityRepository;
import com.smartcbwtf.domain.Facility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.smartcbwtf.util.PaginationUtils.pageRequest;

/**
 * Service for managing CBWTF facility settings.
 * Implements audit logging, soft-lock enforcement, and system readiness checks.
 */
@Service
@Transactional
public class FacilitySettingsService {

        private static final Logger log = LoggerFactory.getLogger(FacilitySettingsService.class);
        private static final int CURRENT_SCHEMA_VERSION = 1;
        private static final int MAX_AGREEMENT_TERMS_TEMPLATE_LENGTH = 20_000;

        private final FacilitySettingsRepository settingsRepository;
        private final SettingsAuditLogRepository auditLogRepository;
        private final AppUserRepository userRepository;
        private final BankAccountRepository bankAccountRepository;
        private final FacilityRepository facilityRepository;

        public FacilitySettingsService(
                        FacilitySettingsRepository settingsRepository,
                        SettingsAuditLogRepository auditLogRepository,
                        AppUserRepository userRepository,
                        BankAccountRepository bankAccountRepository,
                        FacilityRepository facilityRepository) {
                this.settingsRepository = settingsRepository;
                this.auditLogRepository = auditLogRepository;
                this.userRepository = userRepository;
                this.bankAccountRepository = bankAccountRepository;
                this.facilityRepository = facilityRepository;
        }

        /**
         * Get settings for the current facility, creating defaults if not exists.
         */
        public FacilitySettingsDTO getSettings() {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);
                return toDTO(settings);
        }

        /**
         * Check if the system is ready to operate.
         * Fail-closed: returns errors if required settings are missing.
         */
        public SystemReadinessResult checkSystemReadiness() {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                List<String> errors = new ArrayList<>();

                // Legal requirements
                if (isBlank(settings.getLegalName())) {
                        errors.add("Legal name is required for invoice generation");
                }

                // GST requirements
                if (settings.getGstEnabled() && isBlank(settings.getGstin())) {
                        errors.add("GSTIN is required when GST is enabled");
                }

                // Bank account for payments
                boolean hasBankAccount = bankAccountRepository.existsByFacilityId(facilityId);
                if (!hasBankAccount) {
                        errors.add("Bank account is required before accepting payments");
                }

                return errors.isEmpty() ? SystemReadinessResult.success() : SystemReadinessResult.failure(errors);
        }

        /**
         * Update Section 1: Legal & Entity Profile.
         */
        public void updateLegalProfile(LegalProfileDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                // Check if compliance fields are locked
                if (settings.isComplianceLocked()) {
                        if (!Objects.equals(settings.getAuthorizationNumber(), dto.authorizationNumber()) ||
                                        !Objects.equals(settings.getSpcbName(), dto.spcbName()) ||
                                        !Objects.equals(settings.getSpcbState(), dto.spcbState())) {
                                throw new SettingsLockedException(
                                                "Authorization and SPCB fields are locked after first compliance report");
                        }
                }

                // Audit and update each changed field
                auditIfChanged("legal", "legalName", settings.getLegalName(), dto.legalName(), facilityId, ipAddress);
                auditIfChanged("legal", "tradeName", settings.getTradeName(), dto.tradeName(), facilityId, ipAddress);
                auditIfChanged("legal", "authorizationNumber", settings.getAuthorizationNumber(),
                                dto.authorizationNumber(),
                                facilityId, ipAddress);
                auditIfChanged("legal", "spcbName", settings.getSpcbName(), dto.spcbName(), facilityId, ipAddress);
                auditIfChanged("legal", "spcbState", settings.getSpcbState(), dto.spcbState(), facilityId, ipAddress);
                auditIfChanged("legal", "gstin", settings.getGstin(), dto.gstin(), facilityId, ipAddress);
                auditIfChanged("legal", "pan", settings.getPan(), dto.pan(), facilityId, ipAddress);
                auditIfChanged("legal", "registeredAddress", settings.getRegisteredAddress(), dto.registeredAddress(),
                                facilityId, ipAddress);
                auditIfChanged("legal", "registeredState", settings.getRegisteredState(), dto.registeredState(),
                                facilityId,
                                ipAddress);
                auditIfChanged("legal", "registeredPincode", settings.getRegisteredPincode(), dto.registeredPincode(),
                                facilityId, ipAddress);
                auditIfChanged("legal", "officialEmail", settings.getOfficialEmail(), dto.officialEmail(), facilityId,
                                ipAddress);
                auditIfChanged("legal", "officialPhone", settings.getOfficialPhone(), dto.officialPhone(), facilityId,
                                ipAddress);
                auditIfChanged("legal", "logoUrl", settings.getLogoUrl(), dto.logoUrl(), facilityId, ipAddress);
                auditIfChanged("legal", "signatureUrl", settings.getSignatureUrl(), dto.signatureUrl(), facilityId,
                                ipAddress);

                // Apply changes
                settings.setLegalName(dto.legalName());
                settings.setTradeName(dto.tradeName());
                settings.setAuthorizationNumber(dto.authorizationNumber());
                settings.setSpcbName(dto.spcbName());
                settings.setSpcbState(dto.spcbState());
                settings.setGstin(dto.gstin());
                settings.setPan(dto.pan());
                settings.setRegisteredAddress(dto.registeredAddress());
                settings.setRegisteredState(dto.registeredState());
                settings.setRegisteredPincode(dto.registeredPincode());
                settings.setOfficialEmail(dto.officialEmail());
                settings.setOfficialPhone(dto.officialPhone());
                settings.setLogoUrl(dto.logoUrl());
                settings.setLogoChecksum(dto.logoChecksum());
                settings.setSignatureUrl(dto.signatureUrl());
                settings.setSignatureChecksum(dto.signatureChecksum());

                settingsRepository.save(settings);
                log.info("Updated legal profile for facility {}", facilityId);
        }

        /**
         * Update Section 2: Financial & Billing Settings.
         */
        public void updateFinancialSettings(FinancialSettingsDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                // Check if GST rates are locked
                if (settings.isGstLocked()) {
                        if (!Objects.equals(settings.getCgstPercent(), dto.cgstPercent()) ||
                                        !Objects.equals(settings.getSgstPercent(), dto.sgstPercent()) ||
                                        !Objects.equals(settings.getIgstPercent(), dto.igstPercent())) {
                                throw new SettingsLockedException(
                                                "GST rates are locked after first invoice generation");
                        }
                }

                auditIfChanged("financial", "cgstPercent", str(settings.getCgstPercent()), str(dto.cgstPercent()),
                                facilityId,
                                ipAddress);
                auditIfChanged("financial", "sgstPercent", str(settings.getSgstPercent()), str(dto.sgstPercent()),
                                facilityId,
                                ipAddress);
                auditIfChanged("financial", "igstPercent", str(settings.getIgstPercent()), str(dto.igstPercent()),
                                facilityId,
                                ipAddress);
                auditIfChanged("financial", "gstEnabled", str(settings.getGstEnabled()), str(dto.gstEnabled()),
                                facilityId,
                                ipAddress);

                settings.setCgstPercent(dto.cgstPercent());
                settings.setSgstPercent(dto.sgstPercent());
                settings.setIgstPercent(dto.igstPercent());
                settings.setGstEnabled(dto.gstEnabled());

                // Bank details
                auditIfChanged("financial", "bankAccountName", str(settings.getBankAccountName()),
                                str(dto.bankAccountName()),
                                facilityId, ipAddress);
                auditIfChanged("financial", "bankAccountNumber", str(settings.getBankAccountNumber()),
                                str(dto.bankAccountNumber()),
                                facilityId, ipAddress);
                auditIfChanged("financial", "bankName", str(settings.getBankName()), str(dto.bankName()),
                                facilityId, ipAddress);
                auditIfChanged("financial", "bankBranch", str(settings.getBankBranch()), str(dto.bankBranch()),
                                facilityId, ipAddress);
                auditIfChanged("financial", "bankIfsc", str(settings.getBankIfsc()), str(dto.bankIfsc()),
                                facilityId, ipAddress);

                settings.setBankAccountName(dto.bankAccountName());
                settings.setBankAccountNumber(dto.bankAccountNumber());
                settings.setBankName(dto.bankName());
                settings.setBankBranch(dto.bankBranch());
                settings.setBankIfsc(dto.bankIfsc());

                settingsRepository.save(settings);
                log.info("Updated financial settings for facility {}", facilityId);
        }

        /**
         * Update Section 3: Payment & Reminder Settings.
         */
        public void updatePaymentReminders(PaymentReminderDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                auditIfChanged("payment", "gracePeriodDays", str(settings.getGracePeriodDays()),
                                str(dto.gracePeriodDays()),
                                facilityId, ipAddress);
                auditIfChanged("payment", "autoAlertEscalation", str(settings.getAutoAlertEscalation()),
                                str(dto.autoAlertEscalation()), facilityId, ipAddress);

                settings.setGracePeriodDays(dto.gracePeriodDays());
                settings.setAutoAlertEscalation(dto.autoAlertEscalation());

                settingsRepository.save(settings);
                log.info("Updated payment reminder settings for facility {}", facilityId);
        }

        /**
         * Update Section 4: Agreement Rules.
         */
        public void updateAgreementRules(AgreementRulesDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                auditIfChanged("agreement", "defaultAgreementValidityMonths",
                                str(settings.getDefaultAgreementValidityMonths()),
                                str(dto.defaultAgreementValidityMonths()), facilityId, ipAddress);
                auditIfChanged("agreement", "agreementRenewalWindowDays", str(settings.getAgreementRenewalWindowDays()),
                                str(dto.agreementRenewalWindowDays()), facilityId, ipAddress);
                auditIfChanged("agreement", "blockOverlappingAgreements", str(settings.getBlockOverlappingAgreements()),
                                str(dto.blockOverlappingAgreements()), facilityId, ipAddress);

                settings.setDefaultAgreementValidityMonths(dto.defaultAgreementValidityMonths());
                settings.setAgreementRenewalWindowDays(dto.agreementRenewalWindowDays());
                settings.setBlockOverlappingAgreements(dto.blockOverlappingAgreements());

                // Agreement number format fields
                if (dto.agreementNumberPrefix() != null) {
                        auditIfChanged("agreement", "agreementNumberPrefix",
                                        str(settings.getAgreementNumberPrefix()),
                                        str(dto.agreementNumberPrefix()), facilityId, ipAddress);
                        settings.setAgreementNumberPrefix(dto.agreementNumberPrefix());
                }
                if (dto.agreementNumberSeparator() != null) {
                        auditIfChanged("agreement", "agreementNumberSeparator",
                                        str(settings.getAgreementNumberSeparator()),
                                        str(dto.agreementNumberSeparator()), facilityId, ipAddress);
                        settings.setAgreementNumberSeparator(dto.agreementNumberSeparator());
                }
                if (dto.agreementNumberSequenceDigits() != null) {
                        auditIfChanged("agreement", "agreementNumberSequenceDigits",
                                        str(settings.getAgreementNumberSequenceDigits()),
                                        str(dto.agreementNumberSequenceDigits()), facilityId, ipAddress);
                        settings.setAgreementNumberSequenceDigits(dto.agreementNumberSequenceDigits());
                }
                if (dto.agreementNumberIncludeFacilityCode() != null) {
                        auditIfChanged("agreement", "agreementNumberIncludeFacilityCode",
                                        str(settings.getAgreementNumberIncludeFacilityCode()),
                                        str(dto.agreementNumberIncludeFacilityCode()), facilityId, ipAddress);
                        settings.setAgreementNumberIncludeFacilityCode(dto.agreementNumberIncludeFacilityCode());
                }
                if (dto.agreementNumberIncludeYear() != null) {
                        auditIfChanged("agreement", "agreementNumberIncludeYear",
                                        str(settings.getAgreementNumberIncludeYear()),
                                        str(dto.agreementNumberIncludeYear()), facilityId, ipAddress);
                        settings.setAgreementNumberIncludeYear(dto.agreementNumberIncludeYear());
                }
                if (dto.agreementNumberTemplate() != null) {
                        auditIfChanged("agreement", "agreementNumberTemplate",
                                        str(settings.getAgreementNumberTemplate()),
                                        str(dto.agreementNumberTemplate()), facilityId, ipAddress);
                        settings.setAgreementNumberTemplate(blankToNull(dto.agreementNumberTemplate()));
                }
                if (dto.agreementNumberResetFrequency() != null) {
                        auditIfChanged("agreement", "agreementNumberResetFrequency",
                                        str(settings.getAgreementNumberResetFrequency()),
                                        str(dto.agreementNumberResetFrequency()), facilityId, ipAddress);
                        settings.setAgreementNumberResetFrequency(dto.agreementNumberResetFrequency());
                }

                settingsRepository.save(settings);
                log.info("Updated agreement rules for facility {}", facilityId);
        }

        /**
         * Update Section 5: Operational Rules.
         */
        public void updateOperationalRules(OperationalRulesDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                auditIfChanged("operational", "qrValidityDays", str(settings.getQrValidityDays()),
                                str(dto.qrValidityDays()),
                                facilityId, ipAddress);
                auditIfChanged("operational", "allowMultipleActiveQrs", str(settings.getAllowMultipleActiveQrs()),
                                str(dto.allowMultipleActiveQrs()), facilityId, ipAddress);
                auditIfChanged("operational", "requireCbwtfVerification", str(settings.getRequireCbwtfVerification()),
                                str(dto.requireCbwtfVerification()), facilityId, ipAddress);
                auditIfChanged("operational", "gpsGeofenceRadiusM", str(settings.getGpsGeofenceRadiusM()),
                                str(dto.gpsGeofenceRadiusM()), facilityId, ipAddress);
                auditIfChanged("operational", "maxUnverifiedBags", str(settings.getMaxUnverifiedBags()),
                                str(dto.maxUnverifiedBags()), facilityId, ipAddress);
                auditIfChanged("operational", "blueWasteMinPercent", str(settings.getBlueWasteMinPercent()),
                                str(dto.blueWasteMinPercent()), facilityId, ipAddress);

                settings.setQrValidityDays(dto.qrValidityDays());
                settings.setAllowMultipleActiveQrs(dto.allowMultipleActiveQrs());
                settings.setRequireCbwtfVerification(dto.requireCbwtfVerification());
                settings.setGpsGeofenceRadiusM(dto.gpsGeofenceRadiusM());
                settings.setMaxUnverifiedBags(dto.maxUnverifiedBags());
                settings.setBlueWasteMinPercent(dto.blueWasteMinPercent());

                settingsRepository.save(settings);
                log.info("Updated operational rules for facility {}", facilityId);
        }

        /**
         * Update Section 6: Compliance Settings.
         */
        public void updateComplianceSettings(ComplianceSettingsDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                auditIfChanged("compliance", "dailyReportTime", str(settings.getDailyReportTime()),
                                str(dto.dailyReportTime()),
                                facilityId, ipAddress);
                auditIfChanged("compliance", "monthlyReportDay", str(settings.getMonthlyReportDay()),
                                str(dto.monthlyReportDay()), facilityId, ipAddress);
                auditIfChanged("compliance", "annualFormIvDate", str(settings.getAnnualFormIvDate()),
                                str(dto.annualFormIvDate()), facilityId, ipAddress);
                auditIfChanged("compliance", "enforceChecksum", str(settings.getEnforceChecksum()),
                                str(dto.enforceChecksum()),
                                facilityId, ipAddress);

                settings.setDailyReportTime(dto.dailyReportTime());
                settings.setMonthlyReportDay(dto.monthlyReportDay());
                settings.setAnnualFormIvDate(dto.annualFormIvDate());
                settings.setEnforceChecksum(dto.enforceChecksum());

                settingsRepository.save(settings);
                log.info("Updated compliance settings for facility {}", facilityId);
        }

        /**
         * Update Section 7: Email Settings.
         * Note: Sender identity is system-controlled. Only useGenericSender,
         * notificationEmail,
         * and toggle flags can be modified.
         */
        public void updateEmailSettings(EmailSettingsDTO dto, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);

                // Audit editable fields only
                auditIfChanged("email", "useGenericSender", str(settings.getUseGenericSender()),
                                str(dto.useGenericSender()), facilityId, ipAddress);
                auditIfChanged("email", "notificationEmail", settings.getNotificationEmail(),
                                dto.notificationEmail(), facilityId, ipAddress);
                auditIfChanged("email", "ccAdminOnHcfEmails", str(settings.getCcAdminOnHcfEmails()),
                                str(dto.ccAdminOnHcfEmails()), facilityId, ipAddress);
                auditIfChanged("email", "emailNotificationsEnabled", str(settings.getEmailNotificationsEnabled()),
                                str(dto.emailNotificationsEnabled()), facilityId, ipAddress);
                auditIfChanged("email", "inAppAlertsEnabled", str(settings.getInAppAlertsEnabled()),
                                str(dto.inAppAlertsEnabled()), facilityId, ipAddress);

                // Apply only editable fields (sender identity is system-controlled)
                settings.setUseGenericSender(dto.useGenericSender());
                settings.setNotificationEmail(dto.notificationEmail());
                settings.setCcAdminOnHcfEmails(dto.ccAdminOnHcfEmails());
                settings.setEmailNotificationsEnabled(dto.emailNotificationsEnabled());
                settings.setInAppAlertsEnabled(dto.inAppAlertsEnabled());

                // Initialize sender_slug if not set
                if (settings.getSenderSlug() == null || settings.getSenderSlug().isBlank()) {
                        String slug = FacilitySettings.generateSenderSlug(
                                        settings.getTradeName() != null ? settings.getTradeName()
                                                        : "facility-" + facilityId);
                        settings.setSenderSlug(slug);
                        log.info("Generated sender_slug '{}' for facility {}", slug, facilityId);
                }

                settingsRepository.save(settings);
                log.info("Updated email settings for facility {}", facilityId);
        }

        /**
         * Get audit history for the facility.
         */
        public Page<SettingsAuditDTO> getAuditHistory(String section, int page, int size) {
                UUID facilityId = TenantContext.getTenantId();
                var pageable = pageRequest(page, size, 20);

                Page<SettingsAuditLog> logs;
                if (section != null && !section.isBlank()) {
                        logs = auditLogRepository.findByFacilityIdAndSectionOrderByChangedAtDesc(facilityId, section,
                                        pageable);
                } else {
                        logs = auditLogRepository.findByFacilityIdOrderByChangedAtDesc(facilityId, pageable);
                }

                return logs.map(this::toAuditDTO);
        }

        // ==================== Helper Methods ====================

        private FacilitySettings getOrCreateSettings(UUID facilityId) {
                return settingsRepository.findById(facilityId)
                                .orElseGet(() -> {
                                        log.info("Creating new FacilitySettings for facilityId: {}", facilityId);
                                        FacilitySettings newSettings = new FacilitySettings();

                                        Facility facility = facilityRepository.findById(facilityId)
                                                        .orElseThrow(() -> new RuntimeException(
                                                                        "Facility not found: " + facilityId));

                                        log.info("Found Facility entity: {} (ID: {})", facility.getName(),
                                                        facility.getId());

                                        // Set reference to ensure Hibernate maps ID correctly
                                        newSettings.setFacility(facility);

                                        // ROBUST POPULATION: Ensure all NOT NULL DB columns are populated
                                        // 1. Legal & Identity
                                        if (newSettings.getLegalName() == null) {
                                                newSettings.setLegalName(facility.getName());
                                        }
                                        if (newSettings.getTradeName() == null) {
                                                newSettings.setTradeName(facility.getName());
                                        }

                                        // 2. Legacy Sender Fields (Database has NOT NULL constraint)
                                        if (newSettings.getSenderName() == null) {
                                                newSettings.setSenderName(facility.getName());
                                        }
                                        if (newSettings.getSenderEmail() == null) {
                                                String email = facility.getContactEmail();
                                                if (email == null || email.isBlank()) {
                                                        email = "no-reply-" + facility.getCode().toLowerCase()
                                                                        + "@smartcbwtf.com";
                                                }
                                                newSettings.setSenderEmail(email);
                                        }

                                        // 3. Sender Slug (Required for getResolvedSenderEmail)
                                        if (newSettings.getSenderSlug() == null) {
                                                String slug = FacilitySettings.generateSenderSlug(facility.getName());
                                                newSettings.setSenderSlug(slug);
                                        }

                                        // 4. Contact Info
                                        if (newSettings.getOfficialEmail() == null
                                                        && facility.getContactEmail() != null) {
                                                newSettings.setOfficialEmail(facility.getContactEmail());
                                        }
                                        if (newSettings.getOfficialPhone() == null
                                                        && facility.getContactPhone() != null) {
                                                newSettings.setOfficialPhone(facility.getContactPhone());
                                        }

                                        // DO NOT set facilityId manually; @MapsId handles it during persist

                                        log.info("Saving newFacilitySettings with facility set: {}",
                                                        newSettings.getFacility() != null);

                                        try {
                                                return settingsRepository.save(newSettings);
                                        } catch (Exception e) {
                                                log.error("Failed to save FacilitySettings", e);
                                                throw e;
                                        }
                                });
        }

        private void auditIfChanged(String section, String key, String oldValue, String newValue, UUID facilityId,
                        String ipAddress) {
                if (!Objects.equals(oldValue, newValue)) {
                        UUID userId = TenantContext.getUserId();
                        SettingsAuditLog auditLog = new SettingsAuditLog(
                                        facilityId, section, key, oldValue, newValue, userId, ipAddress);
                        auditLog.setSchemaVersion(CURRENT_SCHEMA_VERSION);
                        auditLogRepository.save(auditLog);
                }
        }

        private FacilitySettingsDTO toDTO(FacilitySettings s) {
                return new FacilitySettingsDTO(
                                s.getSettingsVersion(),
                                new LegalProfileDTO(
                                                s.getLegalName(), s.getTradeName(), s.getAuthorizationNumber(),
                                                s.getSpcbName(), s.getSpcbState(), s.getGstin(), s.getPan(),
                                                s.getRegisteredAddress(), s.getRegisteredState(),
                                                s.getRegisteredPincode(),
                                                s.getOfficialEmail(), s.getOfficialPhone(),
                                                s.getLogoUrl(), s.getLogoChecksum(), s.getSignatureUrl(),
                                                s.getSignatureChecksum()),
                                new FinancialSettingsDTO(
                                                s.getCgstPercent(), s.getSgstPercent(), s.getIgstPercent(),
                                                s.getGstEnabled(),
                                                s.getBankAccountName(), s.getBankAccountNumber(),
                                                s.getBankName(), s.getBankBranch(), s.getBankIfsc(),
                                                s.getPaymentQrUrl()),
                                new PaymentReminderDTO(s.getGracePeriodDays(), s.getAutoAlertEscalation()),
                                new AgreementRulesDTO(
                                                s.getDefaultAgreementValidityMonths(),
                                                s.getAgreementRenewalWindowDays(),
                                                s.getBlockOverlappingAgreements(),
                                                s.getAgreementNumberPrefix(),
                                                s.getAgreementNumberSeparator(),
                                                s.getAgreementNumberSequenceDigits(),
                                                s.getAgreementNumberIncludeFacilityCode(),
                                                s.getAgreementNumberIncludeYear(),
                                                s.getAgreementNumberTemplate(),
                                                s.getAgreementNumberResetFrequency(),
                                                s.getAgreementTermsTemplate()),
                                new OperationalRulesDTO(
                                                s.getQrValidityDays(), s.getAllowMultipleActiveQrs(),
                                                s.getRequireCbwtfVerification(),
                                                s.getGpsGeofenceRadiusM(), s.getMaxUnverifiedBags(),
                                                s.getBlueWasteMinPercent()),
                                new ComplianceSettingsDTO(
                                                s.getDailyReportTime(), s.getMonthlyReportDay(),
                                                s.getAnnualFormIvDate(),
                                                s.getEnforceChecksum()),
                                new EmailSettingsDTO(
                                                s.getResolvedSenderName(),
                                                s.getResolvedSenderEmail(),
                                                s.isSenderSlugLocked(),
                                                s.getUseGenericSender(),
                                                s.getNotificationEmail(),
                                                s.getCcAdminOnHcfEmails(),
                                                s.getEmailNotificationsEnabled(),
                                                s.getInAppAlertsEnabled()),
                                new FacilitySettingsDTO.LockedFieldsDTO(
                                                s.isGstLocked(), s.isComplianceLocked(),
                                                s.getFirstQrGeneratedAt() != null,
                                                s.getFirstInvoiceAt(), s.getFirstQrGeneratedAt(),
                                                s.getFirstComplianceReportAt()),
                                s.getCreatedAt(), s.getUpdatedAt());
        }

        private SettingsAuditDTO toAuditDTO(SettingsAuditLog log) {
                String username = userRepository.findById(log.getChangedBy())
                                .map(u -> u.getUsername())
                                .orElse("Unknown");

                return new SettingsAuditDTO(
                                log.getId(), log.getSection(), log.getSettingKey(),
                                log.getOldValue(), log.getNewValue(),
                                log.getChangedBy(), username, log.getChangedAt(), log.getIpAddress());
        }

        private boolean isBlank(String s) {
                return s == null || s.isBlank();
        }

        private String str(Object o) {
                return o == null ? null : o.toString();
        }

        private String blankToNull(String value) {
                if (value == null) {
                        return null;
                }
                String trimmed = value.trim();
                return trimmed.isEmpty() ? null : trimmed;
        }

        /**
         * Update agreement terms template text.
         * This is the default T&C that gets embedded into new agreement PDFs.
         */
        public void updateAgreementTermsTemplate(String termsTemplate, String ipAddress) {
                UUID facilityId = TenantContext.getTenantId();
                FacilitySettings settings = getOrCreateSettings(facilityId);
                String normalizedTermsTemplate = normalizeAgreementTermsTemplate(termsTemplate);

                String oldValue = settings.getAgreementTermsTemplate();
                settings.setAgreementTermsTemplate(normalizedTermsTemplate);
                settingsRepository.save(settings);

                // Audit log
                auditIfChanged("AGREEMENT_RULES", "agreementTermsTemplate",
                                oldValue != null ? truncateForAudit(oldValue) : null,
                                normalizedTermsTemplate != null ? truncateForAudit(normalizedTermsTemplate) : null,
                                facilityId, ipAddress);
        }

        private String normalizeAgreementTermsTemplate(String termsTemplate) {
                if (termsTemplate == null) {
                        return null;
                }
                String normalized = termsTemplate
                                .replace("\r\n", "\n")
                                .replace('\r', '\n')
                                .replace('\t', ' ')
                                .trim();
                if (normalized.isBlank()) {
                        return null;
                }
                if (normalized.length() > MAX_AGREEMENT_TERMS_TEMPLATE_LENGTH) {
                        throw new IllegalArgumentException(
                                        "Agreement terms template must be "
                                                        + MAX_AGREEMENT_TERMS_TEMPLATE_LENGTH
                                                        + " characters or less");
                }
                for (int i = 0; i < normalized.length(); i++) {
                        char c = normalized.charAt(i);
                        if (c < 0x20 && c != '\n') {
                                throw new IllegalArgumentException("Agreement terms template contains invalid characters");
                        }
                }
                return normalized;
        }

        private String truncateForAudit(String val) {
                if (val == null)
                        return null;
                return val.length() > 200 ? val.substring(0, 200) + "..." : val;
        }

        /**
         * Exception thrown when attempting to modify locked settings.
         */
        public static class SettingsLockedException extends RuntimeException {
                public SettingsLockedException(String message) {
                        super(message);
                }
        }
}
