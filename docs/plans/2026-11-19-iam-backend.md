# IAM (backend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o BC `iam` no backend: conta de funcionário com papel, convite, login com sessão longa revogável, redefinição de senha, gestão de usuários e a cadeia de segurança que liga tudo.

**Architecture:** Três aggregates (`User`, `OneTimeToken`, `Session`) em `iam-domain`; use cases concretos em `iam-application`; persistência, listeners e o hash Argon2id em `iam-infrastructure`; controllers em `web`; a cadeia de segurança em `boot`. E-mail de convite e de redefinição saem por domain event depois do commit; cache de sessão é invalidado por evento.

**Tech Stack:** Java 21, Spring Boot 3.4.5, Spring Data JPA, Flyway, Postgres, Redis (`Cache`, `RateLimiter`), `spring-security-crypto` + `bcprov` (Argon2id, já no pom de `iam-infrastructure`), Spring Security (já no `boot`), JUnit 5, Mockito, AssertJ, Testcontainers.

**Spec:** [`docs/system-design/01-iam/`](../system-design/01-iam/README.md): `domain-model.md`, `usecases.md`, `decisions.md` (ADR-1 a ADR-11), mockups em `design/`. **O plano não redefine o domínio**: se uma tarefa pedir mudança nele, volte à spec.

**Escopo:** só o **backend**. O frontend tem plano próprio ([`2026-11-19-iam-frontend.md`](2026-11-19-iam-frontend.md)); o aplicativo (Capacitor) fica para depois.

## Global Constraints

- **JDK 21**; tudo roda a partir de `backend/`; critério de aceite de cada tarefa: `mvn test -Dtest='!IdempotencyGuardPostgresTest'` verde, e `mvn test` verde onde houver Docker (skill `crm-build-and-test`).
- `domain` e `application` são Java puro; a única anotação de framework em `application` é `@UseCase`; Bean Validation só em schemas de request. Nada de `@Component`/`@Service`: beans na `IamBeans` com `new` (§9).
- A hora vem do `Clock` (`Instant now` por parâmetro no domínio). Nenhum `Instant.now()`/`LocalDate.now()` sem relógio.
- Estilo ([`code-style.md`](../rules/code-style.md)): nada de `else`; `if` de uma instrução sem chaves; `catch` em linha própria; 3+ parâmetros um por linha; blocos separados por linha em branco; identificadores em inglês; comentário, mensagem de exceção, log e `@DisplayName` em pt-BR, mensagem minúscula e sem ponto final; toda classe de produção com Javadoc; nenhuma exception nativa; teste com hora fixa.
- Valores da spec: senha de **8 a 30** caracteres, com maiúscula, minúscula, número e especial (ADR-3); sessão de **30 dias** renovada a cada uso, com renovação só se passaram **5 minutos** (ADR-1, ADR-6); login: **5 erros em 15 minutos** por e-mail, **20** por origem (ADR-4); convite **48 h**, redefinição **1 h**, pedidos de redefinição **3 por hora**; cache de sessão **60 s**.
- Código de erro `snake_case`, em inglês, único no BC, exatamente os da tabela de erros de `usecases.md`.
- Segredo (senha, token de sessão, segredo de link) **nunca** em log, em mensagem de exceção nem em `toString()`.
- Commit: Conventional Commits, escopo `iam` (ou `common`, `web`, `boot`), em inglês, **sem `Co-Authored-By`** ([`git-workflow.md`](../rules/git-workflow.md) §3).
- Branch: `feature/iam-*`. A base é `develop`, que o usuário ainda não criou: **pergunte a base antes da Tarefa 1**. Há trabalho não commitado no `main` (docs, skills, ajustes do kernel): não o misture nos commits desta feature.

## Review Focus

Entradas que a spec implica e nenhum caso feliz exercita. Cada uma tem o teste na tarefa dona (marcada abaixo):

1. **Duas requisições consomem o mesmo link ao mesmo tempo:** só uma vence, a outra recebe `link_used` (não 409, não 500). → Tarefa 6.
2. **Login de e-mail inexistente ou de usuário não ativo:** mesma resposta `invalid_credentials` e o `PasswordHasher` roda mesmo assim (sem vazar quem tem conta pelo tempo); e-mail em caixa diferente (`Joao@X.com`) entra. → Tarefa 8.
3. **Sessão de conta com `mustChangePassword` chamando qualquer outra rota:** `403 password_change_required`; e **requisição só com cookie e sem o cabeçalho de CSRF**: `403`. → Tarefa 13.
4. **Senha com espaço nas pontas, acentuada, ou só com caractere "especial" não ASCII:** o espaço não é aparado nem conta como especial; `ç` conta como minúscula; `€` conta como especial. → Tarefa 1.
5. **Sessão revogada ainda no cache, ou Redis fora do ar:** a revogação tira do cache na hora; sem Redis, `AuthenticateSession` lê do banco e continua certa. → Tarefas 8 e 11.

---

## Pré-requisito

### Tarefa 0: Seeder idempotente, transacional e seguro para produção

Pré-requisito do ADR-2 e item aberto do `architecture.md` §13. **Desenho proposto, a aprovar junto com o plano.**

