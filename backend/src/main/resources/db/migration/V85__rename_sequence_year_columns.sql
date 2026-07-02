-- Avoid reserved/generic SQL identifiers in sequence tables.

ALTER TABLE agreement_code_sequence
    RENAME COLUMN year TO sequence_year;

ALTER TABLE agreement_number_sequence
    RENAME COLUMN year TO sequence_year;

DROP INDEX IF EXISTS idx_agreement_number_sequence_scope;

ALTER TABLE agreement_number_sequence
    DROP CONSTRAINT IF EXISTS agreement_number_sequence_facility_id_year_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_agreement_number_sequence_scope
    ON agreement_number_sequence (facility_id, sequence_year, period_month);
