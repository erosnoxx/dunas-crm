-- Tabelas do BC de identidade e acesso. Referencias entre as tres sao por uuid, com FK (mesmo BC);
-- nenhuma coluna aponta para tabela de outro BC. Hash de senha e de token sao TEXT: o tamanho
-- do banco nao acompanha o limite de 30 caracteres da senha.
--
-- updated_at eh nulavel: o primeiro INSERT o deixa nulo (so o UPDATE o preenche).

CREATE TABLE users (
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(80)  NOT NULL,
    email                VARCHAR(254) NOT NULL,   -- ja normalizado em minusculas pelo dominio
    role                 VARCHAR(20)  NOT NULL,   -- ADMINISTRATOR | COMMERCIAL | TECHNICIAN
    status               VARCHAR(20)  NOT NULL,   -- INVITED | ACTIVE | DEACTIVATED
    password_hash        TEXT,                    -- nulo enquanto a conta esta convidada
    must_change_password BOOLEAN      NOT NULL,
    version              BIGINT       NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE one_time_tokens (
    id             UUID PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users (id),
    purpose        VARCHAR(20)  NOT NULL,         -- INVITATION | PASSWORD_RESET
    token_hash     TEXT         NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    used_at        TIMESTAMPTZ,
    invalidated_at TIMESTAMPTZ,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ,
    CONSTRAINT uk_one_time_tokens_token_hash UNIQUE (token_hash)
);

-- invalidar os links pendentes de um usuario para uma finalidade
CREATE INDEX ix_one_time_tokens_user_purpose ON one_time_tokens (user_id, purpose);

CREATE TABLE sessions (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id),
    token_hash   TEXT         NOT NULL,
    device       VARCHAR(80)  NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL,
    last_used_at TIMESTAMPTZ  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    revoked_by   UUID,                            -- quem revogou (usuario do mesmo BC); sem FK: trilha
    version      BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ,
    CONSTRAINT uk_sessions_token_hash UNIQUE (token_hash)
);

-- listar e revogar as sessoes vivas de um usuario
CREATE INDEX ix_sessions_user_active ON sessions (user_id) WHERE revoked_at IS NULL;
