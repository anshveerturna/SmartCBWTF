-- V57: Dues Clearance Workflow for HCF Admin Portal
-- Creates tables for dues clearance requests and report access audit logging

-- Dues clearance request table
-- Workflow: HCF requests → CBWTF verifies → Top Management approves
CREATE TABLE dues_clearance_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- HCF and Agreement binding
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    
    -- Request lifecycle
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    requested_by UUID NOT NULL REFERENCES app_user(id),
    request_notes TEXT,
    
    -- CBWTF verification step
    cbwtf_submitted_at TIMESTAMP,
    cbwtf_submitted_by UUID REFERENCES app_user(id),
    amount_cleared DECIMAL(12, 2),
    cbwtf_notes TEXT,
    
    -- Top Management approval step
    management_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID REFERENCES app_user(id),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    
    -- Report access control
    reports_access_granted_at TIMESTAMP,
    reports_access_revoked_at TIMESTAMP,
    revocation_reason TEXT,
    
    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Report access audit log
-- Every report view/download is logged for regulatory compliance
CREATE TABLE hcf_report_access_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    clearance_request_id UUID REFERENCES dues_clearance_request(id),
    
    report_type VARCHAR(20) NOT NULL, -- MONTHLY, YEARLY
    report_period VARCHAR(20) NOT NULL, -- e.g., "2026-01" or "2026"
    
    accessed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    accessed_by UUID NOT NULL REFERENCES app_user(id),
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    action VARCHAR(20) NOT NULL DEFAULT 'VIEW' -- VIEW, DOWNLOAD
);

-- Indexes for performance
CREATE INDEX idx_dues_clearance_hcf ON dues_clearance_request(hcf_id);
CREATE INDEX idx_dues_clearance_facility ON dues_clearance_request(facility_id);
CREATE INDEX idx_dues_clearance_status ON dues_clearance_request(management_status);
CREATE INDEX idx_dues_clearance_pending ON dues_clearance_request(facility_id, management_status) 
    WHERE management_status = 'PENDING';

CREATE INDEX idx_report_access_hcf ON hcf_report_access_log(hcf_id);
CREATE INDEX idx_report_access_date ON hcf_report_access_log(accessed_at);

-- Constraint: management_status must be valid
ALTER TABLE dues_clearance_request 
    ADD CONSTRAINT chk_management_status 
    CHECK (management_status IN ('PENDING', 'SUBMITTED', 'APPROVED', 'REJECTED'));

COMMENT ON TABLE dues_clearance_request IS 'Dues clearance workflow: HCF requests access, CBWTF submits verification, Top Management approves';
COMMENT ON TABLE hcf_report_access_log IS 'Audit log for HCF report access - required for regulatory compliance';
