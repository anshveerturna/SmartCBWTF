-- V12__create_user_location_table.sql
-- Location tracking for DRIVER and PLANT_OPERATOR users

CREATE TABLE IF NOT EXISTS user_location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient user location queries
CREATE INDEX IF NOT EXISTS idx_user_location_user_time 
ON user_location(user_id, recorded_at DESC);

-- Add owner_name column to facility table
ALTER TABLE facility ADD COLUMN IF NOT EXISTS owner_name VARCHAR(255);

-- Add photo_url column to app_user table
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);

-- Add last_location columns to app_user for quick access
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_location_lat DOUBLE PRECISION;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_location_lon DOUBLE PRECISION;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_location_at TIMESTAMP WITH TIME ZONE;
