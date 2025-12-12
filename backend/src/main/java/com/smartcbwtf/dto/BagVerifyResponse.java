package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for bag verification at CBWTF facility.
 */
public class BagVerifyResponse {

    private String status;        // "OK" or error status
    private UUID bagLabelId;
    private Instant verifiedAt;   // Server timestamp
    private String anomalyState;  // "OK" | "MISMATCH" | "OUT_OF_GEOFENCE"
    private BigDecimal deltaKg;   // Weight difference from HCF collection
    private String message;

    // Constructor for success
    public static BagVerifyResponse success(UUID bagLabelId, Instant verifiedAt, String anomalyState, BigDecimal deltaKg) {
        BagVerifyResponse response = new BagVerifyResponse();
        response.status = "OK";
        response.bagLabelId = bagLabelId;
        response.verifiedAt = verifiedAt;
        response.anomalyState = anomalyState;
        response.deltaKg = deltaKg;
        return response;
    }

    // Constructor for error
    public static BagVerifyResponse error(String status, String message) {
        BagVerifyResponse response = new BagVerifyResponse();
        response.status = status;
        response.message = message;
        return response;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getBagLabelId() {
        return bagLabelId;
    }

    public void setBagLabelId(UUID bagLabelId) {
        this.bagLabelId = bagLabelId;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getAnomalyState() {
        return anomalyState;
    }

    public void setAnomalyState(String anomalyState) {
        this.anomalyState = anomalyState;
    }

    public BigDecimal getDeltaKg() {
        return deltaKg;
    }

    public void setDeltaKg(BigDecimal deltaKg) {
        this.deltaKg = deltaKg;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
