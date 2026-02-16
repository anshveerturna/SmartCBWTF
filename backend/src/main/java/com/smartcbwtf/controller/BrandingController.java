package com.smartcbwtf.controller;

import com.smartcbwtf.dto.settings.BrandingDTO;
import com.smartcbwtf.service.BrandingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * REST Controller for Branding management (logo, colors, footer).
 */
@RestController
@RequestMapping("/api/cbwtf/branding")
@PreAuthorize("hasRole('CBWTF_ADMIN')")
public class BrandingController {

    private final BrandingService brandingService;

    public BrandingController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    /**
     * Get current branding configuration.
     */
    @GetMapping
    public ResponseEntity<BrandingDTO> getBranding() {
        return ResponseEntity.ok(brandingService.getBranding());
    }

    /**
     * Update branding configuration.
     */
    @PutMapping
    public ResponseEntity<Void> updateBranding(
            @Valid @RequestBody BrandingDTO dto,
            HttpServletRequest request) {
        brandingService.updateBranding(dto, extractIpAddress(request));
        return ResponseEntity.ok().build();
    }

    /**
     * Upload a new logo.
     */
    @PostMapping("/logo")
    public ResponseEntity<Map<String, String>> uploadLogo(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        String logoUrl = brandingService.uploadLogo(file, extractIpAddress(request));
        return ResponseEntity.ok(Map.of("logoUrl", logoUrl, "message", "Logo uploaded successfully"));
    }

    /**
     * Delete logo.
     */
    @DeleteMapping("/logo")
    public ResponseEntity<Void> deleteLogo(HttpServletRequest request) {
        brandingService.deleteLogo(extractIpAddress(request));
        return ResponseEntity.ok().build();
    }

    /**
     * Upload a payment QR code image.
     */
    @PostMapping("/payment-qr")
    public ResponseEntity<Map<String, String>> uploadPaymentQr(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        String qrUrl = brandingService.uploadPaymentQr(file, extractIpAddress(request));
        return ResponseEntity.ok(Map.of("paymentQrUrl", qrUrl, "message", "Payment QR uploaded successfully"));
    }

    /**
     * Delete payment QR code.
     */
    @DeleteMapping("/payment-qr")
    public ResponseEntity<Void> deletePaymentQr(HttpServletRequest request) {
        brandingService.deletePaymentQr(extractIpAddress(request));
        return ResponseEntity.ok().build();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
