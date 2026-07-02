ALTER TABLE contact_message
    ADD COLUMN source_ip VARCHAR(64),
    ADD COLUMN user_agent VARCHAR(255);

CREATE INDEX idx_contact_message_source_ip_created_at ON contact_message(source_ip, created_at);
CREATE INDEX idx_contact_message_created_at ON contact_message(created_at);
