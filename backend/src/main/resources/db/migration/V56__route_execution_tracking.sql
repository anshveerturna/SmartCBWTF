-- Route Execution Tracking - V56 Migration
-- Adds timeframe, execution logs, cycle history, and alerts for route compliance

-- ========================================
-- 1. Add Timeframe Columns to Route
-- ========================================
ALTER TABLE route ADD COLUMN IF NOT EXISTS completion_days INTEGER DEFAULT 1;
ALTER TABLE route ADD COLUMN IF NOT EXISTS cycle_start_date DATE;

COMMENT ON COLUMN route.completion_days IS 'Number of days to complete the route (e.g., 1 = daily, 7 = weekly)';
COMMENT ON COLUMN route.cycle_start_date IS 'Start date of current execution cycle';

-- ========================================
-- 2. Route Cycle History Table
-- Stores history of all completed/incomplete cycles
-- ========================================
CREATE TABLE IF NOT EXISTS route_cycle_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    facility_id UUID NOT NULL REFERENCES facility(id) ON DELETE CASCADE,
    staff_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    cycle_number INTEGER NOT NULL,
    cycle_start DATE NOT NULL,
    cycle_end DATE NOT NULL,
    total_waypoints INTEGER NOT NULL,
    completed_waypoints INTEGER NOT NULL DEFAULT 0,
    missed_waypoints INTEGER NOT NULL DEFAULT 0,
    completion_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS, COMPLETED, INCOMPLETE
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT route_cycle_status_check CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'INCOMPLETE'))
);

CREATE INDEX IF NOT EXISTS idx_route_cycle_history_route_id ON route_cycle_history(route_id);
CREATE INDEX IF NOT EXISTS idx_route_cycle_history_facility_id ON route_cycle_history(facility_id);
CREATE INDEX IF NOT EXISTS idx_route_cycle_history_staff_id ON route_cycle_history(staff_id);
CREATE INDEX IF NOT EXISTS idx_route_cycle_history_status ON route_cycle_history(status);
CREATE INDEX IF NOT EXISTS idx_route_cycle_history_cycle_dates ON route_cycle_history(cycle_start, cycle_end);

COMMENT ON TABLE route_cycle_history IS 'Historical record of route execution cycles for compliance reporting';

-- ========================================
-- 3. Route Execution Log Table
-- Tracks individual waypoint coverage per cycle
-- ========================================
CREATE TABLE IF NOT EXISTS route_execution_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id UUID NOT NULL REFERENCES route_cycle_history(id) ON DELETE CASCADE,
    route_id UUID NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    waypoint_id UUID NOT NULL REFERENCES route_waypoint(id) ON DELETE CASCADE,
    hcf_id UUID NOT NULL REFERENCES hcf(id) ON DELETE CASCADE,
    attendance_id UUID REFERENCES attendance(id) ON DELETE SET NULL,
    staff_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    sequence_order INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED, MISSED
    visited_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT route_exec_log_status_check CHECK (status IN ('PENDING', 'COMPLETED', 'MISSED')),
    CONSTRAINT route_exec_log_unique_per_cycle UNIQUE (cycle_id, waypoint_id)
);

CREATE INDEX IF NOT EXISTS idx_route_exec_log_cycle_id ON route_execution_log(cycle_id);
CREATE INDEX IF NOT EXISTS idx_route_exec_log_route_id ON route_execution_log(route_id);
CREATE INDEX IF NOT EXISTS idx_route_exec_log_hcf_id ON route_execution_log(hcf_id);
CREATE INDEX IF NOT EXISTS idx_route_exec_log_attendance_id ON route_execution_log(attendance_id);
CREATE INDEX IF NOT EXISTS idx_route_exec_log_status ON route_execution_log(status);

COMMENT ON TABLE route_execution_log IS 'Per-waypoint execution status within a route cycle';

-- ========================================
-- 4. Route Alert Table
-- Stores alerts for incomplete routes
-- ========================================
CREATE TABLE IF NOT EXISTS route_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    cycle_id UUID NOT NULL REFERENCES route_cycle_history(id) ON DELETE CASCADE,
    facility_id UUID NOT NULL REFERENCES facility(id) ON DELETE CASCADE,
    staff_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    alert_type VARCHAR(30) NOT NULL,  -- ROUTE_INCOMPLETE, WAYPOINT_MISSED
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',  -- INFO, WARNING, CRITICAL
    title VARCHAR(200) NOT NULL,
    message TEXT,
    missed_hcf_count INTEGER NOT NULL DEFAULT 0,
    is_resolved BOOLEAN NOT NULL DEFAULT false,
    resolved_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT route_alert_type_check CHECK (alert_type IN ('ROUTE_INCOMPLETE', 'WAYPOINT_MISSED', 'ROUTE_NOT_STARTED')),
    CONSTRAINT route_alert_severity_check CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_route_alert_route_id ON route_alert(route_id);
CREATE INDEX IF NOT EXISTS idx_route_alert_facility_id ON route_alert(facility_id);
CREATE INDEX IF NOT EXISTS idx_route_alert_is_resolved ON route_alert(is_resolved);
CREATE INDEX IF NOT EXISTS idx_route_alert_created_at ON route_alert(created_at DESC);

COMMENT ON TABLE route_alert IS 'Alerts generated when routes are not completed within timeframe';

-- ========================================
-- 5. Trigger: Auto-update execution log timestamp
-- ========================================
CREATE OR REPLACE FUNCTION update_route_exec_log_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_route_exec_log_update ON route_execution_log;
CREATE TRIGGER trg_route_exec_log_update
    BEFORE UPDATE ON route_execution_log
    FOR EACH ROW EXECUTE FUNCTION update_route_exec_log_timestamp();

-- ========================================
-- 6. Initialize cycle_start_date for active routes
-- ========================================
UPDATE route 
SET cycle_start_date = CURRENT_DATE 
WHERE status = 'ACTIVE' AND cycle_start_date IS NULL;
