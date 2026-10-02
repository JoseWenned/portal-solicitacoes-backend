CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_usuarios_email UNIQUE (email),

    CONSTRAINT ck_usuarios_name
        CHECK (length(btrim(name)) >= 3),

    CONSTRAINT ck_usuarios_email_normalizado
        CHECK (
            email = lower(btrim(email))
            AND length(email) > 0
        )
);