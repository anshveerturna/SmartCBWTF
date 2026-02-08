-- Speed up bag-event to QR lifecycle joins by payload+facility lookup.
CREATE INDEX IF NOT EXISTS idx_qr_payload_facility
ON qr_authorization (facility_id, qr_payload);
