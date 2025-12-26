-- ============================================================
-- V25: Fix audit_log.data_json column type
-- Change from JSONB to TEXT to allow arbitrary audit data
-- ============================================================

-- Convert data_json from JSONB to TEXT
ALTER TABLE audit_log 
    ALTER COLUMN data_json TYPE TEXT;

COMMENT ON COLUMN audit_log.data_json IS 'TEXT: Audit data as JSON string or plain text';
