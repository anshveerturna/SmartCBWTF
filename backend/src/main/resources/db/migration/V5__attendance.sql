-- Attendance table for driver check-ins at HCF locations
CREATE TABLE attendance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_user_id UUID NOT NULL REFERENCES app_user(id),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    event_ts TIMESTAMPTZ NOT NULL,
    gps_lat DOUBLE PRECISION NOT NULL,
    gps_lon DOUBLE PRECISION NOT NULL,
    gps_accuracy_m DOUBLE PRECISION,
    app_device_id VARCHAR(128),
    distance_from_hcf_m DOUBLE PRECISION NOT NULL,
    client_event_id UUID UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Index for cooldown lookup: latest attendance per driver-hcf pair
CREATE INDEX idx_attendance_driver_hcf_ts ON attendance(driver_user_id, hcf_id, event_ts DESC);

-- Index for driver attendance history
CREATE INDEX idx_attendance_driver_ts ON attendance(driver_user_id, event_ts DESC);

-- Index for HCF attendance reports
CREATE INDEX idx_attendance_hcf_ts ON attendance(hcf_id, event_ts DESC);
