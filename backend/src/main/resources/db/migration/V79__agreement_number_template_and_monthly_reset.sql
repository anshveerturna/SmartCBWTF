-- V79: Add template-based agreement number configuration and monthly reset support

ALTER TABLE facility_settings
    ADD COLUMN IF NOT EXISTS agreement_number_template TEXT,
    ADD COLUMN IF NOT EXISTS agreement_number_reset_frequency VARCHAR(20) NOT NULL DEFAULT 'YEARLY';

COMMENT ON COLUMN facility_settings.agreement_number_template IS
    'Optional template for agreement numbers. Supported tokens: {{sequence}}, {{month}}, {{year}}, {{prefix}}, {{facilityCode}}.';
COMMENT ON COLUMN facility_settings.agreement_number_reset_frequency IS
    'Sequence reset cadence: NEVER, YEARLY, or MONTHLY.';

ALTER TABLE agreement_number_sequence
    ADD COLUMN IF NOT EXISTS period_month INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN agreement_number_sequence.period_month IS
    'Reset bucket month. 0 means non-monthly sequence; 1-12 represent January-December.';

ALTER TABLE agreement_number_sequence
    DROP CONSTRAINT IF EXISTS agreement_number_sequence_facility_id_year_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_agreement_number_sequence_scope
    ON agreement_number_sequence (facility_id, year, period_month);
