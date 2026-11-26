# Use cases: 02-catalog

Forma dos use cases (sem interface, sem sufixo, `@UseCase`, `commands` e `queries`): [`architecture.md`](../../rules/architecture.md) §7. Domínio: [`domain-model.md`](domain-model.md). Telas: [`design/`](design/README.md).

> Proposta para aprovação. Os pontos marcados *proposta* não foram decididos pelo usuário.

## Convenções desta feature

**Quem age.** Todo use case autenticado recebe o `Actor` (`record Actor(UUID userId, Role role, UUID sessionId)`, de `shared-application`: [`architecture.md`](../../rules/architecture.md) §3.1), já montado pela `web`. O catálogo checa o papel **no use case**, nunca no controller ([`IAM ADR-5`](../01-iam/decisions.md)), com um método novo do `Actor`: `requireAnyRole(Role...)`, que lança `ForbiddenException` com o código `forbidden`. O `requireAdministrator()` do IAM continua, como atalho de `requireAnyRole(Role.ADMINISTRATOR)`. A regra "quem escreve no catálogo" mora numa classe do próprio subdomínio, `CatalogAccess`, para os 12 commands não a repetirem.

**Papéis** (ADR-6):

| | Administrador | Comercial | Técnico |
|---|---|---|---|
| Consultar fabricante, tipo e modelo | ✔ | ✔ | ✔ |
| Cadastrar, editar, desativar e reativar | ✔ | ✔ | recusado (`forbidden`, 403) |

**Pacotes.** `com.crm.registry.application.catalog.usecases.commands` e `.queries`; contratos em `com.crm.registry.application.catalog.contracts.{repositories,readers}` (subdomínio como primeiro pacote da camada, ADR-1).

**Listagens.** `Pageable` com ordem padrão por nome. `status` padrão `ACTIVE`; `ALL` mostra os dois. Busca de texto: contém, sem diferenciar maiúsculas.

**Nenhum use case lê o relógio:** a feature não tem data de negócio, então não recebe `Clock`.

## Commands

### `RegisterManufacturer`

- **Pacote:** `...catalog.usecases.commands`. **Anotação:** `@UseCase`.
- **Entrada:** ator, `name`.
- **Pré-condições:** papel Administrador ou Comercial (`ForbiddenException`, `forbidden`). Nome ainda não usado, sem diferenciar maiúsculas (`ConflictException`, `manufacturer_name_taken`: o use case confere antes, para a resposta amigável, e o `UNIQUE` do banco é a rede de segurança contra a corrida, que sai como 409 genérico).
- **Fluxo:** `Manufacturer.register(name)`, salva.
- **Retorno:** `UUIDResponse`.
- **Endpoint:** `POST /catalog/manufacturers`.

### `RenameManufacturer`

- **Entrada:** ator, `manufacturerId`, `name`. **Pré-condições:** papel; fabricante existe (`NotFoundException`, `manufacturer_not_found`) e está ativo (`manufacturer_deactivated`); nome livre (`manufacturer_name_taken`).
- **Fluxo:** carrega, `rename`, salva.
- **Endpoint:** `PUT /catalog/manufacturers/{manufacturerId}`.

### `RegisterEquipmentType`

- **Entrada:** ator, `name`, `characteristic` (`name`, `unit`, `required`).
- **Pré-condições:** papel; nome livre (`type_name_taken`); `unit` é um dos valores de `MeasurementUnit` (400 se não for).
- **Fluxo:** `EquipmentType.register`, salva. **Retorno:** `UUIDResponse`.
- **Endpoint:** `POST /catalog/equipment-types`.

### `UpdateEquipmentType`

O painel "Editar tipo" salva nome e característica juntos.

