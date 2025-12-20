-- V13: Add business registration fields to facility table

ALTER TABLE facility ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS gst_number VARCHAR(20);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS aadhar_number VARCHAR(20);

-- Add indexes for lookup
CREATE INDEX IF NOT EXISTS idx_facility_pan ON facility(pan_number) WHERE pan_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_facility_gst ON facility(gst_number) WHERE gst_number IS NOT NULL;
