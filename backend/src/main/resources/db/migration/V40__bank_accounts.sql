-- =====================================================
-- V40: BANK ACCOUNTS ENHANCEMENT FOR PHASE 10
-- Add status, upi_id, created_by, disabled_at columns
-- =====================================================

-- Add new columns to existing bank_account table
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100);
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS disabled_at TIMESTAMPTZ;

-- Create index for status-based queries
CREATE INDEX IF NOT EXISTS idx_bank_account_facility_status ON bank_account(facility_id, status);
