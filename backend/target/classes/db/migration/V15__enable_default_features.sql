-- V15: Enable all default features for existing CBWTFs

-- Insert all feature flags as enabled for all existing facilities
INSERT INTO tenant_feature_flag (facility_id, feature_key, enabled, created_at, updated_at)
SELECT f.id, feature_key, true, NOW(), NOW()
FROM facility f
CROSS JOIN (
    VALUES 
        ('ADVANCED_ANALYTICS'),
        ('ROUTE_OPTIMIZATION'),
        ('CPCB_REPORTING'),
        ('INVOICE_AUTO_SEND'),
        ('PAYMENT_GATEWAY'),
        ('ATTENDANCE_ENFORCEMENT'),
        ('VEHICLE_TRACKING'),
        ('AI_INSIGHTS'),
        ('MULTI_VEHICLE'),
        ('HCF_SELF_SERVICE')
) AS features(feature_key)
ON CONFLICT (facility_id, feature_key) DO UPDATE SET enabled = true, updated_at = NOW();
