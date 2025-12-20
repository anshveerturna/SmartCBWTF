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
        String ownerName,
        String contactEmail,
        String contactPhone,
        Double gpsLat,
        Double gpsLon,
        String panNumber,
        String gstNumber,
        String aadharNumber,
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
                facility.getOwnerName(),
                facility.getContactEmail(),
                facility.getContactPhone(),
                facility.getGpsLat(),
                facility.getGpsLon(),
                facility.getPanNumber(),
                facility.getGstNumber(),
                facility.getAadharNumber(),
                facility.getSubscriptionPlan(),
                facility.getSubscriptionStatus(),
                facility.getSubscriptionExpiresAt(),
                facility.getOnboardedAt(),
                hcfCount,
                activeUserCount,
                features);
    }
}
