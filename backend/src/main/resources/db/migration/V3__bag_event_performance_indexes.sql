-- Performance indexes for alerts/analytics and audit log
CREATE INDEX IF NOT EXISTS idx_bag_event_event_type_event_ts ON bag_event (event_type, event_ts);
CREATE INDEX IF NOT EXISTS idx_bag_event_label ON bag_event (bag_label_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_ts ON audit_log (ts);
