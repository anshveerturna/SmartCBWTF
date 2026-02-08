-- V72: Widen bag_label.qr_code from VARCHAR(255) to TEXT
-- The signed QR JSON payloads (with UUIDs, timestamps, and checksum) exceed 255 chars.
-- Entity already declares columnDefinition = "TEXT" but the DB column was never altered.
ALTER TABLE bag_label ALTER COLUMN qr_code TYPE TEXT;
