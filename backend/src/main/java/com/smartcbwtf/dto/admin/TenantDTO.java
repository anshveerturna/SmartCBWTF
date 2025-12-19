package com.smartcbwtf.dto.admin;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for tenant information.
 */
public record TenantDTO(
        UUID id,
        String code,
        String name,
        String address,
        String contactEmail,
        String contactPhone,
        Double gpsLat,
        Double gpsLon,
        String subscriptionPlan,
        String subscriptionStatus,
        Instant subscriptionExpiresAt,
        Instant onboardedAt,
        int hcfCount,
        int activeUserCount,
        Map<String, Boolean> features) {
    public static TenantDTO from(
            com.smartcbwtf.domain.Facility facility,
            int hcfCount,
            int activeUserCount,
            Map<String, Boolean> features) {
        return new TenantDTO(
                facility.getId(),
                facility.getCode(),
                facility.getName(),
                facility.getAddress(),
                facility.getContactEmail(),
                facility.getContactPhone(),
                facility.getGpsLat(),
                facility.getGpsLon(),
                facility.getSubscriptionPlan(),
                facility.getSubscriptionStatus(),
                facility.getSubscriptionExpiresAt(),
                facility.getOnboardedAt(),
                hcfCount,
                activeUserCount,
                features);
    }
}
