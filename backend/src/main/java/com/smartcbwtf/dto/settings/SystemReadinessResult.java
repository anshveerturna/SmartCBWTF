package com.smartcbwtf.dto.settings;

import java.util.List;

/**
 * Result of system readiness check.
 * Used to enforce fail-closed behavior when required settings are missing.
 */
public record SystemReadinessResult(
        boolean ready,
        List<String> errors) {
    public static SystemReadinessResult success() {
        return new SystemReadinessResult(true, List.of());
    }

    public static SystemReadinessResult failure(List<String> errors) {
        return new SystemReadinessResult(false, errors);
    }
}
