package com.smartcbwtf.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result for a single attendance sync item.
 */
public class AttendanceSyncItemResult {
    private UUID clientEventId;
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private Long cooldownRemainingMs;

    public AttendanceSyncItemResult() {}

    public static AttendanceSyncItemResult success(UUID clientEventId) {
        AttendanceSyncItemResult r = new AttendanceSyncItemResult();
        r.clientEventId = clientEventId;
        r.success = true;
        return r;
    }

    public static AttendanceSyncItemResult error(UUID clientEventId, String errorCode, String errorMessage) {
        AttendanceSyncItemResult r = new AttendanceSyncItemResult();
        r.clientEventId = clientEventId;
        r.success = false;
        r.errorCode = errorCode;
        r.errorMessage = errorMessage;
        return r;
    }

    public static AttendanceSyncItemResult cooldownError(UUID clientEventId, long remainingMs) {
        AttendanceSyncItemResult r = new AttendanceSyncItemResult();
        r.clientEventId = clientEventId;
        r.success = false;
        r.errorCode = "COOLDOWN_ACTIVE";
        r.errorMessage = "Attendance cooldown is active";
        r.cooldownRemainingMs = remainingMs;
        return r;
    }

    // Getters and setters
    public UUID getClientEventId() { return clientEventId; }
    public void setClientEventId(UUID clientEventId) { this.clientEventId = clientEventId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getCooldownRemainingMs() { return cooldownRemainingMs; }
    public void setCooldownRemainingMs(Long cooldownRemainingMs) { this.cooldownRemainingMs = cooldownRemainingMs; }
}
