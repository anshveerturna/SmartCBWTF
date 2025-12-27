-- V33: Add ownership type and rent agreement fields to HCF
-- Ownership can be OWNED or RENTED
-- If RENTED, rent agreement document is mandatory

ALTER TABLE hcf 
ADD COLUMN ownership_type VARCHAR(20) NOT NULL DEFAULT 'OWNED',
ADD COLUMN rent_agreement_url VARCHAR(500);

-- Add constraint for valid ownership types
ALTER TABLE hcf ADD CONSTRAINT chk_ownership_type 
CHECK (ownership_type IN ('OWNED', 'RENTED'));

COMMENT ON COLUMN hcf.ownership_type IS 'Property ownership type: OWNED or RENTED';
COMMENT ON COLUMN hcf.rent_agreement_url IS 'URL to uploaded rent agreement document (required if RENTED)';
