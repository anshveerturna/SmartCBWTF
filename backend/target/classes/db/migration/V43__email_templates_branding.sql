-- Document & Email Template Customization System
-- Adds email templates, branding config, and sender slug support

-- 1. Add sender configuration to facility
ALTER TABLE facility ADD COLUMN IF NOT EXISTS sender_slug VARCHAR(100);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS use_generic_sender BOOLEAN DEFAULT FALSE;
ALTER TABLE facility ADD COLUMN IF NOT EXISTS sender_slug_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE facility ADD COLUMN IF NOT EXISTS cbwtf_notification_email VARCHAR(255);

-- Create unique index on sender_slug
CREATE UNIQUE INDEX IF NOT EXISTS idx_facility_sender_slug ON facility(sender_slug) WHERE sender_slug IS NOT NULL;

-- 2. Email template customization per facility
CREATE TABLE IF NOT EXISTS facility_email_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    template_code VARCHAR(50) NOT NULL,
    subject_template VARCHAR(500),
    body_template TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(facility_id, template_code, version)
);

CREATE INDEX IF NOT EXISTS idx_email_template_facility ON facility_email_template(facility_id);
CREATE INDEX IF NOT EXISTS idx_email_template_active ON facility_email_template(facility_id, template_code, is_active) WHERE is_active = true;

-- 3. Branding configuration per facility
CREATE TABLE IF NOT EXISTS facility_branding (
    facility_id UUID PRIMARY KEY REFERENCES facility(id),
    logo_url VARCHAR(500),
    logo_checksum VARCHAR(64),
    primary_color VARCHAR(7) DEFAULT '#1976d2',
    secondary_color VARCHAR(7) DEFAULT '#424242',
    invoice_footer_text TEXT,
    receipt_footer_text TEXT,
    show_logo_on_invoice BOOLEAN DEFAULT true,
    show_logo_on_receipt BOOLEAN DEFAULT true,
    show_logo_on_email BOOLEAN DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Branding snapshot for document immutability
CREATE TABLE IF NOT EXISTS branding_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    logo_url VARCHAR(500),
    logo_checksum VARCHAR(64),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    footer_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_branding_snapshot_facility ON branding_snapshot(facility_id);

-- 5. Link bills to branding snapshots (commented out - requires table owner permissions)
-- TODO: Run this manually with superuser or ask DBA to grant permissions
-- ALTER TABLE bill ADD COLUMN IF NOT EXISTS branding_snapshot_id UUID REFERENCES branding_snapshot(id);

-- 6. Backfill sender_slug for existing facilities
UPDATE facility 
SET sender_slug = LOWER(REGEXP_REPLACE(COALESCE(code, SUBSTRING(name, 1, 50)), '[^a-zA-Z0-9]', '-', 'g'))
WHERE sender_slug IS NULL AND code IS NOT NULL;

-- 7. Seed default email templates for existing facilities
INSERT INTO facility_email_template (facility_id, template_code, subject_template, body_template, is_active, version)
SELECT 
    f.id,
    'HCF_WELCOME',
    'Welcome to {{facilityName}}',
    '<html><body style="font-family: Arial, sans-serif;">
<h2>Welcome!</h2>
<p>Dear {{hcfName}},</p>
<p>Your registration with <strong>{{facilityName}}</strong> has been successfully completed.</p>
<p><strong>Agreement Number:</strong> {{agreementNumber}}</p>
<p>Our team will review your registration within 2-3 business days.</p>
<p>Best regards,<br>{{facilityName}} Team</p>
</body></html>',
    true,
    1
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_email_template t 
    WHERE t.facility_id = f.id AND t.template_code = 'HCF_WELCOME'
);

INSERT INTO facility_email_template (facility_id, template_code, subject_template, body_template, is_active, version)
SELECT 
    f.id,
    'PAYMENT_REMINDER',
    'Payment Reminder - Invoice {{invoiceNumber}}',
    '<html><body style="font-family: Arial, sans-serif;">
