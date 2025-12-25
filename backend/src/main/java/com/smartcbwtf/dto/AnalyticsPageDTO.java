package com.smartcbwtf.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for the dedicated Analytics Page.
 * All percentages are computed server-side.
 */
public class AnalyticsPageDTO {

    /**
     * Response for total waste collected endpoint.
     */
    public record TotalWasteResponse(
            BigDecimal totalWeightKg,
            String periodLabel,
            long eventCount) {
    }

    /**
     * Response for waste by category endpoint.
     */
    public record WasteByCategoryResponse(
            List<CategoryBreakdown> categories,
            BigDecimal grandTotalKg) {
    }

    /**
     * Individual category breakdown with weight and percentage.
     */
    public record CategoryBreakdown(
            String category,
            BigDecimal weightKg,
            BigDecimal percentContribution) {
    }

    /**
     * HCF option for dropdown selector.
     */
    public record HcfOption(
            UUID id,
            String name) {
    }
}
