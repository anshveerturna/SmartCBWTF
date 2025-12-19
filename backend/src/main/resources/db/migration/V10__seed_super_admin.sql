-- V10: Add missing columns to app_user and seed SUPER_ADMIN user
-- Password: demo123 (bcrypt hash)

-- Add missing columns if they don't exist
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT false;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT now();

-- Create super_admin user (or update if exists)
INSERT INTO app_user (id, username, password_hash, full_name, email, role, active, force_password_change, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001'::uuid,
    'super_admin',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', -- demo123
    'Platform Administrator',
    'super@smartcbwtf.com',
    'SUPER_ADMIN',
    true,
    false,
    NOW()
)
ON CONFLICT (username) DO UPDATE SET
    role = 'SUPER_ADMIN',
    active = true;

-- Update existing demo facility subscription
UPDATE facility SET 
    subscription_plan = 'PRO',
    subscription_status = 'ACTIVE',
    subscription_expires_at = NOW() + INTERVAL '1 year',
    onboarded_at = COALESCE(onboarded_at, NOW())
WHERE code = 'DEMO-CBWTF';
