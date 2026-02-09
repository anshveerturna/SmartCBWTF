-- Add excess_rate_per_kg column to hcf table
-- Used for billing waste above the standard limit (e.g. 277g/bed/day)

ALTER TABLE hcf ADD COLUMN IF NOT EXISTS excess_rate_per_kg DOUBLE PRECISION;

COMMENT ON COLUMN hcf.excess_rate_per_kg IS 'Rate per kg for waste exceeding the standard allowance (e.g. 277g/bed/day). If null, standard facility rate applies.';
