# Use cases: 03-customers

Forma dos use cases (sem interface, sem sufixo, `@UseCase`, `commands` e `queries`): [`architecture.md`](../../rules/architecture.md) §7. Domínio: [`domain-model.md`](domain-model.md). Telas: [`design/`](design/README.md).

> Proposta para aprovação. Os pontos marcados *proposta* não foram decididos pelo usuário, e as rotas e as respostas fecham com o mockup.

## Convenções desta feature

**Quem age.** Todo use case recebe o `Actor` de `shared-application` ([`architecture.md`](../../rules/architecture.md) §3.1), já montado pela `web`, e checa o papel **no use case** com `actor.requireAnyRole(...)`, que lança `ForbiddenException` com o código `forbidden`. A regra "quem escreve no cadastro de clientes" mora numa classe do subdomínio, `CustomerAccess` (`requireEditor`, `requireReader`), no molde do `CatalogAccess`, para os 12 commands não a repetirem. É classe própria, e não o `CatalogAccess`, porque quem edita clientes pode deixar de ser quem edita o catálogo.

**Papéis** (ADR-7):

| | Administrador | Comercial | Técnico |
|---|---|---|---|
| Consultar cliente, local e equipamento | ✔ | ✔ | ✔ |
| Cadastrar, editar, desativar e reativar | ✔ | ✔ | recusado (`forbidden`, 403) |

**Ordem das pré-condições**, em todos os commands: **papel** (403) → **formato** dos campos, ao montar os VOs (400) → **existe** (404) → **ativo** (422) → **unicidade** (409). A ordem é contrato: um Técnico com dado inválido recebe 403, e um documento repetido de cliente com formato inválido recebe 400. Onde a regra de "ativo" está num método de fábrica do aggregate (`register`, `install`), ela roda nessa chamada, que vem antes da checagem de unicidade.

**Pacotes.** `com.crm.registry.application.customers.usecases.commands` e `.queries`; contratos em `com.crm.registry.application.customers.contracts.{repositories,readers}`. Do catálogo, os commands de equipamento usam o `EquipmentModelRepository` (ADR-8).

**Listagens.** `page` e `size` como no catálogo (padrão 20, máximo 100, cortados pela `web`). Ordem padrão por nome. `status` padrão `ACTIVE`; `ALL` mostra os dois.

**Relógio.** Só os commands de equipamento recebem o `Clock` (por construtor), para a regra "data de instalação não é depois de hoje" (`InstallationDate.of(valor, clock)`). Os demais não leem hora.

## Commands: cliente

### `RegisterCustomer`

- **Entrada:** ator, `document`, `name`, `email` (opcional), `phone` (opcional, E.164).
- **Pré-condições:** papel Administrador ou Comercial (`forbidden`). Formato: `Document`, `CustomerName`, `Email`, `Phone` (400). Documento ainda não usado, entre ativos e desativados (`ConflictException`, `customer_document_taken`).
- **Fluxo:** monta os VOs, confere o documento no repository, `Customer.register`, salva.
- **Retorno:** `UUIDResponse`. **Endpoint:** `POST /customers` (201).

### `UpdateCustomer`

- **Entrada:** ator, `customerId`, `name`, `email`, `phone`. O documento **não** vem: não muda (ADR-3). E-mail ou telefone ausente apaga o valor.
- **Pré-condições:** papel; formato (400); cliente existe (`NotFoundException`, `customer_not_found`) e está ativo (`customer_deactivated`).
- **Fluxo:** carrega, `edit`, salva (lock otimista: `concurrent_update`, 409).
- **Endpoint:** `PUT /customers/{customerId}`.

## Commands: local

### `RegisterLocation`

- **Entrada:** ator, `customerId`, `name`, `address` (`zipCode`, `street`, `number`, `complement` opcional, `neighborhood`, `city`, `state`).
- **Pré-condições:** papel; formato de `LocationName` e de cada campo do `Address` (400; `state` fora das 27 siglas também é 400); cliente existe (`customer_not_found`) e está ativo (`customer_deactivated`).
- **Fluxo:** carrega o cliente, `Location.register(customer, name, address)`, salva.
- **Retorno:** `UUIDResponse`. **Endpoint:** `POST /customers/{customerId}/locations` (201).

### `UpdateLocation`

