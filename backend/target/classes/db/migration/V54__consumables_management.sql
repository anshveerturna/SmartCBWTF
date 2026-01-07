-- Consumables Management - Phase 1
-- Tables: consumable_category, consumable_item, consumable_pricing, consumable_quantity_reference

-- Consumable Category (facility-scoped)
CREATE TABLE consumable_category (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    name VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(facility_id, name)
);

CREATE INDEX idx_consumable_category_facility ON consumable_category(facility_id);

-- Consumable Item
CREATE TABLE consumable_item (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    category_id UUID NOT NULL REFERENCES consumable_category(id),
    consumable_code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    hsn_code VARCHAR(20),
    unit_of_measure VARCHAR(50) NOT NULL,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(facility_id, consumable_code)
);

CREATE INDEX idx_consumable_item_facility ON consumable_item(facility_id);
CREATE INDEX idx_consumable_item_category ON consumable_item(category_id);

-- Consumable Pricing (versioned, one active per item)
CREATE TABLE consumable_pricing (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    consumable_item_id UUID NOT NULL REFERENCES consumable_item(id),
    price_per_unit DECIMAL(12,2) NOT NULL,
    gst_rate DECIMAL(5,2) NOT NULL DEFAULT 18.00,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consumable_pricing_item ON consumable_pricing(consumable_item_id);
CREATE INDEX idx_consumable_pricing_active ON consumable_pricing(consumable_item_id, is_active) WHERE is_active = true;

-- Consumable Quantity Reference (planning only)
CREATE TYPE reference_type AS ENUM ('PER_100_BEDS_PER_YEAR', 'PER_MONTH', 'FIXED');

CREATE TABLE consumable_quantity_reference (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    consumable_item_id UUID NOT NULL REFERENCES consumable_item(id) UNIQUE,
    reference_type reference_type NOT NULL,
    reference_quantity DECIMAL(10,2) NOT NULL
);

-- Seed default categories for existing facilities
INSERT INTO consumable_category (facility_id, name, display_order)
SELECT id, 'Bins', 1 FROM facility
UNION ALL
SELECT id, 'Bags', 2 FROM facility
UNION ALL
SELECT id, 'PPE', 3 FROM facility
UNION ALL
SELECT id, 'Sharp Containers', 4 FROM facility
UNION ALL
SELECT id, 'Tools & Equipment', 5 FROM facility;
