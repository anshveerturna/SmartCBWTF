package com.smartcbwtf.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Exception thrown when a feature is disabled for a facility.
 * Returns 403 FORBIDDEN with feature information.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class FeatureDisabledException extends RuntimeException {

    private final String featureKey;
    private final UUID facilityId;
    private final String endpoint;

    public FeatureDisabledException(String featureKey, UUID facilityId) {
        super("Feature " + featureKey + " is not enabled for this CBWTF");
        this.featureKey = featureKey;
        this.facilityId = facilityId;
        this.endpoint = null;
    }

    public FeatureDisabledException(String featureKey, UUID facilityId, String endpoint) {
        super("Feature " + featureKey + " is not enabled for this CBWTF");
        this.featureKey = featureKey;
        this.facilityId = facilityId;
        this.endpoint = endpoint;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public UUID getFacilityId() {
        return facilityId;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
