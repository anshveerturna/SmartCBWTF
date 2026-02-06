-- V69: Billing model update
-- 1. Add excess_rate_per_kg to HCF for per-HCF overcharge on waste above 277g/bed/day
-- 2. Update tax_rate default from 18% to 5%
-- 3. Update existing HCFs with tax_rate=18.0 to 5.0 (only if still at the old default)

-- Add excess rate per kg field to HCF
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS excess_rate_per_kg DOUBLE PRECISION;
COMMENT ON COLUMN hcf.excess_rate_per_kg IS 'Rate charged per kg for waste exceeding 277g/bed/day allowance. NULL means use facility default.';

-- Change tax_rate default to 5%
ALTER TABLE hcf ALTER COLUMN tax_rate SET DEFAULT 5.0;

-- Update existing HCFs that still have the old 18% default to the new 5% default
UPDATE hcf SET tax_rate = 5.0 WHERE tax_rate = 18.0;

COMMENT ON COLUMN hcf.tax_rate IS 'GST tax rate percentage for this HCF. Default 5.0 (5%). Used in invoice and bill generation.';
