-- Route Planning Module - V55 Migration
-- Creates tables for route definition, waypoints, and staff assignment

-- Route Status Enum values: DRAFT, ACTIVE, TEMPORARILY_SUSPENDED
-- Using VARCHAR for flexibility and avoiding enum type migration issues

-- ========================================
-- Route Table (First-Class Entity)
-- ========================================
CREATE TABLE IF NOT EXISTS route (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    color VARCHAR(7) DEFAULT '#3B82F6',  -- Hex color for map display
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    is_active BOOLEAN NOT NULL DEFAULT false,  -- Computed from status
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT route_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'TEMPORARILY_SUSPENDED')),
    CONSTRAINT route_name_unique_per_facility UNIQUE (facility_id, name)
);

-- Index for facility lookups
CREATE INDEX IF NOT EXISTS idx_route_facility_id ON route(facility_id);
CREATE INDEX IF NOT EXISTS idx_route_status ON route(status);
CREATE INDEX IF NOT EXISTS idx_route_is_active ON route(is_active);

-- ========================================
-- Route Waypoint Table (Ordered HCF Stops)
-- ========================================
CREATE TABLE IF NOT EXISTS route_waypoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    hcf_id UUID NOT NULL REFERENCES hcf(id) ON DELETE RESTRICT,  -- Block HCF deletion if in route
    sequence_order INTEGER NOT NULL,
    estimated_stop_minutes INTEGER DEFAULT 15,  -- Optional, for future planning
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Ensure unique ordering per route
    CONSTRAINT route_waypoint_order_unique UNIQUE (route_id, sequence_order),
    -- Prevent duplicate HCFs in same route
    CONSTRAINT route_waypoint_hcf_unique UNIQUE (route_id, hcf_id)
);

-- Indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_route_waypoint_route_id ON route_waypoint(route_id);
CREATE INDEX IF NOT EXISTS idx_route_waypoint_hcf_id ON route_waypoint(hcf_id);

-- ========================================
-- Route Assignment Table (Detachable Staff)
-- ========================================
CREATE TABLE IF NOT EXISTS route_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    staff_id UUID NOT NULL REFERENCES app_user(id) ON DELETE SET NULL,
    vehicle_id UUID REFERENCES vehicle(id) ON DELETE SET NULL,
    assigned_from DATE NOT NULL DEFAULT CURRENT_DATE,
    assigned_to DATE,  -- Null = currently active
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Ensure only one active assignment per route at a time
CREATE UNIQUE INDEX IF NOT EXISTS idx_route_assignment_active_unique 
    ON route_assignment(route_id) WHERE is_active = true;

-- Indexes for lookups
CREATE INDEX IF NOT EXISTS idx_route_assignment_route_id ON route_assignment(route_id);
CREATE INDEX IF NOT EXISTS idx_route_assignment_staff_id ON route_assignment(staff_id);
CREATE INDEX IF NOT EXISTS idx_route_assignment_vehicle_id ON route_assignment(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_route_assignment_is_active ON route_assignment(is_active);

-- ========================================
-- Trigger: Auto-update is_active based on status
-- ========================================
CREATE OR REPLACE FUNCTION update_route_is_active()
RETURNS TRIGGER AS $$
BEGIN
    NEW.is_active := (NEW.status = 'ACTIVE');
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_route_status_change
    BEFORE INSERT OR UPDATE OF status ON route
    FOR EACH ROW EXECUTE FUNCTION update_route_is_active();

-- ========================================
-- Trigger: Auto-update updated_at on assignment
-- ========================================
CREATE OR REPLACE FUNCTION update_route_assignment_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_route_assignment_update
    BEFORE UPDATE ON route_assignment
    FOR EACH ROW EXECUTE FUNCTION update_route_assignment_timestamp();

-- ========================================
-- Comments for documentation
-- ========================================
COMMENT ON TABLE route IS 'Collection routes for waste pickup, independent of staff lifecycle';
COMMENT ON TABLE route_waypoint IS 'Ordered HCF stops within a route';
COMMENT ON TABLE route_assignment IS 'Detachable staff/vehicle assignment to routes, preserves history';
COMMENT ON COLUMN route.status IS 'DRAFT: not yet active, ACTIVE: in use, TEMPORARILY_SUSPENDED: paused (festivals, maintenance)';
COMMENT ON COLUMN route.is_active IS 'Computed from status - true only when status = ACTIVE';
COMMENT ON COLUMN route_waypoint.sequence_order IS 'Order of HCF in route, must be contiguous 1..N';
COMMENT ON COLUMN route_assignment.is_active IS 'Only one active assignment per route allowed';
