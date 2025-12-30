-- V42: CBWTF Admin Settings Engine
-- Creates unified facility_settings table and settings_audit_log for complete settings management

-- Main settings table with all 7 sections
CREATE TABLE IF NOT EXISTS facility_settings (
    facility_id UUID PRIMARY KEY REFERENCES facility(id) ON DELETE CASCADE,
    
    -- Schema versioning for evolution & rollback
    settings_version INTEGER NOT NULL DEFAULT 1,
    
    -- Section 1: Legal & Entity Profile
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    authorization_number VARCHAR(100),
    spcb_name VARCHAR(255),
    spcb_state VARCHAR(100),
    gstin VARCHAR(20),
    pan VARCHAR(20),
    registered_address TEXT,
    registered_state VARCHAR(100),
    registered_pincode VARCHAR(10),
    official_email VARCHAR(255),
    official_phone VARCHAR(20),
    logo_url VARCHAR(512),
    logo_checksum VARCHAR(64),
    signature_url VARCHAR(512),
    signature_checksum VARCHAR(64),
    
    -- Section 2: Financial & Billing
    cgst_percent DECIMAL(5,2) NOT NULL DEFAULT 9.00,
    sgst_percent DECIMAL(5,2) NOT NULL DEFAULT 9.00,
    igst_percent DECIMAL(5,2) NOT NULL DEFAULT 18.00,
    gst_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Section 3: Payment & Reminders (extends FacilityNotificationSettings)
    grace_period_days INTEGER NOT NULL DEFAULT 7,
    auto_alert_escalation BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Section 4: Agreement Rules
    default_agreement_validity_months INTEGER NOT NULL DEFAULT 12,
    agreement_renewal_window_days INTEGER NOT NULL DEFAULT 30,
    block_overlapping_agreements BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Section 5: QR & Operational Rules
    qr_validity_days INTEGER NOT NULL DEFAULT 30,
    allow_multiple_active_qrs BOOLEAN NOT NULL DEFAULT FALSE,
    require_cbwtf_verification BOOLEAN NOT NULL DEFAULT TRUE,
    gps_geofence_radius_m INTEGER NOT NULL DEFAULT 100,
    max_unverified_bags INTEGER NOT NULL DEFAULT 50,
    blue_waste_min_percent DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    
    -- Section 6: Compliance & Reporting
    daily_report_time TIME NOT NULL DEFAULT '23:00:00',
    monthly_report_day INTEGER NOT NULL DEFAULT 1,
    annual_form_iv_date DATE,
    enforce_checksum BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Section 7: Email & Notification
    sender_name VARCHAR(100) NOT NULL DEFAULT 'SmartCBWTF',
    sender_email VARCHAR(255) NOT NULL DEFAULT 'no-reply@smartcbwtf.com',
    cc_admin_on_hcf_emails BOOLEAN NOT NULL DEFAULT TRUE,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- First-use tracking for soft-locks
    first_invoice_at TIMESTAMPTZ,
    first_qr_generated_at TIMESTAMPTZ,
    first_compliance_report_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit log with schema versioning for complete change history
CREATE TABLE IF NOT EXISTS settings_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id) ON DELETE CASCADE,
    section VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by UUID NOT NULL REFERENCES app_user(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),
    schema_version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_settings_audit_facility ON settings_audit_log(facility_id);
CREATE INDEX IF NOT EXISTS idx_settings_audit_changed_at ON settings_audit_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_settings_audit_section ON settings_audit_log(section);

-- Initialize settings for existing facilities with defaults
INSERT INTO facility_settings (facility_id)
SELECT id FROM facility
WHERE id NOT IN (SELECT facility_id FROM facility_settings)
ON CONFLICT (facility_id) DO NOTHING;

-- Copy existing data from facility table where applicable
UPDATE facility_settings fs
SET 
    legal_name = f.name,
    gstin = f.gst_number,
    pan = f.pan_number,
    official_email = f.contact_email,
    official_phone = f.contact_phone,
    registered_address = f.address,
    gps_geofence_radius_m = COALESCE(f.geofence_radius_m, 100)
FROM facility f
WHERE fs.facility_id = f.id
AND fs.legal_name IS NULL;