<h2>Payment Reminder</h2>
<p>Dear {{hcfName}},</p>
<p>This is a reminder that invoice <strong>{{invoiceNumber}}</strong> for <strong>₹{{amountDue}}</strong> is due on <strong>{{dueDate}}</strong>.</p>
<p>Please ensure timely payment to avoid service disruption.</p>
<p>Best regards,<br>{{facilityName}} Team</p>
</body></html>',
    true,
    1
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_email_template t 
    WHERE t.facility_id = f.id AND t.template_code = 'PAYMENT_REMINDER'
);

INSERT INTO facility_email_template (facility_id, template_code, subject_template, body_template, is_active, version)
SELECT 
    f.id,
    'INVOICE_GENERATED',
    'New Invoice {{invoiceNumber}} - {{facilityName}}',
    '<html><body style="font-family: Arial, sans-serif;">
<h2>Invoice Generated</h2>
<p>Dear {{hcfName}},</p>
<p>A new invoice has been generated for your account:</p>
<p><strong>Invoice Number:</strong> {{invoiceNumber}}<br>
<strong>Amount:</strong> ₹{{amount}}<br>
<strong>Due Date:</strong> {{dueDate}}</p>
<p>Please find the invoice attached to this email.</p>
<p>Best regards,<br>{{facilityName}} Team</p>
</body></html>',
    true,
    1
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_email_template t 
    WHERE t.facility_id = f.id AND t.template_code = 'INVOICE_GENERATED'
);

INSERT INTO facility_email_template (facility_id, template_code, subject_template, body_template, is_active, version)
SELECT 
    f.id,
    'PAYMENT_OVERDUE',
    'URGENT: Payment Overdue - Invoice {{invoiceNumber}}',
    '<html><body style="font-family: Arial, sans-serif;">
<h2 style="color: #d32f2f;">Payment Overdue</h2>
<p>Dear {{hcfName}},</p>
<p>Invoice <strong>{{invoiceNumber}}</strong> for <strong>₹{{amountDue}}</strong> is now <strong>{{daysOverdue}} days overdue</strong>.</p>
<p>Please make payment immediately to avoid service suspension.</p>
<p>Best regards,<br>{{facilityName}} Team</p>
</body></html>',
    true,
    1
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_email_template t 
    WHERE t.facility_id = f.id AND t.template_code = 'PAYMENT_OVERDUE'
);

INSERT INTO facility_email_template (facility_id, template_code, subject_template, body_template, is_active, version)
SELECT 
    f.id,
    'AGREEMENT_EXPIRING',
    'Agreement Expiring Soon - {{agreementNumber}}',
    '<html><body style="font-family: Arial, sans-serif;">
<h2>Agreement Expiring Soon</h2>
<p>Dear {{hcfName}},</p>
<p>Your agreement <strong>{{agreementNumber}}</strong> with {{facilityName}} will expire on <strong>{{expiryDate}}</strong>.</p>
<p>Please contact us to renew your agreement and continue uninterrupted service.</p>
<p>Best regards,<br>{{facilityName}} Team</p>
</body></html>',
    true,
    1
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_email_template t 
    WHERE t.facility_id = f.id AND t.template_code = 'AGREEMENT_EXPIRING'
);

INSERT INTO facility_email_template (facility_id, template_code, subject_template, body_template, is_active, version)
SELECT 
    f.id,
    'HCF_CREDENTIALS',
    'Your Login Credentials - {{facilityName}}',
    '<html><body style="font-family: Arial, sans-serif;">
<h2>Your Login Credentials</h2>
<p>Dear {{hcfName}},</p>
<p>Your account has been created. Here are your login details:</p>
<p><strong>Username:</strong> {{username}}<br>
<strong>Login URL:</strong> <a href="{{loginUrl}}">{{loginUrl}}</a></p>
<p>Please change your password after first login.</p>
<p>Best regards,<br>{{facilityName}} Team</p>
</body></html>',
    true,
    1
FROM facility f
WHERE NOT EXISTS (
    SELECT 1 FROM facility_email_template t 
    WHERE t.facility_id = f.id AND t.template_code = 'HCF_CREDENTIALS'
);
