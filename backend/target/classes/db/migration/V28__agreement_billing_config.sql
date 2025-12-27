-- V28: Add agreement billing configuration table
-- Stores billing rates per agreement for waste collection

CREATE TABLE agreement_billing_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES agreement(id) ON DELETE CASCADE,
    
    -- Billing rates
    base_grams_per_bed_per_day INTEGER NOT NULL DEFAULT 270,
    base_rate_per_bed_per_day DECIMAL(10, 2) NOT NULL,
    excess_rate_per_kg DECIMAL(10, 2) NOT NULL,
    
    -- Effective period (null effective_to means currently active)
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    effective_to DATE,
    
    -- Audit fields
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Only one active config per agreement at a time
CREATE UNIQUE INDEX idx_billing_config_active 
ON agreement_billing_config(agreement_id) 
WHERE effective_to IS NULL;

-- For querying historical configs
CREATE INDEX idx_billing_config_agreement ON agreement_billing_config(agreement_id);
CREATE INDEX idx_billing_config_effective ON agreement_billing_config(effective_from, effective_to);

COMMENT ON TABLE agreement_billing_config IS 'Per-agreement billing configuration for waste collection';
COMMENT ON COLUMN agreement_billing_config.base_grams_per_bed_per_day IS 'Base waste allowance included in base rate (default 270g)';
COMMENT ON COLUMN agreement_billing_config.base_rate_per_bed_per_day IS 'Base rate charged per bed per day (INR)';
COMMENT ON COLUMN agreement_billing_config.excess_rate_per_kg IS 'Rate charged for waste exceeding base allowance (INR per kg)';
