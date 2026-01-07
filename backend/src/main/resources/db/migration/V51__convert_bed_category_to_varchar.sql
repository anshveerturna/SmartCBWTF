-- V51: Convert bed_access_category from PostgreSQL enum to VARCHAR
-- This is needed for Hibernate's @Enumerated(EnumType.STRING) to work correctly

-- First, drop the CHECK constraints that reference the enum type
ALTER TABLE hcf DROP CONSTRAINT IF EXISTS chk_portal_access_requires_above_30_beds;
ALTER TABLE hcf DROP CONSTRAINT IF EXISTS chk_approved_category_immutable;

-- Cast the existing values to VARCHAR (text)
ALTER TABLE hcf ALTER COLUMN bed_access_category TYPE VARCHAR(20) USING bed_access_category::text;
ALTER TABLE hcf ALTER COLUMN approved_bed_access_category TYPE VARCHAR(20) USING approved_bed_access_category::text;

-- Drop the PostgreSQL enum type (no longer needed)
DROP TYPE IF EXISTS hcf_bed_access_category;

-- Recreate the constraints with VARCHAR casts
ALTER TABLE hcf ADD CONSTRAINT chk_portal_access_requires_above_30_beds 
    CHECK (portal_access_enabled = false OR bed_access_category = 'ABOVE_30_BEDS');
ALTER TABLE hcf ADD CONSTRAINT chk_approved_category_immutable 
    CHECK (approval_status <> 'APPROVED' OR approved_bed_access_category = bed_access_category);
