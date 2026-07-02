-- Native AgentAI/API integration foundations:
-- OAuth clients, authorization codes, refresh tokens, and idempotency replay.

CREATE TABLE IF NOT EXISTS oauth_client (
    client_id VARCHAR(120) PRIMARY KEY,
    client_secret_hash VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    redirect_uris TEXT,
    allowed_scopes TEXT,
    allowed_grant_types TEXT,
    service_account_user_id UUID REFERENCES app_user(id),
    active BOOLEAN NOT NULL DEFAULT true,
    confidential BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS oauth_authorization_code (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code_hash VARCHAR(160) NOT NULL UNIQUE,
    client_id VARCHAR(120) NOT NULL REFERENCES oauth_client(client_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    redirect_uri TEXT NOT NULL,
    code_challenge VARCHAR(255) NOT NULL,
    code_challenge_method VARCHAR(20) NOT NULL DEFAULT 'S256',
    scope TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_oauth_authorization_code_client
    ON oauth_authorization_code(client_id, expires_at);

CREATE TABLE IF NOT EXISTS oauth_refresh_token (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token_hash VARCHAR(160) NOT NULL UNIQUE,
    client_id VARCHAR(120) NOT NULL REFERENCES oauth_client(client_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id),
    scope TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_oauth_refresh_token_client_user
    ON oauth_refresh_token(client_id, user_id, expires_at);

CREATE TABLE IF NOT EXISTS api_idempotency_record (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    principal_key VARCHAR(300) NOT NULL,
    idempotency_scope VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_status INTEGER NOT NULL,
    response_content_type VARCHAR(255),
    response_body TEXT,
    operation_id VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ,
    replayed_at TIMESTAMPTZ,
    CONSTRAINT uq_api_idempotency_principal_scope_key
        UNIQUE (principal_key, idempotency_scope, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_api_idempotency_expiry
    ON api_idempotency_record(expires_at);
