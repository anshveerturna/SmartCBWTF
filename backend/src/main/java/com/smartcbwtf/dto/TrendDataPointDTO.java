package com.smartcbwtf.dto;

/**
 * Trend data point DTO for charts.
 */
public record TrendDataPointDTO(
        String date,
        double value,
        String category) {
}
