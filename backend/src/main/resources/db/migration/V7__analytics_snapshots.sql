-- V7: Analytics Snapshot Tables
-- Pre-aggregated analytics data for fast dashboard queries

-- Daily waste snapshot (aggregated per HCF per day)
CREATE TABLE daily_waste_snapshot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    snapshot_date DATE NOT NULL,
    
    -- Bag counts
    total_bags INTEGER NOT NULL DEFAULT 0,
    yellow_bags INTEGER NOT NULL DEFAULT 0,
    red_bags INTEGER NOT NULL DEFAULT 0,
    blue_bags INTEGER NOT NULL DEFAULT 0,
    white_bags INTEGER NOT NULL DEFAULT 0,
    
    -- Weight in grams (stored as integer for precision)
    total_weight_grams BIGINT NOT NULL DEFAULT 0,
    yellow_weight_grams BIGINT NOT NULL DEFAULT 0,
    red_weight_grams BIGINT NOT NULL DEFAULT 0,
    blue_weight_grams BIGINT NOT NULL DEFAULT 0,
    white_weight_grams BIGINT NOT NULL DEFAULT 0,
    
    -- Verification stats
    verified_bags INTEGER NOT NULL DEFAULT 0,
    discrepancy_count INTEGER NOT NULL DEFAULT 0,
    missing_bags INTEGER NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    
    -- Unique constraint for upsert operations
    CONSTRAINT uq_daily_snapshot_facility_hcf_date UNIQUE (facility_id, hcf_id, snapshot_date)
);

-- Monthly waste snapshot (aggregated per facility per month)
CREATE TABLE monthly_waste_snapshot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    snapshot_month DATE NOT NULL, -- First day of the month
    
    -- Rollup counts
    total_hcfs_active INTEGER NOT NULL DEFAULT 0,
    total_pickups INTEGER NOT NULL DEFAULT 0,
    total_bags INTEGER NOT NULL DEFAULT 0,
    
    -- Category breakdown
    yellow_bags INTEGER NOT NULL DEFAULT 0,
    red_bags INTEGER NOT NULL DEFAULT 0,
    blue_bags INTEGER NOT NULL DEFAULT 0,
    white_bags INTEGER NOT NULL DEFAULT 0,
    
    -- Weight in grams
    total_weight_grams BIGINT NOT NULL DEFAULT 0,
    yellow_weight_grams BIGINT NOT NULL DEFAULT 0,
    red_weight_grams BIGINT NOT NULL DEFAULT 0,
    blue_weight_grams BIGINT NOT NULL DEFAULT 0,
    white_weight_grams BIGINT NOT NULL DEFAULT 0,
    
    -- Blue waste percentage for SPCB/CPCB compliance
    blue_waste_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    
    -- Revenue (in paise for precision)
    revenue_invoiced_paise BIGINT NOT NULL DEFAULT 0,
    revenue_collected_paise BIGINT NOT NULL DEFAULT 0,
    revenue_outstanding_paise BIGINT NOT NULL DEFAULT 0,
    
    -- Quality metrics
    verified_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    discrepancy_count INTEGER NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    
    -- Unique constraint
    CONSTRAINT uq_monthly_snapshot_facility_month UNIQUE (facility_id, snapshot_month)
);

-- Platform-wide snapshot (for SuperAdmin - aggregated across all tenants)
CREATE TABLE platform_snapshot (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    snapshot_date DATE NOT NULL,
    
    -- Tenant counts
    total_tenants INTEGER NOT NULL DEFAULT 0,
    active_tenants INTEGER NOT NULL DEFAULT 0,
    trial_tenants INTEGER NOT NULL DEFAULT 0,
    expired_tenants INTEGER NOT NULL DEFAULT 0,
    
    -- HCF counts
    total_hcfs INTEGER NOT NULL DEFAULT 0,
    active_hcfs INTEGER NOT NULL DEFAULT 0,
    pending_hcfs INTEGER NOT NULL DEFAULT 0,
    
    -- Waste totals
    total_bags INTEGER NOT NULL DEFAULT 0,
    total_weight_grams BIGINT NOT NULL DEFAULT 0,
    
    -- Revenue totals (in paise)
    platform_revenue_paise BIGINT NOT NULL DEFAULT 0,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    
    -- Unique constraint
    CONSTRAINT uq_platform_snapshot_date UNIQUE (snapshot_date)
);

-- Indexes for common query patterns
CREATE INDEX idx_daily_snapshot_facility_date ON daily_waste_snapshot(facility_id, snapshot_date DESC);
CREATE INDEX idx_daily_snapshot_hcf_date ON daily_waste_snapshot(hcf_id, snapshot_date DESC);
CREATE INDEX idx_monthly_snapshot_facility_month ON monthly_waste_snapshot(facility_id, snapshot_month DESC);
CREATE INDEX idx_platform_snapshot_date ON platform_snapshot(snapshot_date DESC);
