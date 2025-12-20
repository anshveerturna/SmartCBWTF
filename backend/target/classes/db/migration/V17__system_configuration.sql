-- V17: System Configuration Module
-- Enterprise-grade configuration management with versioning and audit

-- Core configuration table
CREATE TABLE system_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL CHECK (value_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON')),
    category VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    requires_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    is_readonly BOOLEAN NOT NULL DEFAULT FALSE,
    validation_rules JSONB,
    version INTEGER NOT NULL DEFAULT 1,
    updated_by UUID REFERENCES app_user(id),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Configuration audit trail
CREATE TABLE system_config_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT NOT NULL,
    changed_by UUID REFERENCES app_user(id),
    changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reason TEXT,
    ip_address VARCHAR(45)
);

-- Indexes
CREATE INDEX idx_system_config_category ON system_config(category);
CREATE INDEX idx_system_config_key ON system_config(config_key);
CREATE INDEX idx_system_config_audit_key ON system_config_audit(config_key);
CREATE INDEX idx_system_config_audit_time ON system_config_audit(changed_at DESC);

-- Seed default configuration values

-- CATEGORY: PLATFORM_GLOBAL
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description) VALUES
('platform.name', 'SmartCBWTF', 'STRING', 'PLATFORM_GLOBAL', 'Platform Name', 'Display name of the platform'),
('platform.support_email', 'support@smartcbwtf.com', 'STRING', 'PLATFORM_GLOBAL', 'Support Email', 'Primary support email address'),
('platform.support_phone', '+91-1800-XXX-XXXX', 'STRING', 'PLATFORM_GLOBAL', 'Support Phone', 'Primary support phone number'),
('platform.timezone', 'Asia/Kolkata', 'STRING', 'PLATFORM_GLOBAL', 'Default Timezone', 'Default timezone for the platform'),
('platform.date_format', 'dd/MM/yyyy', 'STRING', 'PLATFORM_GLOBAL', 'Date Format', 'Default date format'),
('platform.currency', 'INR', 'STRING', 'PLATFORM_GLOBAL', 'Currency', 'Default currency code'),
('platform.maintenance_mode', 'false', 'BOOLEAN', 'PLATFORM_GLOBAL', 'Maintenance Mode', 'Enable maintenance mode to block user access');

INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description, requires_confirmation) VALUES
('platform.maintenance_message', 'System is undergoing scheduled maintenance. Please try again later.', 'STRING', 'PLATFORM_GLOBAL', 'Maintenance Message', 'Message shown during maintenance', true);

-- CATEGORY: SECURITY
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description, validation_rules) VALUES
('security.max_login_attempts', '5', 'NUMBER', 'SECURITY', 'Max Login Attempts', 'Maximum failed login attempts before lockout', '{"min": 3, "max": 10}'),
('security.session_timeout_minutes', '60', 'NUMBER', 'SECURITY', 'Session Timeout', 'Session timeout in minutes', '{"min": 15, "max": 480}'),
('security.password_min_length', '8', 'NUMBER', 'SECURITY', 'Password Min Length', 'Minimum password length', '{"min": 6, "max": 32}'),
('security.password_require_uppercase', 'true', 'BOOLEAN', 'SECURITY', 'Require Uppercase', 'Require uppercase letter in password', NULL),
('security.password_require_number', 'true', 'BOOLEAN', 'SECURITY', 'Require Number', 'Require number in password', NULL),
('security.password_require_special', 'false', 'BOOLEAN', 'SECURITY', 'Require Special Character', 'Require special character in password', NULL),
('security.force_password_reset_first_login', 'false', 'BOOLEAN', 'SECURITY', 'Force Password Reset', 'Force password reset on first login', NULL),
('security.allow_concurrent_sessions', 'true', 'BOOLEAN', 'SECURITY', 'Allow Concurrent Sessions', 'Allow multiple simultaneous sessions', NULL),
('security.max_devices_per_user', '3', 'NUMBER', 'SECURITY', 'Max Devices Per User', 'Maximum devices per user account', '{"min": 1, "max": 10}');

-- CATEGORY: SUBSCRIPTION
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description, validation_rules) VALUES
('subscription.default_duration_months', '12', 'NUMBER', 'SUBSCRIPTION', 'Default Duration', 'Default subscription duration in months', '{"min": 1, "max": 36}'),
('subscription.allowed_plans', '["BASIC","PRO","ENTERPRISE"]', 'JSON', 'SUBSCRIPTION', 'Allowed Plans', 'Available subscription plans', NULL),
('subscription.temp_access_max_days', '30', 'NUMBER', 'SUBSCRIPTION', 'Temp Access Max Days', 'Maximum days for temporary access', '{"min": 1, "max": 90}'),
('subscription.auto_expire_unpaid', 'true', 'BOOLEAN', 'SUBSCRIPTION', 'Auto-Expire Unpaid', 'Automatically expire unpaid subscriptions', NULL),
('subscription.invoice_due_days', '15', 'NUMBER', 'SUBSCRIPTION', 'Invoice Due Days', 'Days until invoice is due', '{"min": 7, "max": 60}'),
('subscription.trial_enabled', 'true', 'BOOLEAN', 'SUBSCRIPTION', 'Trial Enabled', 'Enable trial period for new CBWTFs', NULL),
('subscription.trial_duration_days', '14', 'NUMBER', 'SUBSCRIPTION', 'Trial Duration', 'Trial period duration in days', '{"min": 7, "max": 30}');

