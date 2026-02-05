-- Add manual portal access column for 0-30 beds HCFs
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS portal_access_manually_enabled BOOLEAN DEFAULT FALSE;

-- Add index for querying manually enabled HCFs
CREATE INDEX IF NOT EXISTS idx_hcf_portal_manual ON hcf(portal_access_manually_enabled) WHERE portal_access_manually_enabled = TRUE;
