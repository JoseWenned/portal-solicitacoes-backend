CREATE TABLE solicitacoes (
    id UUID PRIMARY KEY,
    codigo BIGINT GENERATED ALWAYS AS IDENTITY,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    solicitante_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_solicitacoes_codigo UNIQUE (codigo),

    CONSTRAINT fk_solicitacoes_usuario
        FOREIGN KEY (solicitante_id) REFERENCES usuarios (id),

    CONSTRAINT ck_solicitacoes_titulo
        CHECK (length(btrim(titulo)) > 0),

    CONSTRAINT ck_solicitacoes_descricao
        CHECK (length(btrim(descricao)) > 0),

    CONSTRAINT ck_solicitacoes_categoria
        CHECK (
            categoria IN (
                'TI', 'RH', 'COMPRAS', 'FINANCEIRO', 'INFRAESTRUTURA'
            )
        ),

    CONSTRAINT ck_solicitacoes_status
        CHECK (status IN ('ABERTO', 'EM_ATENDIMENTO', 'CONCLUIDO')),

    CONSTRAINT ck_solicitacoes_version
        CHECK (version >= 0)
);

CREATE INDEX idx_solicitacoes_usuario_data
    ON solicitacoes (solicitante_id, created_at DESC, codigo DESC);

CREATE INDEX idx_solicitacoes_usuario_status
    ON solicitacoes (solicitante_id, status);

CREATE INDEX idx_solicitacoes_usuario_categoria
    ON solicitacoes (solicitante_id, categoria);