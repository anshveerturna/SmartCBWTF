-- V45: Email Sender Identity Refactor
-- Enforce system-controlled sender identity for all facility emails

-- Add new sender identity columns
ALTER TABLE facility_settings
ADD COLUMN IF NOT EXISTS sender_slug VARCHAR(50),
ADD COLUMN IF NOT EXISTS use_generic_sender BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS notification_email VARCHAR(255),
ADD COLUMN IF NOT EXISTS first_email_sent_at TIMESTAMP;

-- Generate initial sender_slug from trade_name or facility code
-- Lowercase, alphanumeric + hyphens only
UPDATE facility_settings fs
SET sender_slug = LOWER(
    REGEXP_REPLACE(
        REGEXP_REPLACE(
            COALESCE(fs.trade_name, f.code, 'facility-' || fs.facility_id::text),
            '[^a-zA-Z0-9-]', '-', 'g'
        ),
        '-+', '-', 'g'
    )
)
FROM facility f
WHERE fs.facility_id = f.id
  AND fs.sender_slug IS NULL;

-- Migrate notification_email from official_email if available
UPDATE facility_settings
SET notification_email = official_email
WHERE notification_email IS NULL
  AND official_email IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN facility_settings.sender_slug IS 'System-generated sender address slug. Format: {slug}@smartcbwtf.com. Immutable after first_email_sent_at is set.';
COMMENT ON COLUMN facility_settings.use_generic_sender IS 'If true, use no-reply@smartcbwtf.com instead of facility-specific sender.';
COMMENT ON COLUMN facility_settings.notification_email IS 'Email address where CBWTF receives system notifications (alerts, billing, compliance).';
COMMENT ON COLUMN facility_settings.first_email_sent_at IS 'Timestamp of first email sent. Once set, sender_slug becomes immutable.';

-- Note: sender_name and sender_email columns are kept for backward compatibility
-- but will no longer be used by the application. They can be dropped in a future migration
-- after confirming no external systems depend on them.
