# Catálogo (backend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o BC `registry` com o subdomínio `catalog` no backend: fabricante, tipo (com a característica) e modelo, com cadastro, edição, desativação, listagens, permissões por papel e carga inicial por seeder.

**Architecture:** Três aggregates (`Manufacturer`, `EquipmentType` com `CharacteristicDefinition` interna, `EquipmentModel`) em `registry-domain`; use cases concretos em `registry-application`; persistência, readers e seeder em `registry-infrastructure`; três controllers em `web`. O subdomínio é o primeiro pacote de cada camada (`architecture.md` §4.4). Antes do BC, `Role` e `Actor` sobem para o `shared` (Tarefa 0) para o catálogo receber quem age sem depender do IAM.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Data JPA, Flyway, Postgres, JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit.

**Spec:** [`docs/system-design/02-catalog/`](../system-design/02-catalog/README.md): `domain-model.md`, `usecases.md`, `decisions.md` (ADR-1 a ADR-10), mockups em `design/`. Regras: [`architecture.md`](../rules/architecture.md) §3.1, §4.4. **O plano não redefine o domínio**: se uma tarefa pedir mudança nele, volte à spec.

**Escopo:** só o **backend**. O frontend tem plano próprio ([`2026-11-21-catalog-frontend.md`](2026-11-21-catalog-frontend.md)).

## Global Constraints

- **JDK 21**; tudo roda a partir de `backend/`; critério de aceite de cada tarefa: `mvn test -Dtest='!*PostgresTest,!IamEndToEndTest,!RegistryEndToEndTest'` verde, e `mvn test` verde onde houver Docker (skill `crm-build-and-test`).
- `domain` e `application` são Java puro; a única anotação de framework em `application` é `@UseCase`; Bean Validation só em schemas de request (e fica em `web`, como no IAM). Nada de `@Component`/`@Service`: beans na `RegistryBeans` com `new` (§9).
- Pacotes: `com.crm.registry.<camada>.catalog...` (§4.4). `RegistryBeans` fica em `com.crm.registry.infrastructure`. Migrations em `registry-infrastructure/src/main/resources/db/migration/registry/`.
- Estilo ([`code-style.md`](../rules/code-style.md)): nada de `else`; `if` de uma instrução sem chaves; `catch` em linha própria; 3+ parâmetros um por linha; identificadores em inglês; comentário, mensagem de exceção, log e `@DisplayName` em pt-BR, mensagem minúscula e sem ponto final; toda classe de produção com Javadoc; nenhuma exception nativa.
- Valores da spec: `ManufacturerName` 2 a 80; `TypeName` 2 a 60; `ModelName` 1 a 80; `CharacteristicName` 2 a 40; `CharacteristicAmount` maior que zero, até 3 casas, até 9 dígitos inteiros (`numeric(12,3)`); `MeasurementUnit`: `KILOWATT`, `WATT`, `WATT_PEAK`, `BTU_PER_HOUR`; `CatalogStatus`: `ACTIVE`, `DEACTIVATED`; exatamente **uma** `CharacteristicDefinition` por tipo.
- Papéis: Administrador e Comercial escrevem (`forbidden`, 403, para o Técnico); os três consultam.
- Códigos de erro `snake_case`, exatamente os da tabela de `usecases.md`.
- Commit: Conventional Commits, escopo `registry` (ou `shared`, `iam`, `web`, `boot`), em inglês, **sem `Co-Authored-By`** ([`git-workflow.md`](../rules/git-workflow.md) §3).
- Branch: `feature/catalog-backend`. A base é `develop`, que não tem o IAM nem a spec; **pergunte a base** (hoje a opção é o branch `docs/catalog-spec`, que traz as duas coisas) antes da Tarefa 0.

## Review Focus

1. **Dois cadastros do mesmo nome ao mesmo tempo:** o `UNIQUE` do banco segura, e a resposta é 409, nunca 500. → Tarefa 9.
2. **Mesmo nome em outra caixa ("huawei" × "Huawei"):** `manufacturer_name_taken`; e **renomear para o próprio nome** (ou só trocar a caixa) não conflita consigo mesmo. → Tarefa 6.
3. **Tipo com modelos:** trocar a unidade ou ligar `required` falha com `characteristic_locked` e **não grava nada**; desligar `required` e renomear a característica passam. → Tarefa 7.
4. **Valor do modelo inválido:** zero, negativo, 4 casas decimais, definição de outro tipo, definição repetida, definição obrigatória ausente. → Tarefas 2, 4 e 5.
5. **Técnico escrevendo:** os 12 commands respondem `forbidden` e não tocam o repository; as 4 queries respondem aos três papéis. → Tarefas 6 a 8.

---

## Pré-requisito

### Tarefa 0: `Role` e `Actor` vão para o `shared`

