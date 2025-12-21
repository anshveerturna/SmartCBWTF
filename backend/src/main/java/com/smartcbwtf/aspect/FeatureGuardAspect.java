package com.smartcbwtf.aspect;

import com.smartcbwtf.annotation.RequireFeature;
import com.smartcbwtf.config.TenantContext;
import com.smartcbwtf.service.FeatureGuardService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AOP Aspect that enforces @RequireFeature annotation on controller methods.
 * 
 * Extracts tenant ID from TenantContext and calls FeatureGuardService
 * to ensure the feature is enabled before allowing method execution.
 * 
 * Order(1) ensures this runs before other business logic aspects.
 */
@Aspect
@Component
@Order(1)
public class FeatureGuardAspect {

    private static final Logger log = LoggerFactory.getLogger(FeatureGuardAspect.class);

    private final FeatureGuardService featureGuardService;

    public FeatureGuardAspect(FeatureGuardService featureGuardService) {
        this.featureGuardService = featureGuardService;
    }

    /**
     * Intercept methods annotated with @RequireFeature.
     * Check if the feature is enabled before allowing execution.
     */
    @Around("@annotation(requireFeature)")
    public Object enforceFeature(ProceedingJoinPoint joinPoint, RequireFeature requireFeature) throws Throwable {
        String featureKey = requireFeature.value();
        UUID tenantId = TenantContext.getTenantId();

        // Build endpoint context from method signature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String endpoint = signature.getDeclaringTypeName() + "." + signature.getName();

        log.debug("Feature check: {} for tenant {} at {}", featureKey, tenantId, endpoint);

        // SuperAdmin bypasses feature gates
        if (TenantContext.isSuperAdmin()) {
            log.debug("SuperAdmin bypassed feature check for {}", featureKey);
            return joinPoint.proceed();
        }

        // Enforce feature flag
        featureGuardService.assertEnabled(tenantId, featureKey, endpoint);

        return joinPoint.proceed();
    }
}
