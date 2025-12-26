-- ============================================================
-- V24: Convert TEXT columns to JSONB for proper type handling
-- Enterprise hardening for GPS integration
-- ============================================================

-- 1. Convert gps_vendor_integration.credentials from TEXT to JSONB
-- Handle NULL and empty string cases safely
ALTER TABLE gps_vendor_integration 
    ALTER COLUMN credentials TYPE jsonb 
    USING CASE 
        WHEN credentials IS NULL OR credentials = '' THEN NULL
        ELSE credentials::jsonb 
    END;

-- 2. Convert gps_event.raw_payload from TEXT to JSONB
-- Handle NULL and empty string cases safely
ALTER TABLE gps_event 
    ALTER COLUMN raw_payload TYPE jsonb 
    USING CASE 
        WHEN raw_payload IS NULL OR raw_payload = '' THEN NULL
        ELSE raw_payload::jsonb 
    END;

-- Add comments for documentation
COMMENT ON COLUMN gps_vendor_integration.credentials IS 'JSONB: Vendor auth credentials (encrypted at-rest by application). Structure varies by vendor.';
COMMENT ON COLUMN gps_event.raw_payload IS 'JSONB: Original vendor payload for audit/debug. READ-ONLY, never queried directly.';