[`architecture.md`](../rules/architecture.md) §3.1: o `Role` sobe para `shared-domain` e o `Actor` para `shared-application`. Refactor mecânico: o comportamento do IAM não muda.

**Files:**
- Move: `core/iam/iam-domain/.../vos/Role.java` → `kernel/shared/shared-domain/src/main/java/com/crm/shared/domain/enums/Role.java` (pacote `com.crm.shared.domain.enums`)
- Move: `core/iam/iam-application/.../application/Actor.java` → `kernel/shared/shared-application/src/main/java/com/crm/shared/application/Actor.java` (pacote `com.crm.shared.application`)
- Modify: todo `import com.crm.iam.domain.vos.Role` e `com.crm.iam.application.Actor` em `backend/` (main, test e Javadoc: ver `grep -rl`); `edge/web/pom.xml` (dependência explícita em `shared-application`)
- Test: `kernel/shared/shared-application/src/test/java/com/crm/shared/application/ActorTest.java`

**Interfaces:**
- Produces: `com.crm.shared.domain.enums.Role {ADMINISTRATOR, COMMERCIAL, TECHNICIAN}` (igual ao de hoje). `com.crm.shared.application.Actor(UUID userId, Role role, UUID sessionId)` com `void requireAnyRole(Role... allowed)` (lança `ForbiddenException("forbidden", "você não tem permissão para isto")` se o papel não está na lista) e `void requireAdministrator()` (atalho de `requireAnyRole(Role.ADMINISTRATOR)`).

