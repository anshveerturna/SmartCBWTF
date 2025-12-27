package com.smartcbwtf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for deactivating an HCF (expiring/terminating agreement).
 */
public class DeactivateHcfRequest {

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    /**
     * Whether to terminate (true) or expire (false) the agreement.
     * TERMINATED = early manual termination
     * EXPIRED = natural end or soft deactivation
     */
    private boolean terminate = false;

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }
}
