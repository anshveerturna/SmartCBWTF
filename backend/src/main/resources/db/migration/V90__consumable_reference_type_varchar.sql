-- V90: Convert consumable quantity reference type from PostgreSQL enum to VARCHAR.
-- Hibernate maps project enums as strings, and production runs schema validation.

ALTER TABLE consumable_quantity_reference
    ALTER COLUMN reference_type TYPE VARCHAR(30) USING reference_type::text;

DROP TYPE IF EXISTS reference_type;
