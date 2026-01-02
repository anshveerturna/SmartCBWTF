-- V46: Global Email Templates (SuperAdmin Managed)
-- Replaces facility-level email templates with centralized global templates
-- Only SuperAdmin can manage templates; CBWTF admins cannot edit

-- ============================================================================
-- PART 1: DROP FACILITY-LEVEL TEMPLATE TABLE (IF EXISTS)
-- ============================================================================
DROP TABLE IF EXISTS facility_email_template;

-- ============================================================================
-- PART 2: CREATE GLOBAL EMAIL TEMPLATE TABLE
-- ============================================================================
CREATE TABLE global_email_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code VARCHAR(50) NOT NULL,
    subject TEXT NOT NULL,
    body_html TEXT NOT NULL,
    required_placeholders TEXT[] NOT NULL DEFAULT '{}',
    optional_placeholders TEXT[] NOT NULL DEFAULT '{}',
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(template_code, version)
);

-- Indexes for performance
CREATE INDEX idx_global_email_template_code ON global_email_template(template_code);
CREATE INDEX idx_global_email_template_active ON global_email_template(template_code, is_active) WHERE is_active = true;

-- ============================================================================
-- PART 3: SEED DEFAULT PROFESSIONAL TEMPLATES
-- ============================================================================

-- HCF_WELCOME
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'HCF_WELCOME',
    'Welcome to SmartCBWTF – Registration Received',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>Thank you for registering with <strong>{{facilityName}}</strong>.</p>
  
  <p>Your registration has been received and is currently under review. You will receive a separate email with your login credentials once your account is approved.</p>
  
  <p>If you have any questions, please contact us.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- HCF_CREDENTIALS
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'HCF_CREDENTIALS',
    'Your SmartCBWTF Login Credentials',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>Your account with <strong>{{facilityName}}</strong> has been activated. Please find your login credentials below:</p>
  
  <table style="margin: 20px 0; border-collapse: collapse;">
    <tr><td style="padding: 8px; border: 1px solid #ddd;"><strong>Username:</strong></td><td style="padding: 8px; border: 1px solid #ddd;">{{username}}</td></tr>
    <tr><td style="padding: 8px; border: 1px solid #ddd;"><strong>Password:</strong></td><td style="padding: 8px; border: 1px solid #ddd;">{{password}}</td></tr>
    <tr><td style="padding: 8px; border: 1px solid #ddd;"><strong>Login URL:</strong></td><td style="padding: 8px; border: 1px solid #ddd;"><a href="{{loginUrl}}">{{loginUrl}}</a></td></tr>
  </table>
  
  <p>Please change your password upon first login for security purposes.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'username', 'password', 'loginUrl'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- AGREEMENT_SUBMITTED
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'AGREEMENT_SUBMITTED',
    'Agreement Submitted – Pending Approval',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>Your service agreement with <strong>{{facilityName}}</strong> has been submitted successfully.</p>
  
  <p><strong>Agreement Number:</strong> {{agreementNumber}}<br>
  <strong>Submitted Date:</strong> {{submittedDate}}</p>
  
  <p>Your agreement is currently under review. You will be notified once it has been processed.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'agreementNumber', 'submittedDate'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- AGREEMENT_APPROVED
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'AGREEMENT_APPROVED',
    'Agreement Approved',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>We are pleased to inform you that your service agreement with <strong>{{facilityName}}</strong> has been approved.</p>
  
  <p><strong>Agreement Number:</strong> {{agreementNumber}}<br>
  <strong>Effective From:</strong> {{effectiveDate}}<br>
  <strong>Valid Until:</strong> {{expiryDate}}</p>
  
  <p>You may now use the waste management services as per the terms of your agreement.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'agreementNumber', 'effectiveDate', 'expiryDate'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- AGREEMENT_REJECTED
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'AGREEMENT_REJECTED',
    'Agreement Not Approved',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>We regret to inform you that your service agreement with <strong>{{facilityName}}</strong> could not be approved at this time.</p>
  
  <p><strong>Agreement Number:</strong> {{agreementNumber}}<br>
  <strong>Reason:</strong> {{rejectionReason}}</p>
  
  <p>Please contact us for further clarification or to resubmit your application.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'agreementNumber', 'rejectionReason'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- AGREEMENT_EXPIRY
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'AGREEMENT_EXPIRY',
    'Agreement Expiring Soon – Action Required',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>This is to remind you that your service agreement with <strong>{{facilityName}}</strong> is expiring soon.</p>
  
  <p><strong>Agreement Number:</strong> {{agreementNumber}}<br>
  <strong>Expiry Date:</strong> {{expiryDate}}<br>
  <strong>Days Remaining:</strong> {{daysRemaining}}</p>
  
  <p>Please initiate the renewal process to ensure uninterrupted services.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'agreementNumber', 'expiryDate', 'daysRemaining'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- INVOICE_GENERATED
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'INVOICE_GENERATED',
    'Invoice Generated – {{invoiceNumber}}',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>A new invoice has been generated for your account with <strong>{{facilityName}}</strong>.</p>
  
  <p><strong>Invoice Number:</strong> {{invoiceNumber}}<br>
  <strong>Invoice Date:</strong> {{invoiceDate}}<br>
  <strong>Amount Due:</strong> ₹{{invoiceAmount}}<br>
  <strong>Due Date:</strong> {{dueDate}}</p>
  
  <p>Please ensure timely payment to avoid any service disruption.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'invoiceNumber', 'invoiceDate', 'invoiceAmount', 'dueDate'],
    ARRAY['invoiceUrl'],
    1,
    true
);

