package com.smartcbwtf.service;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.domain.BrandingSnapshot;
import com.smartcbwtf.domain.FacilityBranding;
import com.smartcbwtf.domain.FacilitySettings;
import com.smartcbwtf.domain.SettingsAuditLog;
import com.smartcbwtf.dto.settings.BrandingDTO;
import com.smartcbwtf.repository.BrandingSnapshotRepository;
import com.smartcbwtf.repository.FacilityBrandingRepository;
import com.smartcbwtf.repository.FacilitySettingsRepository;
import com.smartcbwtf.repository.SettingsAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for managing facility branding (logo, colors, footer text).
 */
@Service
@Transactional
public class BrandingService {

    private static final Logger log = LoggerFactory.getLogger(BrandingService.class);
    private static final String LOGO_UPLOAD_DIR = "uploads/branding";
    private static final String PAYMENT_QR_UPLOAD_DIR = "uploads/payment-qr";

    private final FacilityBrandingRepository brandingRepository;
    private final BrandingSnapshotRepository snapshotRepository;
    private final SettingsAuditLogRepository auditLogRepository;
    private final FacilitySettingsRepository settingsRepository;

    public BrandingService(FacilityBrandingRepository brandingRepository,
            BrandingSnapshotRepository snapshotRepository,
            SettingsAuditLogRepository auditLogRepository,
            FacilitySettingsRepository settingsRepository) {
        this.brandingRepository = brandingRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditLogRepository = auditLogRepository;
        this.settingsRepository = settingsRepository;
    }

    /**
     * Get branding for current facility.
     */
    public BrandingDTO getBranding() {
        UUID facilityId = TenantContext.getTenantId();
        FacilityBranding branding = brandingRepository.findById(facilityId)
                .orElseGet(() -> createDefaultBranding(facilityId));
        return toDTO(branding);
    }

    /**
     * Update branding settings.
     */
    public void updateBranding(BrandingDTO dto, String ipAddress) {
        UUID facilityId = TenantContext.getTenantId();
        FacilityBranding branding = brandingRepository.findById(facilityId)
                .orElseGet(() -> createDefaultBranding(facilityId));

        String oldPrimary = branding.getPrimaryColor();

        branding.setPrimaryColor(dto.getPrimaryColor());
        branding.setSecondaryColor(dto.getSecondaryColor());
        branding.setInvoiceFooterText(dto.getInvoiceFooterText());
        branding.setReceiptFooterText(dto.getReceiptFooterText());
        branding.setShowLogoOnInvoice(dto.getShowLogoOnInvoice());
        branding.setShowLogoOnReceipt(dto.getShowLogoOnReceipt());
        branding.setShowLogoOnEmail(dto.getShowLogoOnEmail());
        branding.setUpdatedAt(Instant.now());

        brandingRepository.save(branding);

        auditLog(facilityId, "branding", "primaryColor", oldPrimary, dto.getPrimaryColor(), ipAddress);

        log.info("Updated branding for facility {}", facilityId);
    }

    /**
     * Upload a new logo.
     */
    public String uploadLogo(MultipartFile file, String ipAddress) throws IOException {
        UUID facilityId = TenantContext.getTenantId();

        // Validate file
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("Only PNG and JPEG images are allowed");
        }

        if (file.getSize() > 2 * 1024 * 1024) { // 2MB limit
            throw new IllegalArgumentException("Logo file must be under 2MB");
        }

        // Compute checksum
        String checksum = computeChecksum(file.getBytes());

        // Create directory if not exists
        Path uploadDir = Paths.get(LOGO_UPLOAD_DIR, facilityId.toString());
        Files.createDirectories(uploadDir);