- **Entrada:** ator, `typeId`, `name`, `characteristic` (`name`, `unit`, `required`).
- **Pré-condições:** papel; tipo existe (`type_not_found`) e está ativo (`type_deactivated`); nome livre (`type_name_taken`).
- **Fluxo:** 1. Carrega o tipo. 2. Pergunta ao `EquipmentModelReader` se o tipo tem modelos (`hasModels`). 3. `rename`, depois `changeCharacteristic(..., hasModels)`. 4. Salva (lock otimista: dois ao mesmo tempo, o segundo recebe `ConcurrentUpdateException`, 409).
- **Pós-condições:** com modelos, trocar a unidade ou ligar `required` falha com `characteristic_locked` (422) e nada é gravado. *Risco aceito* (ADR-4): um modelo criado no mesmo instante em que a unidade muda escapa da trava, porque a criação do modelo só lê o tipo.
- **Endpoint:** `PUT /catalog/equipment-types/{typeId}`.

### `RegisterEquipmentModel`

- **Entrada:** ator, `manufacturerId`, `typeId`, `name`, `values` (lista de `definitionId` + `amount`).
- **Pré-condições:** papel; fabricante e tipo existem (`manufacturer_not_found`, `type_not_found`) e estão ativos (`manufacturer_deactivated`, `type_deactivated`); nome livre **para esse fabricante** (`model_name_taken`); valores conferem com o tipo (`characteristic_unknown`, `characteristic_required`); `amount` válido (`invalid_amount`, 400).
- **Ordem das checagens:** o formato (nome e `amount`, 400) e as regras do aggregate (ativos e valores) vêm antes de `model_name_taken`, porque `EquipmentModel.register` as reúne numa só chamada.
- **Fluxo:** 1. Carrega o fabricante e o tipo. 2. `EquipmentModel.register(manufacturer, type, name, values)`. 3. Salva.
- **Retorno:** `UUIDResponse`.
- **Endpoint:** `POST /catalog/equipment-models`.

### `UpdateEquipmentModel`

- **Entrada:** ator, `modelId`, `name`, `values`. Fabricante e tipo **não** vêm: não mudam (ADR-4).
- **Pré-condições:** papel; modelo existe (`model_not_found`) e está ativo (`model_deactivated`); nome livre para o fabricante dele (`model_name_taken`); valores conferem com o tipo.
- **Ordem das checagens:** a mesma do cadastro: formato e regras do aggregate antes de `model_name_taken`.
- **Fluxo:** carrega o modelo e o tipo dele, `edit(type, name, values)`, salva.
- **Endpoint:** `PUT /catalog/equipment-models/{modelId}`.

### Desativar e reativar

Seis use cases com a mesma forma: ator, id; carrega; chama o método de domínio; salva. Sem evento, sem confirmação (ADR-5).

| Use case | Endpoint | Erros específicos |
|---|---|---|
| `DeactivateManufacturer` | `POST /catalog/manufacturers/{id}/deactivation` | `manufacturer_not_found`, `manufacturer_deactivated` |
| `ReactivateManufacturer` | `POST /catalog/manufacturers/{id}/reactivation` | `manufacturer_not_found`, `manufacturer_not_deactivated` |
| `DeactivateEquipmentType` | `POST /catalog/equipment-types/{id}/deactivation` | `type_not_found`, `type_deactivated` |
| `ReactivateEquipmentType` | `POST /catalog/equipment-types/{id}/reactivation` | `type_not_found`, `type_not_deactivated` |
| `DeactivateEquipmentModel` | `POST /catalog/equipment-models/{id}/deactivation` | `model_not_found`, `model_deactivated` |
| `ReactivateEquipmentModel` | `POST /catalog/equipment-models/{id}/reactivation` | `model_not_found`, `model_not_deactivated` |

Todos exigem papel Administrador ou Comercial.

## Queries

Todas `@UseCase(transactional = false)`, abertas aos três papéis. Não carregam aggregate: leem por contrato de leitura em `registry-application` (`application.catalog.contracts.readers`: `ManufacturerReader`, `EquipmentTypeReader`, `EquipmentModelReader`) e devolvem projeção ([`architecture.md`](../../rules/architecture.md) §7.1). Sem cache: lista pequena, e o banco é a fonte de verdade.

### `ListManufacturers`

