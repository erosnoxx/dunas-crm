# Clientes (backend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o subdomínio `customers` do BC `registry` no backend: cliente, local e equipamento instalado, com cadastro, edição, desativação e reativação, a lista com busca (inclusive pela série, com a série que casou), a ficha sem paginação e as permissões por papel.

**Architecture:** Três aggregates próprios (`Customer`, `Location`, `InstalledEquipment`), ligados por `UUID`, em `registry-domain`; o pai entra por parâmetro na criação do filho, e o `EquipmentModel` do catálogo entra por parâmetro no `install` (ADR-8, ADR-10). Use cases concretos em `registry-application`, persistência (uma tabela por aggregate, sem tabela filha) e o reader da lista e da ficha em `registry-infrastructure`, três controllers em `web`. Tudo no pacote `com.crm.registry.<camada>.customers` (`architecture.md` §4.4), no layout reorganizado do PR #7 (o mesmo do catálogo e do IAM). O BC, os módulos Maven e a regra `subdomainsDependOnlyInDeclaredDirection` (`registry.customers → registry.catalog`) **já existem**: não há esqueleto. A única tarefa de base é a 0: a `RegistryBeans` está hoje dentro do subdomínio `catalog` e não pode registrar beans do `customers` sem o `catalog` passar a depender do `customers`.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Data JPA, Flyway, Postgres, JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit.

**Spec:** [`docs/system-design/03-customers/`](../system-design/03-customers/README.md): `domain-model.md`, `usecases.md` (rotas, ordem das pré-condições, tabela de erros, casos de teste), `decisions.md` (ADR-1 a ADR-32), mockups aprovados em 2026-11-22 em `design/`. Regras: [`architecture.md`](../rules/architecture.md) §4.4, §6, §7, §8; [`code-style.md`](../rules/code-style.md). **O plano não redefine o domínio**: se uma tarefa pedir mudança nele, pare, corrija a spec primeiro e diga ao usuário. Onde este texto e o `usecases.md` divergirem, **vale o `usecases.md`**.

**Escopo:** só o **backend**. O frontend tem plano próprio ([`2026-11-22-customers-frontend.md`](2026-11-22-customers-frontend.md)).

## Global Constraints

