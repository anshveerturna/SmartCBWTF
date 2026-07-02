package com.smartcbwtf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcbwtf.config.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * QR Signing Service - Generates and verifies HMAC signatures for QR payloads.
 * 
 * Uses HMAC-SHA256 to create tamper-proof checksums.
 * The signing key is derived from the JWT secret.
 */
@Service
public class QrSigningService {

    private static final Logger log = LoggerFactory.getLogger(QrSigningService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] signingKey;

    public QrSigningService(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret}") String jwtSecret) {
        this.objectMapper = objectMapper;
        JwtService.assertStrongSigningSecret("security.jwt.secret", jwtSecret);
        // Derive QR signing key from JWT secret with a prefix for isolation
        this.signingKey = ("QR_SIGN:" + jwtSecret).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate signed QR payload as JSON string.
     * 
     * @param qrId          UUID of the QrAuthorization record
     * @param agreementId   Agreement UUID
     * @param hcfId         HCF UUID
     * @param facilityId    Facility UUID
     * @param wasteCategory Waste category (YELLOW/RED/BLUE/WHITE)
     * @param validFrom     Start of validity
     * @param validTo       End of validity
     * @return JSON string containing signed payload
     */
    public SignedPayload generateSignedPayload(
            UUID qrId,
            UUID agreementId,
            UUID hcfId,
            UUID facilityId,
            String wasteCategory,
            Instant validFrom,
            Instant validTo) {

        try {
            Map<String, Object> payload = canonicalPayload(
                    qrId.toString(),
                    agreementId.toString(),
                    hcfId.toString(),
                    facilityId.toString(),
                    wasteCategory,
                    validFrom.toString(),
                    validTo.toString());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String checksum = computeHmac(payloadJson);

            // Build final payload with checksum
            Map<String, Object> signedPayload = new LinkedHashMap<>(payload);
            signedPayload.put("checksum", checksum);

            String finalJson = objectMapper.writeValueAsString(signedPayload);

            log.debug("Generated signed QR payload for qrId={}", qrId);
            return new SignedPayload(finalJson, checksum);

        } catch (Exception e) {
            log.error("Failed to generate signed payload", e);
            throw new RuntimeException("Failed to sign QR payload", e);
        }
    }

    /**
     * Verify checksum of a scanned QR payload.
     * 
     * @param qrPayloadJson JSON string from scanned QR
     * @return true if checksum is valid
     */
    public boolean verifyChecksum(String qrPayloadJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(qrPayloadJson, Map.class);

            String providedChecksum = (String) payload.get("checksum");
            if (providedChecksum == null) {
                log.warn("QR payload missing checksum");
                return false;
            }

            // Rebuild payload without checksum to recompute
            Map<String, Object> payloadWithoutChecksum = canonicalPayload(
                    payload.get("qrId"),
                    payload.get("agreementId"),
                    payload.get("hcfId"),
                    payload.get("facilityId"),
                    payload.get("wasteCategory"),
                    payload.get("validFrom"),
                    payload.get("validTo"));

            String recomputedJson = objectMapper.writeValueAsString(payloadWithoutChecksum);
            String expectedChecksum = computeHmac(recomputedJson);

            boolean valid = constantTimeEquals(expectedChecksum, providedChecksum);
            if (!valid) {
                log.warn("QR checksum mismatch - possible tampering");
            }
            return valid;

        } catch (Exception e) {
            log.error("Failed to verify checksum", e);
            return false;
        }
    }

    /**
     * Extract QR ID from payload without full validation.
     * Used for quick lookup before detailed validation.
     */
    public UUID extractQrId(String qrPayloadJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(qrPayloadJson, Map.class);
            String qrIdStr = (String) payload.get("qrId");
            return qrIdStr != null ? UUID.fromString(qrIdStr) : null;
        } catch (Exception e) {
            log.warn("Failed to extract qrId from payload", e);
            return null;
        }
    }

    /**
     * Compute HMAC-SHA256 of input string.
     */
    private String computeHmac(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private Map<String, Object> canonicalPayload(
            Object qrId,
            Object agreementId,
            Object hcfId,
            Object facilityId,
            Object wasteCategory,
            Object validFrom,
            Object validTo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("qrId", qrId);
        payload.put("agreementId", agreementId);
        payload.put("hcfId", hcfId);
        payload.put("facilityId", facilityId);
        payload.put("wasteCategory", wasteCategory);
        payload.put("validFrom", validFrom);
        payload.put("validTo", validTo);
        return payload;
    }

    private boolean constantTimeEquals(String expectedChecksum, String providedChecksum) {
        return MessageDigest.isEqual(
                expectedChecksum.getBytes(StandardCharsets.US_ASCII),
                providedChecksum.getBytes(StandardCharsets.US_ASCII));
    }

    // Result record for signed payload
    public record SignedPayload(String json, String checksum) {
    }
}
