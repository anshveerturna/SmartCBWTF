-- V20: Allow null facility_id for SuperAdmin-level audit entries
-- SuperAdmin profile management actions don't have a facility context

ALTER TABLE subscription_audit ALTER COLUMN facility_id DROP NOT NULL;

-- Add comment for documentation
COMMENT ON COLUMN subscription_audit.facility_id IS 'Facility ID - null for platform-level actions like SuperAdmin profile management';
