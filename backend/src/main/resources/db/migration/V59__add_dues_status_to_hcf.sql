-- V59: Add Dues Clear Status to HCF
-- This optimized field allows for quick access control checks without joining the requests table

ALTER TABLE hcf ADD COLUMN dues_clear_status VARCHAR(20) DEFAULT 'PENDING';

-- Constraint to ensure valid values
ALTER TABLE hcf ADD CONSTRAINT chk_hcf_dues_status 
    CHECK (dues_clear_status IN ('PENDING', 'REQUESTED', 'CLEARED'));

-- Create index for performance
CREATE INDEX idx_hcf_dues_status ON hcf(dues_clear_status);

COMMENT ON COLUMN hcf.dues_clear_status IS 'Current dues clearance status: PENDING, REQUESTED, or CLEARED. Controls access to monthly/yearly reports.';
