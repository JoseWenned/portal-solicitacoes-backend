CREATE TABLE sessoes_autenticacao (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_sessoes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),

    CONSTRAINT uk_sessoes_refresh_token_hash UNIQUE (refresh_token_hash),

    CONSTRAINT ck_sessoes_refresh_token_hash
        CHECK (refresh_token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_sessoes_expiracao
        CHECK (expires_at > created_at),

    CONSTRAINT ck_sessoes_revogacao
        CHECK (revoked_at IS NULL OR revoked_at >= created_at),

    CONSTRAINT ck_sessoes_version
        CHECK (version >= 0)
);

CREATE INDEX idx_sessoes_usuario
    ON sessoes_autenticacao (usuario_id);

CREATE INDEX idx_sessoes_expiracao
    ON sessoes_autenticacao (expires_at);