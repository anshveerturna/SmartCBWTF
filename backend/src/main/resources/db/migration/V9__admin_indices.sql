-- V9: Indices for admin queries and audit table enhancements

-- Optimize subscription expiry queries
CREATE INDEX IF NOT EXISTS idx_facility_subscription_status 
    ON facility(subscription_status);

CREATE INDEX IF NOT EXISTS idx_facility_subscription_expiry 
    ON facility(subscription_status, subscription_expires_at);

-- Add columns to subscription_audit if not present
ALTER TABLE subscription_audit 
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(20) DEFAULT 'FACILITY';

ALTER TABLE subscription_audit 
    ADD COLUMN IF NOT EXISTS entity_id UUID;

ALTER TABLE subscription_audit 
    ADD COLUMN IF NOT EXISTS performed_by_username VARCHAR(100);

ALTER TABLE subscription_audit 
    ADD COLUMN IF NOT EXISTS performed_by_role VARCHAR(30);

-- Update entity_id from facility_id for existing rows
UPDATE subscription_audit 
    SET entity_id = facility_id 
    WHERE entity_id IS NULL AND facility_id IS NOT NULL;

-- Make entity_id NOT NULL after backfill
-- Note: Only do this if there's no existing data, otherwise handle gracefully
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM subscription_audit WHERE entity_id IS NULL LIMIT 1) THEN
        ALTER TABLE subscription_audit ALTER COLUMN entity_id SET NOT NULL;
    END IF;
END $$;

-- Index for audit queries
CREATE INDEX IF NOT EXISTS idx_audit_entity_time 
    ON subscription_audit(entity_type, entity_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_facility_time 
    ON subscription_audit(facility_id, created_at DESC);

-- Feature flags: Add index for quick lookups
CREATE INDEX IF NOT EXISTS idx_feature_flag_facility_key 
    ON tenant_feature_flag(facility_id, feature_key);