-- CATEGORY: FEATURE_DEFAULTS
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description) VALUES
('feature.default_advanced_analytics', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Advanced Analytics', 'Enable advanced analytics by default for new CBWTFs'),
('feature.default_route_optimization', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Route Optimization', 'Enable route optimization by default'),
('feature.default_cpcb_reporting', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'CPCB Reporting', 'Enable CPCB reporting by default'),
('feature.default_invoice_auto_send', 'false', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Invoice Auto-Send', 'Auto-send invoices by default'),
('feature.default_payment_gateway', 'false', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Payment Gateway', 'Enable payment gateway by default'),
('feature.default_attendance_enforcement', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Attendance Enforcement', 'Enable attendance enforcement by default'),
('feature.default_vehicle_tracking', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Vehicle Tracking', 'Enable vehicle tracking by default'),
('feature.default_ai_insights', 'false', 'BOOLEAN', 'FEATURE_DEFAULTS', 'AI Insights', 'Enable AI insights by default'),
('feature.default_multi_vehicle', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'Multi-Vehicle', 'Enable multi-vehicle support by default'),
('feature.default_hcf_self_service', 'true', 'BOOLEAN', 'FEATURE_DEFAULTS', 'HCF Self-Service', 'Enable HCF self-service by default');

-- CATEGORY: OPERATIONAL
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description, validation_rules) VALUES
('operational.default_geofence_radius_meters', '100', 'NUMBER', 'OPERATIONAL', 'Geofence Radius', 'Default geofence radius in meters', '{"min": 10, "max": 500}'),
('operational.blue_waste_min_percentage', '55', 'NUMBER', 'OPERATIONAL', 'Blue Waste Min %', 'Minimum percentage for blue category waste', '{"min": 0, "max": 100}'),
('operational.weight_mismatch_tolerance_percent', '5', 'NUMBER', 'OPERATIONAL', 'Weight Tolerance %', 'Allowed weight mismatch tolerance percentage', '{"min": 1, "max": 20}'),
('operational.max_verification_delay_minutes', '30', 'NUMBER', 'OPERATIONAL', 'Max Verification Delay', 'Maximum delay allowed for bag verification', '{"min": 5, "max": 120}'),
('operational.attendance_distance_tolerance_meters', '50', 'NUMBER', 'OPERATIONAL', 'Attendance Tolerance', 'Distance tolerance for attendance marking', '{"min": 10, "max": 200}'),
('operational.location_update_interval_minutes', '5', 'NUMBER', 'OPERATIONAL', 'Location Update Interval', 'Mobile app location update frequency', '{"min": 1, "max": 30}');

-- CATEGORY: COMPLIANCE
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description, is_sensitive) VALUES
('compliance.cpcb_api_url', 'https://api.cpcb.gov.in', 'STRING', 'COMPLIANCE', 'CPCB API URL', 'Central Pollution Control Board API endpoint', false),
('compliance.spcb_api_url', '', 'STRING', 'COMPLIANCE', 'SPCB API URL', 'State Pollution Control Board API endpoint', false),
('compliance.reporting_frequency', 'MONTHLY', 'STRING', 'COMPLIANCE', 'Reporting Frequency', 'Default compliance reporting frequency', false),
('compliance.auto_submit_reports', 'false', 'BOOLEAN', 'COMPLIANCE', 'Auto-Submit Reports', 'Automatically submit compliance reports', false),
('compliance.data_retention_years', '7', 'NUMBER', 'COMPLIANCE', 'Data Retention', 'Years to retain compliance data', false),
('compliance.export_formats', '["PDF","CSV","EXCEL"]', 'JSON', 'COMPLIANCE', 'Export Formats', 'Allowed export formats for reports', false);

-- CATEGORY: SAFETY_CONTROLS (requires confirmation for changes)
INSERT INTO system_config (config_key, config_value, value_type, category, display_name, description, requires_confirmation) VALUES
('safety.disable_all_logins', 'false', 'BOOLEAN', 'SAFETY_CONTROLS', 'Disable All Logins', 'CRITICAL: Disable all user logins except SuperAdmin', true),
('safety.disable_android_sync', 'false', 'BOOLEAN', 'SAFETY_CONTROLS', 'Disable Android Sync', 'CRITICAL: Disable Android app data synchronization', true),
('safety.disable_qr_verification', 'false', 'BOOLEAN', 'SAFETY_CONTROLS', 'Disable QR Verification', 'CRITICAL: Disable QR code verification', true),
('safety.readonly_mode', 'false', 'BOOLEAN', 'SAFETY_CONTROLS', 'Read-Only Mode', 'CRITICAL: Put system in read-only mode (emergency)', true),
('safety.kill_ai_features', 'false', 'BOOLEAN', 'SAFETY_CONTROLS', 'Kill AI Features', 'CRITICAL: Disable all AI-powered features', true);

COMMENT ON TABLE system_config IS 'System-wide configuration settings with versioning';
COMMENT ON TABLE system_config_audit IS 'Audit log for configuration changes';
