package com.smartcbwtf.dto.admin;

import java.time.LocalDate;

/**
 * Response DTO for platform-wide statistics.
 */
public record PlatformStatsDTO(
        int totalTenants,
        int activeTenants,
        int trialTenants,
        int expiredTenants,
        int suspendedTenants,
        int totalHcfs,
        int totalUsers,
        long totalBagsProcessed,
        LocalDate lastUpdated) {
}
