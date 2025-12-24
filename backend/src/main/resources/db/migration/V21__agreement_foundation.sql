-- V21: Agreement Foundation for CBWTF Admin Portal
-- Strengthens Agreement as first-class ownership object with status enums,
-- dues tracking, termination fields, and snapshot support

-- 1. Add dues_status to Agreement
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS dues_status VARCHAR(20) DEFAULT 'CLEAR';

-- 2. Add termination fields
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS termination_reason TEXT;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS terminated_at TIMESTAMP;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS terminated_by UUID REFERENCES app_user(id);

-- 3. Add created_by for audit
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES app_user(id);

-- 4. Update status values: migrate old statuses to new enum values
UPDATE agreement SET status = 'ACTIVE' WHERE status = 'DRAFT';
UPDATE agreement SET status = 'TERMINATED' WHERE status = 'CANCELLED';

-- 5. Create unique index for ONE ACTIVE agreement per HCF globally
-- This is the critical anti-leakage constraint
CREATE UNIQUE INDEX IF NOT EXISTS idx_agreement_one_active_per_hcf 
ON agreement(hcf_id) WHERE status = 'ACTIVE';

-- 6. Create Agreement Snapshot table for historical accuracy
CREATE TABLE IF NOT EXISTS agreement_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    snapshot_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Frozen values at time of snapshot
    agreement_number VARCHAR(100) NOT NULL,
    hcf_id UUID NOT NULL,
    hcf_name VARCHAR(255) NOT NULL,
    hcf_gst VARCHAR(20),
    hcf_pan VARCHAR(20),
    hcf_address TEXT,
    hcf_beds INTEGER,
    facility_id UUID NOT NULL,
    facility_name VARCHAR(255) NOT NULL,
    per_bed_per_day_rate DECIMAL(10,2) NOT NULL,
    terms_text TEXT,
    status VARCHAR(20) NOT NULL,
    dues_status VARCHAR(20),
    
    -- Why this snapshot was created
    snapshot_reason VARCHAR(50) NOT NULL,
    -- INVOICE_GENERATED, CPCB_REPORT, EXPORT_JOB, DISPUTE_OPENED
    
    created_by UUID REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_agreement_snapshot_agreement 
ON agreement_snapshot(agreement_id);

-- 7. Create Agreement Code Sequence table for human-readable codes
CREATE TABLE IF NOT EXISTS agreement_code_sequence (
    year INTEGER PRIMARY KEY,
    last_value INTEGER NOT NULL DEFAULT 0
);

-- Initialize current year if not exists
INSERT INTO agreement_code_sequence (year, last_value)
VALUES (EXTRACT(YEAR FROM NOW())::INTEGER, 0)
ON CONFLICT (year) DO NOTHING;

-- 8. Add identity_hash to HCF for anti-fraud fingerprinting
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS identity_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_hcf_identity_hash ON hcf(identity_hash);

-- 9. Add index for dues status queries
CREATE INDEX IF NOT EXISTS idx_agreement_dues_status ON agreement(dues_status);

-- 10. Add index for facility + status queries (for CBWTF admin portal)
CREATE INDEX IF NOT EXISTS idx_agreement_facility_status 
ON agreement(facility_id, status);
