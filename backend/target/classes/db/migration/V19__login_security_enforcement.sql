-- V19: Login Security Enforcement
-- Add fields to app_user for tracking failed logins and password reset requirements

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_failed_login_at TIMESTAMP;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- Index for finding locked accounts
CREATE INDEX IF NOT EXISTS idx_app_user_locked ON app_user(locked_until) WHERE locked_until IS NOT NULL;

-- Add audit action types for security events
COMMENT ON COLUMN app_user.failed_login_attempts IS 'Counter for consecutive failed login attempts';
COMMENT ON COLUMN app_user.locked_until IS 'Account locked until this timestamp';
COMMENT ON COLUMN app_user.must_change_password IS 'Force password change on next login';
