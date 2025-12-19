package com.smartcbwtf.dto;

/**
 * Category breakdown DTO for waste analytics.
 */
public record CategoryBreakdownDTO(
        String category,
        int bags,
        double weightKg,
        double percentage) {
}
