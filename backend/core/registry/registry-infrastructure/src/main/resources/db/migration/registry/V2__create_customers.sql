-- Clientes: clientes, locais (com o endereco em colunas, sem tabela propria) e equipamentos
-- instalados. FK so entre tabelas do proprio BC (inclusive para o catalogo); nenhuma aponta para
-- tabela de outro BC. status e state guardam o nome do enum.
--
-- O documento do cliente eh unico (ja normalizado, so letras e digitos) e o numero de serie eh
-- unico por modelo sem diferenciar caixa; ambos cobrem tambem os desativados. O repository traduz
-- a violacao pelo nome do indice no codigo de erro correspondente.
--
-- updated_at eh nulavel: o primeiro INSERT o deixa nulo (so o UPDATE o preenche).

CREATE TABLE customers (
    id         UUID PRIMARY KEY,
    document   VARCHAR(14)  NOT NULL,
    name       VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150),
    email      VARCHAR(254),
    phone      VARCHAR(16),
    status     VARCHAR(20)  NOT NULL,             -- ACTIVE | DEACTIVATED
    version    BIGINT       NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_customers_document ON customers (document);

CREATE TABLE locations (
    id           UUID PRIMARY KEY,
    customer_id  UUID         NOT NULL REFERENCES customers (id),
    name         VARCHAR(60)  NOT NULL,
    zip_code     CHAR(8)      NOT NULL,
    street       VARCHAR(150) NOT NULL,
    number       VARCHAR(15)  NOT NULL,
    complement   VARCHAR(50),
    neighborhood VARCHAR(100) NOT NULL,
    city         VARCHAR(100) NOT NULL,
    state        CHAR(2)      NOT NULL,           -- sigla da UF
    status       VARCHAR(20)  NOT NULL,           -- ACTIVE | DEACTIVATED
    version      BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ
);

CREATE INDEX ix_locations_customer ON locations (customer_id);

CREATE TABLE installed_equipment (
    id            UUID PRIMARY KEY,
    location_id   UUID        NOT NULL REFERENCES locations (id),
    model_id      UUID        NOT NULL REFERENCES equipment_models (id),
    serial_number VARCHAR(60) NOT NULL,
    installed_on  DATE,
    status        VARCHAR(20) NOT NULL,           -- ACTIVE | DEACTIVATED
    version       BIGINT      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_installed_equipment_model_serial ON installed_equipment (model_id, lower(serial_number));

CREATE INDEX ix_installed_equipment_location ON installed_equipment (location_id);
