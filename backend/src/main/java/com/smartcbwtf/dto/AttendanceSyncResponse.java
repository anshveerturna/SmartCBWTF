package com.smartcbwtf.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response for batch attendance sync.
 */
public class AttendanceSyncResponse {
    private int totalReceived;
    private int successCount;
    private int failureCount;
    private List<UUID> successIds = new ArrayList<>();
    private List<AttendanceSyncItemResult> results = new ArrayList<>();

    public int getTotalReceived() { return totalReceived; }
    public void setTotalReceived(int totalReceived) { this.totalReceived = totalReceived; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    public List<UUID> getSuccessIds() { return successIds; }
    public void setSuccessIds(List<UUID> successIds) { this.successIds = successIds; }

    public List<AttendanceSyncItemResult> getResults() { return results; }
    public void setResults(List<AttendanceSyncItemResult> results) { this.results = results; }
}
