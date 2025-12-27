-- V31: Add version column to agreement for renewal tracking
-- On renewal: new agreement gets version = previous + 1

ALTER TABLE agreement ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN agreement.version IS 'Agreement version number, increments on renewal';
