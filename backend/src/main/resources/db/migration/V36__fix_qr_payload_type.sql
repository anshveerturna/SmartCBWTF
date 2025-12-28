-- V36: Fix QR Payload Column Type
-- Change qr_payload from JSONB to TEXT to avoid Hibernate type mismatch

ALTER TABLE qr_authorization 
ALTER COLUMN qr_payload TYPE TEXT USING qr_payload::TEXT;
