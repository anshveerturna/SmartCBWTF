-- V67: Add agreement number format configuration to facility_settings
-- Moves format config from application.yml (global) to per-facility settings

ALTER TABLE facility_settings
    ADD COLUMN agreement_number_prefix VARCHAR(20) NOT NULL DEFAULT 'HCF',
    ADD COLUMN agreement_number_separator VARCHAR(5) NOT NULL DEFAULT '-',
    ADD COLUMN agreement_number_sequence_digits INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN agreement_number_include_facility_code BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN agreement_number_include_year BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN facility_settings.agreement_number_prefix IS 'Middle prefix in agreement number format (e.g., HCF)';
COMMENT ON COLUMN facility_settings.agreement_number_separator IS 'Separator character between parts (e.g., -)';
COMMENT ON COLUMN facility_settings.agreement_number_sequence_digits IS 'Number of zero-padded digits for sequence (e.g., 5 → 00001)';
COMMENT ON COLUMN facility_settings.agreement_number_include_facility_code IS 'Whether to include facility code as first segment';
COMMENT ON COLUMN facility_settings.agreement_number_include_year IS 'Whether to include current year in the format';
