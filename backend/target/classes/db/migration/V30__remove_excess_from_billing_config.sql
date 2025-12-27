-- V30: Remove excess_rate_per_kg from agreement_billing_config
-- Excess rate is GLOBAL (facility-level), not per-agreement
-- This prevents dual source of truth and ensures correct billing calculations

ALTER TABLE agreement_billing_config DROP COLUMN IF EXISTS excess_rate_per_kg;
