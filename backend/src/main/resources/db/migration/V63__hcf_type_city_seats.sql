-- Add HCF type, city, and seat count fields
-- All new category types (Dental, Clinic, Pathology) default to 0-30 beds

ALTER TABLE hcf ADD COLUMN IF NOT EXISTS hcf_type VARCHAR(30) DEFAULT 'HOSPITAL';
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS seat_count INTEGER;

-- Create index for city/state/type filtering
CREATE INDEX IF NOT EXISTS idx_hcf_city ON hcf(city);
CREATE INDEX IF NOT EXISTS idx_hcf_state ON hcf(state);
CREATE INDEX IF NOT EXISTS idx_hcf_type ON hcf(hcf_type);

COMMENT ON COLUMN hcf.hcf_type IS 'HCF facility type: HOSPITAL, DENTAL, CLINIC, PATHOLOGY_COLLECTION, PATHOLOGY_STORAGE';
COMMENT ON COLUMN hcf.city IS 'City where HCF is located';
COMMENT ON COLUMN hcf.seat_count IS 'Number of seats (for Dental/Clinic types)';
