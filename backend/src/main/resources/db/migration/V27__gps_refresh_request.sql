-- Add GPS refresh request timestamp for admin-triggered location updates
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS gps_refresh_requested_at TIMESTAMPTZ;