- **Entrada:** ator, `locationId`, `name`, `address`. O cliente não vem: não muda.
- **Pré-condições:** papel; formato (400); local existe (`location_not_found`) e está ativo (`location_deactivated`). O cliente **não** é conferido: desativado, ele só bloqueia local novo (ADR-6).
- **Fluxo:** carrega, `edit`, salva.
- **Endpoint:** `PUT /locations/{locationId}`.

## Commands: equipamento instalado

### `InstallEquipment`

- **Entrada:** ator, `locationId`, `modelId`, `serialNumber`, `installedOn` (opcional, `yyyy-MM-dd`).
- **Pré-condições:** papel; formato de `SerialNumber` (400) e de `InstallationDate` (`invalid_installation_date`, 400); local existe (`location_not_found`), modelo existe (`model_not_found`); local ativo (`location_deactivated`), cliente do local ativo (`customer_deactivated`), modelo ativo (`model_deactivated`); série livre **para esse modelo**, entre ativos e desativados (`serial_number_taken`, 409).
- **Fluxo:** 1. Monta os VOs. 2. Carrega o local, o cliente dele e o modelo (pelo `EquipmentModelRepository` do catálogo). 3. `InstalledEquipment.install(location, customer, model, serialNumber, installedOn)`. 4. Confere a série no repository. 5. Salva.
- **Retorno:** `UUIDResponse`. **Endpoint:** `POST /locations/{locationId}/equipment` (201).
- A lista de modelos do formulário é a do catálogo, `GET /catalog/equipment-models?status=ACTIVE`: nenhuma query nova.

### `UpdateInstalledEquipment`

- **Entrada:** ator, `equipmentId`, `serialNumber`, `installedOn`. Local e modelo **não** vêm: não mudam (ADR-5).
- **Pré-condições:** papel; formato (400, `invalid_installation_date`); equipamento existe (`installed_equipment_not_found`) e está ativo (`installed_equipment_deactivated`); série livre para o modelo dele, fora ele mesmo (`serial_number_taken`). Local, cliente e modelo **não** são conferidos.
- **Fluxo:** carrega, `edit`, confere a série, salva.
- **Endpoint:** `PUT /installed-equipment/{equipmentId}`.

## Desativar e reativar

Seis use cases com a mesma forma: ator, id; papel; carrega; chama o método de domínio; salva. Sem evento, sem confirmação, sem efeito nos filhos (ADR-6).

| Use case | Endpoint | Erros específicos |
|---|---|---|
| `DeactivateCustomer` | `POST /customers/{id}/deactivation` | `customer_not_found`, `customer_deactivated` |
| `ReactivateCustomer` | `POST /customers/{id}/reactivation` | `customer_not_found`, `customer_not_deactivated` |
| `DeactivateLocation` | `POST /locations/{id}/deactivation` | `location_not_found`, `location_deactivated` |
| `ReactivateLocation` | `POST /locations/{id}/reactivation` | `location_not_found`, `location_not_deactivated` |
| `DeactivateInstalledEquipment` | `POST /installed-equipment/{id}/deactivation` | `installed_equipment_not_found`, `installed_equipment_deactivated` |
| `ReactivateInstalledEquipment` | `POST /installed-equipment/{id}/reactivation` | `installed_equipment_not_found`, `installed_equipment_not_deactivated` |

Reativar não confere o pai (*proposta*, como o catálogo): reativar um local de cliente desativado é permitido.

## Queries

Todas `@UseCase(transactional = false)`, abertas aos três papéis. Não carregam aggregate: leem por contrato de leitura em `application.customers.contracts.readers` (`CustomerReader`) e devolvem projeção ([`architecture.md`](../../rules/architecture.md) §7.1). A implementação do reader junta as tabelas do catálogo para os nomes de modelo, fabricante e tipo (mesmo BC, ADR-8). Sem cache.

### `ListCustomers`

