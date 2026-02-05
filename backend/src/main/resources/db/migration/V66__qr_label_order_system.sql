-- QR Label Order System
-- HCFs can request QR labels from CBWTF or self-generate (with different pricing)

CREATE TABLE qr_label_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    agreement_id UUID REFERENCES agreement(id),
    waste_category VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0 AND quantity <= 500),
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('HCF_SELF', 'CBWTF_REQUEST')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'FULFILLED', 'REJECTED', 'CANCELLED')),
    notes TEXT,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    fulfilled_at TIMESTAMP WITH TIME ZONE,
    fulfilled_by UUID REFERENCES app_user(id),
    pdf_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_qr_label_order_hcf ON qr_label_order(hcf_id);
CREATE INDEX idx_qr_label_order_facility ON qr_label_order(facility_id);
CREATE INDEX idx_qr_label_order_status ON qr_label_order(status);
CREATE INDEX idx_qr_label_order_type ON qr_label_order(order_type);

-- Add default QR pricing to system config
INSERT INTO system_config (config_key, config_value, value_type, display_name, description, category, created_at, updated_at)
VALUES 
    ('qr.price.hcf_self_per_unit', '5.00', 'NUMBER', 'QR Price (HCF Self)', 'Price per QR label when HCF self-generates', 'OPERATIONAL', NOW(), NOW()),
    ('qr.price.cbwtf_request_per_unit', '10.00', 'NUMBER', 'QR Price (CBWTF)', 'Price per QR label when requested from CBWTF', 'OPERATIONAL', NOW(), NOW()),
    ('qr.max_quantity_per_order', '500', 'NUMBER', 'Max QR per Order', 'Maximum QR labels per order', 'OPERATIONAL', NOW(), NOW())
ON CONFLICT (config_key) DO NOTHING;
