package com.smartcbwtf.dto;

import java.util.List;

/**
 * Dashboard metrics response DTO.
 */
public record DashboardMetricsDTO(
        int totalBags,
        double totalWeightKg,
        int verifiedBags,
        int mismatchCount,
        int missingBags,
        double blueWastePercentage,
        List<CategoryBreakdownDTO> categoryBreakdown,
        double revenueInvoiced,
        double revenueCollected,
        double revenueOutstanding) {
}