- **JDK 21**; tudo roda a partir de `backend/`; critério de aceite de cada tarefa: `mvn test -Dtest='!*PostgresTest,!IamEndToEndTest,!RegistryEndToEndTest,!CustomersEndToEndTest'` verde, e `mvn test` verde onde houver Docker (skill `crm-build-and-test`). A tarefa que cria ou muda `*PostgresTest` só fecha com ele rodado **com Docker**.
- `domain` e `application` são Java puro; a única anotação de framework em `application` é `@UseCase`; Bean Validation só nos requests de `web`. Beans na `RegistryBeans` com `new` (§9), nada de `@Component`.
- **Layout** (o do catálogo depois do PR #7; pacotes novos marcados): domínio `com.crm.registry.domain.customers.{entities,vos,enums}`; application `com.crm.registry.application.customers` com `CustomerAccess`, `contracts.repositories`, `contracts.readers`, `criteria`, `schemas`, `usecases.commands`, `usecases.queries`; infraestrutura `com.crm.registry.infrastructure.customers.{mappers,persistence.entities,persistence.jpa,repositories,readers}`; beans na `com.crm.registry.infrastructure.config.RegistryBeans` (Tarefa 0); web `com.crm.web.registry` e `com.crm.web.registry.requests`. Nomes como no código: `<Nome>RepositoryImpl` e `<Nome>ReaderImpl`, **sem** o prefixo `Jpa` (o `architecture.md` §10.1 e as skills `crm-add-*` ainda dizem `Jpa<Nome>RepositoryImpl`: siga o código e não edite o `architecture.md` neste plano). Migration `V2__create_customers.sql` em `registry-infrastructure/src/main/resources/db/migration/registry/`.
- **O que o `customers` importa do `catalog`** (ADR-8; a direção contrária é proibida pelo ArchUnit): no domínio, só `EquipmentModel` (parâmetro do `install`); na application, `EquipmentModelRepository` e, nos schemas da ficha, `CatalogStatus` e `MeasurementUnit` (`domain.catalog.enums`); na infraestrutura, as entidades do catálogo (`catalog.persistence.entities`) nas consultas do reader e as classes `catalog.readers.CatalogQueries` e `catalog.repositories.UniqueConstraint` (que passam a públicas). Nada mais. O `catalog` nunca importa nada do `customers`; a `RegistryBeans`, fora dos dois subdomínios, importa os dois.
- **Ordem das pré-condições, em todo command** (`usecases.md`, é contrato): **papel** (403) → **formato** (400, montando **todos** os VOs antes de ler o repository) → **existe** (404) → **ativo** (422) → **regra do aggregate** (`contact_required`, `trade_name_not_allowed`: 422) → **unicidade** (409). Cada command tem teste da ordem.
- **Ator:** `CustomerAccess.requireEditor(actor)` nos commands e `CustomerAccess.requireReader(actor)` nas queries, sempre a **primeira** linha; ator nulo falha ao ser dereferenciado ali, **sem** `NullPointerException` explícita nem `Objects.requireNonNull` (`code-style.md` §6). Papel se confere só no use case, nunca no controller.
- Estilo ([`code-style.md`](../rules/code-style.md)): nada de `else`; `if` de uma instrução sem chaves; `catch` em linha própria; 3+ parâmetros um por linha; identificadores em inglês; Javadoc em toda classe de produção; comentário, mensagem de exceção, log e `@DisplayName` em pt-BR; mensagem minúscula e sem ponto final; nenhuma exception nativa. **Nenhuma mensagem leva CPF, CNPJ, e-mail ou telefone** (`domain-model.md`).
- Valores da spec: `CustomerName` 2 a 150 (nome e nome fantasia); `LocationName` 2 a 60; `SerialNumber` 1 a 60, guardado como digitado; `InstallationDate` não depois de hoje (`Clock`); `CustomerStatus {ACTIVE, DEACTIVATED}`; VOs de `shared-domain` como estão (`Document`, `Email`, `Phone`, `Address` e os de endereço, `UF`).
- Códigos de erro `snake_case`, **exatamente** os da tabela de `usecases.md`, com a mensagem de lá; `model_not_found` e `model_deactivated` reaproveitam o texto do catálogo.
- **Campo opcional:** `null` é ausência; texto em branco é 400 do VO (ADR-31). Nada de converter `""` em ausência.
- **Persistência:** uma tabela por aggregate, sem tabela filha nem coluna de versão fora do aggregate (`domain-model.md`, Persistência); unicidade no banco (`uk_customers_document`, `uk_installed_equipment_model_serial`), traduzida pelo nome do índice no código de erro; o use case confere antes só para a resposta amigável.
- Commit: Conventional Commits, escopo `registry` (ou `web`, `boot`), em inglês, **sem `Co-Authored-By`** ([`git-workflow.md`](../rules/git-workflow.md) §3).
- Branch: `feature/customers-backend`, base `develop` **depois do merge do PR da spec** (`docs/customers-spec`). **Pergunte a base ao usuário antes da Tarefa 1**, como no catálogo.

## Review Focus

1. **Ordem das pré-condições:** Técnico com campo inválido → 403 sem tocar repository; documento repetido com nome inválido → 400; documento repetido sem contato → 422 `contact_required`, não 409; cliente desativado editado sem contato → `customer_deactivated`; id inexistente com campo inválido → 400. → Tarefas 5 a 7.
2. **Documento repetido em outra formatação e cadastro simultâneo:** "123.456.789-09" × "12345678909" → 409 `customer_document_taken`; duas threads com o mesmo documento → uma grava, a outra `ConflictException` com o código, **nunca** 500. → Tarefas 9 e 13.
3. **Série:** corrigir a própria série (ou só a caixa) não conflita consigo mesma (`exceptId`); a mesma série em outro modelo grava; `:exceptId is null` funciona no Postgres real. → Tarefas 7 e 9.
4. **Edição concorrente:** salvar uma cópia velha de cliente, local ou equipamento → `ConcurrentUpdateException` (`concurrent_update`), sem sobrescrever. → Tarefa 9.
5. **Busca e série casada:** acha por trecho de nome, nome fantasia, documento sem pontuação, cidade de local ativo e série de equipamento ativo; `matchedSerials` com no máximo três, vazio sem texto; a paginação não se multiplica por local. → Tarefa 10.
6. **Pai desativado:** bloqueia só filho novo; editar, desativar e reativar local de cliente desativado e equipamento de local desativado gravam (ADR-30). → Tarefas 3, 4, 6 e 7.
7. **Requests:** `status=FOO` → 400; data inválida → 400; `PUT /customers/{id}` ignora `document` no corpo; `web` não importa `registry-domain`. → Tarefas 11 a 13.

---

## Pré-requisito

### Tarefa 0: `RegistryBeans` sai do subdomínio `catalog`

O PR #7 pôs a `@Configuration` do BC em `com.crm.registry.infrastructure.catalog.config`. Registrar ali os beans do `customers` faria o `catalog` importar o `customers`, o que a `subdomainsDependOnlyInDeclaredDirection` recusa. O `architecture.md` §9 pede **um arquivo por BC**, então a saída é a mesma do IAM (`com.crm.iam.infrastructure.config.IamBeans`): a classe sobe para o nível do BC. **Decisão de arquitetura a confirmar com o usuário antes de executar** (a alternativa, uma `CustomersBeans` própria, contraria o §9 como está escrito).

**Files:**
- Move: `core/registry/registry-infrastructure/src/main/java/com/crm/registry/infrastructure/catalog/config/RegistryBeans.java` → `.../com/crm/registry/infrastructure/config/RegistryBeans.java` (pacote `com.crm.registry.infrastructure.config`)
- Modify: quem importar a classe (`grep -rl "catalog.config.RegistryBeans" backend/`)

**Interfaces:** nenhuma nova; a classe e os beans são os de hoje. O pacote `config` não é subdomínio (não está no `SUBDOMAIN_DEPENDENCIES`), então pode importar `catalog` e `customers`.

- [ ] **Step 1:** rodar `mvn -pl edge/boot -am test -Dtest=BoundedContextRulesTest` → PASS antes da mudança (linha de base).
- [ ] **Step 2: Mover** com `git mv`, trocar o `package` e os imports; nenhuma outra mudança.
- [ ] **Step 3: Rodar:** `mvn test -Dtest='!*PostgresTest,!IamEndToEndTest,!RegistryEndToEndTest'` → PASS, e, com Docker, `RegistryEndToEndTest` (o contexto sobe com a configuração no lugar novo).
- [ ] **Step 4: Commit:** `refactor(registry): move the registry beans out of the catalog subdomain`.

---

## Domínio (`registry-domain`)

### Tarefa 1: Value Objects e enum do `customers`

**Files:** `core/registry/registry-domain/src/main/java/com/crm/registry/domain/customers/vos/{CustomerName,LocationName,SerialNumber,InstallationDate}.java` e `.../domain/customers/enums/CustomerStatus.java` (pacotes novos, como `catalog/vos` e `catalog/enums`); testes em `src/test/java/com/crm/registry/domain/customers/vos/`.

**Interfaces (produz):**
- `CustomerName.of(String)`, `LocationName.of(String)`, `SerialNumber.of(String)`: `extends StringValueObject` (como `ManufacturerName`); `type()` = `"nome do cliente"`, `"nome do local"`, `"número de série"`; `checkLength` com 2–150, 2–60, 1–60 (mensagem da base: `"<tipo> deve ter entre <min> e <max> caracteres"`). `SerialNumber` não troca a caixa.
- `InstallationDate.of(LocalDate value, Clock clock)`: `extends LocalDateValueObject`, no molde do `BirthDate`; depois de `LocalDate.now(clock)` → `ValidationException("invalid_installation_date", "a data de instalação não pode ser depois de hoje")`; hoje é válido; nulo recusado com `ValidationException` (o use case só chama com valor).
- `enum CustomerStatus {ACTIVE, DEACTIVATED}`.

- [ ] **Step 1: Testes que falham** (`@DisplayName` em pt-BR): `CustomerNameTest` aceita 2 e 150 e recusa 1 e 151, nulo e só espaços; `LocationNameTest` 2/60 e 1/61; `SerialNumberTest` 1/60 e 61, **`keepsTheCaseAsTyped`** (`"hv22Ab"` fica `"hv22Ab"`), corta as pontas; `InstallationDateTest` com `Clock.fixed(2026-11-22)`: hoje e ontem passam, amanhã falha com `getCode()` = `invalid_installation_date`; `CustomerStatusTest` (os dois valores).
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/registry/registry-domain -am test` → FAIL.
- [ ] **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the customers value objects`.

### Tarefa 2: Aggregate `Customer`

**Files:** `.../domain/customers/entities/Customer.java`; teste `CustomerTest`.

**Interfaces (produz):**
- `Customer extends AggregateRoot`: `static Customer register(Document document, CustomerName name, CustomerName tradeName, Email email, Phone phone)` (nasce `ACTIVE`; `tradeName`, `email`, `phone` podem ser `null`); `static Customer reconstitute(UUID id, Long version, Document document, CustomerName name, CustomerName tradeName, Email email, Phone phone, CustomerStatus status)`; `void edit(CustomerName name, CustomerName tradeName, Email email, Phone phone)`; `void deactivate()`; `void reactivate()`; getters `getDocument()`, `getName()`, `getTradeName(): Optional<CustomerName>`, `getEmail(): Optional<Email>`, `getPhone(): Optional<Phone>`, `getStatus()`, `isActive()`. Sem setter de documento (ADR-3, ADR-12).
- Erros (`DomainException(code, mensagem)`, textos da tabela): `contact_required` ("informe pelo menos um contato: e-mail ou telefone") quando `email` e `phone` são nulos; `trade_name_not_allowed` ("nome fantasia só vale para pessoa jurídica") quando há `tradeName` e `document.isIndividual()`; `customer_deactivated` ("este cliente está desativado") em `edit` e `deactivate` fora de `ACTIVE`; `customer_not_deactivated` ("este cliente não está desativado") em `reactivate` fora de `DEACTIVATED`. Ordem em `edit`: `customer_deactivated` → `contact_required` → `trade_name_not_allowed`; em `register`: `contact_required` → `trade_name_not_allowed`.

- [ ] **Step 1: Testes que falham:** `registersActiveWithOnlyAPhone`, `...WithOnlyAnEmail`; `registerWithoutContactFails` (`contact_required`); `registerCpfWithTradeNameFails` (`trade_name_not_allowed`); `registerCnpjWithTradeName`; `editChangesNameTradeNameAndContactButNeverTheDocument`; `editCanDropOneContactButNotBoth`; `editCanDropTheTradeName`; **`editOfADeactivatedCustomerWithoutContactFailsAsDeactivated`** (ordem); `deactivateTwiceFails`; `reactivateAnActiveOneFails`; `reconstituteKeepsIdAndVersion`; nenhum domain event; **`errorMessagesNeverCarryTheDocument`**.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the Customer aggregate`.

### Tarefa 3: Aggregate `Location`

**Files:** `.../domain/customers/entities/Location.java`; teste `LocationTest`.

**Interfaces:**
- Consumes: `Customer` (Tarefa 2).
- Produz: `Location extends AggregateRoot`: `static Location register(Customer customer, LocationName name, Address address)`; `static Location reconstitute(UUID id, Long version, UUID customerId, LocationName name, Address address, CustomerStatus status)`; `void edit(LocationName name, Address address)`; `deactivate()`, `reactivate()`; getters `getCustomerId()` (imutável), `getName()`, `getAddress()`, `getStatus()`, `isActive()`.
- Erros: `register` com cliente desativado → `customer_deactivated`; `edit`/`deactivate` fora de `ACTIVE` → `location_deactivated` ("este local está desativado"); `reactivate` fora de `DEACTIVATED` → `location_not_deactivated` ("este local não está desativado"). `edit`, `deactivate` e `reactivate` **não** recebem o cliente (ADR-6, ADR-30).

- [ ] **Step 1: Testes que falham:** `registersActiveForAnActiveCustomer` (guarda o `customerId`); `registerForADeactivatedCustomerFails`; `editChangesNameAndAddress`; `editOfADeactivatedLocationFails`; `deactivateAndReactivate` com os dois erros; `complementMayBeAbsent` (`Address` com complemento nulo); `reconstituteKeepsIdAndVersion`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the Location aggregate`.

### Tarefa 4: Aggregate `InstalledEquipment`

**Files:** `.../domain/customers/entities/InstalledEquipment.java`; teste `InstalledEquipmentTest` (modelo montado com `EquipmentModel.register`/`reconstitute` do catálogo).

**Interfaces:**
- Consumes: `Customer`, `Location` (Tarefas 2 e 3); `com.crm.registry.domain.catalog.entities.EquipmentModel` (`getId()`, `isActive()`).
- Produz: `InstalledEquipment extends AggregateRoot`: `static InstalledEquipment install(Location location, Customer customer, EquipmentModel model, SerialNumber serialNumber, InstallationDate installedOn)` (`installedOn` pode ser `null`); `static InstalledEquipment reconstitute(UUID id, Long version, UUID locationId, UUID modelId, SerialNumber serialNumber, InstallationDate installedOn, CustomerStatus status)`; `void edit(SerialNumber serialNumber, InstallationDate installedOn)`; `deactivate()`, `reactivate()`; getters `getLocationId()`, `getModelId()` (imutáveis), `getSerialNumber()`, `getInstalledOn(): Optional<InstallationDate>`, `getStatus()`, `isActive()`.
- Erros, nesta ordem no `install`: `location_deactivated` → `customer_deactivated` → `model_deactivated` ("este modelo está desativado", o texto do catálogo). `location.getCustomerId()` diferente de `customer.getId()` → `DomainException("o cliente informado não é o dono do local")`, **sem código** (erro de programação, sem linha na tabela pública: `domain-model.md`). `edit`/`deactivate` fora de `ACTIVE` → `installed_equipment_deactivated` ("este equipamento está desativado"); `reactivate` → `installed_equipment_not_deactivated` ("este equipamento não está desativado").

- [ ] **Step 1: Testes que falham:** `installsActiveWithModelAndLocation`; `installWithoutDateKeepsItAbsent`; `installInADeactivatedLocationFails`; **`installForADeactivatedCustomerWithAnActiveLocationFails`** (sem propagação, ADR-6); `installOfADeactivatedModelFails`; `installAcceptsAModelWhoseManufacturerIsDeactivated` (só o modelo é conferido); `installWithAnotherCustomersLocationFailsWithoutCode`; `editChangesSerialAndDateButNeverLocationNorModel`; `editOfADeactivatedEquipmentFails`; `deactivateAndReactivate`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar** e `mvn -pl edge/boot -am test -Dtest=BoundedContextRulesTest` (o domínio do `customers` só enxerga `EquipmentModel` do catálogo).
- [ ] **Step 5: Commit:** `feat(registry): add the InstalledEquipment aggregate`.

---

## Aplicação (`registry-application`)

Pacote base `com.crm.registry.application.customers`. Testes com Mockito (repositories e reader `mock`), um `CustomersFixtures` de teste no molde do `CatalogFixtures`, `Clock.fixed` só nos de equipamento. Os testes chamam o use case sem o aspect: o erro de VO chega como `ValidationException` (400 depois do aspect) e o de aggregate como `DomainException` (422).

### Tarefa 5: `CustomerAccess`, contratos dos repositories e os commands de cliente

**Files:**
- Create: `CustomerAccess.java`; `contracts/repositories/{CustomerRepository,LocationRepository,InstalledEquipmentRepository}.java`; `criteria/{CustomerCriteria,LocationCriteria,InstalledEquipmentCriteria}.java` (como `catalog/criteria`); `usecases/commands/{OptionalValue,RegisterCustomer,UpdateCustomer,DeactivateCustomer,ReactivateCustomer}.java`
- Test: `CustomerAccessTest`, `usecases/commands/*CustomerTest.java`, `OptionalValueTest`

**Interfaces (produz):**
- `CustomerAccess` (`final`, estático, Javadoc citando o ADR-7): `requireEditor(Actor)` = `actor.requireAnyRole(ADMINISTRATOR, COMMERCIAL)`; `requireReader(Actor)` = os três papéis.
- `CustomerRepository extends Repository<Customer, CustomerCriteria>`: `boolean existsByDocument(Document document)` (o documento é imutável: sem `exceptId`). `LocationRepository extends Repository<Location, LocationCriteria>` (nada além do herdado). `InstalledEquipmentRepository extends Repository<InstalledEquipment, InstalledEquipmentCriteria>`: `boolean existsByModelAndSerialNumber(UUID modelId, SerialNumber serialNumber, UUID exceptId)` (sem caixa; `exceptId` nulo = nenhum). `Criteria` records vazios, como o `EquipmentModelCriteria`.
- `OptionalValue.of(String value, Function<String, T> factory): T` (`final`, estático): `null` → `null`; qualquer outro texto passa pelo VO (em branco vira 400 do VO).
- `RegisterCustomer.Params(Actor actor, String document, String name, String tradeName, String email, String phone)` → `UUIDResponse`. Fluxo: `requireEditor` → `Document.of`, `CustomerName.of`, `OptionalValue.of` para fantasia, e-mail e telefone → `Customer.register` → `existsByDocument` → `ConflictException("customer_document_taken", "já existe um cliente com este documento")` → `save`.
- `UpdateCustomer.Params(Actor actor, UUID customerId, String name, String tradeName, String email, String phone)` (**sem documento**). Fluxo: `requireEditor` → VOs → `findById` (`NotFoundException("customer_not_found", "cliente não encontrado")`) → `edit` → `save` (versão lida: `concurrent_update` sai do repository).
- `DeactivateCustomer.Params(Actor actor, UUID customerId)`, `ReactivateCustomer.Params(...)`.

- [ ] **Step 1: Testes que falham:** `CustomerAccessTest` (Técnico recusado no editor, aceito no leitor); por command: `registersAndReturnsTheId`; `technicianWithAnInvalidFieldIsForbiddenAndTheRepositoryIsUntouched` (`verifyNoInteractions`); `administratorAndCommercialCanWrite`; **`repeatedDocumentWithAnInvalidNameIsABadRequest`** (`existsByDocument` nunca chamado); **`repeatedDocumentWithoutContactIsContactRequired`**; `repeatedDocumentIsAConflictAndNothingIsSaved` (a mensagem não contém o documento); `blankTradeNameIsABadRequest` e `nullTradeNameIsAbsent`; `updateDoesNotReceiveTheDocument`; **`updateWithAnInvalidNameOfAnUnknownCustomerIsABadRequest`** (`findById` nunca chamado); `updateNotFound`; **`updateOfADeactivatedCustomerWithoutContactIsDeactivated`**; `updateCpfWithTradeNameFails`; desativar e reativar salvam o novo status e devolvem 404 do id inexistente.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/registry/registry-application -am test` → FAIL. **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the customer commands and the customers access rule`.

### Tarefa 6: Commands de local

**Files:** `schemas/AddressInput.java`; `usecases/commands/{AddressParser,RegisterLocation,UpdateLocation,DeactivateLocation,ReactivateLocation}.java`; testes.

**Interfaces:**
- Consumes: Tarefa 5 (`CustomerAccess`, `CustomerRepository`, `LocationRepository`, `OptionalValue`).
- `record AddressInput(String zipCode, String street, String number, String complement, String neighborhood, String city, String state)`.
- `AddressParser.parse(AddressInput input): Address` (`final`, como o `MeasurementUnitParser`): monta `ZipCode`, `Street`, `AddressNumber`, `OptionalValue.of(complement, AddressComplement::of)`, `Neighborhood`, `CityName`; `state` pelo nome exato do `UF`, senão `BadRequestException("uf inválida", List.of(new InvalidParam("address.state", "uf desconhecida")))`. Entrada nula é erro de programação (a `web` exige o objeto): dereferencia, sem checagem explícita.
- `RegisterLocation.Params(Actor actor, UUID customerId, String name, AddressInput address)` → `UUIDResponse`: `requireEditor` → `LocationName.of` + `AddressParser.parse` → `customer_not_found` → `Location.register` (`customer_deactivated`) → `save`.
- `UpdateLocation.Params(Actor actor, UUID locationId, String name, AddressInput address)`: `requireEditor` → VOs → `NotFoundException("location_not_found", "local não encontrado")` → `edit` (`location_deactivated`) → `save`. O cliente **não** é lido.
- `DeactivateLocation.Params(Actor actor, UUID locationId)`, `ReactivateLocation.Params(...)`.

- [ ] **Step 1: Testes que falham:** `registersForAnActiveCustomer`; `technicianIsForbiddenAndTheRepositoriesAreUntouched`; **`invalidZipCodeOfAnUnknownCustomerIsABadRequest`** (ordem); `zipCodeWithAndWithoutDashStoresTheSameDigits`; `zipCodeWith7DigitsIsABadRequest`; `unknownStateIsABadRequestNamingTheParam`; `nullComplementIsAbsent`; `registerForADeactivatedCustomerFails` (nada salvo); **`updateOfALocationOfADeactivatedCustomerSaves`** (ADR-30; `CustomerRepository` intocado); `updateOfADeactivatedLocationFails`; **`deactivateAndReactivateALocationOfADeactivatedCustomer`**; `notFound`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the location commands`.

### Tarefa 7: Commands de equipamento instalado

**Files:** `usecases/commands/{InstallEquipment,UpdateInstalledEquipment,DeactivateInstalledEquipment,ReactivateInstalledEquipment}.java`; testes.

**Interfaces:**
- Consumes: Tarefa 5 (repositories, `CustomerAccess`); `EquipmentModelRepository` do catálogo; `java.time.Clock` por construtor nos dois primeiros.
- `InstallEquipment(InstalledEquipmentRepository repository, LocationRepository locationRepository, CustomerRepository customerRepository, EquipmentModelRepository modelRepository, Clock clock)`; `Params(Actor actor, UUID locationId, UUID modelId, String serialNumber, LocalDate installedOn)` → `UUIDResponse`. Fluxo: `requireEditor` → `SerialNumber.of`, `InstallationDate.of(installedOn, clock)` se não nulo → local (`location_not_found`) → modelo (`NotFoundException("model_not_found", "modelo não encontrado")`) → cliente do local (`customer_not_found`; a FK garante que existe) → `InstalledEquipment.install` → `existsByModelAndSerialNumber(modelId, serial, null)` → `ConflictException("serial_number_taken", "este modelo já tem um equipamento com esse número de série")` → `save`.
- `UpdateInstalledEquipment(InstalledEquipmentRepository repository, Clock clock)`; `Params(Actor actor, UUID equipmentId, String serialNumber, LocalDate installedOn)`: `requireEditor` → VOs → `NotFoundException("installed_equipment_not_found", "equipamento não encontrado")` → `edit` → `existsByModelAndSerialNumber(model, serial, equipmentId)` → `save`. Local, cliente e modelo **não** são lidos.
- `DeactivateInstalledEquipment.Params(Actor actor, UUID equipmentId)`, `ReactivateInstalledEquipment.Params(...)`.

- [ ] **Step 1: Testes que falham:** `installsAndReturnsTheId`; `technicianIsForbidden`; **`futureDateOfAnUnknownLocationIsABadRequest`** (`invalid_installation_date`, nenhum repository lido); `todayIsAccepted`; `unknownModelIsModelNotFound`; `deactivatedLocationFails`; **`activeLocationOfADeactivatedCustomerFails`**; `deactivatedModelFails` (nada salvo); **`repeatedSerialIsCheckedAfterTheActiveRules`** (modelo desativado e série repetida → `model_deactivated`); `repeatedSerialIsAConflict`; **`updateToItsOwnSerialInAnotherCaseDoesNotConflictWithItself`** (`exceptId` = o próprio id); `updateToASerialOfAnotherEquipmentIsAConflict`; **`updateOfAnEquipmentInADeactivatedLocationSaves`** (ADR-30); `updateOfADeactivatedEquipmentFails`; desativar e reativar.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the installed equipment commands`.

### Tarefa 8: Reader, schemas e queries

**Files:** `contracts/readers/CustomerReader.java`; `schemas/{CustomerStatusFilter,CustomerFilter,CustomerSummary,CustomerDetail}.java`; `usecases/queries/{ListCustomers,GetCustomer}.java`; testes.

**Interfaces (produz):**
- `enum CustomerStatusFilter {ACTIVE, DEACTIVATED, ALL}` com `static orActive(CustomerStatusFilter)` (próprio do `customers`, como o `CustomerStatus`: não reaproveita o `StatusFilter` do catálogo).
- `record CustomerFilter(String text, CustomerStatusFilter status)` (status nulo vira `ACTIVE` no construtor compacto).
- `record CustomerSummary(UUID id, String name, String tradeName, String document, Document.DocumentType documentType, CustomerStatus status, long locationCount, long equipmentCount, List<CityView> cities, List<MatchedSerial> matchedSerials)`, com `record CityView(String city, UF state)` e `record MatchedSerial(String serialNumber, String modelName)`. **Sem e-mail e telefone** (ADR-29).
- `record CustomerDetail(UUID id, String document, Document.DocumentType documentType, String name, String tradeName, String email, String phone, CustomerStatus status, List<LocationView> locations)`; `LocationView(UUID id, String name, CustomerStatus status, AddressView address, List<EquipmentView> equipment)`; `AddressView(String zipCode, String street, String number, String complement, String neighborhood, String city, UF state)`; `EquipmentView(UUID id, String serialNumber, LocalDate installedOn, CustomerStatus status, ModelRef model, Ref manufacturer, Ref type, CharacteristicView characteristic)`; `ModelRef(UUID id, String name, CatalogStatus status)`; `Ref(UUID id, String name)`; `CharacteristicView(String name, MeasurementUnit unit, BigDecimal amount)` (nulo quando o modelo não tem valor).
- `CustomerReader`: `Page<CustomerSummary> search(CustomerFilter filter, Pageable pageable)`; `Optional<CustomerDetail> findDetail(UUID customerId)`.
- `ListCustomers.Params(Actor actor, CustomerFilter filter, Pageable pageable)` e `GetCustomer.Params(Actor actor, UUID customerId)` (`customer_not_found`): `@UseCase(transactional = false)`, `CustomerAccess.requireReader` antes do reader.

- [ ] **Step 1: Testes que falham:** `eachQueryAnswersTheThreeRoles` (o reader é chamado para Administrador, Comercial e Técnico); `nullStatusFilterReachesTheReaderAsActive`; `getCustomerNotFound`; `nullActorFailsBeforeTheReader` (`assertThatThrownBy(...)` qualquer exception, `verifyNoInteractions(reader)`; sem NPE explícita no código).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the customers queries`.

---

## Infraestrutura (`registry-infrastructure`)

Pacote base `com.crm.registry.infrastructure.customers`, com os subpacotes do catálogo (`mappers`, `persistence.entities`, `persistence.jpa`, `repositories`, `readers`). Siga a skill `crm-add-persistence` (menos o prefixo `Jpa`, ver Global Constraints) e o catálogo como referência.

### Tarefa 9: Migration, entidades, mappers e repositories

**Files:**
- Create: `src/main/resources/db/migration/registry/V2__create_customers.sql`
- Create: `persistence/entities/{CustomerEntity,LocationEntity,InstalledEquipmentEntity}.java`, `mappers/{CustomerMapper,LocationMapper,InstalledEquipmentMapper}.java`, `persistence/jpa/{CustomerJpaRepository,LocationJpaRepository,InstalledEquipmentJpaRepository}.java`, `repositories/{CustomerRepositoryImpl,LocationRepositoryImpl,InstalledEquipmentRepositoryImpl}.java`
- Modify: `infrastructure/catalog/repositories/UniqueConstraint.java` (classe e `isViolated` passam a `public`, sem outra mudança); `infrastructure/config/RegistryBeans.java` (mappers e os três repositories, numa seção "clientes: persistência")
- Test: `customers/mappers/*MapperTest.java` (unitário) e `customers/persistence/CustomersPersistencePostgresTest.java` (Testcontainers, no molde do `CatalogPersistencePostgresTest`; `@EntityScan(basePackageClasses = {ManufacturerEntity.class, CustomerEntity.class})` e `@EnableJpaRepositories(basePackageClasses = {ManufacturerJpaRepository.class, CustomerJpaRepository.class})`, porque a FK e os testes usam o catálogo)

**Interfaces:**
- Consumes: contratos da Tarefa 5; aggregates das Tarefas 2 a 4.
- Migration exatamente a tabela de `domain-model.md` (Persistência): `customers`, `locations`, `installed_equipment`, **cada uma** com `id UUID PRIMARY KEY` sem default, `version BIGINT NOT NULL`, `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ` **nulável**; `CREATE UNIQUE INDEX uk_customers_document ON customers (document)`; `CREATE UNIQUE INDEX uk_installed_equipment_model_serial ON installed_equipment (model_id, lower(serial_number))`; FKs `locations.customer_id → customers`, `installed_equipment.location_id → locations`, `installed_equipment.model_id → equipment_models`; índices `ix_locations_customer`, `ix_installed_equipment_location`. Comentário de cabeçalho como o da V1.
- **Uma entidade por tabela, nenhuma filha** (`domain-model.md`): o endereço são colunas de `LocationEntity` (`zipCode`, `street`, `number`, `complement`, `neighborhood`, `city`, `state` como `UF` em `@Enumerated(STRING)`). Assim o `save` do `RepositoryImpl` confere só a versão do próprio aggregate e não existe o problema de filha versionada que o `EquipmentTypeRepositoryImpl` teve de contornar. `zip_code char(8)` e `state char(2)`: mapeie com `@JdbcTypeCode(SqlTypes.CHAR)`; se o `ddl-auto=validate` recusar, **pare e sinalize** (não troque o tipo da coluna em silêncio: é a spec).
- Mappers levam a versão e nunca `createdAt`/`updatedAt`; opcional ausente vira coluna nula e volta como ausente.
- `CustomerJpaRepository.existsByDocument(String document)`: `select count(c) > 0 from CustomerEntity c where c.document = :document`. `InstalledEquipmentJpaRepository.existsByModelAndSerialNumber(UUID modelId, String serial, UUID exceptId)`: `e.modelId = :modelId and lower(e.serialNumber) = lower(:serial) and (:exceptId is null or e.id <> :exceptId)`.
- `CustomerRepositoryImpl` e `InstalledEquipmentRepositoryImpl` sobrescrevem `translatingConflicts` como o `ManufacturerRepositoryImpl`: `uk_customers_document` → `ConflictException("customer_document_taken", ...)`; `uk_installed_equipment_model_serial` → `ConflictException("serial_number_taken", ...)`; o resto sobe como veio.

- [ ] **Step 1: Testes que falham:** `*MapperTest` (ida e volta preserva todos os campos e a versão; versão nula = linha nova; opcionais nulos voltam ausentes; complemento ausente). `CustomersPersistencePostgresTest`: `savesAndLoadsTheThreeAggregates` (versão 0, editar sobe para 1); **`sameDocumentInAnotherFormattingIsAConflictWithItsCode`**; **`twoRegistrationsOfTheSameDocumentAtTheSameTimeLeaveOneAndConflictTheOther`** (duas threads com `"529.982.247-25"` e `"52998224725"`, `CountDownLatch` como no catálogo: um vencedor, uma `ConflictException` com `customer_document_taken`, nunca outra exception, uma linha); **`sameSerialInAnotherCaseForTheSameModelIsAConflict`** e `...TwoAtTheSameTime...`; `sameSerialForAnotherModelIsAllowed`; **`existsByModelAndSerialNumberWithNullExceptIdWorksOnPostgres`** e `...IgnoresTheExceptedId`; **`staleEditOfACustomerIsAConcurrentUpdate`**, `...OfALocation...`, `...OfAnInstalledEquipment...` (duas cópias lidas, a segunda salva antes, a primeira → `ConcurrentUpdateException`, o dado gravado é o da segunda); `deactivatedRowsStillHoldTheDocumentAndTheSerial`.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/registry/registry-infrastructure -am test -Dtest='*MapperTest'` → FAIL; o `PostgresTest` exige Docker.
- [ ] **Step 3: Implementar.** O erro entre migration e entity só aparece com o banco de pé.
- [ ] **Step 4: Rodar e ver passar**, com Docker: `mvn -pl core/registry/registry-infrastructure -am test -Dtest='CustomersPersistencePostgresTest,CatalogPersistencePostgresTest'` (o catálogo continua verde com a V2).
- [ ] **Step 5: Commit:** `feat(registry): add the customers persistence`.

### Tarefa 10: Reader da lista e da ficha

**Files:**
- Create: `customers/readers/CustomerReaderImpl.java`
- Modify: `infrastructure/catalog/readers/CatalogQueries.java` (classe e `hasText`, `containing`, `where`, `orderBy`, `page` passam a `public`; `filterByStatus` continua do catálogo); `infrastructure/config/RegistryBeans.java` (bean `CustomerReader`)
- Test: `customers/readers/CustomersReaderPostgresTest.java` (mesma configuração da Tarefa 9; pode ser a mesma classe se ficar legível)

**Interfaces:**
- Consumes: `CustomerReader` e schemas (Tarefa 8); entidades da Tarefa 9 e do catálogo (`EquipmentModelEntity`, `ManufacturerEntity`, `EquipmentTypeEntity`, `CharacteristicDefinitionEntity`).
- **`search`**: uma consulta paginada sobre `CustomerEntity` com `locationCount` (subconsulta: locais ativos) e `equipmentCount` (subconsulta: equipamentos ativos em locais ativos); a situação filtra `c.status` (`ALL` não filtra); com texto (`CatalogQueries.containing`, a mesma busca do catálogo: ADR-15): `lower(c.name) like`, `lower(c.tradeName) like`, `exists` de local **ativo** com `lower(l.city) like`, `exists` de equipamento **ativo** (pelos locais do cliente) com `lower(e.serialNumber) like`, e `c.document like :document` só quando o texto, sem o que não é letra ou dígito e em maiúsculas, não fica vazio. Ordem por `CatalogQueries.orderBy("c", pageable)` (nome, depois id). Contagem total com o mesmo `where`. **Nenhuma junção** na consulta paginada (as linhas não se multiplicam).
- Depois, para os ids da página: as cidades (`select distinct l.customerId, l.city, l.state ... where l.status = ACTIVE ... order by l.city`) e, **só com texto**, as séries casadas (equipamento ativo, `lower(e.serialNumber) like :text`, com `EquipmentModelEntity.name`, por série), cortadas em **três** por cliente (ADR-27). `documentType` vem de `Document.of(document).getType()`.
- **`findDetail`**: o cliente; os locais (`order by case when l.status = ACTIVE then 0 else 1 end, lower(l.name)`); os equipamentos dos locais com modelo, fabricante, tipo e o valor da característica por `left join` dos valores (`order by` ativos primeiro, `lower(t.name)`, `lower(m.name)`, `e.serialNumber`), agrupados por local.

- [ ] **Step 1: Testes que falham:** `searchByNameTradeNameCityDocumentAndSerial` (cada um acha o cliente; `"123.456"` acha pelo documento; `%` e `_` literais); `searchIgnoresTheCityOfADeactivatedLocation` e `...TheSerialOfADeactivatedEquipment`; `sameSerialInTwoModelsOfTwoCustomersReturnsBoth`; **`matchedSerialsHasAtMostThreeInSerialOrderWithTheModelName`**; **`matchedSerialsIsEmptyWithoutTextOrWhenTheTextMatchedTheName`**; **`aCustomerWithManyLocationsIsOneRowAndTheTotalIsRight`** (paginação); `countsOnlyActiveLocationsAndActiveEquipmentInActiveLocations`; `citiesAreDistinctAndOnlyFromActiveLocations`; `statusFilterAllAndDefault`; `detailBringsDeactivatedLocationsAndEquipmentInTheSpecOrder`; `detailOfAModelWithoutValueHasNoCharacteristic`; `detailNotFoundIsEmpty`.
- [ ] **Step 2: Rodar e ver falhar** (com Docker). **Step 3: Implementar.** **Step 4: Rodar e ver passar**, inclusive o `CatalogPersistencePostgresTest` (a visibilidade nova não muda o catálogo).
- [ ] **Step 5: Commit:** `feat(registry): add the customers reader`.

---

## Borda (`web`, `boot`)

### Tarefa 11: Beans dos use cases e `CustomersController`

**Files:**
- Modify: `infrastructure/config/RegistryBeans.java` (os 12 commands e as 2 queries, com `new`; `InstallEquipment` e `UpdateInstalledEquipment` recebem o `Clock` do `TimeConfig`)
- Create: `edge/web/src/main/java/com/crm/web/registry/CustomersController.java`; `web/registry/requests/{RegisterCustomerRequest,UpdateCustomerRequest}.java`
- Test: `edge/web/src/test/java/com/crm/web/registry/CustomersControllerTest.java` (com `IamMvc.standalone`)

**Interfaces:**
- Rotas de `usecases.md`: `POST /customers` (201 + `UUIDResponse`), `GET /customers` (`text`, `status` por `@ModelAttribute CustomerFilter`; `page` padrão 0, `size` padrão 20 e cortado em 1..100), `GET /customers/{id}` (`CustomerDetail`), `PUT /customers/{id}` (204), `POST /customers/{id}/deactivation` e `/reactivation` (204).
- `RegisterCustomerRequest(@NotBlank @Size(max = 18) String document, @NotBlank @Size(min = 2, max = 150) String name, @Size(min = 2, max = 150) String tradeName, @Size(max = 254) String email, @Size(max = 16) String phone)`; `UpdateCustomerRequest` igual **sem** `document` (o campo no corpo é ignorado). Mensagens do Bean Validation como as do catálogo ("é obrigatório", "deve ter de 2 a 150 caracteres").
- O controller só monta `Params` e chama o use case, com o `Actor` do `ActorArgumentResolver`; **nenhum import de `registry-domain`** e nenhuma checagem de papel.

- [ ] **Step 1: Testes que falham:** `registerRespondsCreatedWithTheId` (o `Params` chega com os cinco campos, opcionais `null`); `updateRespondsNoContentAndIgnoresADocumentInTheBody`; `deactivateAndReactivateRespondNoContent`; `listPassesTextStatusPageAndSize` (e `size=500` vira 100, `size=0` vira 1); **`unknownStatusIsA400`** (`status=FOO`); `emptyNameIsA400WithInvalidParams`; `nameWith151CharactersIsA400`; `domainErrorsKeepTheirCode` (`customer_document_taken` → 409 com `code`); `detailRespondsTheDetail`; `withoutActorIsA401`.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl edge/web -am test -Dtest='CustomersControllerTest'`. **Step 3: Implementar.** **Step 4: Rodar e ver passar**, e `BoundedContextRulesTest`.
- [ ] **Step 5: Commit:** `feat(web): add the customers controller`.

### Tarefa 12: Controllers de local e de equipamento instalado

**Files:**
- Create: `web/registry/{LocationsController,InstalledEquipmentController}.java`; `web/registry/requests/{LocationRequest,AddressRequest,InstallEquipmentRequest,UpdateInstalledEquipmentRequest}.java`
- Test: `LocationsControllerTest`, `InstalledEquipmentControllerTest`

**Interfaces:**
- Consumes: beans da Tarefa 11.
- `LocationsController`: `POST /customers/{customerId}/locations` (201), `PUT /locations/{id}` (204), `POST /locations/{id}/deactivation` e `/reactivation` (204). `LocationRequest(@NotBlank @Size(min = 2, max = 60) String name, @NotNull @Valid AddressRequest address)`; `AddressRequest(@NotBlank @Size(max = 9) String zipCode, @NotBlank @Size(min = 2, max = 150) String street, @NotBlank @Size(max = 15) String number, @Size(min = 1, max = 50) String complement, @NotBlank @Size(min = 2, max = 100) String neighborhood, @NotBlank @Size(max = 100) String city, @NotBlank @Size(min = 2, max = 2) String state)`; o controller converte em `AddressInput`.
- `InstalledEquipmentController`: `POST /locations/{locationId}/equipment` (201), `PUT /installed-equipment/{id}` (204), `POST /installed-equipment/{id}/deactivation` e `/reactivation` (204). `InstallEquipmentRequest(@NotNull UUID modelId, @NotBlank @Size(max = 60) String serialNumber, LocalDate installedOn)`; `UpdateInstalledEquipmentRequest(@NotBlank @Size(max = 60) String serialNumber, LocalDate installedOn)`; a data é `yyyy-MM-dd` (texto inválido é 400 "corpo da requisição inválido").
- Uma classe de `web` com use cases do `customers` e do `catalog` seria permitida (mesmo BC, ADR-8), mas nenhuma destas precisa.

- [ ] **Step 1: Testes que falham:** por controller, `registerRespondsCreatedWithTheId`, `updateRespondsNoContent`, `deactivateAndReactivateRespondNoContent`, `domainErrorsKeepTheirCode` (`serial_number_taken` → 409; `location_deactivated` → 422); `addressMissingIsA400WithInvalidParams` (`address`); `stateWith3LettersIsA400`; `zipCodeWith10CharactersIsA400`; **`invalidDateIsA400`** (`"2026-13-40"`); `nullDateReachesTheUseCaseAsNull`; `withoutActorIsA401`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar**, e `BoundedContextRulesTest`.
- [ ] **Step 5: Commit:** `feat(web): add the location and installed equipment controllers`.

### Tarefa 13: Ponta a ponta e fechamento

**Files:**
- Create: `edge/boot/src/test/java/com/crm/CustomersEndToEndTest.java`; `edge/boot/src/test/java/com/crm/EndToEndAccounts.java` (extrai o `adminBrowser` e o `technicianBrowser` do `RegistryEndToEndTest`, que passa a usá-lo sem mudar de comportamento)
- Modify: `RegistryEndToEndTest.java` (só o uso do auxiliar); `docs/system-design/03-customers/README.md` (critério de aceite do backend)

- [ ] **Step 1: Escrever o teste** (`@SpringBootTest` com Postgres e Redis do Testcontainers, como o `RegistryEndToEndTest`; exige Docker): o Administrador entra; cria fabricante, tipo e modelo pelo catálogo; `POST /customers` com `"529.982.247-25"` → 201; **`"52998224725"` → 409 `customer_document_taken`** (a mensagem não tem o documento); sem contato → 422 `contact_required`; CPF com nome fantasia → 422 `trade_name_not_allowed`; `PUT /customers/{id}` com outro `document` no corpo → 204 e o documento não muda; local com CEP `"01310-100"` → 201 e a ficha devolve `01310100`; equipamento com série `"HV2240187563"` → 201; a mesma série em minúsculas no mesmo modelo → 409 `serial_number_taken`; `GET /customers?text=hv2240` acha o cliente com `matchedSerials[0].serialNumber`; desativar o cliente tira da lista padrão e `status=ALL` o mostra; o local do cliente desativado se edita (204) e local novo nele → 422 `customer_deactivated`; **o Técnico** (convidado pelo IAM) lista e abre a ficha (200) e recebe 403 `forbidden` ao cadastrar; **sem sessão → 401**; **`status=FOO` → 400**; **cadastro simultâneo** de dois clientes com o mesmo documento por duas threads HTTP → um 201 e um 409 com o código, nunca 500.
- [ ] **Step 2: Rodar e ver falhar,** depois ajustar até passar: `mvn -pl edge/boot -am test -Dtest=CustomersEndToEndTest`.
- [ ] **Step 3: Rodar a suíte inteira** (`mvn test`, JDK 21, com Docker): verde, inclusive `BoundedContextRulesTest`, `ClockRulesTest`, `FlywayConfigTest`, `IamEndToEndTest` e `RegistryEndToEndTest`. Sem Docker, rode com `-Dtest='!*PostgresTest,!IamEndToEndTest,!RegistryEndToEndTest,!CustomersEndToEndTest'` e **diga no PR** que os testes com Docker não rodaram (e não marque o critério de aceite).
- [ ] **Step 4:** marcar no `README` da feature só o critério do `mvn test` e conferir com `git status` que só entraram arquivos desta feature.
- [ ] **Step 5: Commit:** `test(boot): cover the customers end to end`.

---

## Notas de consistência com a spec

Conferido antes de gravar: cada tarefa consome só assinaturas de tarefas anteriores (a `RegistryBeans` muda de lugar na Tarefa 0, antes de qualquer bean novo; os três repositories nascem juntos na Tarefa 5 porque as Tarefas 6 e 7 os usam; o reader nasce na 8 e se implementa na 10; os beans dos use cases entram na 11, antes dos controllers). Onde o plano segue a spec contra o costume do catálogo:

- **Formato antes de existe:** o `RegisterEquipmentModel` do catálogo lê o repository antes de montar o `ModelName`; aqui todo command monta os VOs antes do primeiro `findById`, como o `usecases.md` manda, e há teste da ordem em cada um.
- **Colunas padrão:** as três tabelas são aggregates e levam `id`, `version`, `created_at`, `updated_at` (§8); não há tabela de chave composta nem filha versionada nesta feature.
- **Ator nulo:** nenhuma checagem explícita nem `NullPointerException` lançada; o `CustomerAccess` dereferencia o ator, como o `CatalogAccess`.
- **Dono do local no `install`:** sem código público (`domain-model.md`), então `DomainException` só com mensagem (422 sem `code`), e não um código inventado como o `type_mismatch` do catálogo.
- **`contact_required` antes de `trade_name_not_allowed`:** a spec não ordena os dois (ambos 422); o plano fixa esta ordem e a testa.
- **Opcional em branco:** 400 do VO, não ausência (ADR-31); o plano não converte `""` em `null`.
- **`CustomerStatusFilter` próprio:** a spec dá ao `customers` um enum de situação próprio; o filtro segue a mesma lógica em vez de reaproveitar o `StatusFilter` do catálogo.
- **`CatalogQueries` e `UniqueConstraint` públicos:** o ADR-15 manda buscar "como o catálogo faz hoje" e deixar a mudança de acento num lugar só; a visibilidade é a menor mudança que cumpre isso.
- **`char(8)` e `char(2)`:** a spec fixa os tipos; o plano os mapeia em vez de trocá-los, e manda parar se o Hibernate recusar.
- **`RegistryBeans` fora do `catalog` (Tarefa 0):** o §9 do `architecture.md` pede um arquivo de beans por BC e a regra de subdomínio proíbe `catalog → customers`; com a classe dentro de `catalog.config` as duas não cabem juntas, e o plano a sobe para o nível do BC, como a `IamBeans`, pedindo a confirmação do usuário.
- **Nomes sem `Jpa`:** o código depois do PR #7 usa `<Nome>RepositoryImpl` e `<Nome>ReaderImpl`; o `architecture.md` §10.1 e as skills ainda dizem `Jpa<Nome>RepositoryImpl`. O plano segue o código e deixa a correção da regra para o usuário.
