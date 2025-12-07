-- Baseline schema for Smart CBWTF
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE facility (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    gps_lat DOUBLE PRECISION,
    gps_lon DOUBLE PRECISION,
    geofence_radius_m INTEGER,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE hcf (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    number_of_beds INTEGER,
    gps_lat DOUBLE PRECISION NOT NULL,
    gps_lon DOUBLE PRECISION NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE agreement (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agreement_number VARCHAR(100) UNIQUE NOT NULL,
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    start_date DATE NOT NULL,
    end_date DATE,
    per_bed_per_day_rate NUMERIC(12,2) NOT NULL,
    terms_text TEXT,
    pdf_url TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(32) NOT NULL,
    facility_id UUID REFERENCES facility(id),
    hcf_id UUID REFERENCES hcf(id),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE bag_label (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    category VARCHAR(32) NOT NULL,
    serial_no VARCHAR(64) NOT NULL,
    qr_code VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(32) NOT NULL,
    issued_at TIMESTAMPTZ DEFAULT now(),
    used_at TIMESTAMPTZ,
    void_reason TEXT
);

CREATE TABLE bag_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bag_label_id UUID NOT NULL REFERENCES bag_label(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    event_type VARCHAR(32) NOT NULL,
    event_ts TIMESTAMPTZ NOT NULL,
    gps_lat DOUBLE PRECISION NOT NULL,
    gps_lon DOUBLE PRECISION NOT NULL,
    weight_kg NUMERIC(12,3) NOT NULL,
    collected_by_user_id UUID NOT NULL REFERENCES app_user(id),
    app_device_id VARCHAR(128),
    anomaly_state VARCHAR(32),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE invoice (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    hcf_id UUID NOT NULL REFERENCES hcf(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    beds INTEGER,
    per_bed_per_day_rate NUMERIC(12,2) NOT NULL,
    base_amount NUMERIC(12,2) NOT NULL,
    tax_amount NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    pdf_url TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(32) NOT NULL,
    actor_user_id UUID REFERENCES app_user(id),
    ts TIMESTAMPTZ DEFAULT now(),
    data_json JSONB,
    data_hash VARCHAR(128)
);

CREATE TABLE export_job (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_type VARCHAR(64) NOT NULL,
    requested_by_user_id UUID REFERENCES app_user(id),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    artifact_url TEXT,
    params_json JSONB
);
