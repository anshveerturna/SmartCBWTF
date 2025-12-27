-- V32: Seed billing configs for existing agreements that don't have one
-- Uses default values: 277g base allowance, ₹15.50 base rate

INSERT INTO agreement_billing_config 
  (agreement_id, base_grams_per_bed_per_day, base_rate_per_bed_per_day, effective_from, created_by)
SELECT 
  a.id, 
  277, 
  15.50, 
  COALESCE(a.start_date, CURRENT_DATE),
  '00000000-0000-0000-0000-000000000000'::uuid
FROM agreement a
WHERE NOT EXISTS (
  SELECT 1 FROM agreement_billing_config abc WHERE abc.agreement_id = a.id
);
