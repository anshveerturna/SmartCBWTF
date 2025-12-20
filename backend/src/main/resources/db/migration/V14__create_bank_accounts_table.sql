-- V14: Create bank_account table for CBWTF Admin bank account management

CREATE TABLE bank_account (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id) ON DELETE CASCADE,
  account_name VARCHAR(100) NOT NULL,
  account_number VARCHAR(30) NOT NULL,
  ifsc_code VARCHAR(20) NOT NULL,
  bank_name VARCHAR(100) NOT NULL,
  branch_name VARCHAR(100),
  is_primary BOOLEAN DEFAULT false,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_bank_account_facility ON bank_account(facility_id);

-- Ensure only one primary account per facility
CREATE UNIQUE INDEX idx_bank_account_primary ON bank_account(facility_id) WHERE is_primary = true;
