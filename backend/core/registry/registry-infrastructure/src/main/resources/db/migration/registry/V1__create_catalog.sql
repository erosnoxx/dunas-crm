-- Catalogo de equipamentos: fabricantes, tipos (com a definicao da caracteristica) e modelos (com
-- os valores). FK so entre tabelas do proprio catalogo; nenhuma aponta para tabela de outro BC.
-- status e unit guardam o nome do enum.
--
-- O nome eh unico sem diferenciar caixa: indice unico sobre lower(name). O repository traduz a
-- violacao pelo nome do indice no codigo de erro do nome repetido.
--
-- updated_at eh nulavel: o primeiro INSERT o deixa nulo (so o UPDATE o preenche).

CREATE TABLE manufacturers (
    id         UUID PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    status     VARCHAR(20) NOT NULL,              -- ACTIVE | DEACTIVATED
    version    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_manufacturers_name ON manufacturers (lower(name));

CREATE TABLE equipment_types (
    id         UUID PRIMARY KEY,
    name       VARCHAR(60) NOT NULL,
    status     VARCHAR(20) NOT NULL,              -- ACTIVE | DEACTIVATED
    version    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_equipment_types_name ON equipment_types (lower(name));

CREATE TABLE characteristic_definitions (
    id         UUID PRIMARY KEY,
    type_id    UUID        NOT NULL REFERENCES equipment_types (id),
    name       VARCHAR(40) NOT NULL,
    unit       VARCHAR(20) NOT NULL,              -- KILOWATT | WATT | WATT_PEAK | BTU_PER_HOUR
    required   BOOLEAN     NOT NULL,
    version    BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE INDEX ix_characteristic_definitions_type ON characteristic_definitions (type_id);

CREATE TABLE equipment_models (
    id              UUID PRIMARY KEY,
    manufacturer_id UUID        NOT NULL REFERENCES manufacturers (id),
    type_id         UUID        NOT NULL REFERENCES equipment_types (id),
    name            VARCHAR(80) NOT NULL,
    status          VARCHAR(20) NOT NULL,         -- ACTIVE | DEACTIVATED
    version         BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_equipment_models_manufacturer_name ON equipment_models (manufacturer_id, lower(name));

-- listar os modelos de um tipo e responder se o tipo tem modelos (trava da caracteristica)
CREATE INDEX ix_equipment_models_type ON equipment_models (type_id);

-- valor eh value object do modelo: a chave eh (modelo, definicao), sem id, versao nem datas proprias;
-- o modelo o grava inteiro a cada save e a versao do modelo cobre a concorrencia
CREATE TABLE model_characteristic_values (
    model_id      UUID           NOT NULL REFERENCES equipment_models (id),
    definition_id UUID           NOT NULL REFERENCES characteristic_definitions (id),
    amount        NUMERIC(12, 3) NOT NULL,
    PRIMARY KEY (model_id, definition_id)
);
