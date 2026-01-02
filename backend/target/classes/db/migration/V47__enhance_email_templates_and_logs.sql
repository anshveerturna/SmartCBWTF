-- V47__enhance_email_templates_and_logs.sql

-- 1. Add category column to global_email_template
ALTER TABLE global_email_template ADD COLUMN category VARCHAR(50);

-- 2. Populate categories for existing templates
UPDATE global_email_template SET category = 'REGISTRATION' WHERE template_code IN ('HCF_WELCOME', 'HCF_CREDENTIALS', 'AGREEMENT_SUBMITTED', 'AGREEMENT_APPROVED', 'AGREEMENT_REJECTED');
UPDATE global_email_template SET category = 'BILLING' WHERE template_code IN ('INVOICE_GENERATED');
UPDATE global_email_template SET category = 'PAYMENT' WHERE template_code IN ('PAYMENT_REMINDER', 'PAYMENT_OVERDUE', 'PAYMENT_RECEIVED');
UPDATE global_email_template SET category = 'COMPLIANCE' WHERE template_code IN ('AGREEMENT_EXPIRY_WARNING', 'AGREEMENT_EXPIRY', 'REPORT_GENERATED');

-- Set default for any others (SYSTEM)
UPDATE global_email_template SET category = 'SYSTEM' WHERE category IS NULL;

-- Make category non-nullable after population
ALTER TABLE global_email_template ALTER COLUMN category SET NOT NULL;


-- 3. Enhance email_dispatch_log for regulator-grade audit
-- Add snapshot columns to store exactly what was sent (for legal proof)
ALTER TABLE email_dispatch_log ADD COLUMN body_snapshot TEXT;
ALTER TABLE email_dispatch_log ADD COLUMN placeholders_snapshot JSONB;

-- Note: We do not backport data for existing logs as these snapshots didn't exist.
-- New logs will have these fields populated.
