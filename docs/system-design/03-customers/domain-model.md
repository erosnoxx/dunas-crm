# Domain model: 03-customers

**Este arquivo é a fonte de verdade do domínio desta feature.** Nenhum `CLAUDE.md`, skill ou código o duplica: todos o referenciam. Casos de uso e códigos de erro: [`usecases.md`](usecases.md). Decisões: [`decisions.md`](decisions.md). Telas: [`design/`](design/README.md).

> Proposta para aprovação. Este modelo só fecha depois de os mockups de [`design/`](design/README.md) serem aprovados (regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md)).

## Nota de proveniência

Modelado a partir das decisões do usuário em [`decisions.md`](decisions.md), do padrão do [catálogo](../02-catalog/domain-model.md) e da leitura dos VOs de `shared-domain`. O que **eu** escolhi sem decisão prévia do usuário está marcado como *proposta* e tem ADR ou pendência.

## BC e subdomínio

BC **`registry`**, subdomínio **`customers`** (ADR-1). Os três aggregates abaixo moram em `com.crm.registry.domain.customers`. Referências entre eles e para o modelo do catálogo são `UUID` no campo; quando uma regra precisa do estado do outro aggregate, ele **entra por parâmetro** no método, nunca por consulta ([`architecture.md`](../../rules/architecture.md) §6.4), como o `EquipmentModel` recebe o fabricante e o tipo.

## Fronteiras de consistência: por que três aggregates

A pergunta é se `Location` e `InstalledEquipment` são entidades internas do `Customer` ou aggregates próprios. Pesando as invariantes e o tamanho (*proposta*, ADR-10):

| Invariante | Onde se garante | Precisa de um aggregate só? |
|---|---|---|
| Documento único entre **todos** os clientes | índice `UNIQUE` no banco | não: nenhum aggregate enxerga os outros clientes |
| Série única por modelo, entre **todos** os clientes | índice `UNIQUE` no banco | não: cruza clientes, nunca cabe num aggregate |
| Local novo só em cliente ativo | `Location.register(customer, ...)` | não: o cliente entra por parâmetro |
| Equipamento novo só em local, cliente e modelo ativos | `InstalledEquipment.install(location, customer, model, ...)` | não: os três entram por parâmetro |

Nenhuma invariante exige carregar cliente, locais e equipamentos juntos. Os motivos para separar:

- **O equipamento é referenciado de fora pelo local** (e, depois, a usina do monitoramento também vai apontar para o local). No DDD só a raiz de um aggregate é referenciável de fora; local como entidade interna do cliente obrigaria a referência a ser "cliente + local".
- **Tamanho.** Um cliente comercial tem dezenas de equipamentos (cada unidade é uma linha: ADR-5). Com tudo num aggregate, cadastrar um equipamento regravaria o cliente inteiro e disputaria a versão dele com quem edita o telefone.
- **Ciclos independentes.** Cada um se cria, edita, desativa e reativa sozinho, com a própria rota e o próprio formulário.

**Risco aceito:** "local novo só em cliente ativo" é conferido na leitura do cliente, sem trava; um local criado no instante em que o cliente é desativado escapa. O estado resultante (local ativo em cliente desativado) é o mesmo que a desativação sem propagação já produz (ADR-6), então nada fica inválido. O mesmo vale para o equipamento.

## Aggregates

### `Customer` (aggregate root)

O cliente, pessoa física ou jurídica. **Fronteira de consistência:** o próprio cadastro. A unicidade do documento é do banco, traduzida em `ConflictException` pelo repository.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `document` | `Document` (shared) | CPF ou CNPJ; o tipo (física ou jurídica) vem dele, nunca de um campo à parte (ADR-2) |
| `name` | `CustomerName` | nome da pessoa ou razão social, de 2 a 120 caracteres (*proposta*) |
| `email` | `Email` (shared), opcional | ausência é `null` no aggregate, nunca um VO vazio |
| `phone` | `Phone` (shared), opcional | idem |
| `status` | `CustomerStatus` | `ACTIVE` ou `DEACTIVATED` |