- **Entrada:** `text`, `status`, `Pageable`. **Retorno:** `Page<ManufacturerSummary>`: `id`, `name`, `status`, `modelCount`.
- **Endpoint:** `GET /catalog/manufacturers`. A lista de escolha do formulário de modelo é a mesma chamada com `status=ACTIVE`.

### `ListEquipmentTypes`

- **Entrada:** `status`, `Pageable`. **Retorno:** `Page<EquipmentTypeSummary>`: `id`, `name`, `status`, `modelCount` e as `characteristics` (`id`, `name`, `unit`, `required`). O formulário de modelo monta o campo da característica a partir desta resposta.
- **Endpoint:** `GET /catalog/equipment-types`. Serve também ao painel "Editar tipo".

### `ListEquipmentModels`

- **Entrada:** `text` (modelo ou fabricante), `typeId`, `status`, `Pageable`; o filtro é o `EquipmentModelFilter`.
- **Retorno:** `Page<EquipmentModelSummary>`: `id`, `name`, `status`, fabricante (`id`, `name`), tipo (`id`, `name`) e `values` (`definitionId`, nome e unidade da característica, `amount`).
- **Endpoint:** `GET /catalog/equipment-models`.

### `GetEquipmentModel`

- **Entrada:** `modelId`. **Retorno:** o resumo acima. Erro: `model_not_found`.
- **Endpoint:** `GET /catalog/equipment-models/{modelId}`.

## Erros

Contrato público ([`architecture.md`](../../rules/architecture.md) §7.3). Códigos únicos no BC.

| `code` | Status | Mensagem (pt-BR) | Quando |
|---|---|---|---|
| `forbidden` | 403 | você não tem permissão para isto | papel sem direito de escrever |
| `manufacturer_not_found` | 404 | fabricante não encontrado | id inexistente |
| `type_not_found` | 404 | tipo não encontrado | id inexistente |
| `model_not_found` | 404 | modelo não encontrado | id inexistente |
| `manufacturer_name_taken` | 409 | já existe um fabricante com este nome | nome repetido |
| `type_name_taken` | 409 | já existe um tipo com este nome | nome repetido |
| `model_name_taken` | 409 | este fabricante já tem um modelo com esse nome | nome repetido no fabricante |
| `manufacturer_deactivated` | 422 | este fabricante está desativado | editar ou criar modelo com fabricante desativado; desativar de novo |
| `manufacturer_not_deactivated` | 422 | este fabricante não está desativado | reativar quem está ativo |
| `type_deactivated` | 422 | este tipo está desativado | idem, para tipo |
| `type_not_deactivated` | 422 | este tipo não está desativado | idem |
| `model_deactivated` | 422 | este modelo está desativado | editar ou desativar de novo |
| `model_not_deactivated` | 422 | este modelo não está desativado | idem |
| `characteristic_locked` | 422 | o tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar | trocar unidade ou ligar `required` com modelos |
| `characteristic_unknown` | 422 | a característica informada não existe neste tipo | valor para definição que o tipo não tem |
| `characteristic_required` | 422 | informe a característica `<nome>` | definição obrigatória sem valor |
| `invalid_amount` | 400 | o valor precisa ser maior que zero, com até 3 casas decimais | `CharacteristicAmount` inválido |
| `concurrent_update` | 409 | o registro foi alterado ao mesmo tempo por outra operação; tente de novo | lock otimista (código e mensagem do kernel) |

Nome fora do tamanho (fabricante, tipo, modelo, característica) sai como 400 pela validação do VO, com a mensagem do próprio VO.

## Consumo de integration event

Nenhum (ver [`domain-model.md`](domain-model.md)).

## Fora de escopo

- **Apagar** fabricante, tipo ou modelo: só desativa (ADR-5).
- **Mudar o fabricante ou o tipo de um modelo:** cadastra-se um novo e desativa-se o antigo (ADR-4).
- **Mais de uma característica por tipo, ficha técnica, foto, histórico de alterações, importação por planilha** (README da feature).
- **Quantas unidades usam o modelo:** assunto da feature 03, porque o catálogo nunca enxerga cliente.
- **Eventos de catálogo para outros BCs:** nascem com o primeiro consumidor.
