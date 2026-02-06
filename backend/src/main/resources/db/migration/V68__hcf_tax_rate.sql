-- Add tax_rate column to hcf table (percentage, e.g. 18.0 = 18% GST)
-- Default 18.0% which is standard Indian GST for biomedical waste services
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS tax_rate DOUBLE PRECISION DEFAULT 18.0;

-- Comment for documentation
COMMENT ON COLUMN hcf.tax_rate IS 'GST tax rate percentage for this HCF. Default 18.0 (18%). Used in invoice and bill generation.';
