CREATE TABLE agreement_correction_request (
    id UUID PRIMARY KEY,
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    field_name VARCHAR(255) NOT NULL,
    current_value TEXT,
    requested_value TEXT,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    requested_by UUID NOT NULL REFERENCES app_user(id),
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reviewed_by UUID REFERENCES app_user(id),
    reviewed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_correction_req_facility_status ON agreement_correction_request(facility_id, status);
CREATE INDEX idx_correction_req_agreement ON agreement_correction_request(agreement_id);
