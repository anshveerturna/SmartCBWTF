package com.smartcbwtf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for computing HCF identity fingerprints.
 * Used for duplicate detection and anti-fraud measures.
 * 
 * Fingerprint = SHA256(normalized(name + gst + pan + lat/lon))
 */
@Service
public class HCFIdentityService {

    private static final Logger log = LoggerFactory.getLogger(HCFIdentityService.class);

    /**
     * Compute identity fingerprint for HCF.
     * 
     * @param name HCF name
     * @param gst  GST number
     * @param pan  PAN number
     * @param lat  Latitude
     * @param lon  Longitude
     * @return SHA256 hash as hex string
     */
    public String computeFingerprint(String name, String gst, String pan, Double lat, Double lon) {
        String normalized = normalize(name) + "|" +
                normalize(gst) + "|" +
                normalize(pan) + "|" +
                roundCoords(lat, lon, 3); // 3 decimal places ≈ 100m precision

        String hash = sha256(normalized);
        log.debug("Computed identity hash for HCF: {} -> {}",
                mask(normalized), hash.substring(0, 16) + "...");
        return hash;
    }

    /**
     * Check if two fingerprints are similar (exact match for now).
     * Future: could implement fuzzy matching.
     */
    public boolean areSimilar(String hash1, String hash2) {
        if (hash1 == null || hash2 == null)
            return false;
        return hash1.equals(hash2);
    }

    /**
     * Normalize a string for consistent hashing.
     * - Uppercase
     * - Remove special characters
     * - Trim whitespace
     */
    private String normalize(String s) {
        if (s == null)
            return "";
        return s.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .trim();
    }

    /**
     * Round coordinates to specified decimal places.
     * 3 decimals ≈ 100m precision
     * 4 decimals ≈ 10m precision
     */
    private String roundCoords(Double lat, Double lon, int decimals) {
        if (lat == null || lon == null)
            return "0|0";
        double factor = Math.pow(10, decimals);
        return Math.round(lat * factor) + "|" + Math.round(lon * factor);
    }

    /**
     * Compute SHA256 hash of input string.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Mask sensitive data for logging.
     */
    private String mask(String s) {
        if (s == null || s.length() <= 8)
            return "****";
        return s.substring(0, 4) + "..." + s.substring(s.length() - 4);
    }
}
