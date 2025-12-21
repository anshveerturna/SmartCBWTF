package com.smartcbwtf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declaratively require a feature flag to be enabled.
 * Backed by FeatureGuardAspect via Spring AOP.
 * 
 * Usage:
 * 
 * @RequireFeature(TenantFeatureFlag.AI_INSIGHTS)
 *                                                public ResponseEntity<?>
 *                                                getInsights() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFeature {
    /**
     * The feature key that must be enabled (e.g., TenantFeatureFlag.AI_INSIGHTS).
     */
    String value();
}
