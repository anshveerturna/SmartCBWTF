package com.smartcbwtf.domain.enums;

/**
 * Status values for collection routes.
 * 
 * DRAFT - Route is being designed, not yet operational
 * ACTIVE - Route is in active use for waste collection
 * TEMPORARILY_SUSPENDED - Route paused (festivals, vehicle maintenance,
 * seasonal)
 */
public enum RouteStatus {
    DRAFT,
    ACTIVE,
    TEMPORARILY_SUSPENDED
}
