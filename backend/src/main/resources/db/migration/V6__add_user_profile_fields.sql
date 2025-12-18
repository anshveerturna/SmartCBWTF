-- V5__add_user_profile_fields.sql
-- Add additional profile fields to app_user table for profile screen

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS dob DATE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS profile_photo_url TEXT;

-- Add indexes for common lookups
CREATE INDEX IF NOT EXISTS idx_app_user_phone ON app_user(phone);

-- Update existing test user with sample profile data
UPDATE app_user 
SET phone = '9876543210',
    gender = 'MALE'
WHERE username = 'driver1' AND phone IS NULL;