- **Entrada:** `text`, `status`, `page`, `size`; o filtro é o `CustomerFilter`.
- **Busca:** `text` casa o nome, o documento (sem pontuação) ou a cidade de um local ativo, sem diferenciar maiúsculas ([`domain-model.md`](domain-model.md#persistência)).
- **Retorno:** `Page<CustomerSummary>`: `id`, `name`, `document`, `documentType` (`CPF` ou `CNPJ`), `email`, `phone`, `status`, `locationCount` (ativos), `equipmentCount` (ativos, em locais ativos), `cities` (cidade e UF distintas dos locais ativos). *Proposta*: as colunas da tela fecham com o mockup.
- **Endpoint:** `GET /customers`.

### `GetCustomer`

- **Entrada:** `customerId`. **Erro:** `customer_not_found`.
- **Retorno:** `CustomerDetail`: os campos do cliente e **todos** os locais (ativos e desativados), cada um com o endereço, a situação e **todos** os seus equipamentos: `id`, `serialNumber`, `installedOn`, `status`, modelo (`id`, `name`, `status`), fabricante (`id`, `name`), tipo (`id`, `name`) e o valor da característica com a unidade. Sem paginação interna (*proposta*; tamanho em [`open-questions.md`](open-questions.md)).
- **Endpoint:** `GET /customers/{customerId}`. É a ficha do cliente; a edição de local e de equipamento lê dela, sem `GET` próprio.

## Erros

Contrato público ([`architecture.md`](../../rules/architecture.md) §7.3). Códigos únicos no BC: os do catálogo reaproveitados aqui (`model_not_found`, `model_deactivated`) têm o mesmo significado e a mesma mensagem.

| `code` | Status | Mensagem (pt-BR) | Quando |
|---|---|---|---|
| `forbidden` | 403 | você não tem permissão para isto | Técnico tentando escrever |
| `customer_not_found` | 404 | cliente não encontrado | id inexistente |
| `location_not_found` | 404 | local não encontrado | id inexistente |
| `installed_equipment_not_found` | 404 | equipamento não encontrado | id inexistente |
| `model_not_found` | 404 | modelo não encontrado | instalar com modelo inexistente (código do catálogo) |
| `customer_document_taken` | 409 | já existe um cliente com este documento | CPF ou CNPJ repetido, inclusive de cliente desativado |
| `serial_number_taken` | 409 | este modelo já tem um equipamento com esse número de série | série repetida no modelo, inclusive de equipamento desativado |
| `customer_deactivated` | 422 | este cliente está desativado | editar; criar local; instalar em local de cliente desativado; desativar de novo |
| `customer_not_deactivated` | 422 | este cliente não está desativado | reativar quem está ativo |
| `location_deactivated` | 422 | este local está desativado | editar; instalar em local desativado; desativar de novo |
| `location_not_deactivated` | 422 | este local não está desativado | reativar quem está ativo |
| `installed_equipment_deactivated` | 422 | este equipamento está desativado | editar; desativar de novo |
| `installed_equipment_not_deactivated` | 422 | este equipamento não está desativado | reativar quem está ativo |
| `model_deactivated` | 422 | este modelo está desativado | instalar modelo desativado (código do catálogo) |
| `invalid_installation_date` | 400 | a data de instalação não pode ser depois de hoje | `InstallationDate` no futuro |
| `concurrent_update` | 409 | o registro foi alterado ao mesmo tempo por outra operação; tente de novo | lock otimista (código e mensagem do kernel) |

Sem código próprio, como 400 com a mensagem do VO: documento inválido (as mensagens do `Document`: "documento inválido: deve ser um cpf ou cnpj válido", "cpf inválido", "cnpj inválido"), e-mail, telefone ("formato de telefone inválido — use E.164 (ex: +5511999999999)"), cada campo do endereço, e os tamanhos de `CustomerName`, `LocationName` e `SerialNumber`. O frontend confere CPF, CNPJ, e-mail e CEP antes de enviar, e converte o telefone para E.164; o 400 do servidor é a rede de segurança.

## Consumo de integration event

Nenhum (ver [`domain-model.md`](domain-model.md#domain-events-e-integration-events)).

## Fora de escopo

- **Apagar** cliente, local ou equipamento: só desativa (ADR-6).
- **Trocar o documento** de um cliente, **mover um equipamento** de local ou **trocar o modelo** dele (ADR-3, ADR-5).
- **Lista geral de equipamentos** (todos os clientes, por modelo ou por série): a busca por série fica em [`open-questions.md`](open-questions.md).
- **Contatos múltiplos, consulta de CEP, garantia, foto, histórico, importação em lote, junção de duplicados** (README da feature).