**Files:**
- Modify: `backend/kernel/common/common-infrastructure/src/main/java/com/crm/common/infrastructure/seed/Seeder.java`
- Modify: `.../seed/DatabaseSeeder.java`, `.../seed/SeedConfig.java`
- Test: `backend/kernel/common/common-infrastructure/src/test/java/com/crm/common/infrastructure/seed/DatabaseSeederTest.java`
- Modify: `docs/rules/architecture.md` §13 (remover o item "Seeder")

**Interfaces:**
- Produces: `Seeder<T>` ganha `boolean isAlreadySeeded()`. `DatabaseSeeder(List<Seeder<?>> seeders, ObjectMapper objectMapper, Environment environment, TransactionTemplate transactionTemplate)`.

- [ ] **Step 1: Escrever os testes que falham** (fakes de `Seeder`, `Environment` com `MockEnvironment`, `TransactionTemplate` mockado que executa o callback):
  - `skipsSeederThatIsAlreadySeeded`: `isAlreadySeeded()==true` → `seed` nunca chamado.
  - `seedsInsideATransaction`: `seed` roda dentro de `transactionTemplate.execute`.
  - `failsBootWhenAPlaceholderIsMissing`: JSON com `${CRM_ADMIN_EMAIL}` sem a propriedade → lança `InternalServerErrorException` com a mensagem `seed: variável não definida: CRM_ADMIN_EMAIL` (sem o valor de nenhuma outra variável).
  - `failurePropagatesAndStopsNextSeeders`: o primeiro `seed` lança → o segundo não roda e a exception sobe (o boot falha).
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl kernel/common/common-infrastructure -am test -Dtest=DatabaseSeederTest` → FAIL (método inexistente).
- [ ] **Step 3: Implementar** `isAlreadySeeded()` no contrato; em `DatabaseSeeder.run`, por seeder: pular se já semeado; resolver o JSON com `environment.resolveRequiredPlaceholders` (traduzindo `IllegalArgumentException` para `InternalServerErrorException`); chamar `seed` em `transactionTemplate.execute`. `SeedConfig` injeta o `useCaseTransactionTemplate`.
- [ ] **Step 4: Rodar e ver passar:** o mesmo comando → PASS; depois o `mvn test` do módulo inteiro.
- [ ] **Step 5: Atualizar `architecture.md` §13** e commitar: `git commit -m "feat(common): make the seeder idempotent, transactional and strict about placeholders"`.

---

## Domínio (`iam-domain`)

Pacote base `com.crm.iam.domain`, com `vos`, `entities`, `events`. Testes em `core/iam/iam-domain/src/test/java/...`, JUnit puro. Antes de começar, o `pom.xml` de `iam-domain` precisa de `assertj-core` em escopo `test` (versão do BOM do Spring Boot).

### Tarefa 1: Value Objects e enums

**Files:** `vos/{UserName,Password,PasswordHash,TokenSecret,TokenHash,DeviceLabel}.java`, `vos/{Role,UserStatus,TokenPurpose}.java`; testes `vos/*Test.java`.

**Interfaces (produz):**
- `UserName.of(String)`: 2 a 80 caracteres. `DeviceLabel.of(String)`: 1 a 80.
- `Password.of(String)` (`extends ValueObject<String>`, **não apara espaços**). `toString()` devolve `"***"`; `equals`/`hashCode` não expõem o valor. Erro: `ValidationException("invalid_password", <mensagem do primeiro critério que falta>)`; critérios na ordem: tamanho mínimo, tamanho máximo, maiúscula (`Character.isUpperCase`), minúscula (`isLowerCase`), número (`isDigit`), especial (nem letra, nem dígito, nem espaço em branco).
- `PasswordHash.of(String)`: recusa texto que não comece com `$argon2id$`.
- `TokenSecret.generate(): TokenSecret` (32 bytes de `SecureRandom`, base64 url-safe sem padding = 43 caracteres); `TokenSecret.of(String)` (recusa formato diferente); `hash(): TokenHash`; `toString()` mascarado.
- `TokenHash.of(String)`: SHA-256 em hexadecimal minúsculo, 64 caracteres.
- Enums `Role {ADMINISTRATOR, COMMERCIAL, TECHNICIAN}`, `UserStatus {INVITED, ACTIVE, DEACTIVATED}`, `TokenPurpose {INVITATION, PASSWORD_RESET}`.

- [ ] **Step 1: Testes que falham**, um `@DisplayName` em pt-BR por linha:
  - `Password`: aceita `"Dunas#2026"`; recusa com `invalid_password` e a mensagem do critério certo: 7 caracteres, 31 caracteres, sem maiúscula, sem minúscula, sem número, sem especial; **`" Dunas#2026 "` não é aparada** (a senha guardada mantém os espaços, e `"Aa1     "` (espaços no lugar do especial) é recusada com o critério do caractere especial); `"Açúcar1€"` é aceita (`ç` minúscula, `€` especial); `toString()` não contém o valor.
  - `TokenSecret`: `generate()` duas vezes dá valores diferentes; tem 43 caracteres url-safe; `hash()` é determinístico, tem 64 caracteres hex, e `of(secret.getValue()).hash()` é igual ao primeiro; `toString()` não contém o valor.
  - `UserName`, `DeviceLabel`, `PasswordHash`, `TokenHash`: nulo, vazio, limites, formato (cada um com a mensagem esperada).
- [ ] **Step 2: Rodar e ver falhar:** `mvn -pl core/iam/iam-domain -am test` → FAIL (classes inexistentes).
- [ ] **Step 3: Implementar** conforme as assinaturas; `UserName`, `DeviceLabel`, `PasswordHash`, `TokenHash` estendem `StringValueObject` (`type()` constante, `customValidate`); mensagens em pt-BR minúsculo.
- [ ] **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(iam): add value objects and enums`.

### Tarefa 2: Aggregate `User`

**Files:** `entities/User.java`, `events/{UserDeactivated,UserRoleChanged}.java`; teste `entities/UserTest.java`.

**Interfaces:**
- Consome: Tarefa 1; `Email` de `shared-domain`; `AggregateRoot` (`registerEvent`, `pullEvents`).
- Produz: `static User invite(UserName name, Email email, Role role, Instant now)`; `static User reconstitute(UUID id, Long version, UserName name, Email email, Role role, UserStatus status, PasswordHash passwordHash, boolean mustChangePassword)`; `void acceptInvitation(PasswordHash)`; `void changePassword(PasswordHash)`; `void resetPassword(PasswordHash)`; `void changeRole(UUID actorId, Role newRole)`; `void deactivate(UUID actorId, Instant now)`; `void reactivate()`; `void markPasswordChangeRequired()`; getters (Lombok) e `Optional<PasswordHash> getPasswordHash()`. Eventos `record UserDeactivated(UUID aggregateId, Instant occurredOn) implements DomainEvent` e `UserRoleChanged` idem.

- [ ] **Step 1: Testes que falham** (códigos e transições da tabela de `domain-model.md`):
  - `invite` cria `INVITED`, sem senha, `mustChangePassword=false`, id novo, sem versão, sem evento.
  - `acceptInvitation` → `ACTIVE` com hash; em `ACTIVE`/`DEACTIVATED` → `DomainException` `user_not_invited`.
  - `changePassword`/`resetPassword`: só `ACTIVE` (senão `user_not_active`); `changePassword` zera `mustChangePassword`.
  - `changeRole(actorId, …)`: com `actorId == id` → `cannot_change_own_role`; usuário não `ACTIVE` → `user_not_active`; sucesso registra `UserRoleChanged`.
  - `deactivate`: `actorId == id` → `cannot_deactivate_self`; já `DEACTIVATED` → `user_deactivated`; vale para `INVITED` e `ACTIVE`; registra `UserDeactivated` com `occurredOn == now`.
  - `reactivate`: `DEACTIVATED` com senha → `ACTIVE`; sem senha → `INVITED`; fora de `DEACTIVATED` → `user_not_deactivated`.
  - invariante: `ACTIVE` sempre tem senha (reconstituir `ACTIVE` sem hash lança `DomainException`).
  - `pullEvents()` devolve e esvazia; `reconstitute` não registra evento.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar** com enum e guardas (ADR-9), `UUID.randomUUID()` no `invite`, guard clauses sem `else`.
- [ ] **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(iam): add User aggregate`.

### Tarefa 3: Aggregate `OneTimeToken`

**Files:** `entities/OneTimeToken.java`, `events/OneTimeTokenIssued.java`; teste `entities/OneTimeTokenTest.java`.

**Interfaces:**
- Produz: `static OneTimeToken issue(UUID userId, TokenPurpose purpose, TokenSecret secret, Duration validity, Instant now)` (guarda `secret.hash()`, `expiresAt = now + validity`, registra `OneTimeTokenIssued(aggregateId, occurredOn, userId, purpose, secret)`); `static reconstitute(UUID id, Long version, UUID userId, TokenPurpose purpose, TokenHash tokenHash, Instant expiresAt, Instant usedAt, Instant invalidatedAt)`; `void consume(Instant now)`; `void invalidate(Instant now)`; getters.

- [ ] **Step 1: Testes que falham:** `consume` antes de `expiresAt` marca `usedAt`; em `now == expiresAt` ou depois → `link_expired`; já usado → `link_used`; invalidado → `link_used`; `invalidate` é idempotente e não altera quem já foi usado; o evento carrega o segredo e a finalidade; o aggregate guardado **não** expõe o segredo (nenhum getter devolve `TokenSecret`; `toString()` não o contém).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(iam): add OneTimeToken aggregate`.

### Tarefa 4: Aggregate `Session`

**Files:** `entities/Session.java`, `events/SessionRevoked.java`; teste `entities/SessionTest.java`.

**Interfaces:**
- Produz: `static Session start(UUID userId, TokenSecret secret, DeviceLabel device, Duration validity, Instant now)`; `static reconstitute(UUID id, Long version, UUID userId, TokenHash tokenHash, DeviceLabel device, Instant startedAt, Instant lastUsedAt, Instant expiresAt, Instant revokedAt, UUID revokedBy)`; `boolean renewIfDue(Instant now, Duration validity, Duration minInterval)`; `void revoke(UUID by, Instant now)`; `boolean isActiveAt(Instant now)`; `record SessionRevoked(UUID aggregateId, Instant occurredOn, TokenHash tokenHash)`.

- [ ] **Step 1: Testes que falham:** `start` define `expiresAt = now + 30 dias` e `lastUsedAt = now`; `isActiveAt` é falso em `expiresAt` e depois, e se revogada; `renewIfDue` com 4 min 59 s desde o último uso devolve `false` e não muda nada; com 5 min devolve `true`, `lastUsedAt = now`, `expiresAt = now + validity`; em sessão inativa lança `DomainException` `session_invalid`; `revoke` preenche `revokedAt`/`revokedBy` e registra `SessionRevoked` com o hash; **segunda revogação não muda nada e não registra evento**.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(iam): add Session aggregate`.

---

## Aplicação (`iam-application`)

Pacote base `com.crm.iam.application`. O `pom.xml` precisa de `mockito-core` e `assertj-core` em escopo `test` (versões do BOM).

### Tarefa 5: Contratos, `Actor`, configuração e schemas

**Files:** `Actor.java`, `IamSettings.java`, `contracts/repositories/*`, `contracts/readers/*`, `contracts/services/PasswordHasher.java`, `schemas/*`. Sem teste próprio além de `IamSettingsTest` (validação do construtor); o comportamento é coberto nas tarefas seguintes.

**Interfaces (produz):**
- `record Actor(UUID userId, Role role, UUID sessionId)`.
- `record IamSettings(Duration sessionValidity, Duration sessionRenewInterval, Duration sessionCacheTtl, Duration invitationValidity, Duration passwordResetValidity, RateLimit loginPerEmail, RateLimit loginPerOrigin, RateLimit passwordResetPerEmail)`; construtor recusa nulo e duração não positiva (`InternalServerErrorException`).
- `interface UserRepository extends Repository<User, UserCriteria>` com `Optional<User> findByEmail(Email)`; `interface OneTimeTokenRepository extends Repository<OneTimeToken, OneTimeTokenCriteria>` com `Optional<OneTimeToken> findByTokenHash(TokenHash)`, `List<OneTimeToken> findPending(UUID userId, TokenPurpose purpose)` e `boolean consume(OneTimeToken token)` (update condicional: só grava se ainda não foi usado nem invalidado; `false` se perdeu a corrida); `interface SessionRepository extends Repository<Session, SessionCriteria>` com `Optional<Session> findByTokenHash(TokenHash)` e `List<Session> findActiveByUserId(UUID userId, Instant now)`. Os três `*Criteria` são `record` vazios que implementam `Criteria`.
- `interface UserReader { Page<UserSummary> search(UserFilter filter, Pageable pageable); Optional<UserSummary> findSummary(UUID id); }`; `interface SessionReader { List<SessionSummary> findActiveByUser(UUID userId, UUID currentSessionId, Instant now); Optional<SessionPrincipal> findPrincipal(TokenHash tokenHash); }`.
- `interface PasswordHasher { PasswordHash hash(Password password); boolean matches(Password password, PasswordHash hash); }`.
- Schemas (`record`): `SessionResponse(String token, Instant expiresAt, boolean mustChangePassword)`, `UserSummary(UUID id, String name, String email, Role role, UserStatus status, Instant lastUsedAt)`, `UserFilter(Role role, UserStatus status, String text)`, `SessionSummary(UUID id, String device, Instant startedAt, Instant lastUsedAt, boolean current)`, `UserDetail(UserSummary user, List<SessionSummary> sessions)`, `CurrentUser(UUID id, String name, String email, Role role, boolean mustChangePassword)`, `SessionPrincipal(UUID sessionId, UUID userId, Role role, UserStatus userStatus, boolean mustChangePassword, Instant lastUsedAt, Instant expiresAt, Instant revokedAt)` (serializável em JSON pelo `Cache`).

- [ ] **Step 1:** escrever `IamSettingsTest` (duração zero ou nula é recusada) e rodar: FAIL. **Step 2:** criar as interfaces, records e `IamSettings`. **Step 3:** `mvn -pl core/iam/iam-application -am test` → PASS. **Step 4: Commit:** `feat(iam): add application contracts and schemas`.

### Tarefa 6: Persistência: migration, entities, mappers e repositories

**Files (em `iam-infrastructure`, pacote `com.crm.iam.infrastructure.persistence`):**
- Create: `src/main/resources/db/migration/iam/V1__create_iam_tables.sql`
- Create: `UserEntity`, `OneTimeTokenEntity`, `SessionEntity` (estendem `JpaEntity<UUID>`), `*JpaRepository`, `*Mapper` (`EntityMapper`), `JpaUserRepositoryImpl`, `JpaOneTimeTokenRepositoryImpl`, `JpaSessionRepositoryImpl`, `JpaUserReaderImpl`, `JpaSessionReaderImpl`.
- Test: `*MapperTest`, `IamPersistencePostgresTest` (Testcontainers).

**Interfaces:** implementa os contratos da Tarefa 5. Tabelas, colunas, `UNIQUE` e índices exatamente como na seção "Persistência" de `domain-model.md`; `updated_at` **nulável**; id sem default; sem FK para tabela de outro BC (FK entre as três do IAM é permitida). `JpaOneTimeTokenRepositoryImpl.consume` faz `UPDATE one_time_tokens SET used_at = :now, version = version + 1 WHERE id = :id AND used_at IS NULL AND invalidated_at IS NULL` e devolve `rows == 1`. `JpaUserReaderImpl.search` filtra `role`, `status` e `text` (nome **ou** e-mail, `LIKE` sem diferenciar caixa) e calcula `lastUsedAt` como o maior `last_used_at` das sessões do usuário (vazio se nunca entrou). `JpaSessionReaderImpl.findPrincipal` junta sessão e usuário em uma consulta.

- [ ] **Step 1: Testes que falham:**
  - `*MapperTest`: ida e volta `toPersistence → toDomain` preserva todos os campos e a **versão**; versão nula ↔ linha nova; `createdAt`/`updatedAt` nunca chegam ao domínio.
  - `IamPersistencePostgresTest` (mesmo padrão de `IdempotencyGuardPostgresTest`, só roda com Docker): a migration passa com `ddl-auto=validate`; e-mail repetido (caixa diferente) → `ConflictException`; `findByEmail`; `findActiveByUserId` ignora revogada e expirada; `search` por papel, por situação e por texto, paginando; **`consume` chamado por duas threads sobre o mesmo token: exatamente uma recebe `true`** (Review Focus 1).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** (skill `crm-add-persistence`). **Step 4: Rodar e ver passar** (`-Dtest='!IdempotencyGuardPostgresTest'` quando sem Docker, e dizer no PR que o teste de Postgres não rodou).
- [ ] **Step 5: Commit:** `feat(iam): add persistence for users, tokens and sessions`.

### Tarefa 7: `PasswordHasher` com Argon2id

**Files:** `iam-infrastructure/.../security/Argon2PasswordHasherImpl.java`; teste `Argon2PasswordHasherImplTest`.

**Interfaces:** implementa `PasswordHasher` com `Argon2PasswordEncoder` de `spring-security-crypto` (parâmetros pelo construtor, vindos de `IamProperties` na Tarefa 11).

- [ ] **Step 1: Testes que falham:** `hash` devolve `PasswordHash` válido (`$argon2id$`); duas chamadas com a mesma senha dão hashes diferentes; `matches` é verdadeiro para a senha certa e falso para a errada; o hash não contém a senha; `hash` com parâmetros de teste leves roda em menos de 1 s.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(iam): add argon2id password hasher`.
- [ ] **Step 6 (medição, não bloqueia o commit):** na VPS-alvo (ou numa máquina equivalente), medir o tempo de `hash` com os parâmetros padrão e ajustar `crm.iam.argon2.*` para algo entre 200 e 500 ms; registrar os valores na `open-questions.md` da feature.

### Tarefa 8: Use cases de autenticação

**Files (em `com.crm.iam.application.usecases.commands`):** `Login`, `Logout`, `AcceptInvitation`, `RequestPasswordReset`, `ResetPassword`, `ChangePassword`, `AuthenticateSession`. Testes `*Test` ao lado.

**Interfaces:** consomem as Tarefas 1 a 5 e `RateLimiter`, `Cache`, `EventPublisher`, `Clock`. Assinaturas:
- `SessionResponse Login.execute(Login.Params)` com `record Params(String email, String password, String device, String origin)`.
- `void Logout.execute(Actor)`.
- `SessionResponse AcceptInvitation.execute(Params(String token, String password, String device))`.
- `void RequestPasswordReset.execute(Params(String email, String origin))`.
- `void ResetPassword.execute(Params(String token, String password))`.
- `void ChangePassword.execute(Params(Actor actor, String currentPassword, String newPassword))`.
- `AuthenticatedSession AuthenticateSession.execute(String token)`, com `record AuthenticatedSession(Actor actor, boolean mustChangePassword)`.
Todas `@UseCase`; `AuthenticateSession` e `Logout` usam `@UseCase`; nenhuma usa `retriable`.

- [ ] **Step 1: Testes que falham** (Mockito nos contratos, `Clock.fixed`, `InOrder` onde a ordem importa), um por linha:
  - `Login`: acerto devolve `SessionResponse` e salva a `Session`; chama `consume` dos dois limites **antes** de conferir a senha e `refund` dos dois no acerto, e **não** chama `refund` no erro; e-mail inexistente, senha errada, usuário `INVITED` e `DEACTIVATED` lançam a **mesma** `UnauthorizedException` `invalid_credentials`; **nos casos de e-mail inexistente e de usuário não ativo, `PasswordHasher.matches` é chamado mesmo assim** (contra um hash fixo) (Review Focus 2); `"Joao@X.com"` encontra o usuário cadastrado como `joao@x.com`; limite estourado propaga `RateLimitExceededException` e não consulta o repository.
  - `AcceptInvitation`: token desconhecido → `UnprocessableEntityException` `link_used`; expirado → `link_expired`; `consume` devolvendo `false` → `link_used`; sucesso ativa o usuário, salva o hash e abre `Session`; senha fraca → `invalid_password` e o token **não** é consumido (validar `Password` antes de consumir).
  - `RequestPasswordReset`: e-mail desconhecido, `INVITED` ou `DEACTIVATED` → retorna normalmente e **não** emite token; `ACTIVE` invalida os pendentes, emite e publica `OneTimeTokenIssued`; limite estourado → `rate_limited`.
  - `ResetPassword`: consome, troca o hash e revoga **todas** as sessões ativas do usuário publicando `SessionRevoked` de cada; não abre sessão.
  - `ChangePassword`: senha atual errada → `UnprocessableEntityException` `wrong_current_password`; sucesso revoga **as outras** sessões e mantém a do `Actor`; zera `mustChangePassword`.
  - `Logout`: revoga a sessão do `Actor` e publica `SessionRevoked`; segunda chamada não publica de novo.
  - `AuthenticateSession`: usa `Cache.getOrLoad` com a chave `iam:session:<hash>` e o TTL de `IamSettings`; token desconhecido, sessão revogada, sessão expirada e usuário não `ACTIVE` → `UnauthorizedException` `session_invalid`; só carrega e salva a `Session` quando `renewIfDue` é devido (`lastUsedAt` do principal com 4 min → nenhuma escrita; com 6 min → uma escrita); **`Cache` que lança a cada chamada não derruba** (o loader cai no `SessionReader`) (Review Focus 5).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** (skill `crm-add-use-case`). **Step 4: Rodar e ver passar.**
- [ ] **Step 5: Commit:** `feat(iam): add authentication use cases`.

### Tarefa 9: Use cases de usuários e sessões

**Files:** `InviteUser`, `ResendInvitation`, `ChangeUserRole`, `DeactivateUser`, `ReactivateUser`, `RevokeSession`, `RevokeAllSessions`, `RevokeUserSessions` (interno, sem `Actor`, chamado pelos listeners; em `commands`) e testes.

**Interfaces:** `UUIDResponse InviteUser.execute(Params(Actor actor, String name, String email, String role))`; `void ResendInvitation.execute(Params(Actor actor, UUID userId))`; `void ChangeUserRole.execute(Params(Actor actor, UUID userId, String role))`; `void DeactivateUser.execute(Params(Actor, UUID userId))`; `void ReactivateUser.execute(Params(Actor, UUID userId))`; `void RevokeSession.execute(Params(Actor, UUID sessionId))`; `void RevokeAllSessions.execute(Params(Actor, UUID userId, boolean keepCurrent))`; `void RevokeUserSessions.execute(Params(UUID userId, UUID revokedBy))` (revoga todas as ativas do usuário e publica `SessionRevoked` de cada; `revokedBy` é o autor da desativação).

- [ ] **Step 1: Testes que falham:**
  - Todos os de Administrador: `Actor` com papel diferente → `ForbiddenException` `forbidden` e nenhum repository é tocado.
  - `InviteUser`: e-mail já existente → `ConflictException` `email_already_registered`; sucesso salva o `User` e o token `INVITATION` com validade de 48 h e publica `OneTimeTokenIssued`; `role` inválido → `BadRequestException`.
  - `ResendInvitation`: usuário inexistente → `NotFoundException` `user_not_found`; não `INVITED` → `user_not_invited`; invalida os pendentes antes de emitir o novo.
  - `ChangeUserRole`, `DeactivateUser`: sobre si mesmo → os códigos de domínio (`cannot_change_own_role`, `cannot_deactivate_self`) chegam sem `try/catch` no use case; publicam `UserRoleChanged`/`UserDeactivated`.
  - `ReactivateUser`: fora de `DEACTIVATED` → `user_not_deactivated`.
  - `RevokeSession`: sessão de outro por não-Administrador → `forbidden`; sessão inexistente → `session_not_found`; Administrador revoga de qualquer um; idempotente.
  - `RevokeUserSessions`: revoga todas as ativas e publica um `SessionRevoked` por sessão; sem sessões, não publica nada.
  - `RevokeAllSessions`: `keepCurrent=true` preserva a sessão do `Actor`; usuário diferente por não-Administrador → `forbidden`; `keepCurrent=true` com `userId` de outro é recusado (`BadRequestException`).
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(iam): add user and session management use cases`.

### Tarefa 10: Queries

**Files:** `GetCurrentUser`, `ListUsers`, `GetUser`, `ListSessions` (em `queries`, `@UseCase(transactional = false)`) e testes.

**Interfaces:** `CurrentUser GetCurrentUser.execute(Actor)`; `Page<UserSummary> ListUsers.execute(Params(Actor, UserFilter, Pageable))`; `UserDetail GetUser.execute(Params(Actor, UUID userId))`; `List<SessionSummary> ListSessions.execute(Params(Actor, UUID userId))`.

- [ ] **Step 1: Testes que falham:** `ListUsers`/`GetUser` por não-Administrador → `forbidden`; `GetUser` inexistente → `user_not_found`; `ListSessions` do próprio usuário passa, de outro por não-Administrador → `forbidden`, e só a sessão do `Actor` vem com `current=true`; `GetCurrentUser` devolve `mustChangePassword`.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(iam): add queries`.

---

## Infraestrutura (`iam-infrastructure`)

### Tarefa 11: Listeners, `IamBeans`, propriedades e seeder do primeiro Administrador

**Files:** `com.crm.iam.infrastructure.{IamBeans,IamProperties}`, `listeners/{OneTimeTokenIssuedListener,SessionRevokedListener,UserDeactivatedListener,UserRoleChangedListener}`, `email/IamEmailTemplates`, `seed/{AdminSeeder,AdminSeed}`, `src/main/resources/seed/iam-admin.json`; testes ao lado.

**Interfaces:**
- `IamProperties`: `record` com `@ConfigurationProperties("crm.iam")` e `@DefaultValue` para tudo que a spec fixa (30 d, 5 min, 60 s, 48 h, 1 h, limites) e `appUrl` (base dos links do e-mail) e `argon2`. Sem default para a senha do Administrador.
- `IamBeans`: registra, com `new`, `IamSettings`, mappers, repositories, readers, `PasswordHasher`, todos os use cases das Tarefas 8 a 10, listeners e `AdminSeeder`.
- Listeners: `@EventListener` (e `@Async` no de e-mail, no executor de `AsyncConfig`), **só traduzem e chamam**: `OneTimeTokenIssuedListener` → busca o `User` e envia `EmailMessage` pelo `EmailSenderService` com o link `{appUrl}/convite?token=<segredo>` ou `{appUrl}/redefinir-senha?token=<segredo>`; `SessionRevokedListener` → `Cache.evict("iam:session:" + hash)`; `UserDeactivatedListener` → chama `RevokeUserSessions` (que publica `SessionRevoked`, e o `SessionRevokedListener` remove cada chave); `UserRoleChangedListener` → remove as chaves de cada sessão ativa do usuário.
- `AdminSeeder implements Seeder<AdminSeed>`, `record AdminSeed(String name, String email, String password)`; `isAlreadySeeded()` é `userRepository.count() > 0`; `seed` faz `User.invite` → `acceptInvitation(hash)` → `markPasswordChangeRequired()` e salva. `iam-admin.json` usa `${CRM_ADMIN_NAME}`, `${CRM_ADMIN_EMAIL}`, `${CRM_ADMIN_PASSWORD}`, **sem default em nenhum perfil**: variável ausente derruba o boot (Tarefa 0).

- [ ] **Step 1: Testes que falham:**
  - Listener de e-mail: o `EmailMessage` tem o destinatário certo, o assunto, e o HTML e o texto contêm o link com o segredo; **nada é logado com o segredo** (capturar o appender e conferir); falha do `EmailSenderService` não propaga para quem publicou o evento.
  - `SessionRevokedListener` remove exatamente a chave `iam:session:<hash>` (Review Focus 5); `UserDeactivatedListener` revoga todas as sessões ativas; `UserRoleChangedListener` remove as chaves de cada sessão.
  - `AdminSeederTest`: com usuários existentes, `isAlreadySeeded()` é verdadeiro e nada é criado; sem usuários, cria um `ACTIVE`, `ADMINISTRATOR`, `mustChangePassword=true`, com o hash (nunca a senha) guardado.
  - `IamPropertiesTest`: os defaults batem com os valores da spec.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(iam): wire listeners, beans and the initial administrator seed`.

---

## Bordas (`web`, `boot`)

### Tarefa 12: Controllers

**Files:** `edge/web/src/main/java/com/crm/web/iam/{AuthController,UsersController,SessionsController}.java`, `.../iam/requests/*`, `.../iam/ActorArgumentResolver.java`, `WebIamConfig.java`; testes `*ControllerTest` com `MockMvc` standalone.

**Interfaces:** rotas exatamente as de `usecases.md` (`POST /auth/login`, `POST /auth/logout`, `POST /auth/invitation/accept`, `POST /auth/password/forgot` (204), `POST /auth/password/reset`, `PUT /auth/password`, `GET /auth/me`, `GET|POST /users`, `GET /users/{id}`, `POST /users/{id}/invitation`, `PUT /users/{id}/role`, `POST /users/{id}/deactivation`, `POST /users/{id}/reactivation`, `GET|DELETE /users/{id}/sessions`, `DELETE /sessions/{id}`). O controller só injeta use cases e **nunca** `domain` nem repository. `ActorArgumentResolver` lê o `Actor` do atributo de requisição `ActorRequestAttribute.NAME` (constante em `web`), que a cadeia de `boot` preenche (Tarefa 13); sem ele → `UnauthorizedException`. `origin` vem do IP do cliente (`server.forward-headers-strategy=native` já está ligado). O cabeçalho `X-Device-Name` (app) ou o `User-Agent` vira o rótulo do aparelho, truncado em 80 caracteres e com padrão `"Aparelho desconhecido"`. **Credencial ([`architecture.md`](../rules/architecture.md) §14):** `AuthController` devolve, em `login` e `invitation/accept`, `SessionView(expiresAt, mustChangePassword)` com `Set-Cookie: crm_session=<token>; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=34560000` (o `Secure` fica desligado só no perfil `dev`). O token **nunca** vai no corpo. `logout` limpa o cookie. O formato do aplicativo (token no corpo com `X-Client: app` e `Authorization: Bearer`) fica para o plano do Capacitor.

- [ ] **Step 1: Testes que falham:** cada rota chama o use case certo com os parâmetros certos e devolve o status esperado (200/201/204); `PUT /auth/password` e `POST /auth/login` com corpo inválido → 400 com `invalidParams`; erro de use case sai como `ProblemDetail` com `code` (via `RestExceptionHandler` existente); rota autenticada sem `Actor` → 401.
  - Credencial: `login` traz `Set-Cookie` com `HttpOnly`, `SameSite=Lax` e o corpo **sem** nenhum campo `token`; o mesmo vale para `invitation/accept`; `logout` responde com `Set-Cookie` de `Max-Age=0`.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(web): add iam controllers`.

### Tarefa 13: Cadeia de segurança

**Files:** `edge/boot/pom.xml` (dependência explícita em `iam-application`); `edge/boot/src/main/java/com/crm/boot/security/{SecurityConfig,SessionAuthenticationFilter}.java`; testes `SecurityChainTest` (`@SpringBootTest` leve com `MockMvc`, ou slice com `@WebMvcTest` mais a cadeia, e `AuthenticateSession` mockado).

**Interfaces:** `SecurityFilterChain` stateless; **CSRF ligado** com `CookieCsrfTokenRepository.withHttpOnlyFalse()` (cookie `XSRF-TOKEN`, cabeçalho `X-XSRF-TOKEN`; [`architecture.md`](../rules/architecture.md) §14); públicas só `POST /auth/login`, `/auth/invitation/accept`, `/auth/password/forgot`, `/auth/password/reset` e `GET /actuator/health` (e o springdoc só no perfil `dev`); o resto exige autenticação. `SessionAuthenticationFilter` lê o token do cookie `crm_session` e chama `AuthenticateSession.execute(token)`, põe o `Actor` em `ActorRequestAttribute.NAME` e no `SecurityContext`; `UnauthorizedException` vira `401` com `ProblemDetail` e `code=session_invalid` (a exception handler MVC não alcança o filtro: serializar à mão, no mesmo formato de `RestExceptionHandler`). **Sessão restrita:** com `mustChangePassword`, qualquer rota fora de `PUT /auth/password`, `POST /auth/logout` e `GET /auth/me` → `403` com `code=password_change_required`.

- [ ] **Step 1: Testes que falham:** rota protegida sem cookie → 401; token inválido → 401 `session_invalid`; token válido chega ao controller com o `Actor` certo; **sessão com `mustChangePassword` em `GET /users` → 403 `password_change_required`, e em `PUT /auth/password`, `POST /auth/logout`, `GET /auth/me` passa** (Review Focus 3); as quatro rotas públicas respondem sem token; **cookie válido em `POST` sem `X-XSRF-TOKEN` → 403; cookie e `X-XSRF-TOKEN` certos passam**.
- [ ] **Step 2 a 4:** falhar, implementar, passar.
- [ ] **Step 5: Rodar os testes de arquitetura:** `mvn test -Dtest=BoundedContextRulesTest`. Se falhar por `boot` depender de `iam-application`, a exceção já está declarada em `architecture.md` §4.1: restrinja-a ao pacote `com.crm.boot.security..` na regra e documente em §11. **Não afrouxe nenhuma outra regra.**
- [ ] **Step 6: Commit:** `feat(boot): add session security chain`.

### Tarefa 14: Propriedades, ponta a ponta e fechamento

**Files:** `edge/boot/src/main/resources/application.properties` (`crm.iam.*` comentadas, sem acento), teste `IamEndToEndTest` (`@SpringBootTest` com Testcontainers Postgres e Redis; só roda com Docker. O RabbitMQ não é necessário: o IAM não publica integration event); skills; docs.

- [ ] **Step 1: Teste ponta a ponta que falha:** com o seeder criando o Administrador (variáveis de teste), fluxo completo: login → `mustChangePassword` → `403` em `GET /users` → `PUT /auth/password` → `GET /users` 200 → convidar Técnico (e-mail capturado por um `EmailSenderService` falso) → `POST /auth/invitation/accept` com o link → login do Técnico → Administrador encerra a sessão do Técnico → a próxima chamada do Técnico é `401 session_invalid` **imediatamente** → desativar → login do Técnico falha com `invalid_credentials` → reativar → login volta a funcionar. Mais: limite de login (a sexta tentativa errada em 15 min dá `429 rate_limited`, mesmo para e-mail inexistente).
- [ ] **Step 2: Rodar e ver falhar; Step 3: corrigir o que o teste achar; Step 4: rodar e ver passar** (`mvn test` inteiro, em máquina com Docker).
- [ ] **Step 5: Fechamento da documentação:** (a) `open-questions.md` da feature: remover o que virou código (seeder, hash), manter o rótulo do aparelho se mudou; (b) skills: `crm-add-use-case` passa a apontar `Login`/`ListUsers` e os controllers como referência viva e a descrever o contrato de leitura; `crm-domain-rules` registra o ADR-9 como padrão de ciclo simples; `crm-domain-events` aponta `OneTimeTokenIssuedListener` como referência de listener; (c) `README` da feature: marcar o critério de aceite do backend; (d) conferir os links relativos de `docs/`.
- [ ] **Step 6: Commit:** `docs(iam): close the backend plan and update skills` (e `test(iam): add end-to-end flow`, em commits separados).

---

## Fora deste plano

- **Formato do aplicativo** (`Authorization: Bearer`, `X-Client: app` e o token no corpo do login, CORS para a origem do webview, `crm.cors.allowed-origins`): entra com o plano do Capacitor, que ainda não existe ([`architecture.md`](../rules/architecture.md) §14).
- **Frontend** das telas aprovadas em [`design/`](../system-design/01-iam/design/README.md): [`2026-11-19-iam-frontend.md`](2026-11-19-iam-frontend.md). O aplicativo (Capacitor) fica para depois.
- **Rotas do frontend nos links de e-mail** (`/convite?token=`, `/redefinir-senha?token=`): as mesmas que o plano do frontend implementa.
- **Limpeza** periódica de sessões e tokens antigos.
- **MFA do Administrador**, login social, SSO, cliente final e link mágico (README da feature).
