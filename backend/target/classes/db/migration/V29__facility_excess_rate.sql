-- V29: Add global excess rate to facility
-- Excess rate is CBWTF-level operational policy, not per-agreement

ALTER TABLE facility
ADD COLUMN excess_rate_per_kg DECIMAL(10,2) NOT NULL DEFAULT 50.00,
ADD COLUMN excess_rate_effective_from DATE NOT NULL DEFAULT CURRENT_DATE;

COMMENT ON COLUMN facility.excess_rate_per_kg IS 'Global excess waste rate (INR/kg) - resolved at billing time';
COMMENT ON COLUMN facility.excess_rate_effective_from IS 'Date from which this excess rate is effective';
