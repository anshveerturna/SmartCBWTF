-- V58: Consumable Orders for HCF Portal
-- Orders are independent of dues status per regulatory requirements

-- Consumable Order (HCF places order)
CREATE TABLE consumable_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Bindings
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    
    -- Order lifecycle
    order_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Amounts
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    
    -- Notes
    hcf_notes TEXT,
    cbwtf_notes TEXT,
    
    -- Timestamps
    ordered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ordered_by UUID NOT NULL REFERENCES app_user(id),
    confirmed_at TIMESTAMP,
    confirmed_by UUID REFERENCES app_user(id),
    dispatched_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancellation_reason TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Order Line Items
CREATE TABLE consumable_order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    order_id UUID NOT NULL REFERENCES consumable_order(id) ON DELETE CASCADE,
    consumable_item_id UUID NOT NULL REFERENCES consumable_item(id),
    
    -- Captured at order time (immutable)
    item_name VARCHAR(200) NOT NULL,
    unit_of_measure VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    price_per_unit DECIMAL(12,2) NOT NULL,
    gst_rate DECIMAL(5,2) NOT NULL,
    
    -- Calculated
    line_subtotal DECIMAL(12,2) NOT NULL,
    line_gst DECIMAL(12,2) NOT NULL,
    line_total DECIMAL(12,2) NOT NULL
);

-- Indexes
CREATE INDEX idx_consumable_order_hcf ON consumable_order(hcf_id);
CREATE INDEX idx_consumable_order_facility ON consumable_order(facility_id);
CREATE INDEX idx_consumable_order_status ON consumable_order(status);
CREATE INDEX idx_consumable_order_date ON consumable_order(ordered_at);
CREATE INDEX idx_consumable_order_item_order ON consumable_order_item(order_id);

-- Unique order number per facility
CREATE UNIQUE INDEX idx_consumable_order_number ON consumable_order(facility_id, order_number);

-- Constraints
ALTER TABLE consumable_order 
    ADD CONSTRAINT chk_order_status 
    CHECK (status IN ('PENDING', 'CONFIRMED', 'DISPATCHED', 'DELIVERED', 'CANCELLED'));

COMMENT ON TABLE consumable_order IS 'HCF consumable orders - independent of dues status';
COMMENT ON TABLE consumable_order_item IS 'Order line items with pricing captured at order time';
