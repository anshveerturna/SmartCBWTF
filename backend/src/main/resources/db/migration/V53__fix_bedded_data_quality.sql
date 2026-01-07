-- V53: Force Recalculate HCF Bed Access Categories & Fix Data Quality
-- Fixes incorrect backfill where HCFs > 30 beds were set to BEDS_0_TO_30 due to null 'bedded' flag

-- Drop constraints temporarily to allow updates
ALTER TABLE hcf DROP CONSTRAINT IF EXISTS chk_approved_category_immutable;
ALTER TABLE hcf DROP CONSTRAINT IF EXISTS chk_portal_access_requires_above_30_beds;

-- First, fix data quality: If an HCF has beds, it must be bedded
UPDATE hcf
SET bedded = true
WHERE number_of_beds IS NOT NULL AND number_of_beds > 0 AND (bedded IS NULL OR bedded = false);

-- Force update all records regardless of current value, now with correct bedded flag
UPDATE hcf
SET bed_access_category = CASE
    WHEN bedded = false OR bedded IS NULL THEN 'BEDS_0_TO_30'
    WHEN number_of_beds IS NULL THEN 'BEDS_0_TO_30'
    WHEN number_of_beds <= 30 THEN 'BEDS_0_TO_30'
    ELSE 'ABOVE_30_BEDS'
END,
portal_access_enabled = CASE
    WHEN bedded = true AND number_of_beds IS NOT NULL AND number_of_beds > 30 THEN TRUE
    ELSE FALSE
END;

-- Sync approved category snapshot
UPDATE hcf
SET approved_bed_access_category = bed_access_category
WHERE approval_status = 'APPROVED';

-- Recreate constraints
ALTER TABLE hcf ADD CONSTRAINT chk_portal_access_requires_above_30_beds 
    CHECK (portal_access_enabled = false OR bed_access_category = 'ABOVE_30_BEDS');

ALTER TABLE hcf ADD CONSTRAINT chk_approved_category_immutable 
    CHECK (approval_status <> 'APPROVED' OR approved_bed_access_category = bed_access_category);

