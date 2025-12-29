-- =============================================================================
-- V37: Billing & Invoice System (Financial-Grade, Immutable)
-- =============================================================================
-- This migration creates the core billing infrastructure:
-- 1. billing_lock - Prevents concurrent/duplicate bill generation
-- 2. billing_snapshot - Freezes commercial rates at billing time
-- 3. bill - Immutable financial calculation result
-- 4. invoice_sequence - GST-compliant gap-proof numbering
-- 5. invoice - Legal document linked to bill
-- =============================================================================

-- 1. BILLING LOCK
-- Ensures only one billing process runs per facility per month
-- Insert fails if lock exists → prevents race conditions
CREATE TABLE billing_lock (
    billing_month DATE NOT NULL,
    facility_id UUID NOT NULL REFERENCES facility(id),
    locked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    locked_by UUID NOT NULL,
    PRIMARY KEY (billing_month, facility_id)
);

COMMENT ON TABLE billing_lock IS 'Prevents concurrent billing for same facility/month';

-- 2. BILLING SNAPSHOT
-- Freezes all commercial parameters at billing time
-- Guarantees historical billing correctness even if rates change
CREATE TABLE billing_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    billing_month DATE NOT NULL,
    
    -- From Agreement (immutable commercial terms)
    bed_count INTEGER NOT NULL,
    base_grams_per_bed_per_day NUMERIC(10,2) NOT NULL,
    base_rate_per_bed_per_day NUMERIC(10,2) NOT NULL,
    agreement_version INTEGER NOT NULL DEFAULT 1,
    
    -- From Facility (resolved at billing time)
    excess_rate_per_kg NUMERIC(10,2) NOT NULL,
    excess_rate_effective_from DATE NOT NULL,
    
    -- Integrity verification
    snapshot_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE(agreement_id, billing_month)
);

CREATE INDEX idx_snapshot_facility ON billing_snapshot(facility_id, billing_month);

COMMENT ON TABLE billing_snapshot IS 'Frozen commercial parameters for billing period';
COMMENT ON COLUMN billing_snapshot.snapshot_hash IS 'SHA256 of all rate fields for integrity verification';

-- 3. BILL
-- Immutable financial calculation result
-- NEVER updated or deleted
CREATE TABLE bill (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_id UUID NOT NULL REFERENCES billing_snapshot(id),
    agreement_id UUID NOT NULL REFERENCES agreement(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    billing_month DATE NOT NULL,
    
    -- Aggregated pickup data (provenance)
    pickup_weight_kg NUMERIC(12,3) NOT NULL,
    pickup_event_count INTEGER NOT NULL,
    pickup_event_hash VARCHAR(64) NOT NULL,
    
    -- Calculated amounts (all in INR, 2 decimal precision)
    base_allowance_kg NUMERIC(12,3) NOT NULL,
    excess_weight_kg NUMERIC(12,3) NOT NULL,
    base_amount NUMERIC(12,2) NOT NULL,
    excess_amount NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    cgst NUMERIC(12,2) NOT NULL,
    sgst NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    
    -- Status (always FINALIZED for immutability)
    status VARCHAR(20) NOT NULL DEFAULT 'FINALIZED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    UNIQUE(agreement_id, billing_month),
    CONSTRAINT chk_bill_status CHECK (status = 'FINALIZED'),
    CONSTRAINT chk_bill_amounts CHECK (
        base_amount >= 0 AND
        excess_amount >= 0 AND
        subtotal = base_amount + excess_amount AND
        total_amount = subtotal + cgst + sgst
    )
);

CREATE INDEX idx_bill_facility ON bill(facility_id, billing_month);
CREATE INDEX idx_bill_status ON bill(status);

COMMENT ON TABLE bill IS 'Immutable financial calculation - NEVER UPDATE/DELETE';
COMMENT ON COLUMN bill.pickup_event_hash IS 'SHA256 of sorted pickup_event IDs for auditability';
COMMENT ON COLUMN bill.status IS 'Always FINALIZED - immutable record';

-- 4. INVOICE SEQUENCE
-- Generates gap-proof, facility-scoped, financial-year-scoped invoice numbers
CREATE TABLE invoice_sequence (
    facility_id UUID NOT NULL REFERENCES facility(id),
    financial_year VARCHAR(10) NOT NULL, -- e.g., '2025-26'
    last_number INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (facility_id, financial_year)
);

COMMENT ON TABLE invoice_sequence IS 'Gap-proof invoice numbering per facility per FY';

-- 5. INVOICE
-- Legal GST document derived from bill
-- Invoice totals MUST EXACTLY MATCH bill totals
CREATE TABLE invoice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL UNIQUE REFERENCES bill(id),
    facility_id UUID NOT NULL REFERENCES facility(id),
    
    -- GST-compliant numbering: FACILITY_CODE/YYYY-YY/NNNNNN
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL,
    financial_year VARCHAR(10) NOT NULL,
    
    -- Amount (must match bill.total_amount)
    total_amount NUMERIC(12,2) NOT NULL,
    
    -- Integrity hash: SHA256(bill_id + total_amount + invoice_number)
    integrity_hash VARCHAR(64) NOT NULL,
    
    -- PDF storage
    pdf_url TEXT,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_facility ON invoice(facility_id, financial_year);
CREATE INDEX idx_invoice_date ON invoice(invoice_date);

COMMENT ON TABLE invoice IS 'GST-compliant legal document - derived from bill only';
COMMENT ON COLUMN invoice.invoice_number IS 'Format: FACILITY_CODE/YYYY-YY/NNNNNN';
COMMENT ON COLUMN invoice.integrity_hash IS 'SHA256(bill_id || total_amount || invoice_number) for tamper detection';

-- =============================================================================
-- AUDIT EVENT TYPES (for reference)
-- =============================================================================
-- BILLING_LOCK_ACQUIRED
-- BILLING_LOCK_RELEASED
-- BILLING_SNAPSHOT_CREATED
-- BILL_GENERATED
-- INVOICE_GENERATED
-- BILLING_FAILED
-- =============================================================================
