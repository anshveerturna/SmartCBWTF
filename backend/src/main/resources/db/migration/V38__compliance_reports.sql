-- =====================================================
-- V38: PHASE 8 - COMPLIANCE & REPORTING ENGINE
-- Immutable, snapshot-based compliance reports
-- Regulator-grade, SPCB/CPCB aligned
-- =====================================================

-- Report lock (prevents duplicate generation)
CREATE TABLE report_generation_lock (
  report_type VARCHAR(50) NOT NULL,
  period_key VARCHAR(20) NOT NULL, -- e.g., '2024-12-29' or '2024-12' or '2024-25'
  facility_id UUID NOT NULL REFERENCES facility(id),
  locked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (report_type, period_key, facility_id)
);

-- Daily Compliance Report
CREATE TABLE daily_compliance_report (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id),
  report_date DATE NOT NULL,
  report_version INTEGER NOT NULL DEFAULT 1,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status VARCHAR(20) NOT NULL DEFAULT 'READY', -- READY / FLAGGED
  data_completeness VARCHAR(20) NOT NULL DEFAULT 'COMPLETE', -- COMPLETE / PARTIAL
  
  -- Source window (exact data range for audit trail)
  source_window_from TIMESTAMPTZ NOT NULL,
  source_window_to TIMESTAMPTZ NOT NULL,
  
  -- Aggregated Data (immutable JSON)
  data_json JSONB NOT NULL,
  
  -- Pre-generated PDF (byte-identical re-downloads)
  pdf_bytes BYTEA,
  
  -- Integrity
  checksum VARCHAR(64) NOT NULL,
  created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  
  UNIQUE(facility_id, report_date)
);

-- Monthly Compliance Report
CREATE TABLE monthly_compliance_report (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id),
  report_month DATE NOT NULL, -- First day of month
  report_version INTEGER NOT NULL DEFAULT 1,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  data_completeness VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
  
  source_window_from TIMESTAMPTZ NOT NULL,
  source_window_to TIMESTAMPTZ NOT NULL,
  
  data_json JSONB NOT NULL,
  pdf_bytes BYTEA,
  checksum VARCHAR(64) NOT NULL,
  created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  
  UNIQUE(facility_id, report_month)
);

-- Annual Compliance Report (Form IV - SPCB)
-- Financial Year: April 1 → March 31 (strictly enforced)
CREATE TABLE annual_compliance_report (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id),
  financial_year VARCHAR(10) NOT NULL, -- e.g., '2024-25' (Apr 2024 - Mar 2025)
  report_version INTEGER NOT NULL DEFAULT 1,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  data_completeness VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
  
  -- FY boundary: always Apr 1 00:00 to Mar 31 23:59:59 IST
  source_window_from TIMESTAMPTZ NOT NULL,
  source_window_to TIMESTAMPTZ NOT NULL,
  
  data_json JSONB NOT NULL,
  pdf_bytes BYTEA,
  excel_bytes BYTEA, -- Form IV Excel export
  checksum VARCHAR(64) NOT NULL,
  created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  
  UNIQUE(facility_id, financial_year)
);

-- Barcode Compliance Report
CREATE TABLE barcode_compliance_report (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id),
  report_date DATE NOT NULL,
  report_type VARCHAR(20) NOT NULL, -- DAILY / MONTHLY
  report_version INTEGER NOT NULL DEFAULT 1,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  data_completeness VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
  
  source_window_from TIMESTAMPTZ NOT NULL,
  source_window_to TIMESTAMPTZ NOT NULL,
  
  data_json JSONB NOT NULL,
  pdf_bytes BYTEA,
  checksum VARCHAR(64) NOT NULL,
  created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  
  UNIQUE(facility_id, report_date, report_type)
);

-- Violation Report
CREATE TABLE violation_report (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  facility_id UUID NOT NULL REFERENCES facility(id),
  report_date DATE NOT NULL,
  report_version INTEGER NOT NULL DEFAULT 1,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  violation_count INTEGER NOT NULL DEFAULT 0,
  data_completeness VARCHAR(20) NOT NULL DEFAULT 'COMPLETE',
  
  source_window_from TIMESTAMPTZ NOT NULL,
  source_window_to TIMESTAMPTZ NOT NULL,
  
  data_json JSONB NOT NULL,
  pdf_bytes BYTEA,
  checksum VARCHAR(64) NOT NULL,
  created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  
  UNIQUE(facility_id, report_date)
);

-- Performance indexes
CREATE INDEX idx_daily_report_facility_date ON daily_compliance_report(facility_id, report_date DESC);
CREATE INDEX idx_monthly_report_facility ON monthly_compliance_report(facility_id, report_month DESC);
CREATE INDEX idx_annual_report_facility ON annual_compliance_report(facility_id, financial_year DESC);
CREATE INDEX idx_barcode_report_facility ON barcode_compliance_report(facility_id, report_date DESC);
CREATE INDEX idx_violation_report_facility ON violation_report(facility_id, report_date DESC);
CREATE INDEX idx_report_lock_facility ON report_generation_lock(facility_id);
