-- V35: QR Authorization System
-- Creates QR authorization table with agreement binding, category, validity window,
-- status tracking, and pickup event linkage for legal traceability.

CREATE TABLE qr_authorization (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Agreement binding (mandatory)
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    
    -- Category and validity
    waste_category VARCHAR(20) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    
    -- Status lifecycle: ACTIVE -> USED -> VERIFIED (or EXPIRED/REVOKED/BLOCKED)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Signed payload stored for reference and verification
    qr_payload JSONB NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    
    -- Audit fields
    created_by UUID NOT NULL REFERENCES app_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Lifecycle timestamps
    used_at TIMESTAMP,
    verified_at TIMESTAMP,
    
    -- Pickup traceability: links to bag_event for legal disputes
    pickup_event_id UUID,
    
    -- Constraints
    CONSTRAINT chk_qr_valid_period CHECK (valid_to > valid_from),
    CONSTRAINT chk_qr_category CHECK (waste_category IN ('YELLOW', 'RED', 'BLUE', 'WHITE')),
    CONSTRAINT chk_qr_status CHECK (status IN ('ACTIVE', 'USED', 'VERIFIED', 'EXPIRED', 'REVOKED', 'BLOCKED'))
);

-- Indexes for common query patterns
CREATE INDEX idx_qr_agreement ON qr_authorization(agreement_id);
CREATE INDEX idx_qr_validity ON qr_authorization(valid_from, valid_to);
CREATE INDEX idx_qr_facility_status ON qr_authorization(facility_id, status);
CREATE INDEX idx_qr_hcf ON qr_authorization(hcf_id);
CREATE INDEX idx_qr_status ON qr_authorization(status);

-- Index for SLA breach detection: find USED QRs older than SLA threshold
CREATE INDEX idx_qr_used_at ON qr_authorization(used_at) WHERE status = 'USED';

COMMENT ON TABLE qr_authorization IS 'QR codes are legal authorization instruments for waste movement, bound to agreements';
COMMENT ON COLUMN qr_authorization.pickup_event_id IS 'Links to bag_event for legal traceability in disputes';
COMMENT ON COLUMN qr_authorization.checksum IS 'HMAC-SHA256 signature to prevent tampering';
