-- Fix missing columns in facility_settings table that caused 500 errors

-- Agreement Terms Template (Text for PDF generation)
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS agreement_terms_template TEXT;

-- Sender Identity Fields (Email/notification settings)
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS sender_slug VARCHAR(50);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS use_generic_sender BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS notification_email VARCHAR(255);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS first_email_sent_at TIMESTAMP WITH TIME ZONE;

-- New toggle fields if they don't exist
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS cc_admin_on_hcf_emails BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS in_app_alerts_enabled BOOLEAN DEFAULT TRUE NOT NULL;
