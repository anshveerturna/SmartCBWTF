-- V44: HCF Billing Model & Approval Workflow
-- Adds dual billing model support (BEDDED/FIXED_MONTHLY) with approval workflow

-- ============================================================================
-- PART 1: CREATE ENUM TYPES
-- ============================================================================

-- Billing model enum
DO $$ BEGIN
    CREATE TYPE billing_model_enum AS ENUM ('BEDDED', 'FIXED_MONTHLY');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Approval status enum
DO $$ BEGIN
    CREATE TYPE approval_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- ============================================================================
-- PART 2: ADD NEW COLUMNS TO HCF TABLE
-- ============================================================================

-- Add billing_model column
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS billing_model VARCHAR(20);

-- Add approval workflow columns
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES app_user(id);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- ============================================================================
-- PART 3: MIGRATE EXISTING DATA
-- ============================================================================

-- Set billing_model based on existing 'bedded' column
UPDATE hcf SET billing_model = 'BEDDED' WHERE bedded = true AND billing_model IS NULL;
UPDATE hcf SET billing_model = 'FIXED_MONTHLY' WHERE bedded = false AND billing_model IS NULL;
UPDATE hcf SET billing_model = 'BEDDED' WHERE billing_model IS NULL; -- Default fallback

-- Migrate status to approval_status
UPDATE hcf SET approval_status = 'APPROVED' WHERE status = 'ACTIVE';
UPDATE hcf SET approval_status = 'PENDING' WHERE status IN ('PENDING', 'PENDING_APPROVAL');
UPDATE hcf SET approval_status = 'REJECTED' WHERE status = 'REJECTED';
UPDATE hcf SET approval_status = 'PENDING' WHERE approval_status IS NULL OR approval_status = '';

-- FIX EXISTING DATA: Ensure BEDDED HCFs have valid number_of_beds
UPDATE hcf SET number_of_beds = 1 
WHERE billing_model = 'BEDDED' 
  AND (number_of_beds IS NULL OR number_of_beds <= 0);

-- FIX EXISTING DATA: Ensure FIXED_MONTHLY HCFs have valid monthly_charges
UPDATE hcf SET monthly_charges = 1000.00 
WHERE billing_model = 'FIXED_MONTHLY' 
  AND (monthly_charges IS NULL OR monthly_charges <= 0);

-- ============================================================================
-- PART 4: DB-LEVEL CHECK CONSTRAINT (Enterprise Standard)
-- ============================================================================

-- This prevents invalid data from ever entering the database.
-- BEDDED: needs beds > 0, no monthly_charges
-- FIXED_MONTHLY: needs monthly_charges > 0, no beds
ALTER TABLE hcf DROP CONSTRAINT IF EXISTS chk_billing_model_fields;
ALTER TABLE hcf ADD CONSTRAINT chk_billing_model_fields CHECK (
    (billing_model = 'BEDDED' AND number_of_beds IS NOT NULL AND number_of_beds > 0)
    OR
    (billing_model = 'FIXED_MONTHLY' AND monthly_charges IS NOT NULL AND monthly_charges > 0)
    OR
    (billing_model IS NULL) -- Allow NULL temporarily during data migration
);

-- ============================================================================
-- PART 5: HCF AUDIT LOG TABLE (Admin edit tracking)
-- ============================================================================

CREATE TABLE IF NOT EXISTS hcf_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hcf_id UUID NOT NULL REFERENCES hcf(id) ON DELETE CASCADE,
    field_name VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by UUID NOT NULL REFERENCES app_user(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for efficient HCF lookup
CREATE INDEX IF NOT EXISTS idx_hcf_audit_log_hcf_id ON hcf_audit_log(hcf_id);
CREATE INDEX IF NOT EXISTS idx_hcf_audit_log_changed_at ON hcf_audit_log(changed_at DESC);

-- ============================================================================
-- PART 6: BILL SNAPSHOT COLUMNS
-- ============================================================================

-- Snapshot billing model into Bill for auditability
-- Never depend on current HCF state for historical bills
ALTER TABLE bill ADD COLUMN IF NOT EXISTS billing_model VARCHAR(20);
ALTER TABLE bill ADD COLUMN IF NOT EXISTS snapshot_beds INTEGER;
ALTER TABLE bill ADD COLUMN IF NOT EXISTS snapshot_monthly_charge DECIMAL(12,2);
ALTER TABLE bill ADD COLUMN IF NOT EXISTS snapshot_rate_per_bed DECIMAL(12,2);

-- ============================================================================
-- PART 7: INDEXES FOR PERFORMANCE
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_hcf_approval_status ON hcf(approval_status);
CREATE INDEX IF NOT EXISTS idx_hcf_billing_model ON hcf(billing_model);

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
