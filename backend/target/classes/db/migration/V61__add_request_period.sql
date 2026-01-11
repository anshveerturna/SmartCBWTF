ALTER TABLE dues_clearance_request 
ADD COLUMN request_month INTEGER,
ADD COLUMN request_year INTEGER;

-- Optional: Add constraint to ensure uniqueness? 
-- A facility shouldn't have multiple PENDING requests for the same month.
-- For now, we handle logic in app.
