# Domain model: 02-catalog

**Este arquivo é a fonte de verdade do domínio desta feature.** Nenhum `CLAUDE.md`, skill ou código o duplica: todos o referenciam. Casos de uso e códigos de erro: [`usecases.md`](usecases.md). Decisões: [`decisions.md`](decisions.md). Telas: [`design/`](design/README.md).

> Proposta para aprovação. O mockup está aprovado; este modelo e o `usecases.md` ainda não.

## Nota de proveniência

Modelado do zero, a partir das decisões do usuário em [`decisions.md`](decisions.md) e do mockup aprovado. O que **eu** escolhi sem decisão prévia do usuário está marcado como *proposta* e tem ADR.

## BC e subdomínio

BC **`registry`**, subdomínio **`catalog`** (ADR-1). Os três aggregates abaixo moram em `com.crm.registry.domain.catalog`. Referências entre eles são `UUID` (mesmo BC, mas cada um é fronteira de consistência própria: [`architecture.md`](../../rules/architecture.md) §6.1). O catálogo **não conhece** cliente, local nem equipamento instalado: quem aponta para o modelo é a feature 03, de mão única.

## Aggregates

### `Manufacturer` (aggregate root)

O fabricante. **Fronteira de consistência:** o próprio cadastro. A unicidade do nome é do banco (`UNIQUE` sobre o nome em minúsculas), traduzida em `ConflictException` pelo repository.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `name` | `ManufacturerName` | de 2 a 80 caracteres |
| `status` | `CatalogStatus` | `ACTIVE` ou `DEACTIVATED` |

**Invariantes:** só um fabricante `ACTIVE` pode ser renomeado.

**Métodos de domínio:**

| Método | Efeito | Erros de domínio (`code`) |
|---|---|---|
| `register(name)` | cria `ACTIVE` | — |
| `rename(name)` | troca o nome | `manufacturer_deactivated` |
| `deactivate()` | `ACTIVE` → `DEACTIVATED` | `manufacturer_deactivated` (já está) |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` | `manufacturer_not_deactivated` |

### `EquipmentType` (aggregate root)

O tipo de equipamento (Inversor, Ar-condicionado). **Fronteira de consistência:** o tipo e a **definição das características** dele, que só existem dentro dele. É o tipo que sabe quais valores um modelo dele precisa trazer.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `name` | `TypeName` | de 2 a 60 caracteres |
| `status` | `CatalogStatus` | |
| `characteristics` | lista de `CharacteristicDefinition` | **exatamente uma** nesta versão (ADR-3) |

**Entidade interna: `CharacteristicDefinition`** (`DomainEntity`, sem repository; carregada e salva pelo tipo).

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio; é o que o valor do modelo referencia |
| `name` | `CharacteristicName` | de 2 a 40 caracteres, ex. "Potência", "Capacidade" |
| `unit` | `MeasurementUnit` | enum fechado (abaixo) |
| `required` | `boolean` | se o modelo precisa trazer o valor |

**Invariantes:**
- O tipo tem exatamente uma definição de característica (a regra relaxa quando a segunda característica entrar: ADR-3).
- **Trava:** se o tipo **já tem modelos**, a unidade não muda e `required` não passa de `false` para `true` (os modelos existentes ficariam inválidos). O nome da característica e `required` de `true` para `false` mudam sempre (ADR-4).
- Só um tipo `ACTIVE` pode ser alterado.

**Dado externo por parâmetro.** O tipo não sabe se há modelos: o use case consulta o repository e passa `hasModels` ao método, como pede [`architecture.md`](../../rules/architecture.md) §6.4 (dado externo entra por parâmetro, nunca por consulta).

**Métodos de domínio:**

| Método | Efeito | Erros (`code`) |
|---|---|---|
| `register(name, characteristicName, unit, required)` | cria `ACTIVE`, com a definição | — |
| `rename(name)` | troca o nome do tipo | `type_deactivated` |
| `changeCharacteristic(name, unit, required, hasModels)` | altera a definição | `type_deactivated`, `characteristic_locked` |
| `deactivate()` | `ACTIVE` → `DEACTIVATED` | `type_deactivated` (já está) |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` | `type_not_deactivated` |
| `checkValues(values)` | confere os valores de um modelo contra as definições; **não muda estado** | `characteristic_unknown`, `characteristic_required` |

`checkValues` é a regra "o modelo traz o que o tipo pede": mora no tipo porque é o dono das definições, e o modelo a chama passando o tipo como parâmetro. Valor para definição que não existe: `characteristic_unknown`. Definição `required` sem valor: `characteristic_required`.

### `EquipmentModel` (aggregate root)

O modelo de um fabricante, de um tipo. **Fronteira de consistência:** o modelo e seus valores de característica (objetos de valor, sem identidade própria). A unicidade (fabricante, nome) é do banco.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `manufacturerId` | `UUID` | do `Manufacturer`; **não muda** depois de salvo (ADR-4) |
| `typeId` | `UUID` | do `EquipmentType`; **não muda** depois de salvo (ADR-4) |
| `name` | `ModelName` | de 1 a 80 caracteres |
| `values` | lista de `CharacteristicValue` | confere com as definições do tipo |
| `status` | `CatalogStatus` | |

**Invariantes:**
- `manufacturerId` e `typeId` são imutáveis.
- Os `values` sempre conferem com o tipo no momento em que são gravados (`EquipmentType.checkValues`).
- Só se cria um modelo com fabricante e tipo `ACTIVE`; só se edita um modelo `ACTIVE`.

**Métodos de domínio** (o fabricante e o tipo entram por parâmetro, nunca por consulta):

