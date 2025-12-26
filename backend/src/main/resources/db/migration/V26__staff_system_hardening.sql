-- Phase 4: Staff System Hardening
-- Adds staff GPS tracking, attendance facility linkage, and enforces username uniqueness

-- 1. Global username uniqueness (DB-level enforcement)
-- This ensures no duplicate usernames across the entire system
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_user_username_unique ON app_user(username);

-- 2. Staff GPS tracking table (APPEND-ONLY - no updates or deletes allowed)
-- Used for real-time tracking of DRIVER and PLANT_OPERATOR staff from Android app
CREATE TABLE user_gps_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    staff_user_id UUID NOT NULL REFERENCES app_user(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    speed DECIMAL(6, 2),
    heading DECIMAL(5, 2),
    accuracy_m DECIMAL(6, 2),
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ DEFAULT now(),
    source VARCHAR(20) DEFAULT 'ANDROID_APP',
    client_event_id UUID UNIQUE NOT NULL,
    CONSTRAINT user_gps_event_immutable CHECK (true) -- Marker: append-only table
);

-- Indexes for efficient queries
CREATE INDEX idx_user_gps_event_staff_ts ON user_gps_event(staff_user_id, recorded_at DESC);
CREATE INDEX idx_user_gps_event_facility_ts ON user_gps_event(facility_id, recorded_at DESC);
CREATE INDEX idx_user_gps_event_client_id ON user_gps_event(client_event_id);

-- 3. Add facility_id to attendance for efficient tenant-scoped queries
-- This denormalizes facility_id from staff user for faster queries
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS facility_id UUID REFERENCES facility(id);

-- Backfill facility_id from driver's facility
UPDATE attendance 
SET facility_id = (
    SELECT u.facility_id 
    FROM app_user u 
    WHERE u.id = attendance.driver_user_id
)
WHERE facility_id IS NULL;

-- Index for facility-scoped attendance queries
CREATE INDEX IF NOT EXISTS idx_attendance_facility_ts ON attendance(facility_id, event_ts DESC);

-- 4. Add last_gps_at and last_gps_lat/lon to app_user for quick "online" status checks
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_gps_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_gps_lat DECIMAL(10, 7);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_gps_lon DECIMAL(10, 7);

-- 5. Index for staff queries by facility and role
CREATE INDEX IF NOT EXISTS idx_app_user_facility_role ON app_user(facility_id, role);

COMMENT ON TABLE user_gps_event IS 'Append-only GPS tracking for staff users (DRIVER, PLANT_OPERATOR). No updates or deletes.';
COMMENT ON COLUMN attendance.facility_id IS 'Denormalized from driver for efficient tenant-scoped queries.';