-- PAYMENT_REMINDER
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'PAYMENT_REMINDER',
    'Payment Reminder – Invoice {{invoiceNumber}}',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>This is a friendly reminder regarding the pending payment for the following invoice:</p>
  
  <p><strong>Invoice Number:</strong> {{invoiceNumber}}<br>
  <strong>Amount Due:</strong> ₹{{amountDue}}<br>
  <strong>Due Date:</strong> {{dueDate}}</p>
  
  <p>Please arrange for payment at your earliest convenience.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'invoiceNumber', 'amountDue', 'dueDate'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- PAYMENT_OVERDUE
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'PAYMENT_OVERDUE',
    'Payment Overdue – Immediate Attention Required',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>This is to notify you that the following payment is overdue:</p>
  
  <p><strong>Invoice Number:</strong> {{invoiceNumber}}<br>
  <strong>Amount Overdue:</strong> ₹{{amountDue}}<br>
  <strong>Days Past Due:</strong> {{daysPastDue}}</p>
  
  <p>Please settle this amount immediately to avoid any service-related actions.</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'invoiceNumber', 'amountDue', 'daysPastDue'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- PAYMENT_RECEIVED
INSERT INTO global_email_template (template_code, subject, body_html, required_placeholders, optional_placeholders, version, is_active)
VALUES (
    'PAYMENT_RECEIVED',
    'Payment Received – Thank You',
    '<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"></head>
<body style="font-family: Arial, sans-serif; font-size: 14px; color: #333; line-height: 1.6;">
  <p>Dear {{hcfName}},</p>
  
  <p>We have received your payment. Thank you for your prompt settlement.</p>
  
  <p><strong>Invoice Number:</strong> {{invoiceNumber}}<br>
  <strong>Amount Received:</strong> ₹{{amountReceived}}<br>
  <strong>Payment Date:</strong> {{paymentDate}}<br>
  <strong>Receipt Number:</strong> {{receiptNumber}}</p>
  
  <p>Regards,<br><strong>{{facilityName}}</strong></p>
</body>
</html>',
    ARRAY['hcfName', 'facilityName', 'invoiceNumber', 'amountReceived', 'paymentDate', 'receiptNumber'],
    ARRAY[]::TEXT[],
    1,
    true
);

-- ============================================================================
-- MIGRATION COMPLETE
-- ============================================================================