**Invariantes:** só um cliente `ACTIVE` pode ser editado. O documento não muda depois do cadastro (*proposta*, ADR-3; pendência em [`open-questions.md`](open-questions.md)).

**Métodos de domínio:**

| Método | Efeito | Erros de domínio (`code`) |
|---|---|---|
| `register(document, name, email, phone)` | cria `ACTIVE` | — |
| `edit(name, email, phone)` | troca nome e contato (e-mail e telefone podem ser apagados) | `customer_deactivated` |
| `deactivate()` | `ACTIVE` → `DEACTIVATED` | `customer_deactivated` (já está) |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` | `customer_not_deactivated` |

### `Location` (aggregate root)

Um endereço do cliente onde há (ou haverá) instalação. **Fronteira de consistência:** o próprio local.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `customerId` | `UUID` | do `Customer`; **não muda** depois de salvo |
| `name` | `LocationName` | de 2 a 60 caracteres, ex. "Casa", "Filial Centro" (*proposta*) |
| `address` | `Address` (shared) | CEP, logradouro, número, complemento opcional, bairro, cidade, UF |
| `status` | `CustomerStatus` | |

**Invariantes:** `customerId` é imutável; só se cria local em cliente `ACTIVE`; só se edita local `ACTIVE`. O nome não é único no cliente e o mesmo endereço pode se repetir (*proposta*: nenhuma regra pedida, e recusar exigiria comparar endereço digitado à mão).

**Métodos de domínio** (o cliente entra por parâmetro):

| Método | Efeito | Erros (`code`) |
|---|---|---|
| `register(customer, name, address)` | cria `ACTIVE` | `customer_deactivated` |
| `edit(name, address)` | troca nome e endereço | `location_deactivated` |
| `deactivate()` | `ACTIVE` → `DEACTIVATED` | `location_deactivated` (já está) |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` | `location_not_deactivated` |

`edit` e `reactivate` não olham o cliente: o cliente desativado só bloqueia local **novo** (ADR-6, como o tipo desativado no catálogo).

### `InstalledEquipment` (aggregate root)

Uma unidade física de um modelo do catálogo, instalada num local. **Fronteira de consistência:** a própria unidade. A unicidade (modelo, série) é do banco.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `locationId` | `UUID` | do `Location`; **não muda** depois de salvo (ADR-5) |
| `modelId` | `UUID` | do `EquipmentModel` do catálogo; **não muda** depois de salvo (ADR-5) |
| `serialNumber` | `SerialNumber` | texto livre, de 1 a 60 caracteres (*proposta*) |
| `installedOn` | `InstallationDate`, opcional | não pode ser depois de hoje |
| `status` | `CustomerStatus` | |

O cliente não é campo: é o do local. Guardar os dois abriria espaço para discordarem.

**Invariantes:**
- `locationId` e `modelId` são imutáveis.
- Só se cadastra equipamento com o local `ACTIVE`, o cliente do local `ACTIVE` e o modelo `ACTIVE` (ADR-6). O fabricante e o tipo do modelo **não** são conferidos: desativá-los não propaga para o modelo ([`02-catalog` ADR-5](../02-catalog/decisions.md)).
- Só se edita equipamento `ACTIVE`.

**Métodos de domínio** (local, cliente e modelo entram por parâmetro):

