-- =====================================================
-- V39: PHASE 9 - EMAIL & ALERTS ENGINE
-- Regulator-grade, idempotent email and alerting system
-- =====================================================

-- Facility notification settings (CBWTF configurable)
CREATE TABLE facility_notification_settings (
  facility_id UUID PRIMARY KEY REFERENCES facility(id),
  payment_reminder_start_days INTEGER NOT NULL DEFAULT 7,
  payment_reminder_frequency_days INTEGER NOT NULL DEFAULT 3,
  max_overdue_reminders INTEGER NOT NULL DEFAULT 5,
  agreement_expiry_warning_days INTEGER NOT NULL DEFAULT 30,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Email dispatch log (IMMUTABLE)
CREATE TABLE email_dispatch_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL, -- Business event ID (idempotency key)
  template_code VARCHAR(50) NOT NULL,
  template_version INTEGER NOT NULL DEFAULT 1,
  recipient_email VARCHAR(255) NOT NULL,
  cc_email VARCHAR(255),
  facility_id UUID NOT NULL REFERENCES facility(id),
  entity_type VARCHAR(50) NOT NULL,
  entity_id UUID NOT NULL,
  reminder_sequence INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL, -- SENT / FAILED / SKIPPED
  failure_reason TEXT,
  subject TEXT NOT NULL,
  checksum VARCHAR(64) NOT NULL,
  sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  -- Idempotency: one business event → one email per template
  UNIQUE(event_id, template_code)
);

-- Alert (IMMUTABLE except is_read)
CREATE TABLE alert (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL, -- Business event ID (idempotency key)
  facility_id UUID NOT NULL REFERENCES facility(id),
  severity VARCHAR(20) NOT NULL, -- INFO / WARN / CRITICAL
  type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  related_entity_type VARCHAR(50),
  related_entity_id UUID,
  is_read BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  -- Idempotency: one business event → one alert per type
  UNIQUE(event_id, type)
);

-- Performance indexes
CREATE INDEX idx_email_log_facility ON email_dispatch_log(facility_id, sent_at DESC);
CREATE INDEX idx_email_log_entity ON email_dispatch_log(entity_type, entity_id);
CREATE INDEX idx_email_log_event ON email_dispatch_log(event_id);
CREATE INDEX idx_alert_facility ON alert(facility_id, created_at DESC);
CREATE INDEX idx_alert_unread ON alert(facility_id, is_read) WHERE NOT is_read;
CREATE INDEX idx_alert_type ON alert(facility_id, type);
CREATE INDEX idx_alert_event ON alert(event_id);
