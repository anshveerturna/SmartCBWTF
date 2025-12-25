-- ============================================================
-- V22: Vehicle & GPS Tracking Tables
-- Phase 3: Enterprise GPS Integration
-- ============================================================

-- 1. GPS Vendor Integration Registry (per-CBWTF config)
-- Stores vendor-specific configuration, auth, and integration type
CREATE TABLE gps_vendor_integration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    vendor VARCHAR(50) NOT NULL,              -- WHEELSEYE, FLEETX, GENERIC
    integration_type VARCHAR(20) NOT NULL,    -- WEBHOOK, POLLING
    auth_type VARCHAR(20),                    -- API_KEY, BASIC, OAUTH, NONE
    credentials TEXT,                         -- JSON string, encrypted at-rest by app layer
    webhook_url TEXT,                         -- for webhook-based integrations
    polling_interval_seconds INTEGER DEFAULT 60,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(facility_id, vendor)
);

CREATE INDEX idx_gps_vendor_facility ON gps_vendor_integration(facility_id);

-- 2. Vehicle table (CBWTF-owned)
CREATE TABLE vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    registration_number VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(50),                 -- TRUCK, VAN, AUTO
    gps_device_id VARCHAR(100),               -- IMEI / device identifier
    gps_vendor VARCHAR(50),                   -- WHEELSEYE, FLEETX, GENERIC
    assigned_driver_id UUID REFERENCES app_user(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    gps_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- CONNECTED, ONLINE, OFFLINE, ERROR, PENDING
    last_gps_at TIMESTAMP,                    -- last known GPS timestamp
    last_latitude DECIMAL(10,7),              -- cached last position
    last_longitude DECIMAL(10,7),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(facility_id, registration_number)
);

CREATE INDEX idx_vehicle_facility ON vehicle(facility_id);
CREATE INDEX idx_vehicle_gps_device ON vehicle(gps_device_id);
CREATE INDEX idx_vehicle_driver ON vehicle(assigned_driver_id);

-- 3. GPS Event (Append-Only, Legal-Grade, Immutable)
-- CRITICAL: NO UPDATES, NO DELETES - EVER
CREATE TABLE gps_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    speed DECIMAL(6,2),                       -- km/h
    heading DECIMAL(5,2),                     -- degrees 0-360
    altitude DECIMAL(8,2),                    -- meters
    accuracy DECIMAL(6,2),                    -- meters
    recorded_at TIMESTAMP NOT NULL,           -- device timestamp
    received_at TIMESTAMP NOT NULL DEFAULT NOW(), -- server timestamp
    source VARCHAR(20) NOT NULL,              -- MOBILE_APP, IOT_DEVICE, VENDOR_API
    raw_payload TEXT                          -- original vendor payload for audit
);

-- Performance index for time-series queries
CREATE INDEX idx_gps_vehicle_time ON gps_event(vehicle_id, recorded_at DESC);
CREATE INDEX idx_gps_received ON gps_event(received_at DESC);

-- 4. GPS Ingestion Health Log (observability)
-- One row per facility+vendor, updated on each ingestion
CREATE TABLE gps_ingestion_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    vendor VARCHAR(50) NOT NULL,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    last_failure_reason TEXT,
    success_count BIGINT NOT NULL DEFAULT 0,
    failure_count BIGINT NOT NULL DEFAULT 0,
    events_ingested_today BIGINT NOT NULL DEFAULT 0,
    last_event_count INTEGER DEFAULT 0,       -- events in last ingestion
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(facility_id, vendor)
);

CREATE INDEX idx_ingestion_log_facility ON gps_ingestion_log(facility_id);

-- 5. GPS Device Binding History (audit trail)
-- Tracks all device-to-vehicle binding/unbinding operations
CREATE TABLE gps_device_binding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(100) NOT NULL,
    vehicle_id UUID REFERENCES vehicle(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    vendor VARCHAR(50),
    action VARCHAR(20) NOT NULL,              -- BOUND, UNBOUND
    performed_by UUID REFERENCES app_user(id),
    performed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT
);

CREATE INDEX idx_binding_device ON gps_device_binding(device_id, performed_at DESC);
CREATE INDEX idx_binding_vehicle ON gps_device_binding(vehicle_id, performed_at DESC);
CREATE INDEX idx_binding_facility ON gps_device_binding(facility_id);

-- ============================================================
-- Comments for documentation
-- ============================================================
COMMENT ON TABLE gps_event IS 'Append-only GPS events. NO UPDATES, NO DELETES - legal/audit requirement.';
COMMENT ON TABLE gps_vendor_integration IS 'Vendor-specific GPS integration configuration per CBWTF facility.';
COMMENT ON TABLE gps_device_binding IS 'Audit trail for GPS device-to-vehicle binding operations.';
COMMENT ON COLUMN vehicle.gps_status IS 'PENDING=not configured, CONNECTED=device bound, ONLINE=active in 15min, OFFLINE=stale, ERROR=integration issue';
