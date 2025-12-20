-- V16: Error Reporting System
-- Allows CBWTFs, HCFs, and app users to report errors
-- Includes auto-detected system issues

CREATE TABLE system_error (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    error_code VARCHAR(50),
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    source VARCHAR(30) NOT NULL DEFAULT 'USER_REPORTED',
    component VARCHAR(50),
    facility_id UUID REFERENCES facility(id) ON DELETE SET NULL,
    hcf_id UUID REFERENCES hcf(id) ON DELETE SET NULL,
    reported_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    stack_trace TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_severity CHECK (severity IN ('CRITICAL', 'ERROR', 'WARNING', 'INFO')),
    CONSTRAINT chk_source CHECK (source IN ('USER_REPORTED', 'AUTO_DETECTED', 'API_ERROR', 'MOBILE_APP')),
    CONSTRAINT chk_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'IGNORED'))
);

-- Indexes for efficient querying
CREATE INDEX idx_system_error_status ON system_error(status);
CREATE INDEX idx_system_error_severity ON system_error(severity);
CREATE INDEX idx_system_error_facility ON system_error(facility_id);
CREATE INDEX idx_system_error_created ON system_error(created_at DESC);
CREATE INDEX idx_system_error_open_severity ON system_error(status, severity) WHERE status = 'OPEN';

-- Comments for clarity
COMMENT ON TABLE system_error IS 'Stores all system errors - user reported and auto-detected';
COMMENT ON COLUMN system_error.source IS 'USER_REPORTED: submitted by user, AUTO_DETECTED: found by scheduled check, API_ERROR: caught exception, MOBILE_APP: from mobile app';
COMMENT ON COLUMN system_error.component IS 'Which part of system: BACKEND, WEB, MOBILE, DATABASE, SUBSCRIPTION, HCF_MANAGEMENT, etc.';