- [ ] **Step 1: Teste que falha** (`ActorTest`, JUnit puro): `requireAnyRoleAllowsAListedRole` (Comercial passa em `requireAnyRole(ADMINISTRATOR, COMMERCIAL)`); `requireAnyRoleRefusesAnotherRole` (Técnico lança `ForbiddenException` com `getCode()` igual a `forbidden`); `requireAdministratorRefusesCommercial`. O `pom.xml` de `shared-application` já tem JUnit; adicione `assertj-core` em `test` se faltar.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl kernel/shared/shared-application -am test -Dtest=ActorTest` → FAIL (classe inexistente).
- [ ] **Step 3: Mover** os dois arquivos com `git mv`, trocar o `package`, e acrescentar `requireAnyRole`. Atualizar os imports com `grep -rl` e `sed` sobre `backend/`; incluir no `edge/web/pom.xml` a dependência `shared-application` (hoje só chega por transitividade). Nenhuma outra mudança de comportamento.
- [ ] **Step 4: Rodar tudo:** `mvn test -Dtest='!*PostgresTest,!IamEndToEndTest'` → PASS (o IAM inteiro continua verde), e `BoundedContextRulesTest` verde.
- [ ] **Step 5: Commit:** `refactor(shared): move the role and the actor to the shared kernel`.

---

## BC e domínio (`registry-domain`)

### Tarefa 1: Esqueleto do BC `registry` e regra de ArchUnit dos subdomínios

**Files:**
- Create: `core/registry/pom.xml` (agregador, cópia de `core/iam/pom.xml`), `core/registry/registry-{domain,application,infrastructure}/pom.xml` (dependências da skill `crm-add-bounded-context`; `registry-application` também depende de `shared-application`)
- Modify: `core/pom.xml` (`<module>registry</module>`), `edge/boot/pom.xml` (`registry-infrastructure`), `edge/web/pom.xml` (`registry-application`)
- Modify: `edge/boot/src/test/java/com/crm/BoundedContextRulesTest.java`
- Create: `core/registry/registry-infrastructure/src/main/java/com/crm/registry/infrastructure/RegistryBeans.java` (vazia por ora, `@Configuration`, Javadoc)

**Interfaces:**
- Produces: a regra `subdomainsDependOnlyInDeclaredDirection` e o mapa `SUBDOMAIN_DEPENDENCIES` (`"registry.catalog" → {}`, `"registry.customers" → {"registry.catalog"}`) no teste. Para cada par de subdomínios do mesmo BC cuja direção **não** está no mapa, `noClasses().that().resideInAPackage("com.crm.registry.*.<a>..").should().dependOnClassesThat().resideInAPackage("com.crm.registry.*.<b>..")`, pelo mesmo `check(...)` das outras regras (que já aceita regra sem classe).

- [ ] **Step 1: Escrever a regra** no `BoundedContextRulesTest` (`@DisplayName("subdomínios do mesmo BC só dependem na direção declarada")`, método `subdomainsDependOnlyInDeclaredDirection`), montando as proibições a partir do mapa.
- [ ] **Step 2: Rodar:** `mvn -pl edge/boot -am test -Dtest=BoundedContextRulesTest` → PASS (o BC ainda não tem classes; a regra roda vazia, como as demais).
- [ ] **Step 3: Criar os poms** e registrar os módulos; `mvn -q -DskipTests compile` na raiz do `backend/` compila. Não crie pasta de migration vazia (o Flyway só vê pasta com `.sql`).
- [ ] **Step 4: Rodar:** `mvn test -Dtest='!*PostgresTest,!IamEndToEndTest'` → PASS.
- [ ] **Step 5: Commit:** `feat(registry): add the registry bounded context skeleton and the subdomain rule`.

### Tarefa 2: Value Objects e enums

**Files:** `registry-domain/src/main/java/com/crm/registry/domain/catalog/vos/{ManufacturerName,TypeName,ModelName,CharacteristicName,CharacteristicAmount,CharacteristicValue,MeasurementUnit,CatalogStatus}.java`; testes `.../vos/*Test.java`. O `pom.xml` precisa de `assertj-core` em `test`.

**Interfaces (produz):**
- `ManufacturerName.of(String)`, `TypeName.of(String)`, `ModelName.of(String)`, `CharacteristicName.of(String)`: `extends StringValueObject`, com os limites do Global Constraints (nulo, vazio e fora do limite recusados com `ValidationException`, mensagem em pt-BR minúsculo).
- `CharacteristicAmount.of(BigDecimal)` e `of(String)`: `extends BigDecimalValueObject`; recusa com `ValidationException("invalid_amount", "o valor precisa ser maior que zero, com até 3 casas decimais")` zero, negativo, mais de 3 casas e mais de 9 dígitos inteiros.
- `record CharacteristicValue(UUID definitionId, CharacteristicAmount amount)` (recusa `definitionId` nulo).
- Enums `MeasurementUnit {KILOWATT, WATT, WATT_PEAK, BTU_PER_HOUR}` e `CatalogStatus {ACTIVE, DEACTIVATED}`.

- [ ] **Step 1: Testes que falham**, um `@DisplayName` em pt-BR por linha: nomes aceitam o limite inferior e superior e recusam um a menos e um a mais (`"A"` para fabricante, 81 caracteres para modelo etc.); `CharacteristicAmount` aceita `"3"`, `"12000"`, `"0.001"`, `"999999999.999"` e recusa `"0"`, `"-1"`, `"1.2345"`, `"1000000000"`, e o texto não numérico; `CharacteristicValue` recusa id nulo.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/registry/registry-domain -am test` → FAIL.
- [ ] **Step 3: Implementar** (bases `StringValueObject` e `BigDecimalValueObject` de `common-domain`, como `UserName` e `Quantity`).
- [ ] **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the catalog value objects`.

### Tarefa 3: Aggregate `Manufacturer`

**Files:** `registry-domain/.../catalog/entities/Manufacturer.java`; teste `.../entities/ManufacturerTest.java`.

**Interfaces (produz):**
- `Manufacturer extends AggregateRoot`. `static Manufacturer register(ManufacturerName name)` (nasce `ACTIVE`); `static Manufacturer reconstitute(UUID id, Long version, ManufacturerName name, CatalogStatus status)`; `void rename(ManufacturerName)`, `void deactivate()`, `void reactivate()`; getters `getName()`, `getStatus()`, `isActive()`.
- Erros de `DomainException(code, mensagem)`: `rename` e `deactivate` fora de `ACTIVE` → `manufacturer_deactivated` ("este fabricante está desativado"); `reactivate` fora de `DEACTIVATED` → `manufacturer_not_deactivated` ("este fabricante não está desativado").

- [ ] **Step 1: Testes que falham:** `registersActive`; `renameChangesTheName`; `renameOfADeactivatedManufacturerFails` (`manufacturer_deactivated`); `deactivateTwiceFails`; `reactivateAnActiveOneFails` (`manufacturer_not_deactivated`); `reactivateBringsItBackToActive`; `reconstituteKeepsIdAndVersion`; nenhum registra domain event (`pullEvents()` vazio).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the Manufacturer aggregate`.

### Tarefa 4: Aggregate `EquipmentType` e `CharacteristicDefinition`

**Files:** `.../catalog/entities/{EquipmentType,CharacteristicDefinition}.java`; testes `EquipmentTypeTest`.

**Interfaces (produz):**
- `CharacteristicDefinition extends DomainEntity`: `static create(CharacteristicName, MeasurementUnit, boolean required)`; `static reconstitute(UUID id, CharacteristicName, MeasurementUnit, boolean required)`; getters.
- `EquipmentType extends AggregateRoot`: `static register(TypeName name, CharacteristicName characteristicName, MeasurementUnit unit, boolean required)`; `static reconstitute(UUID id, Long version, TypeName name, CatalogStatus status, List<CharacteristicDefinition> characteristics)` (recusa lista que não tenha **exatamente uma** definição); `void rename(TypeName)`; `void changeCharacteristic(CharacteristicName name, MeasurementUnit unit, boolean required, boolean hasModels)`; `deactivate()`, `reactivate()`; `void checkValues(List<CharacteristicValue> values)`; `getCharacteristic(): CharacteristicDefinition` (a única), `isActive()`.
- Erros: `type_deactivated`, `type_not_deactivated` (como no fabricante, "este tipo está desativado" etc.); `characteristic_locked` (422, "o tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar") quando `hasModels` e a unidade muda **ou** `required` passa de `false` para `true`; `checkValues`: valor cuja `definitionId` não é a do tipo, ou repetida → `characteristic_unknown` ("a característica informada não existe neste tipo"); definição `required` sem valor → `characteristic_required` ("informe a característica " + nome).

- [ ] **Step 1: Testes que falham:** `registersWithOneCharacteristic`; `changeCharacteristicWithoutModelsChangesUnitAndRequired`; **`changeCharacteristicWithModelsRefusesANewUnit`** e **`...RefusesTurningRequiredOn`** (`characteristic_locked`, e o estado do aggregate não muda); **`...WithModelsAcceptsANewNameAndTurningRequiredOff`**; `renameAndDeactivateOfADeactivatedTypeFail`; `reconstituteRefusesZeroOrTwoCharacteristics`; `checkValuesAcceptsTheRightDefinition`; `checkValuesRefusesAnotherDefinition`; `checkValuesRefusesARepeatedDefinition`; `checkValuesRefusesAMissingRequiredValue` (mensagem com o nome); `checkValuesAcceptsNoValueWhenNotRequired`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** (a definição é criada por `register` com `CharacteristicDefinition.create`; `changeCharacteristic` troca nome, unidade e `required` da definição existente, mantendo o id). **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the EquipmentType aggregate with its characteristic`.

### Tarefa 5: Aggregate `EquipmentModel`

**Files:** `.../catalog/entities/EquipmentModel.java`; teste `EquipmentModelTest`.

**Interfaces (produz):**
- `EquipmentModel extends AggregateRoot`: `static register(Manufacturer manufacturer, EquipmentType type, ModelName name, List<CharacteristicValue> values)`; `static reconstitute(UUID id, Long version, UUID manufacturerId, UUID typeId, ModelName name, List<CharacteristicValue> values, CatalogStatus status)`; `void edit(EquipmentType type, ModelName name, List<CharacteristicValue> values)`; `deactivate()`, `reactivate()`; getters (`getManufacturerId()`, `getTypeId()` imutáveis, `getValues()` devolve cópia imutável).
- Erros: `register` com fabricante desativado → `manufacturer_deactivated`, com tipo desativado → `type_deactivated`; `edit`/`deactivate` fora de `ACTIVE` → `model_deactivated` ("este modelo está desativado"); `reactivate` fora de `DEACTIVATED` → `model_not_deactivated`; `register` e `edit` chamam `type.checkValues(values)`; `edit` com um `type` cujo id não é o `typeId` do modelo → `DomainException` (código `type_mismatch`, "o tipo informado não é o deste modelo"; erro de programação, sem linha na tabela pública).

- [ ] **Step 1: Testes que falham:** `registersActiveWithTheValues`; `registerRefusesADeactivatedManufacturer`; `registerRefusesADeactivatedType`; `registerRefusesValuesThatDoNotMatchTheType` (propaga `characteristic_required`); `editChangesNameAndValuesButNeverManufacturerNorType`; `editOfADeactivatedModelFails`; `editAcceptsADeactivatedType` (só modelo **novo** é bloqueado); `editRefusesAnotherType`; `deactivateAndReactivate` com os dois erros; `getValuesCannotBeChanged`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the EquipmentModel aggregate`.

---

## Aplicação (`registry-application`)

Pacote base `com.crm.registry.application.catalog`. Testes em `core/registry/registry-application/src/test/java/...` com Mockito (repositories e readers `mock`), `Clock` não é usado (a feature não tem data de negócio). `pom.xml` de teste: `mockito`, `assertj-core`.

### Tarefa 6: Contratos, `CatalogAccess` e os commands de fabricante

**Files:**
- Create: `contracts/repositories/{ManufacturerRepository,ManufacturerCriteria,EquipmentTypeRepository,EquipmentTypeCriteria,EquipmentModelRepository,EquipmentModelCriteria}.java`; `CatalogAccess.java`; `usecases/commands/{RegisterManufacturer,RenameManufacturer,DeactivateManufacturer,ReactivateManufacturer}.java`
- Test: `usecases/commands/*Test.java`, `CatalogAccessTest.java`

**Interfaces (produz):**
- Repositories (`extends Repository<A, C>`, `Criteria` records vazios como o `UserCriteria`): `ManufacturerRepository.existsByName(ManufacturerName name, UUID exceptId)`, `EquipmentTypeRepository.existsByName(TypeName name, UUID exceptId)`, `EquipmentModelRepository.existsByManufacturerAndName(UUID manufacturerId, ModelName name, UUID exceptId)`. `exceptId` nulo = sem exceção; a comparação do nome ignora maiúsculas.
- `CatalogAccess.requireEditor(Actor actor)`: `actor.requireAnyRole(Role.ADMINISTRATOR, Role.COMMERCIAL)` (classe `final`, método estático, Javadoc dizendo que é a regra do ADR-6).
- Commands (`@UseCase`, `@RequiredArgsConstructor`, `record Params(Actor actor, ...)`, `execute(Params)`):
  - `RegisterManufacturer.Params(Actor actor, String name)` → `UUIDResponse`; `manufacturer_name_taken` (`ConflictException`, 409, "já existe um fabricante com este nome") se `existsByName(name, null)`.
  - `RenameManufacturer.Params(Actor actor, UUID manufacturerId, String name)`; `NotFoundException("manufacturer_not_found", "fabricante não encontrado")`; `existsByName(name, manufacturerId)`.
  - `DeactivateManufacturer.Params(Actor actor, UUID manufacturerId)` e `ReactivateManufacturer.Params(...)`.
  - Todos: `CatalogAccess.requireEditor` **antes** de tocar o repository; carregam, chamam o método de domínio, `save`. Sem evento.

- [ ] **Step 1: Testes que falham** (um por command): `registersAndReturnsTheId`; `technicianIsForbiddenAndTheRepositoryIsUntouched` (`forbidden`, `verifyNoInteractions`); `administratorAndCommercialCanWrite`; `refusesARepeatedNameInAnotherCase` (existsByName devolve `true` → `manufacturer_name_taken`, `save` nunca chamado); **`renameToTheSameNameDoesNotConflictWithItself`** (o repository é consultado com o `exceptId` do próprio fabricante); `notFound`; `deactivateAndReactivateSaveTheNewStatus`.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/registry/registry-application -am test` → FAIL. **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the manufacturer commands and the catalog access rule`.

### Tarefa 7: Commands de tipo

**Files:** `usecases/commands/{RegisterEquipmentType,UpdateEquipmentType,DeactivateEquipmentType,ReactivateEquipmentType}.java`, `MeasurementUnitParser.java` (pacote `usecases.commands`, `final`, como o `RoleParser`: nome exato do enum, senão `BadRequestException("unidade inválida", [InvalidParam("unit", "unidade desconhecida")])`); `contracts/readers/EquipmentModelReader.java`; testes.

**Interfaces:**
- Consumes: Tarefa 6 (`EquipmentTypeRepository`, `CatalogAccess`).
- Produz: `EquipmentModelReader.hasModelsOfType(UUID typeId): boolean` (o contrato completo do reader vem na Tarefa 8; aqui só este método, e a Tarefa 8 acrescenta o resto).
- `record CharacteristicInput(String name, String unit, boolean required)` (em `schemas`).
- `RegisterEquipmentType.Params(Actor actor, String name, CharacteristicInput characteristic)` → `UUIDResponse`; `type_name_taken` (409, "já existe um tipo com este nome").
- `UpdateEquipmentType.Params(Actor actor, UUID typeId, String name, CharacteristicInput characteristic)`: carrega, `reader.hasModelsOfType`, `rename`, `changeCharacteristic(..., hasModels)`, **um** `save` no fim; `type_not_found` (404).
- `DeactivateEquipmentType.Params(Actor, UUID typeId)`, `ReactivateEquipmentType.Params(...)`.

- [ ] **Step 1: Testes que falham:** os do Review Focus 2 e 5 para tipo (`technicianIsForbidden`, `refusesARepeatedNameInAnotherCase`, `renameToTheSameNameDoesNotConflictWithItself`); **`updateWithModelsRefusesANewUnitAndSavesNothing`** (`characteristic_locked`, `verify(repository, never()).save(any())`); `updateWithModelsAcceptsTurningRequiredOff`; `updateWithoutModelsChangesTheUnit`; `unknownUnitIsABadRequest` (400, parâmetro `unit`); `typeNotFound`; desativar e reativar.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the equipment type commands`.

### Tarefa 8: Commands de modelo e queries

**Files:** `usecases/commands/{RegisterEquipmentModel,UpdateEquipmentModel,DeactivateEquipmentModel,ReactivateEquipmentModel}.java`; `schemas/{StatusFilter,ManufacturerFilter,EquipmentModelFilter,ManufacturerSummary,EquipmentTypeSummary,EquipmentModelSummary,ValueInput}.java`; `contracts/readers/{ManufacturerReader,EquipmentTypeReader,EquipmentModelReader}.java` (completa o da Tarefa 7); `usecases/queries/{ListManufacturers,ListEquipmentTypes,ListEquipmentModels,GetEquipmentModel}.java`; testes.

**Interfaces:**
- `enum StatusFilter {ACTIVE, DEACTIVATED, ALL}` (em `application`, para a `web` ligar o parâmetro sem enxergar o domínio; nulo vale `ACTIVE`).
- `record ValueInput(UUID definitionId, String amount)`.
- Filtros: `ManufacturerFilter(String text, StatusFilter status)`; `EquipmentModelFilter(String text, UUID typeId, StatusFilter status)`. A lista de tipos recebe só `StatusFilter`.
- Respostas: `ManufacturerSummary(UUID id, String name, CatalogStatus status, long modelCount)`; `EquipmentTypeSummary(UUID id, String name, CatalogStatus status, long modelCount, List<CharacteristicView> characteristics)` com `CharacteristicView(UUID id, String name, MeasurementUnit unit, boolean required)`; `EquipmentModelSummary(UUID id, String name, CatalogStatus status, Ref manufacturer, Ref type, List<ValueView> values)` com `Ref(UUID id, String name)` e `ValueView(UUID definitionId, String name, MeasurementUnit unit, BigDecimal amount)`.
- Readers: `ManufacturerReader.search(ManufacturerFilter, Pageable): Page<ManufacturerSummary>`; `EquipmentTypeReader.search(StatusFilter, Pageable): Page<EquipmentTypeSummary>`; `EquipmentModelReader.search(EquipmentModelFilter, Pageable): Page<EquipmentModelSummary>`, `findSummary(UUID id): Optional<EquipmentModelSummary>`, `hasModelsOfType(UUID typeId): boolean`.
- `RegisterEquipmentModel.Params(Actor actor, UUID manufacturerId, UUID typeId, String name, List<ValueInput> values)` → `UUIDResponse`: carrega fabricante e tipo (`manufacturer_not_found`, `type_not_found`), `existsByManufacturerAndName` (`model_name_taken`, 409, "este fabricante já tem um modelo com esse nome"), `EquipmentModel.register`, `save`. `UpdateEquipmentModel.Params(Actor actor, UUID modelId, String name, List<ValueInput> values)`: carrega o modelo e o tipo dele, `existsByManufacturerAndName(..., modelId)`, `edit`, `save`. `model_not_found` (404).
- Queries (`@UseCase(transactional = false)`, `Params` com `Actor`): sem checagem de papel (os três consultam, mas o `Actor` é exigido: nulo → erro de programação, não de negócio); `ListManufacturers.Params(Actor, ManufacturerFilter, Pageable)`, `ListEquipmentTypes.Params(Actor, StatusFilter, Pageable)`, `ListEquipmentModels.Params(Actor, EquipmentModelFilter, Pageable)`, `GetEquipmentModel.Params(Actor, UUID modelId)` (`model_not_found`).

- [ ] **Step 1: Testes que falham:** `technicianIsForbiddenToWriteButCanQuery` (cada query chama o reader para os três papéis); `registerModelRefusesARepeatedNameForTheSameManufacturer` (e aceita o mesmo nome para outro fabricante: `existsByManufacturerAndName` é chamado com o `manufacturerId` certo); `registerModelRefusesAnInvalidAmount` (`"0"`, `"1.2345"` → `invalid_amount`, 400, `save` nunca); `registerModelRefusesAValueOfAnotherType`; `registerModelRefusesAMissingRequiredValue`; `registerModelRefusesADeactivatedManufacturer` e `...Type`; `updateKeepsManufacturerAndType`; `updateOfADeactivatedModelFails`; `defaultStatusFilterIsActive` (filtro nulo chega ao reader como `ACTIVE`).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** (`ValueInput.amount` vira `CharacteristicAmount.of(String)`; `ValidationException` do VO sai como 400 pelo aspect). **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the model commands and the catalog queries`.

---

## Infraestrutura (`registry-infrastructure`)

Pacote base `com.crm.registry.infrastructure.catalog`. Siga a skill `crm-add-persistence` e o `iam-infrastructure` como referência.

### Tarefa 9: Migration, entidades JPA, mappers, repositories e readers

**Files:**
- Create: `src/main/resources/db/migration/registry/V1__create_catalog.sql`
- Create: `persistence/{ManufacturerEntity,EquipmentTypeEntity,CharacteristicDefinitionEntity,EquipmentModelEntity,ModelCharacteristicValueEntity}.java`, `persistence/{ManufacturerMapper,EquipmentTypeMapper,EquipmentModelMapper}.java`, `persistence/{ManufacturerJpaRepository,EquipmentTypeJpaRepository,EquipmentModelJpaRepository}.java`, `persistence/{JpaManufacturerRepositoryImpl,JpaEquipmentTypeRepositoryImpl,JpaEquipmentModelRepositoryImpl}.java`, `persistence/{JpaManufacturerReaderImpl,JpaEquipmentTypeReaderImpl,JpaEquipmentModelReaderImpl}.java`
- Modify: `RegistryBeans.java` (mappers, repositories, readers; os use cases das Tarefas 6 a 8 entram na Tarefa 11)
- Test: `persistence/*MapperTest.java` (unitário) e `persistence/CatalogPersistencePostgresTest.java` (Testcontainers, como o `IamPersistencePostgresTest`)

**Interfaces:**
- Consumes: contratos das Tarefas 6 a 8.
- Migration exatamente como em `domain-model.md` (Persistência): `manufacturers`, `equipment_types` com `UNIQUE` em `lower(name)` (índice único de expressão); `characteristic_definitions` (FK para o tipo); `equipment_models` (FKs, `UNIQUE (manufacturer_id, lower(name))`, índice em `type_id`); `model_characteristic_values` (`PK (model_id, definition_id)`, `amount numeric(12,3)`). Todas com `id uuid PK` sem default, `version bigint not null`, `created_at timestamptz not null`, `updated_at timestamptz` **nulável**.
- Os mappers levam a versão e nunca `createdAt`/`updatedAt`; o `EquipmentTypeMapper` carrega e salva a definição (entidade interna) junto com o tipo; o `EquipmentModelMapper`, os valores.
- `existsByName`/`existsByManufacturerAndName` com a comparação por `lower(...)` e a exceção por id (`@Query` com `(:exceptId is null or id <> :exceptId)`).
- Readers com `EntityManager` e JPQL de projeção, como o `JpaUserReaderImpl`: ordenação por nome com campos permitidos numa lista fechada; `text` com `escape` do `LIKE` (a busca do modelo casa o nome do modelo **e** o do fabricante); `modelCount` por subconsulta; `StatusFilter.ALL` não filtra.

- [ ] **Step 1: Testes que falham:** `*MapperTest` (ida e volta preserva todos os campos e a versão; versão nula = linha nova; o mapper do tipo preserva o id da definição). `CatalogPersistencePostgresTest`: `savesAndLoadsAModelWithItsValue`; **`secondManufacturerWithTheSameNameInAnotherCaseIsAConflict`** (`ConflictException`); **`twoSavesOfTheSameNameAtTheSameTimeLeaveOneAndConflictTheOther`** (duas threads, uma vence, a outra `ConflictException`, nunca outra exception); `sameModelNameForAnotherManufacturerIsAllowed`; `existsByNameIgnoresTheExceptedId`; `searchFiltersByTypeStatusAndText` (casa por nome do fabricante e por nome do modelo; `%` e `_` no texto valem como literais); `hasModelsOfType`.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/registry/registry-infrastructure -am test -Dtest='*MapperTest'` → FAIL; o `PostgresTest` exige Docker.
- [ ] **Step 3: Implementar.** O erro de coluna entre migration e entity só aparece com o banco de pé (`ddl-auto=validate`).
- [ ] **Step 4: Rodar e ver passar** (mappers sem Docker; `PostgresTest` onde houver Docker).
- [ ] **Step 5: Commit:** `feat(registry): add the catalog persistence and readers`.

### Tarefa 10: Seeder do catálogo

**Files:**
- Create: `seed/{CatalogSeeder,CatalogSeed}.java`, `src/main/resources/seed/catalog.json` (conteúdo `[]`), `seed/CatalogSeederTest.java`
- Modify: `RegistryBeans.java` (bean `CatalogSeeder`)

**Interfaces:**
- `CatalogSeed(List<ManufacturerSeed> manufacturers, List<TypeSeed> types, List<ModelSeed> models)`; `ManufacturerSeed(String name)`; `TypeSeed(String name, String characteristicName, String unit, boolean required)`; `ModelSeed(String manufacturer, String type, String name, String amount)` (referência por **nome**, a característica é a única do tipo). O JSON é uma lista com **um** objeto `CatalogSeed` (o `Seeder<T>` lê `List<T>`).
- `CatalogSeeder implements Seeder<CatalogSeed>`: `isAlreadySeeded()` = existe algum fabricante **ou** algum tipo (`count() > 0` nos dois repositories); `seed` grava **pelo domínio** (`register`, na ordem fabricantes, tipos, modelos; nome que não existe, unidade desconhecida ou valor inválido lançam `InternalServerErrorException` em pt-BR dizendo qual item, sem repetir dado); `resourcePath() = "seed/catalog.json"`; `recordType() = CatalogSeed.class`.

- [ ] **Step 1: Testes que falham** (repositories `mock`): `isAlreadySeededWhenThereIsAManufacturerOrAType`; `isNotSeededWhenTheCatalogIsEmpty`; `seedsManufacturersTypesAndModelsThroughTheDomain` (cria o inversor `Potência/kW` e o modelo `SUN2000-3KTL-L1` com `3`); `seedWithAnUnknownManufacturerInAModelFailsNamingTheModel`; `seedWithAnInvalidAmountFails`; `emptyListSeedsNothing`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(registry): add the catalog seeder`. O arquivo `catalog.json` nasce `[]`: a lista do dono entra depois, como dado, sem mudar código (`open-questions.md`).

---

## Borda (`web`, `boot`)

### Tarefa 11: Beans dos use cases e os três controllers

**Files:**
- Modify: `RegistryBeans.java` (os 12 commands e as 4 queries, com `new`, como o `IamBeans`)
- Create: `edge/web/src/main/java/com/crm/web/registry/{ManufacturersController,EquipmentTypesController,EquipmentModelsController}.java`; `web/registry/requests/{ManufacturerRequest,EquipmentTypeRequest,CharacteristicRequest,RegisterEquipmentModelRequest,UpdateEquipmentModelRequest,ValueRequest}.java`
- Test: `edge/web/src/test/java/com/crm/web/registry/*ControllerTest.java`; reaproveitar o `IamMvc.standalone` (extrair para um `Mvc` comum no mesmo pacote de teste é aceitável)

**Interfaces:**
- Rotas e verbos exatamente os de `usecases.md` (`/catalog/manufacturers`, `/catalog/equipment-types`, `/catalog/equipment-models`, `POST .../{id}/deactivation` e `.../reactivation`). Criar responde `201` com `UUIDResponse`; alterar, desativar e reativar respondem `204`; listas respondem `Page<...>` com `page` (padrão 0), `size` (padrão 20, máximo 100) e o filtro por `@ModelAttribute`; `GET /catalog/equipment-models/{id}` responde `EquipmentModelSummary`.
- Requests com Bean Validation (`@NotBlank`, `@Size` com os limites do domínio, `@NotNull` na lista de valores; `unit` e `amount` são `String`, o use case converte). O controller só chama o use case, com o `Actor` do `ActorArgumentResolver`; **nenhum import de `registry-domain`**.

- [ ] **Step 1: Testes que falham** (MockMvc standalone, use cases `mock`): por controller, `registerRespondsCreatedWithTheId`; `updateRespondsNoContent`; `deactivateAndReactivateRespondNoContent`; `listPassesPageSizeAndFilter` (e `size=500` vira 100); `emptyNameIsA400WithInvalidParams`; `domainErrorsKeepTheirCode` (use case lançando `ConflictException("manufacturer_name_taken", ...)` → 409 com `code` no corpo); `withoutActorIsA401`.
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl edge/web -am test -Dtest='*ControllerTest'`. **Step 3: Implementar.** **Step 4: Rodar e ver passar**, e `BoundedContextRulesTest` (a `web` não pode enxergar `registry-domain`).
- [ ] **Step 5: Commit:** `feat(web): add the catalog controllers`.

### Tarefa 12: Ponta a ponta e fechamento

**Files:**
- Create: `edge/boot/src/test/java/com/crm/RegistryEndToEndTest.java`; extrair os auxiliares do `IamEndToEndTest` (`Browser`, `Reply`, relógio e e-mail falsos) para classes de teste comuns do pacote `com.crm`, sem mudar o comportamento do teste do IAM.
- Modify: `docs/system-design/02-catalog/README.md` (marcar o critério de aceite do backend).

- [ ] **Step 1: Escrever o teste** (`@SpringBootTest` com Postgres e Redis do Testcontainers, como o `IamEndToEndTest`; exige Docker): o Administrador semeado entra e troca a senha; `POST /catalog/manufacturers` → `201`; o mesmo nome em outra caixa → `409` com `manufacturer_name_taken`; cria o tipo "Inversor" (Potência, `KILOWATT`) e o modelo `SUN2000-3KTL-L1` com `3`; `GET /catalog/equipment-models?text=sun2000` acha o modelo; desativar e reativar; **o Técnico** (convidado e aceito pelo fluxo do IAM) consulta (`200`) e recebe `403 forbidden` ao cadastrar; sem sessão `401`.
- [ ] **Step 2: Rodar e ver falhar,** depois ajustar até passar: `mvn -pl edge/boot -am test -Dtest=RegistryEndToEndTest`.
- [ ] **Step 3: Rodar a suíte inteira** (`mvn test`, JDK 21, com Docker): verde, inclusive `BoundedContextRulesTest`, `AntiCorruptionLayerRulesTest`, `FlywayConfigTest` e o `IamEndToEndTest`. Sem Docker, rode com `-Dtest='!*PostgresTest,!IamEndToEndTest,!RegistryEndToEndTest'` e **diga no PR** que os testes com Docker não rodaram.
- [ ] **Step 4: Atualizar o README da feature** e rodar `git status` para conferir que só entraram arquivos desta feature (há mudança de `infra/` em outro branch: não misture).
- [ ] **Step 5: Commit:** `test(boot): cover the catalog end to end`.
