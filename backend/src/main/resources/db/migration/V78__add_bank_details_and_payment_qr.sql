-- Add bank details and payment QR URL to facility_settings
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(200);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(50);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS bank_name VARCHAR(200);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS bank_branch VARCHAR(200);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS bank_ifsc VARCHAR(20);
ALTER TABLE facility_settings ADD COLUMN IF NOT EXISTS payment_qr_url VARCHAR(512);
