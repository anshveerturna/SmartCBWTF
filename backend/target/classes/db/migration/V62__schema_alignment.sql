-- V62: Schema Alignment - Add Missing Columns
-- This migration ensures all JPA entity columns exist in the database
-- Uses ALTER TABLE to handle existing tables properly

-- ============================================================================
-- 1. SUBSCRIPTION_AUDIT - Add missing columns from entity
-- ============================================================================
ALTER TABLE subscription_audit ADD COLUMN IF NOT EXISTS old_value VARCHAR(255);
ALTER TABLE subscription_audit ADD COLUMN IF NOT EXISTS new_value VARCHAR(255);

-- Migrate data from legacy columns if they exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'subscription_audit' AND column_name = 'old_plan') THEN
        UPDATE subscription_audit 
        SET old_value = COALESCE(old_value, old_plan::text)
        WHERE old_value IS NULL AND old_plan IS NOT NULL;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'subscription_audit' AND column_name = 'new_plan') THEN
        UPDATE subscription_audit 
        SET new_value = COALESCE(new_value, new_plan::text)
        WHERE new_value IS NULL AND new_plan IS NOT NULL;
    END IF;
END $$;

-- ============================================================================
-- 2. HCF_AUDIT_LOG - Add missing columns to existing table
-- ============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'hcf_audit_log') THEN
        CREATE TABLE hcf_audit_log (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            hcf_id UUID,
            facility_id UUID,
            action VARCHAR(50) NOT NULL,
            field_name VARCHAR(100),
            old_value TEXT,
            new_value TEXT,
            performed_by UUID,
            performed_by_username VARCHAR(100),
            performed_by_role VARCHAR(50),
            notes TEXT,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
    ELSE
        -- Table exists, add missing columns
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS hcf_id UUID;
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS facility_id UUID;
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS action VARCHAR(50);
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS field_name VARCHAR(100);
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS old_value TEXT;
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS new_value TEXT;
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS performed_by UUID;
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS performed_by_username VARCHAR(100);
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS performed_by_role VARCHAR(50);
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS notes TEXT;
        ALTER TABLE hcf_audit_log ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
    END IF;
END $$;

-- Create indices only if columns exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'hcf_audit_log' AND column_name = 'created_at') THEN
        CREATE INDEX IF NOT EXISTS idx_hcf_audit_hcf ON hcf_audit_log(hcf_id, created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_hcf_audit_facility ON hcf_audit_log(facility_id, created_at DESC);
    END IF;
END $$;

-- ============================================================================
-- 3. SETTINGS_AUDIT_LOG - Create or update
-- ============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'settings_audit_log') THEN
        CREATE TABLE settings_audit_log (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            entity_type VARCHAR(50) NOT NULL,
            entity_id UUID NOT NULL,
            setting_key VARCHAR(100) NOT NULL,
            old_value TEXT,
            new_value TEXT,
            changed_by UUID,
            changed_by_username VARCHAR(100),
            notes TEXT,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
        CREATE INDEX idx_settings_audit_entity ON settings_audit_log(entity_type, entity_id, created_at DESC);
    END IF;
END $$;

-- ============================================================================
-- 4. SYSTEM_CONFIG_AUDIT - Create or update
-- ============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'system_config_audit') THEN
        CREATE TABLE system_config_audit (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            config_key VARCHAR(100) NOT NULL,
            old_value TEXT,
            new_value TEXT NOT NULL,
            changed_by UUID,
            changed_by_username VARCHAR(100),
            reason TEXT,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
        CREATE INDEX idx_system_config_audit_key ON system_config_audit(config_key, created_at DESC);
    END IF;
END $$;

-- ============================================================================
-- 5. BILL - Add missing columns
-- ============================================================================
ALTER TABLE bill ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE bill ADD COLUMN IF NOT EXISTS hcf_id UUID;
ALTER TABLE bill ADD COLUMN IF NOT EXISTS invoice_id UUID;
ALTER TABLE bill ADD COLUMN IF NOT EXISTS notes TEXT;

-- ============================================================================
-- 6. INVOICE_PAYMENT - Create if not exists
-- ============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'invoice_payment') THEN
        CREATE TABLE invoice_payment (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            invoice_id UUID NOT NULL,
            facility_id UUID NOT NULL,
            amount NUMERIC(12,2) NOT NULL,
            payment_date DATE NOT NULL,
            payment_method VARCHAR(50),
            reference_number VARCHAR(100),
            notes TEXT,
            created_by UUID,
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
        CREATE INDEX idx_invoice_payment_invoice ON invoice_payment(invoice_id);
        CREATE INDEX idx_invoice_payment_facility ON invoice_payment(facility_id, payment_date DESC);
    END IF;
END $$;

-- ============================================================================
-- 7. HCF - Add missing columns
-- ============================================================================
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS billing_model VARCHAR(50);
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS requires_bill_approval BOOLEAN DEFAULT FALSE;
ALTER TABLE hcf ADD COLUMN IF NOT EXISTS bill_approver_id UUID;

-- ============================================================================
-- 8. AGREEMENT - Add missing columns
-- ============================================================================
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS base_rate_per_bed_per_day NUMERIC(12,2);
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS base_grams_per_bed_per_day NUMERIC(12,2);
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 1;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS effective_from DATE;
ALTER TABLE agreement ADD COLUMN IF NOT EXISTS effective_to DATE;

-- ============================================================================
-- 9. FACILITY - Add missing columns
-- ============================================================================
ALTER TABLE facility ADD COLUMN IF NOT EXISTS excess_rate_per_kg NUMERIC(12,2);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS excess_rate_effective_from DATE;
ALTER TABLE facility ADD COLUMN IF NOT EXISTS gst_number VARCHAR(20);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS cin_number VARCHAR(30);
ALTER TABLE facility ADD COLUMN IF NOT EXISTS registration_number VARCHAR(50);

-- ============================================================================
-- 10. USER - Add missing columns for account locking
-- ============================================================================
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS account_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_failed_login_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';

-- ============================================================================
-- 11. BILLING_SNAPSHOT - Add missing columns
-- ============================================================================
ALTER TABLE billing_snapshot ADD COLUMN IF NOT EXISTS hcf_id UUID;
ALTER TABLE billing_snapshot ADD COLUMN IF NOT EXISTS billing_model VARCHAR(50);

-- ============================================================================
-- DONE
-- ============================================================================