| Método | Efeito | Erros (`code`) |
|---|---|---|
| `register(manufacturer, type, name, values)` | cria `ACTIVE` | `manufacturer_deactivated`, `type_deactivated`, `characteristic_unknown`, `characteristic_required` |
| `edit(type, name, values)` | troca nome e valores | `model_deactivated`, `characteristic_unknown`, `characteristic_required` |
| `deactivate()` | `ACTIVE` → `DEACTIVATED` | `model_deactivated` (já está) |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` | `model_not_deactivated` |

`edit` recebe o tipo do modelo (o use case carrega pelo `typeId`) e ele pode estar desativado: o tipo desativado só bloqueia modelo **novo**.

## Value Objects

| VO | Base | Regra de validação | Fica em |
|---|---|---|---|
| `ManufacturerName` | `StringValueObject` | de 2 a 80 caracteres | `registry-domain` |
| `TypeName` | `StringValueObject` | de 2 a 60 caracteres | `registry-domain` |
| `ModelName` | `StringValueObject` | de 1 a 80 caracteres | `registry-domain` |
| `CharacteristicName` | `StringValueObject` | de 2 a 40 caracteres | `registry-domain` |
| `CharacteristicAmount` | `BigDecimalValueObject` | maior que zero, até 3 casas decimais, até 9 dígitos inteiros (*proposta*, ADR-9) | `registry-domain` |
| `CharacteristicValue` | record de VOs | `definitionId` (UUID) + `CharacteristicAmount`; um valor por definição | `registry-domain` |
| `MeasurementUnit` | enum | `KILOWATT` (kW), `WATT` (W), `WATT_PEAK` (Wp), `BTU_PER_HOUR` (BTU/h) | `registry-domain` |
| `CatalogStatus` | enum | `ACTIVE`, `DEACTIVATED` | `registry-domain` |

As mensagens de erro de cada VO vêm na implementação, em pt-BR minúsculo ([`code-style.md`](../../rules/code-style.md)). O símbolo da unidade (kW, BTU/h) é texto de tela, nunca do domínio. **Enum só onde o código age diferente por valor** (unidade de medida e situação); fabricante, tipo e modelo são cadastro, nunca enum (ADR-2). Nada aqui é "de fabricante": a neutralidade da [§4.3](../../rules/architecture.md) se cumpre porque nome de marca e modelo são dado.

Nenhum VO sobe para `shared-domain`: só o catálogo os usa. `Email`, `Document` e `Address` de `shared-domain` não são usados nesta feature (são do `customers`).

## Specifications, Policies e Domain Services

Nenhum. A regra que liga os aggregates ("o modelo traz o que o tipo pede") mora no `EquipmentType`, dono das definições, e o modelo a chama com o tipo por parâmetro; as demais regras pertencem a um aggregate só. Não há decisão que varie por contexto.

## Ciclo de vida

`Manufacturer`, `EquipmentType` e `EquipmentModel` têm o mesmo ciclo, de dois estados, como **enum com guardas no aggregate**, sem a base `StateMachine` (mesmo critério do [`IAM ADR-9`](../01-iam/decisions.md)):

```
ACTIVE ──deactivate──▶ DEACTIVATED ──reactivate──▶ ACTIVE
```

| De | Para | Quem | Trava |
|---|---|---|---|
| `ACTIVE` | `DEACTIVATED` | Administrador ou Comercial | `<x>_deactivated` se já está |
| `DEACTIVATED` | `ACTIVE` | Administrador ou Comercial | `<x>_not_deactivated` se já está ativo |

Desativar **não propaga**: desativar fabricante ou tipo não desativa os modelos dele (ADR-5). Nada se apaga.

## Integration events

**Nenhum publicado, nenhum consumido.** O mesmo BC (`customers`) enxerga o catálogo diretamente, de mão única. Quando um **outro** BC (monitoramento, manutenção, proposta) precisar saber de um modelo, o integration event nasce com a necessidade, e isto vira ADR.

## Persistência

Migrations em `registry-infrastructure/src/main/resources/db/migration/registry/`, numeradas de `V1__`. FK entre as tabelas do catálogo é permitida (mesmo BC); nenhuma para tabela de outro BC. Colunas padrão de toda tabela: `id uuid PK`, `version`, `created_at`, `updated_at` ([`architecture.md`](../../rules/architecture.md) §8).

| Tabela | Colunas | Restrições e índices |
|---|---|---|
| `manufacturers` | `name`, `status` | `UNIQUE (lower(name))` |
| `equipment_types` | `name`, `status` | `UNIQUE (lower(name))` |
| `characteristic_definitions` | `type_id`, `name`, `unit`, `required` | `FK type_id → equipment_types`; índice em `(type_id)` |
| `equipment_models` | `manufacturer_id`, `type_id`, `name`, `status` | `FK` para `manufacturers` e `equipment_types`; `UNIQUE (manufacturer_id, lower(name))`; índice em `(type_id)` |
| `model_characteristic_values` | `model_id`, `definition_id`, `amount numeric(12,3)` | `PK (model_id, definition_id)`; `FK` para `equipment_models` e `characteristic_definitions` |

- O estado (`status`) e a unidade (`unit`) são colunas de texto com o nome do enum.
- A busca textual da lista de modelos casa o nome do modelo e o do fabricante, sem diferenciar maiúsculas (acento fica como está: ver [`open-questions.md`](open-questions.md)).
- **Seeder** (`CatalogSeeder`, em `registry-infrastructure`): lê `seed/catalog.json` do classpath e grava pelo domínio. Só roda se o catálogo está **vazio** (`isAlreadySeeded` = existe algum fabricante ou tipo), então reiniciar não recria nem sobrescreve o que a equipe alterou (ADR-7). O conteúdo vem da lista do dono; nenhuma característica é inventada.
