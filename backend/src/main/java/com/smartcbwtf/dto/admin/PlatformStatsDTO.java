package com.smartcbwtf.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for platform-wide statistics.
 */
public record PlatformStatsDTO(
                int totalCBWTFs,
                int activeCBWTFs,
                int trialCBWTFs,
                int expiredCBWTFs,
                int suspendedCBWTFs,
                int totalHcfs,
                int totalUsers,
                long totalBagsProcessed,
                BigDecimal totalRevenue,
                int pendingErrors,
                List<SystemErrorDTO> recentErrors,
                LocalDate lastUpdated) {

        /**
         * Represents a system error for SuperAdmin monitoring.
         */
        public record SystemErrorDTO(
                        String id,
                        String timestamp,
                        String severity,
                        String component,
                        String message,
                        String cbwtfCode,
                        boolean resolved) {
        }
}
