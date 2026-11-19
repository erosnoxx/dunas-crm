-- Deduplicacao por chave de idempotencia (IdempotencyGuard). Mesmo padrao publico de
-- Stripe/Adyen/PayPal para API que move dinheiro: chave -> resultado, com estado explicito
-- para distinguir "ja concluido" de "em andamento agora" de "falhou, pode tentar de novo".
--
-- idempotency_key eh a propria PK -- nao precisa de indice a mais para os tres casos do
-- IdempotencyGuard (chave nova, repetida, ou FAILED sendo reaberta): todos leem/escrevem por PK.

CREATE TABLE idempotent_execution (
    id                   VARCHAR(255) PRIMARY KEY,
    request_fingerprint  VARCHAR(64)  NOT NULL,   -- SHA-256 hex do payload, 64 caracteres
    status               VARCHAR(20)  NOT NULL,   -- IN_PROGRESS | COMPLETED | FAILED
    result_payload       TEXT,                    -- JSON, preenchido so em COMPLETED
    failure_reason       TEXT,                    -- preenchido so em FAILED
    created_at           TIMESTAMPTZ  NOT NULL,
    completed_at         TIMESTAMPTZ
);