| Método | Efeito | Erros (`code`) |
|---|---|---|
| `install(location, customer, model, serialNumber, installedOn)` | cria `ACTIVE` | `location_deactivated`, `customer_deactivated`, `model_deactivated` |
| `edit(serialNumber, installedOn)` | troca série e data (*proposta*: a série muda para corrigir digitação; pendência em [`open-questions.md`](open-questions.md)) | `installed_equipment_deactivated` |
| `deactivate()` | `ACTIVE` → `DEACTIVATED` | `installed_equipment_deactivated` (já está) |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` | `installed_equipment_not_deactivated` |

`install` confere que `customer` é o dono do `location` (`location.customerId == customer.id`); divergência é erro de programação do use case, não regra de negócio, e não tem código público. `model_deactivated` é o mesmo código do catálogo (o código é único no BC, e o significado é o mesmo).

## Value Objects

| VO | Base | Regra de validação | Fica em |
|---|---|---|---|
| `Document` | `StringValueObject` | **já existe.** Remove tudo que não é letra ou dígito e põe em maiúsculas; 11 caracteres é CPF, 14 é CNPJ (inclusive o alfanumérico), com dígitos verificadores conferidos; outro tamanho recusa ("documento inválido: deve ser um cpf ou cnpj válido", "cpf inválido", "cnpj inválido"). Tem `getType()`, `isIndividual()`, `isOrganization()` e `maskedForDisplay()` | `shared-domain` |
| `Email` | `StringValueObject` | **já existe.** Minúsculas, até 254 caracteres, formato `nome@dominio.tld` | `shared-domain` |
| `Phone` | `StringValueObject` | **já existe.** E.164 estrito: `+`, código do país e de 7 a 15 dígitos, sem separador (`^\+[1-9]\d{6,14}$`); a formatação para leitura é da tela | `shared-domain` |
| `Address` | record de VOs | **já existe.** Exige CEP, logradouro, número, bairro, cidade e UF; o complemento aceita ausência (`null`) | `shared-domain` |
| `ZipCode` | `StringValueObject` | **já existe.** Remove o que não é dígito e exige 8; `formatted()` devolve `00000-000` | `shared-domain` |
| `Street` | `StringValueObject` | **já existe.** De 2 a 150 caracteres | `shared-domain` |
| `AddressNumber` | `StringValueObject` | **já existe.** Texto ("S/N", "123A"), em maiúsculas, de 1 a 15 caracteres | `shared-domain` |
| `AddressComplement` | `StringValueObject` | **já existe.** De 1 a 50 caracteres | `shared-domain` |
| `Neighborhood` | `StringValueObject` | **já existe.** De 2 a 100 caracteres | `shared-domain` |
| `CityName` | `StringValueObject` | **já existe.** De 1 a 100 caracteres | `shared-domain` |
| `UF` | enum | **já existe.** As 27 siglas | `shared-domain` |
| `CustomerName` | `StringValueObject` | de 2 a 120 caracteres (*proposta*) | `registry-domain` |
| `LocationName` | `StringValueObject` | de 2 a 60 caracteres (*proposta*) | `registry-domain` |
| `SerialNumber` | `StringValueObject` | de 1 a 60 caracteres, guardado como digitado (sem trocar caixa) (*proposta*) | `registry-domain` |
| `InstallationDate` | `LocalDateValueObject` | não pode ser depois de hoje; "hoje" vem de um `Clock` recebido, como o `BirthDate` (`of(valor, clock)`) | `registry-domain` |
| `CustomerStatus` | enum | `ACTIVE`, `DEACTIVATED` | `registry-domain` |

Todo VO de `StringValueObject` já corta espaço das pontas e recusa vazio. Nenhum VO novo sobe para `shared-domain`: só o `customers` os usa (critério da [`architecture.md`](../../rules/architecture.md) §3.1). `CustomerStatus` é enum próprio e não reaproveita o `CatalogStatus`, para a situação do cliente não depender de uma mudança no catálogo (*proposta*).

**Mensagens que não podem carregar o documento.** O log do `@UseCase` escreve a mensagem da exceção ([`code-style.md`](../../rules/code-style.md) §5): nenhuma mensagem deste subdomínio inclui CPF, CNPJ, e-mail ou telefone.

## Specifications, Policies e Domain Services

Nenhum. Cada regra tem um dono natural: "cadastro novo só com o pai ativo" mora no aggregate criado, que recebe o pai por parâmetro; a unicidade é do banco. Não há decisão que varie por contexto.

## Ciclo de vida

Os três aggregates têm o ciclo de dois estados do catálogo, como **enum com guardas no aggregate**, sem a base `StateMachine`:

```
ACTIVE ──deactivate──▶ DEACTIVATED ──reactivate──▶ ACTIVE
```

| De | Para | Quem | Trava |
|---|---|---|---|
| `ACTIVE` | `DEACTIVATED` | Administrador ou Comercial | `<x>_deactivated` se já está |
| `DEACTIVATED` | `ACTIVE` | Administrador ou Comercial | `<x>_not_deactivated` se já está ativo |

Desativar **não propaga** (ADR-6): desativar o cliente não desativa os locais, e desativar o local não desativa os equipamentos. O pai desativado só bloqueia filho **novo**. Nada se apaga.

## Domain events e integration events

**Nenhum publicado, nenhum consumido.** Nenhuma reação dentro do BC depende de um fato do `customers`, e nenhum outro BC existe que precise dele. Quando o monitoramento, a manutenção ou a proposta (outros BCs) precisarem de cliente ou local, o integration event nasce com a necessidade, e isso vira ADR.

## Persistência

Migration `V2__create_customers.sql` em `registry-infrastructure/src/main/resources/db/migration/registry/`. FK para as tabelas do catálogo é permitida, por ser o mesmo BC ([`architecture.md`](../../rules/architecture.md) §4.4). Colunas padrão de toda tabela: `id uuid PK`, `version`, `created_at`, `updated_at` nulável ([`architecture.md`](../../rules/architecture.md) §8).

| Tabela | Colunas | Restrições e índices |
|---|---|---|
| `customers` | `document varchar(14)`, `name varchar(120)`, `email varchar(254)` nulável, `phone varchar(16)` nulável, `status varchar(20)` | `UNIQUE (document)` (`uk_customers_document`) |
| `locations` | `customer_id`, `name varchar(60)`, `zip_code char(8)`, `street varchar(150)`, `number varchar(15)`, `complement varchar(50)` nulável, `neighborhood varchar(100)`, `city varchar(100)`, `state char(2)`, `status varchar(20)` | `FK customer_id → customers`; índice em `(customer_id)` |
| `installed_equipment` | `location_id`, `model_id`, `serial_number varchar(60)`, `installed_on date` nulável, `status varchar(20)` | `FK location_id → locations`; `FK model_id → equipment_models`; `UNIQUE (model_id, lower(serial_number))` (`uk_installed_equipment_model_serial`); índice em `(location_id)` |

- **Unicidade do documento:** o `Document` já guarda só letras e dígitos, em maiúsculas, então `UNIQUE (document)` compara o documento normalizado: "123.456.789-09" e "12345678909" colidem. O índice cobre clientes **desativados** também (ADR-3): o cadastro antigo se reativa, não se duplica.
- **Unicidade da série:** sem diferenciar maiúsculas, como os nomes do catálogo, e também sobre os desativados (ADR-5).
- **Concorrência:** o use case confere a unicidade antes, para a resposta amigável; na corrida, o repository traduz a violação do índice pelo nome dele no mesmo código de erro (como o `UniqueConstraint` do catálogo). Edições simultâneas do mesmo aggregate caem no lock otimista (`concurrent_update`, 409).
- O estado (`status`) e a UF (`state`) são texto com o nome do enum. O endereço são colunas de `locations`, não tabela própria: é VO do local.
- **Busca da lista de clientes** (*proposta*): o texto casa, sem diferenciar maiúsculas, o nome do cliente, a cidade de algum local **ativo** dele, e o documento (o texto sem pontuação, contido no `document`, só quando sobra algum caractere depois de tirar a pontuação). Acento fica como no catálogo: ver [`open-questions.md`](open-questions.md).
