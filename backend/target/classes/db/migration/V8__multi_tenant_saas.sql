-- V8: Multi-Tenant SaaS Configuration
-- Subscription management, feature flags, and tenant lifecycle

-- Add subscription columns to facility table
ALTER TABLE facility 
    ADD COLUMN subscription_plan VARCHAR(20) DEFAULT 'BASIC',
    ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'ACTIVE',
    ADD COLUMN subscription_expires_at TIMESTAMP,
    ADD COLUMN onboarded_at TIMESTAMP DEFAULT now(),
    ADD COLUMN onboarded_by UUID REFERENCES app_user(id);

-- Add constraint for valid subscription plans
ALTER TABLE facility 
    ADD CONSTRAINT chk_subscription_plan 
    CHECK (subscription_plan IN ('BASIC', 'PRO', 'ENTERPRISE', 'TRIAL'));

-- Add constraint for valid subscription statuses
ALTER TABLE facility 
    ADD CONSTRAINT chk_subscription_status 
    CHECK (subscription_status IN ('ACTIVE', 'TRIAL', 'EXPIRED', 'SUSPENDED', 'CANCELLED'));

-- Feature flags per tenant
CREATE TABLE tenant_feature_flag (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id) ON DELETE CASCADE,
    feature_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    config JSONB,  -- Optional configuration for the feature
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_tenant_feature UNIQUE (facility_id, feature_key)
);

-- Predefined feature keys (reference only, not enforced)
COMMENT ON TABLE tenant_feature_flag IS 'Feature flags per tenant. Common keys: 
    ADVANCED_ANALYTICS - Access to detailed analytics dashboards
    MULTI_VEHICLE - Track multiple vehicles per facility
    INVOICE_AUTO_SEND - Auto-email invoices on generation
    CPCB_REPORTING - CPCB/SPCB compliance report generation
    ROUTE_OPTIMIZATION - AI-powered route suggestions
    HCF_SELF_SERVICE - Allow HCF self-registration';

-- Subscription audit log
CREATE TABLE subscription_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    facility_id UUID NOT NULL REFERENCES facility(id),
    action VARCHAR(50) NOT NULL,  -- CREATED, PLAN_CHANGED, RENEWED, EXPIRED, SUSPENDED, CANCELLED
    old_plan VARCHAR(20),
    new_plan VARCHAR(20),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    performed_by UUID REFERENCES app_user(id),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Index for subscription queries
CREATE INDEX idx_facility_subscription_status ON facility(subscription_status);
CREATE INDEX idx_facility_subscription_expires ON facility(subscription_expires_at);
CREATE INDEX idx_tenant_feature_flag_facility ON tenant_feature_flag(facility_id);
CREATE INDEX idx_subscription_audit_facility ON subscription_audit(facility_id, created_at DESC);

-- Update existing facilities with default subscription data
UPDATE facility 
SET subscription_plan = 'PRO',
    subscription_status = 'ACTIVE',
    subscription_expires_at = now() + INTERVAL '1 year',
    onboarded_at = created_at
WHERE subscription_plan IS NULL;
