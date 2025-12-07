-- Add deduplication constraint for bag events
CREATE UNIQUE INDEX IF NOT EXISTS idx_bag_event_dedupe
ON bag_event (bag_label_id, event_type, event_ts);
