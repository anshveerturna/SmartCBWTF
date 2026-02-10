CREATE TABLE contact_message (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    organization VARCHAR(255),
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'UNREAD'
);

CREATE INDEX idx_contact_message_email ON contact_message(email);
CREATE INDEX idx_contact_message_status ON contact_message(status);
