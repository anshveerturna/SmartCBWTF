-- V65: Update portal access constraint to allow manual override for 0-30 beds HCFs
-- This enables CBWTF admins to manually enable portal access for small HCFs

-- Drop the old constraint
ALTER TABLE hcf DROP CONSTRAINT IF EXISTS chk_portal_access_requires_above_30_beds;

-- Add updated constraint that allows portal access if:
-- 1. portal_access_enabled = false (not enabled), OR
-- 2. bed_access_category = 'ABOVE_30_BEDS' (auto-eligible), OR
-- 3. portal_access_manually_enabled = true (manual override for small HCFs)
ALTER TABLE hcf ADD CONSTRAINT chk_portal_access_requires_above_30_beds 
    CHECK (
        portal_access_enabled = false 
        OR bed_access_category = 'ABOVE_30_BEDS' 
        OR portal_access_manually_enabled = true
    );
