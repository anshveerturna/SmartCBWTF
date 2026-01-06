-- V48: Bill Adjustment System
-- Adds adjustment/concession fields to bill table and creates bill_version audit table

-- Add adjustment fields to bill table
ALTER TABLE bill 
ADD COLUMN IF NOT EXISTS adjustment_amount DECIMAL(12,2),
ADD COLUMN IF NOT EXISTS adjustment_reason TEXT,
ADD COLUMN IF NOT EXISTS adjusted_by UUID,
ADD COLUMN IF NOT EXISTS adjusted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS bill_version INTEGER DEFAULT 1,
ADD COLUMN IF NOT EXISTS final_payable_amount DECIMAL(12,2);

-- Increase status column length to accommodate FINALIZED_WITH_ADJUSTMENT
ALTER TABLE bill ALTER COLUMN status TYPE VARCHAR(30);

-- Create bill_version table for audit trail
CREATE TABLE IF NOT EXISTS bill_version (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL REFERENCES bill(id) ON DELETE RESTRICT,
    version INTEGER NOT NULL,
    original_total DECIMAL(12,2) NOT NULL,
    adjustment_amount DECIMAL(12,2),
    final_amount DECIMAL(12,2) NOT NULL,
    adjustment_reason VARCHAR(500),
    adjusted_by UUID NOT NULL,
    adjusted_at TIMESTAMP NOT NULL,
    UNIQUE(bill_id, version)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_bill_version_bill ON bill_version(bill_id);
CREATE INDEX IF NOT EXISTS idx_bill_version_adjusted_at ON bill_version(adjusted_at);

-- Backfill final_payable_amount = total_amount for existing bills
UPDATE bill SET final_payable_amount = total_amount WHERE final_payable_amount IS NULL;

-- Ensure bill_version defaults to 1 for existing bills
UPDATE bill SET bill_version = 1 WHERE bill_version IS NULL;

-- Add NOT NULL constraint after backfill
ALTER TABLE bill ALTER COLUMN bill_version SET NOT NULL;

COMMENT ON TABLE bill_version IS 'Immutable audit trail for bill adjustments (concessions). Never delete.';
COMMENT ON COLUMN bill.adjustment_amount IS 'Concession amount (negative value). Original bill total is preserved.';
COMMENT ON COLUMN bill.final_payable_amount IS 'total_amount + adjustment_amount. This is what HCF pays.';
COMMENT ON COLUMN bill.status IS 'DRAFT, FINALIZED, or FINALIZED_WITH_ADJUSTMENT';
