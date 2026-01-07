-- V50: HCF Bed Threshold Access Control - Backfill
-- Backfills existing HCFs based on their bed count
-- Note: V49 already created the columns and enum type

-- Only run backfill if column exists and has null values
-- The column bed_access_category was created as enum type hcf_bed_access_category by V49

-- Backfill existing HCFs based on their bed count
-- Rule: non_bedded OR null beds OR beds <= 30 → BEDS_0_TO_30
--       beds > 30 → ABOVE_30_BEDS
UPDATE hcf
SET bed_access_category = CASE
    WHEN bedded = false OR bedded IS NULL THEN 'BEDS_0_TO_30'::hcf_bed_access_category
    WHEN number_of_beds IS NULL THEN 'BEDS_0_TO_30'::hcf_bed_access_category
    WHEN number_of_beds <= 30 THEN 'BEDS_0_TO_30'::hcf_bed_access_category
    ELSE 'ABOVE_30_BEDS'::hcf_bed_access_category
END,
portal_access_enabled = CASE
    WHEN bedded = true AND number_of_beds IS NOT NULL AND number_of_beds > 30 THEN TRUE
    ELSE FALSE
END
WHERE bed_access_category IS NULL;

-- For already approved HCFs, also set the approved_bed_access_category
UPDATE hcf
SET approved_bed_access_category = bed_access_category
WHERE approval_status = 'APPROVED' AND approved_bed_access_category IS NULL;

-- Create index for filtering by category if not exists
CREATE INDEX IF NOT EXISTS idx_hcf_bed_access_category ON hcf(bed_access_category);

