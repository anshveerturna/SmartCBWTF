package com.smartcbwtf.controller;

import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.dto.*;
import com.smartcbwtf.service.QrAuthorizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Mobile QR Controller for Android app scan operations.
 * 
 * ACCESS: DRIVER, PLANT_OPERATOR
 * Used for pickup and verification scans.
 */
@RestController
@RequestMapping("/api/mobile/qr")
@PreAuthorize("hasAnyRole('DRIVER', 'PLANT_OPERATOR')")
public class MobileQrController {

    private static final Logger log = LoggerFactory.getLogger(MobileQrController.class);

    private final QrAuthorizationService qrService;

    public MobileQrController(QrAuthorizationService qrService) {
        this.qrService = qrService;
    }

    /**
     * Scan QR for waste pickup at HCF.
     * 
     * Flow:
     * 1. Driver scans QR
     * 2. Backend validates QR (status, validity, agreement)
     * 3. QR marked as USED
     * 4. Returns validation result
     * 
     * The mobile app should:
     * 1. Create BagEvent first
     * 2. Call this endpoint with pickupEventId
     * 3. Proceed if successful
     */
    @PostMapping("/scan/pickup")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<?> scanForPickup(@Valid @RequestBody QrScanRequest request) {
        UUID userId = TenantContext.getUserId();

        if (request.getPickupEventId() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "pickupEventId is required"));
        }

        QrAuthorizationService.QrValidationResult result = qrService.validateAndMarkUsed(
                request.getQrPayloadJson(),
                request.getPickupEventId(),
                userId);

        if (!result.valid()) {
            log.warn("QR pickup scan failed: {}", result.message());
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", result.message()));
        }

        log.info("QR pickup scan successful: {} by driver {}", result.qr().getId(), userId);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "qrId", result.qr().getId(),
                "wasteCategory", result.qr().getWasteCategory(),
                "hcfName", result.qr().getHcf().getName()));
    }

    /**
     * Scan QR for verification at CBWTF.
     * 
     * Flow:
     * 1. Operator scans same QR that was used for pickup
     * 2. Backend validates QR is in USED status
     * 3. QR marked as VERIFIED
     * 4. Returns validation result
     */
    @PostMapping("/scan/verify")
    @PreAuthorize("hasRole('PLANT_OPERATOR')")
    public ResponseEntity<?> scanForVerification(@Valid @RequestBody QrScanRequest request) {
        UUID userId = TenantContext.getUserId();

        QrAuthorizationService.QrValidationResult result = qrService.validateAndMarkVerified(
                request.getQrPayloadJson(),
                userId);

        if (!result.valid()) {
            log.warn("QR verification scan failed: {}", result.message());
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", result.message()));
        }

        log.info("QR verification scan successful: {} by operator {}", result.qr().getId(), userId);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "qrId", result.qr().getId(),
                "wasteCategory", result.qr().getWasteCategory(),
                "hcfName", result.qr().getHcf().getName(),
                "verifiedAt", result.qr().getVerifiedAt()));
    }
}
