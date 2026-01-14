-- V49: HCF Bed Threshold Access Control - Schema Changes
-- Creates the bed access category enum and adds columns to HCF table

-- Create enum type for bed access category
DO $$ BEGIN
    CREATE TYPE hcf_bed_access_category AS ENUM ('BEDS_0_TO_30', 'ABOVE_30_BEDS');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Add bed_access_category column if not exists
DO $$ BEGIN
    ALTER TABLE hcf ADD COLUMN bed_access_category hcf_bed_access_category;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

-- Add approved_bed_access_category column if not exists
DO $$ BEGIN
    ALTER TABLE hcf ADD COLUMN approved_bed_access_category hcf_bed_access_category;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

-- Add portal_access_enabled column if not exists
DO $$ BEGIN
    ALTER TABLE hcf ADD COLUMN portal_access_enabled BOOLEAN DEFAULT FALSE;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;

-- Add bedded column if not exists (for non-bedded HCFs like pharmacies)
DO $$ BEGIN
    ALTER TABLE hcf ADD COLUMN bedded BOOLEAN DEFAULT TRUE;
EXCEPTION
    WHEN duplicate_column THEN null;
END $$;