        // Save file
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = "logo_" + System.currentTimeMillis() + extension;
        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());

        // Update branding
        FacilityBranding branding = brandingRepository.findById(facilityId)
                .orElseGet(() -> createDefaultBranding(facilityId));

        // Create relative URL for browser access
        String oldLogo = branding.getLogoUrl();
        String logoUrl = "/" + LOGO_UPLOAD_DIR + "/" + facilityId.toString() + "/" + filename;
        branding.setLogoUrl(logoUrl);
        branding.setLogoChecksum(checksum);
        branding.setUpdatedAt(Instant.now());
        brandingRepository.save(branding);

        auditLog(facilityId, "branding", "logo", oldLogo, logoUrl, ipAddress);

        log.info("Uploaded logo for facility {}: {} (checksum: {})", facilityId, logoUrl, checksum);

        return logoUrl;
    }

    /**
     * Upload a payment QR code image.
     * Saves the image and stores the URL in FacilitySettings.
     */
    public String uploadPaymentQr(MultipartFile file, String ipAddress) throws IOException {
        UUID facilityId = TenantContext.getTenantId();

        // Validate file
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("Only PNG and JPEG images are allowed");
        }

        if (file.getSize() > 2 * 1024 * 1024) { // 2MB limit
            throw new IllegalArgumentException("Payment QR file must be under 2MB");
        }

        // Create directory if not exists
        Path uploadDir = Paths.get(PAYMENT_QR_UPLOAD_DIR, facilityId.toString());
        Files.createDirectories(uploadDir);

        // Save file
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = "payment_qr_" + System.currentTimeMillis() + extension;
        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());

        // Update settings
        FacilitySettings settings = settingsRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility settings not found"));

        String oldUrl = settings.getPaymentQrUrl();
        String qrUrl = "/" + PAYMENT_QR_UPLOAD_DIR + "/" + facilityId.toString() + "/" + filename;
        settings.setPaymentQrUrl(qrUrl);
        settingsRepository.save(settings);

        auditLog(facilityId, "financial", "paymentQrUrl", oldUrl, qrUrl, ipAddress);

        log.info("Uploaded payment QR for facility {}: {}", facilityId, qrUrl);

        return qrUrl;
    }

    /**
     * Delete payment QR code.
     */
    public void deletePaymentQr(String ipAddress) {
        UUID facilityId = TenantContext.getTenantId();
        FacilitySettings settings = settingsRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalStateException("Facility settings not found"));

        if (settings.getPaymentQrUrl() != null) {
            String oldUrl = settings.getPaymentQrUrl();
            settings.setPaymentQrUrl(null);
            settingsRepository.save(settings);

            auditLog(facilityId, "financial", "paymentQrUrl", oldUrl, null, ipAddress);

            log.info("Deleted payment QR for facility {}", facilityId);
        }
    }

    /**
     * Delete logo.
     */
    public void deleteLogo(String ipAddress) {
        UUID facilityId = TenantContext.getTenantId();
        FacilityBranding branding = brandingRepository.findById(facilityId).orElse(null);

        if (branding != null && branding.getLogoUrl() != null) {
            String oldLogo = branding.getLogoUrl();
            branding.setLogoUrl(null);
            branding.setLogoChecksum(null);
            branding.setUpdatedAt(Instant.now());
            brandingRepository.save(branding);

            auditLog(facilityId, "branding", "logo", oldLogo, null, ipAddress);

            log.info("Deleted logo for facility {}", facilityId);
        }
    }

    /**
     * Create a branding snapshot for document generation.
     * This freezes the current branding state.
     */
    public BrandingSnapshot createSnapshot(UUID facilityId, String documentType) {
        FacilityBranding branding = brandingRepository.findById(facilityId)
                .orElseGet(() -> createDefaultBranding(facilityId));

        BrandingSnapshot snapshot = new BrandingSnapshot();
        snapshot.setFacilityId(facilityId);
        snapshot.setLogoUrl(branding.getLogoUrl());
        snapshot.setLogoChecksum(branding.getLogoChecksum());
        snapshot.setPrimaryColor(branding.getPrimaryColor());
        snapshot.setSecondaryColor(branding.getSecondaryColor());

        // Use appropriate footer based on document type
        if ("receipt".equalsIgnoreCase(documentType)) {
            snapshot.setFooterText(branding.getReceiptFooterText());
        } else {
            snapshot.setFooterText(branding.getInvoiceFooterText());
        }

        snapshot.setCreatedAt(Instant.now());

        return snapshotRepository.save(snapshot);
    }

    /**
     * Get snapshot for rendering a document.
     */
    public BrandingSnapshot getSnapshot(UUID snapshotId) {
        return snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Branding snapshot not found: " + snapshotId));
    }

    private FacilityBranding createDefaultBranding(UUID facilityId) {
        FacilityBranding branding = new FacilityBranding();
        branding.setFacilityId(facilityId);
        branding.setPrimaryColor("#1976d2");
        branding.setSecondaryColor("#424242");
        branding.setShowLogoOnInvoice(true);
        branding.setShowLogoOnReceipt(true);
        branding.setShowLogoOnEmail(true);
        branding.setUpdatedAt(Instant.now());
        return brandingRepository.save(branding);
    }

    private String computeChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private BrandingDTO toDTO(FacilityBranding b) {
        BrandingDTO dto = new BrandingDTO();
        dto.setLogoUrl(b.getLogoUrl());
        dto.setLogoChecksum(b.getLogoChecksum());
        dto.setPrimaryColor(b.getPrimaryColor());
        dto.setSecondaryColor(b.getSecondaryColor());
        dto.setInvoiceFooterText(b.getInvoiceFooterText());
        dto.setReceiptFooterText(b.getReceiptFooterText());
        dto.setShowLogoOnInvoice(b.getShowLogoOnInvoice());
        dto.setShowLogoOnReceipt(b.getShowLogoOnReceipt());
        dto.setShowLogoOnEmail(b.getShowLogoOnEmail());
        return dto;
    }

    private void auditLog(UUID facilityId, String section, String key, String oldValue, String newValue,
            String ipAddress) {
        SettingsAuditLog log = new SettingsAuditLog();
        log.setFacilityId(facilityId);
        log.setSection(section);
        log.setSettingKey(key);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangedBy(TenantContext.getUserId());
        log.setIpAddress(ipAddress);
        log.setChangedAt(Instant.now());
        auditLogRepository.save(log);
    }
}
