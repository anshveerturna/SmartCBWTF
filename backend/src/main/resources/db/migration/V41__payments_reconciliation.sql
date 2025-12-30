-- =====================================================
-- V41: PAYMENTS & RECONCILIATION ENGINE
-- Regulator-grade, immutable payment tracking
-- =====================================================

-- Payment modes: UPI / NET_BANKING / DEBIT_CARD / CREDIT_CARD

-- Payment record (IMMUTABLE - NEVER UPDATE)
CREATE TABLE payment (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id),
  hcf_id UUID NOT NULL REFERENCES hcf(id),
  bank_account_id UUID REFERENCES bank_account(id),
  payment_date DATE NOT NULL,
  amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
  mode VARCHAR(20) NOT NULL, -- UPI / NET_BANKING / DEBIT_CARD / CREDIT_CARD
  reference_number VARCHAR(100),
  payer_name VARCHAR(255),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by UUID,
  checksum VARCHAR(64) NOT NULL
);

-- Partial unique: only enforce uniqueness when reference_number is not null
CREATE UNIQUE INDEX idx_payment_ref_unique 
  ON payment(facility_id, reference_number, mode) 
  WHERE reference_number IS NOT NULL;

-- Payment reversal (IMMUTABLE - links original to reversal)
-- This is HOW BANKS DO CHARGEBACKS - no mutation of original payment
CREATE TABLE payment_reversal (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  original_payment_id UUID NOT NULL REFERENCES payment(id),
  reversal_payment_id UUID NOT NULL REFERENCES payment(id),
  reason TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by UUID,
  
  UNIQUE(original_payment_id)
);

-- Invoice-Payment allocation (many-to-many)
CREATE TABLE invoice_payment (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id UUID NOT NULL REFERENCES invoice(id),
  payment_id UUID NOT NULL REFERENCES payment(id),
  allocated_amount DECIMAL(15,2) NOT NULL CHECK (allocated_amount > 0),
  allocated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  UNIQUE(invoice_id, payment_id)
);

-- HCF Advance Ledger (IMMUTABLE - tracks overpayments)
-- advance_balance(hcf) = SUM(amount) - never stored as column
CREATE TABLE hcf_advance_ledger (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  hcf_id UUID NOT NULL REFERENCES hcf(id),
  source_payment_id UUID NOT NULL REFERENCES payment(id),
  amount DECIMAL(15,2) NOT NULL, -- positive = credit, negative = used
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  checksum VARCHAR(64) NOT NULL
);

-- Receipt sequence (per facility per FY - like invoice numbers)
CREATE TABLE payment_receipt_sequence (
  facility_id UUID NOT NULL REFERENCES facility(id),
  financial_year VARCHAR(9) NOT NULL, -- e.g., '2024-2025'
  last_number INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY(facility_id, financial_year)
);

-- Payment receipt (IMMUTABLE)
CREATE TABLE payment_receipt (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id UUID NOT NULL REFERENCES payment(id),
  receipt_number VARCHAR(50) NOT NULL,
  pdf_bytes BYTEA,
  checksum VARCHAR(64) NOT NULL,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  
  UNIQUE(payment_id)
);

-- Performance indexes
CREATE INDEX idx_payment_facility ON payment(facility_id, payment_date DESC);
CREATE INDEX idx_payment_hcf ON payment(hcf_id, payment_date DESC);
CREATE INDEX idx_invoice_payment_invoice ON invoice_payment(invoice_id);
CREATE INDEX idx_invoice_payment_payment ON invoice_payment(payment_id);
CREATE INDEX idx_advance_ledger_hcf ON hcf_advance_ledger(hcf_id);
CREATE INDEX idx_payment_reversal_original ON payment_reversal(original_payment_id);
