-- Tabela do outbox transacional.
--
-- Pertence a common-infrastructure, não a um bounded context: o mecanismo é compartilhado por
-- todos. Por isso tem location e histórico próprios (flyway_schema_history_common), no mesmo
-- padrão de "quem é dono do schema é dono da migration" usado por BC.

CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY,
    aggregate_id   UUID        NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT        NOT NULL,
    occurred_on    TIMESTAMPTZ NOT NULL,
    status         VARCHAR(20) NOT NULL,
    attempt_count  INTEGER     NOT NULL DEFAULT 0,
    next_retry_at  TIMESTAMPTZ,
    processed_at   TIMESTAMPTZ,
    failure_reason TEXT
);

-- Índice que serve o SELECT do despachante (status PENDING, janela de retry vencida, mais
-- antigo primeiro). Sem ele o worker varre a tabela inteira a cada ciclo, e a tabela só cresce.
-- Parcial de propósito: linha processada ou morta nunca é lida por esse caminho.
CREATE INDEX idx_outbox_event_dispatchable
    ON outbox_event (next_retry_at, occurred_on)
    WHERE status = 'PENDING';

-- Serve a limpeza periódica das linhas já processadas.
CREATE INDEX idx_outbox_event_processed_at
    ON outbox_event (processed_at)
    WHERE status = 'PROCESSED';
