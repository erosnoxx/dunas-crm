# Arquitetura — crm

Fonte de verdade da arquitetura do backend. Os `CLAUDE.md` e as skills `crm-*` **referenciam** este documento: nenhum deles reexplica o que está aqui.

Se uma regra deste documento conflitar com outro arquivo do repositório, **este documento vence**. Se conflitar com o que uma tarefa pede, pare e pergunte: não invente convenção.

**Como ler.** Regra que um teste ArchUnit trava aparece numa linha, com o nome do teste (**Travado:**), sem justificativa longa: quebrar o build já é a explicação. O texto longo fica para o que o código não impede sozinho. A numeração das seções é referenciada por comentários no código (`architecture.md §8`): não renumere.

---

## 1. O problema que esta arquitetura resolve

DDD + Clean Architecture em que cada bounded context (BC) é um **módulo Maven de verdade**, dividido em camadas. Isolamento por convenção não se sustenta: com módulos de camada compartilhados e BCs como subpastas, nada impede um domínio de importar outro em tempo de compilação. Aqui o Maven é o guarda: se `a-application` não declara dependência em `b-application`, o import não compila. A regra sai do texto e entra no build.

O Maven só barra o que não está declarado, e um `pom.xml` pode ser editado. Por isso o ArchUnit (§11) é a segunda cerca, e ele roda no `mvn test`.

---

## 2. Estrutura do repositório

```
crm/
  backend/     módulos Maven (único artefato deployável: edge/boot)
  frontend/    web responsiva
  mobile/      aplicativo
  infra/       docker, envs, scripts
  docs/        rules/ (este documento e as demais regras), system-design/, plans/, diagrams/, market-research/
```

`docs/system-design/` guarda o domínio e os casos de uso de cada feature (modelo em `_template/`). Este documento cobre só o que vale para o sistema todo.

---

## 3. Módulos Maven

```
backend/
  pom.xml                       pai (spring-boot-starter-parent; versões de terceiros no dependencyManagement)

  kernel/
    common/                     kernel TÉCNICO: framework, zero significado de negócio
      common-domain/
      common-application/
      common-infrastructure/
    shared/                     kernel de NEGÓCIO: conceitos usados por mais de um BC
      shared-domain/
      shared-application/
      shared-infrastructure/

  core/                         agregador dos bounded contexts
    iam/
      iam-domain/
      iam-application/
      iam-infrastructure/

  edge/                         borda: o que enxerga mais de um BC
    orchestration/              composição cross-BC, independente de mecanismo de entrega (§5.4)
    web/                        controllers e tradução de erro HTTP
    boot/                       @SpringBootApplication, único deployável
```

`kernel`, `kernel/common`, `kernel/shared`, `core`, `core/<bc>` e `edge` são **agregadores** (`packaging=pom`, sem código). Só os módulos folha têm `src/`.

`groupId` é `com.crm`; o pacote base é `com.crm`. O pacote de um módulo é o nome do agregador sem prefixo: `com.crm.common.domain`, `com.crm.shared.domain`, `com.crm.iam.application`, `com.crm.web`, `com.crm.boot`.

### 3.1 As duas "commons"

Confundir as duas é o erro mais fácil e mais caro: é assim que regra de negócio vaza para o framework e vice-versa.

**`common`: técnico.** Esqueleto reutilizável, sem nenhum significado de negócio.

| Módulo | Conteúdo |
|---|---|
| `common-domain` | `DomainEntity`, `AggregateRoot` (+ domain events e versão), `DomainEvent`, `IntegrationEvent`, `ValueObject<T>` e as bases `StringValueObject`, `BigDecimalValueObject`, `LocalDateValueObject`, `TimeRange`, `Url`, `DomainException`, `ValidationException`, `StateMachine`, `Specification<T>` |
| `common-application` | `@UseCase`, `@CriteriaField`, `Repository<A, C>`, `Criteria`, `Page`/`Pageable`, `EventPublisher`, contratos de outbox (`OutboxWriter` e `OutboxReader`, separados), `IntegrationEventSender`, `Cache`, `IdempotencyGuard`, `RateLimiter`, `FileStorage`, `EmailSenderService`, `BaseMapper`, as exceptions de application (as que carregam status HTTP) |
| `common-infrastructure` | `TimeConfig` (o `Clock`), `JpaEntity` e o listener de timestamps, `EntityMapper`, `RepositoryImpl`, `CriteriaUtils`, o aspect de `@UseCase`, `EventPublisher` (Spring), outbox (JPA + job de despacho), RabbitMQ, cache e rate limit (Redis), armazenamento S3, SMTP, `IdempotencyGuard` (JPA), observabilidade (correlação), execução assíncrona, seeder |

**`shared`: negócio.** Conceitos de **domínio** que mais de um BC precisa (`Money`, `Email`, `Document`, `Address`...), e os schemas de resposta comuns (`UUIDResponse`). Herdam dos tipos base de `common`.

- `shared-domain`: VOs de `address`, `contact`, `identity` (com os validadores de CPF e CNPJ, inclusive o alfanumérico) e `misc`, mais enums de negócio (`UF`).
- `shared-application`: schemas de request/response compartilhados.
- `shared-infrastructure`: vazio até existir algo que opere sobre os VOs compartilhados sem pertencer a nenhum BC. **Não forçar conteúdo**: módulo vazio é um estado legítimo.

**Regras absolutas:**

- Nada de regra de negócio em `common`; nada de framework em `shared`.
- `common-domain` não depende de nada. `shared-*` só depende do `common-*` **da mesma camada**.
- Nem `common` nem `shared` conhecem um BC. Nunca. **Travado:** `BoundedContextRulesTest` (`commonDoesNotDependOnBoundedContextsOrSharedKernel`, `sharedKernelDoesNotDependOnBoundedContexts`).

**Onde mora uma classe, em caso de dúvida:** se o nome faz sentido num sistema que não faz o que este faz, é `common`. Se só faz sentido aqui, é `shared` (quando mais de um BC usa) ou o próprio BC (quando só um usa). Na dúvida, comece dentro do BC: promover depois é barato, despromover não é.

---

## 4. Regra de dependência

Dentro de cada BC (exemplo com `iam`):

```
iam-domain          →  common-domain         +  shared-domain
iam-application     →  common-application    +  iam-domain      (+ shared-application, se usar)
iam-infrastructure  →  common-infrastructure +  iam-application
```

Nos módulos de borda:

```
orchestration  →  common-application + <bc>-application dos BCs que compõe
web            →  <bc>-application (só dos BCs que expõem endpoint) + orchestration
boot           →  web + orchestration + common-infrastructure + <bc>-infrastructure de todos os BCs
                  + iam-application (só a cadeia de segurança, §4.1)
```

### 4.1 Regras que nunca são quebradas

- **Nenhum BC depende de outro BC**, em nenhuma camada, nem no `pom.xml` nem por import. Referência cross-BC é sempre **UUID cru**: nunca relação JPA, nunca FK no banco, nunca aggregate ou repository do outro BC. **Travado:** `noBoundedContextDependsOnAnother`, `noCyclesBetweenBoundedContexts`.
- **Comunicação entre BCs é por integration event** (§5).
- Direção única: `domain ← application ← infrastructure`. **Travado:** `domainDoesNotDependOnOuterLayers`, `applicationDoesNotDependOnInfrastructure`.
- **`domain` é Java puro**: sem Spring, JPA, Jackson nem Bean Validation. O domínio valida pelos Value Objects, em código. **Travado:** `domainDoesNotDependOnFramework`.
- **`application` é Java puro**, com duas exceções declarativas: as anotações do projeto (`@UseCase`) e **Bean Validation** (`jakarta.validation`) nos schemas de request, que entra na mesma categoria do Lombok. Sem `@Transactional`, `@Service`, `@Retryable`, `jakarta.persistence`. **Travado:** `applicationDoesNotDependOnFramework`.
- **`web` injeta use cases e nada mais.** Não depende de `<bc>-domain` nem da infraestrutura, exceto `..infrastructure.integrations..` do BC que recebe um webhook (§4.3). **Travado:** `webDependsOnlyOnApplicationAndIntegrations`.
- **Uma classe de `web` depende da `application` de no máximo um BC.** Se o fluxo precisa de mais, a composição mora em `orchestration` (§5.4). **Travado:** `webDependsOnAtMostOneBoundedContextPerClass`.
- **`boot` recebe só configuração que enxerga mais de um BC**: a cadeia de segurança, o Flyway dos módulos, CORS global, Actuator, profiles, o filtro de correlação. Config de um BC só mora na `<bc>-infrastructure` dele (§9).
- **A cadeia de segurança de `boot` usa um use case do `iam`** (`AuthenticateSession`, que devolve quem está agindo) e é a **única** classe de `boot` que depende de `iam-application`: é a exceção declarada. Ela também impõe a sessão restrita (conta que precisa trocar a senha só chama `ChangePassword`, `Logout` e `GetCurrentUser`). Nenhum outro BC enxerga o IAM: as demais features recebem o `Actor` já montado pela `web`. O ArchUnit confere se a regra continua válida quando a cadeia for escrita.

Os BCs são descobertos do classpath (todo `com.crm.<nome>` com camada `domain`, exceto `common` e `shared`): um BC novo entra nas regras sem editar nenhum teste.

### 4.2 Adicionar um BC novo

Pasta `core/<bc>/` com `pom.xml` agregador e três módulos folha; `<module>` registrado em `core/pom.xml`; as dependências da §4; `<bc>-infrastructure` adicionado ao `boot`; e, se o BC tem tabelas, migrations em `db/migration/<bc>/` (§8). Nada mais: o ArchUnit e o Flyway descobrem o BC sozinhos. Passo a passo: skill `crm-add-bounded-context`.

### 4.3 Sistemas de terceiro: Anti-Corruption Layer

**O domínio é neutro e isolado.** Nenhum conceito de sistema de terceiro o contamina: status, códigos, nomes de campo, formatos, regras de interpretação. Toda integração com terceiro passa por um Translator, sem exceção.

Terceiro é sistema de fora acessado pela rede (um gateway de pagamento, a nuvem de um fabricante de inversor, um provedor de e-mail). A nossa infraestrutura (Postgres, Redis, RabbitMQ) não entra: o modelo dela é nosso.

```
<bc>.application.contracts.services
  <Nome>Service                           contrato, desenhado a partir do domínio

<bc>.infrastructure.integrations.<nome>
  <Fornecedor><Nome>ServiceImpl           implementa o contrato; chama o Client e o Translator
  <Fornecedor>Client                      fala com o terceiro; só conhece o modelo dele
  <Fornecedor>Translator                  única classe que conhece o modelo externo e o nosso
  dtos/                                   o modelo externo; nunca sai da integração
```

Exemplo real: `common.infrastructure.integrations.smtp` (`SmtpClient`, `SmtpTranslator`, `SmtpEmailSenderServiceImpl`). Integração usada por mais de um BC mora em `common.infrastructure.integrations.<nome>`, com o contrato em `common-application`.

- O contrato e os tipos dele nascem do domínio, nunca espelham a API externa. Trocar de fornecedor não muda uma linha de `domain` ou `application`.
- O `ServiceImpl` orquestra: pede o formato externo ao Translator, chama o Client, pede a volta ao Translator.
- Fornecedor com **várias marcas para o mesmo conceito** (ex. um contrato único, uma implementação por fabricante) segue a mesma forma: uma pasta `integrations.<marca>` por fornecedor, todas atrás do mesmo contrato.
- **O que não pode acontecer:** enum ou VO do domínio espelhando valor externo; lógica de interpretação ("status X ou Y conta como pago") fora do Translator; contrato com assinatura copiada da API externa; implementação repassando dado 1:1 sem traduzir conceito.
- Todo Translator tem teste unitário com payload de exemplo real do terceiro, sem rede.

**Travado:** `AntiCorruptionLayerRulesTest`: só `..integrations..` fala com terceiro; DTO externo só é acessado pela própria integração; todo `*ServiceImpl` de integração depende do Translator dela; `*Translator` só existe em integração. **Ao adicionar uma integração que use SDK próprio, registre o pacote do SDK na lista `THIRD_PARTY_ACCESS` do teste**: com SDK, o modelo externo são as classes dele, e fora da lista nada impede que vazem.

**Webhook de entrada.** O controller mora em `web` como todo endpoint: recebe o DTO do terceiro, entrega ao Translator e chama o use case com tipos nossos. Para isso `web` enxerga `..infrastructure.integrations..` do BC que recebe o webhook, e nada mais da infraestrutura dele: um controller que injetasse repository ou entidade JPA pularia o use case (transação, log, regras). Quando o `pom.xml` de `web` passa a depender de uma `<bc>-infrastructure`, a cerca contra repository e JPA deixa de ser do Maven e passa a ser só do ArchUnit.

---

## 5. Comunicação entre bounded contexts

**Um mecanismo assíncrono: integration event, via outbox + RabbitMQ.** Não há chamada síncrona entre BCs.

- O produtor grava o integration event em `outbox_event` **na mesma transação** do aggregate: ou o estado e o evento persistem juntos, ou nenhum dos dois.
- `OutboxDispatcherJob` lê os pendentes (`FOR UPDATE SKIP LOCKED`, em lote) e entrega ao `IntegrationEventSender` (RabbitMQ). A entrega é **at-least-once**: todo consumidor é idempotente.
- Os contratos são separados: **writer** (usado pela `<bc>-application` produtora) e **reader** (usado pelo job), ambos em `common-application`, implementados em `common-infrastructure`.
- O consumo entra pela **`<bc>-infrastructure` do BC consumidor**. O listener é um adapter de entrada, como um controller: traduz a mensagem e chama um use case da própria `<bc>-application`. Queue, binding e DLQ de cada BC consumidor moram na infraestrutura dele; a exchange e a fábrica de listener (sem requeue infinito: falha vai para a DLQ) são do `common`.
- O outbox **grava sempre**. Sem `IntegrationEventSender` registrado, os eventos acumulam como `PENDING` e saem quando ele existir: nunca se descarta evento em silêncio.
- Falha do broker aborta o ciclo sem penalizar evento. Falha de **uma** mensagem usa backoff exponencial até `crm.outbox.max-attempts`; estourado, vira `DEAD`, sai da fila, loga em `ERROR` e **não é reprocessado sozinho**: é desenhado para forçar alguém a olhar.

### 5.1 Domain event ≠ integration event

| | Domain event | Integration event |
|---|---|---|
| Alcance | dentro do mesmo BC | cruza BC |
| Transporte | in-process, entregue **depois do commit** (sem transação ativa, na hora) | outbox + RabbitMQ |
| Contrato | interno, muda livremente | público, versionado com cuidado; `eventType()` é a routing key |

Em ambos o **aggregate só registra** o evento (`registerEvent`). Quem **publica** é a `application`, depois do `save`: `publisher.publishAll(aggregate.pullEvents())`. O domínio nunca publica, e o repository nunca publica por conta própria. Puxe os eventos da instância que você alterou, não da devolvida pelo `save` (a devolvida vem do banco, sem eventos).

### 5.2 Não existe chamada síncrona entre BCs

Nem leitura direta da tabela de outro BC, nem chamada ao use case dele: ambas exigiriam que um BC dependesse do outro. Os dois problemas que isso resolveria têm substituto próprio:

- **Ler um fato de outro BC** → projeção local (§5.3).
- **Comandar escrita em outro BC e usar o resultado no mesmo request** → orquestração em `orchestration` (§5.4).

### 5.3 Leitura de fato alheio: projeção local

O BC mantém uma cópia local, pequena e só-leitura, do fato, atualizada exclusivamente por listener de integration event, e lê a **própria** tabela. A tabela mora na `<bc>-infrastructure` do consumidor.

Serve só para leitura que **tolera estar um pouco velha**, em que o pior caso é a tela parecer estranha por um instante, nunca uma invariante quebrada. A defasagem no caminho feliz é de segundos, dominada por `crm.outbox.poll-delay`. Trate como estado de carregamento na UI.

**Decisão que precisa estar certa agora** (estoque, saldo, vaga) não é fato a copiar: é invariante que mora inteira dentro do BC dono, decidida na transação dele. Quem precisa dela manda o BC dono **fazer** (§5.4), nunca consulta uma cópia.

### 5.4 Comando cross-BC: orquestração em `orchestration`

A regra nunca foi "nenhum código conhece dois BCs", foi "nenhum BC depende de outro BC". `orchestration` é irmão de `web`: depende da `-application` de cada BC que compõe, não sabe nada de HTTP (qualquer ponto de entrada futuro chama a mesma classe), e nenhum BC depende dela.

- **Sem transação cross-BC.** Os passos não são atômicos: se o segundo falha depois do primeiro, não há rollback. É uma saga orquestrada, e **a sequência e a compensação de cada fluxo são escritas à mão, uma classe por fluxo**, nunca um runner genérico com hook `compensate()`. Isso é deliberado: o runner esconderia a decisão "o que fazer se falhar depois de ter dado certo" atrás de um método fácil de esquecer.
- A classe de orquestração não leva anotação (como um use case); o bean é registrado numa `@Configuration` do módulo (§9).
- **A única peça compartilhada é a deduplicação:** `IdempotencyGuard.runOnce(chave, fingerprint, tipo, operação)`. A chave vem de **quem chama** (o cliente repete a mesma ao repetir a requisição); gerá-la dentro do orquestrador anularia a deduplicação.

| Situação | Comportamento |
|---|---|
| Chave nova | executa, grava o resultado |
| Repetida, mesmo payload, `COMPLETED` | devolve o resultado gravado, sem executar |
| Repetida, mesmo payload, `IN_PROGRESS` | `ConflictException` (409) |
| Repetida, mesmo payload, `FAILED` | reabre para nova tentativa, de forma atômica |
| Repetida, payload diferente | `ConflictException` (409) |

Reivindicar a chave e registrar o resultado são **duas transações curtas e separadas**, e `operation` roda fora das duas. Isso não é detalhe: uma transação em volta de `operation` faria cada `@UseCase` chamado entrar nela, fundindo BCs numa transação de fato e prendendo locks pelo fluxo inteiro. O mesmo guard serve a qualquer entrada que possa chegar duplicada (webhook).

### 5.5 O caso comum: nenhum dos dois

Um use case que só lê e escreve aggregates do **próprio** BC roda numa transação ACID normal: a maioria absoluta dos casos, sem mecanismo nenhum. Cruzar fronteira de BC é a exceção que precisa de justificativa, não um padrão a aplicar por reflexo.

```
Tudo que o use case lê e escreve é do próprio BC?  → @UseCase comum.
Precisa LER fato de outro BC, e atraso não quebra nada?  → projeção local (§5.3)
Precisa COMANDAR escrita em outro BC e usar o resultado?  → orchestration (§5.4)
```

---

## 6. Regras de modelagem

### 6.1 Entidade e aggregate

Todo aggregate é uma entidade; nem toda entidade é um aggregate.

| | Entidade interna | Aggregate root |
|---|---|---|
| Base | `DomainEntity` | `AggregateRoot` |
| Registra domain event | não | sim |
| Repository próprio | **não**: carregada e salva pelo root | sim: `Repository<A extends AggregateRoot, C extends Criteria>` |
| Decide-se por | só existe acessada através de outra | algum use case a busca ou salva sozinha |

- **O id é `UUID`, gerado pelo domínio** (`DomainEntity` o cria; recusa id nulo). O banco nunca gera id. Igualdade é classe + id.
- **O domínio não tem `createdAt`/`updatedAt`**: esses campos são da persistência (§8). Data que importa ao negócio ganha nome de negócio (`placedAt`, `expiresAt`).
- Mutação só por método de domínio nomeado pela intenção; nenhum setter público. Criação por factory `create`; reconstrução do banco por `reconstitute`, que recebe a **versão** lida (`AggregateRoot` a guarda).
- Aggregate é **fronteira de consistência transacional**. Consistência imediata: no mesmo aggregate, ou em dois do mesmo BC alterados na mesma transação (exceção pragmática; a regra que os liga mora no domínio, em quem é dono dela ou num Domain Service, nunca no use case). Consistência eventual: aggregates separados + domain event (§5.1).
- **Value Object nunca aceita `null`**: a base `ValueObject` lança `ValidationException("o valor não pode ser nulo")`, que sai como 400 (§7.3). Cada VO acrescenta a regra própria e uma mensagem específica quando faz sentido.
- Os aggregates de cada BC e o modelo deles ficam em `docs/system-design/<feature>/domain-model.md`, nunca duplicados aqui.

### 6.2 Cache

Redis. **Invalidação por evento é o mecanismo principal**; TTL curto é rede de segurança. O banco é a fonte de verdade: lock de recurso disputado é por constraint ou `FOR UPDATE`, **nunca** por Redis. Redis é otimização de leitura, não mecanismo de correção.

### 6.3 Hora

**A única fonte de hora é o bean `Clock`** de `TimeConfig`, no fuso `America/Sao_Paulo` (o nome do fuso, e não `-03:00`, acompanha mudança de horário de verão).

- **O domínio nunca lê o relógio.** Método que depende de tempo recebe `Instant now` ou um `Clock` como parâmetro (ex. `BirthDate.of(valor, clock)`). Toda regra de tempo fica determinística em teste.
- **Application e infrastructure** recebem o `Clock` injetado, inclusive para os timestamps de persistência, o outbox e a idempotência.
- `System.nanoTime()` é permitido: mede duração.

**Travado:** `ClockRulesTest`: nenhuma leitura direta de hora em produção, e só `TimeConfig` cria um relógio de sistema.

### 6.4 Onde mora cada regra de negócio

A regra mora no domínio; o use case só orquestra (carrega, chama o domínio, salva, publica).

| A regra... | Mora em |
|---|---|
| precisa ser verdade para o objeto existir válido | entidade ou VO |
| é uma condição sim/não reutilizável ou combinável | **Specification** (`Specification<T>` em `common-domain`; classe nomeada no BC) |
| é uma decisão que muda conforme o contexto | **Policy**: interface própria de cada decisão, uma implementação por variante |
| liga aggregates do mesmo BC sem dono natural | **Domain Service**: sem estado, sem repository |
| é o ciclo de vida de um aggregate (status e transições) | **State machine** sobre `StateMachine` de `common-domain` |

Não existe interface genérica `Policy` nem anotação `@DomainService`/`@Policy`: cada Policy tem assinatura própria. Dado externo entra por parâmetro ou construtor, nunca por consulta. Specification é predicado em memória; busca e listagem continuam em `Criteria`. Domain Service nunca cruza BC (isso é orquestração).

---

## 7. Use cases

### 7.1 Forma

- **Classe concreta, sem interface e sem sufixo `UseCase`**: `CancelVisit`, não `CancelVisitUseCase`. É ela que `web`, `orchestration` e os listeners injetam; a classe concreta é mockável.
- **Sem regra de negócio**: carrega, chama o domínio, salva, publica. Um `if` de negócio aqui desce para entidade, Specification, Policy ou Domain Service (§6.4). Exceção: checagem que depende de um `<Nome>Service` de capacidade externa que o domínio não enxerga.
- Mora em `application.usecases.commands` (muda estado, sempre pelo aggregate; devolve `UUIDResponse` na criação ou nada) ou `application.usecases.queries` (só lê, não carrega aggregate, devolve DTO/projeção; `@UseCase(transactional = false)`). **Não é CQRS**: um banco, um modelo, só separação de pacote.
- Um método público, `execute(...)`; `record Params` aninhado quando há mais de um argumento; `@RequiredArgsConstructor`.
- O `Clock` vem por construtor e o use case passa `Instant.now(clock)` ao domínio.

### 7.2 Comportamento transversal: `@UseCase`

Anotação própria em `common-application`, **sem Spring**:

| Parâmetro | Default | Quando mexer |
|---|---|---|
| `transactional` | `true` | query desliga |
| `retriable` | `false` | só quem chama serviço externo diretamente, e só com efeito idempotente: o retry reexecuta o use case inteiro |
| `maxRetries` | `3` | conta repetições, não tentativas |
| `circuitBreaker` | `false` | só quem chama serviço externo diretamente |

O aspect (`common-infrastructure`) vale para todos os BCs sem registro extra. Ordem, de fora para dentro: **log → retry → circuit breaker → transação → tradução de exception → use case**. A transação fica dentro do retry (cada tentativa tem a sua), e a tradução é a mais interna. Só `ExternalServiceException` e falha técnica crua são retentáveis; erro de negócio nunca. O log registra nome, duração e, em falha, tipo e mensagem da exceção: **nunca os argumentos**, e por isso a mensagem de exceção nunca carrega valor sensível. Sucesso de command vai para INFO, de query para DEBUG, falha para WARN.

**Nunca** `@Transactional` nem `@Retryable` do Spring na `application`.

### 7.3 Erros: status HTTP e código para o frontend

O aspect traduz na saída do use case, e a `web` só conhece tipos de application:

| Origem | Vira | Status |
|---|---|---|
| `ValidationException` (VO inválido) | `BadRequestException` | 400 |
| `DomainException` (regra violada) | `UnprocessableEntityException` | 422 |
| as demais `ApplicationException` | elas mesmas | 401, 403, 404, 409, 429, 503; fora disso, 500 |

`ValidationException` estende `DomainException`: o `catch` dela vem antes, senão todo VO inválido viraria 422.

Vários erros dividem o mesmo status, e decidir pelo texto quebraria o frontend a cada ajuste de redação. Por isso um erro pode levar um **código estável**, `snake_case` em inglês, único dentro do BC: `DomainException` e `ApplicationException` têm um campo `code` opcional, o aspect o preserva na tradução, e o `RestExceptionHandler` o põe no `ProblemDetail`:

```json
{ "status": 422, "detail": "este link expirou", "code": "link_expired" }
```

Campos inválidos de Bean Validation saem em `invalidParams`. **O código é contrato público**: renomear ou remover quebra o frontend e se faz junto com ele. Cada feature lista os códigos que usa no `usecases.md`.

---

## 8. Persistência e migrations

Postgres + Flyway, **um schema único (`public`)**. `ddl-auto=validate` e `open-in-view=false`: as migrations são a fonte de verdade do schema, e o Hibernate só valida.

**Cada módulo é dono das suas migrations e do seu histórico:**

```
core/iam/iam-infrastructure/src/main/resources/db/migration/iam/V1__create_users.sql
kernel/common/common-infrastructure/src/main/resources/db/migration/common/V1__create_outbox_event.sql
```

- Cada módulo numera de `V1__` em diante, **independente dos outros**. A tabela de histórico é `flyway_schema_history_<módulo>`.
- **Não há lista de módulos para editar**: `FlywayConfig` (em `boot`) descobre do classpath toda pasta `db/migration/<módulo>/` com `.sql` e registra um `Flyway` por módulo, rodando **antes** do `EntityManagerFactory`. O baseline é a versão 0, por módulo, porque o segundo Flyway encontra um schema já com tabelas.
- `common-infrastructure` também é dono de tabelas (`outbox_event`, `idempotent_execution`), em `db/migration/common/`: são mecanismos de todos os BCs, e pô-las na migration de um BC amarraria infraestrutura compartilhada ao ciclo dele.
- **Por que não numeração global:** com vários branches e um BC por feature, "o BC A cria a migration e o BC B mergeia primeiro" é rotina, e o Flyway trata isso como *out-of-order*. Histórico por módulo elimina a classe inteira do problema.
- **FK cross-BC é proibida.** O ArchUnit não lê SQL: a regra vive na revisão. Referência a outro BC é coluna `uuid` sem `REFERENCES`.
- **O id nunca é gerado pelo banco**: a coluna recebe o `UUID` do domínio. Sem sequence, identity nem `@GeneratedValue`.

**Lock otimista em todo aggregate.** O save grava o aggregate inteiro; sem versão, duas transações que leram o mesmo estado fariam a segunda desfazer a primeira em silêncio. `AggregateRoot` guarda a versão, a tabela tem a coluna, `JpaEntity` a mapeia com `@Version`, e o save com versão diferente falha como `ConcurrentUpdateException` (409): a pessoa tenta de novo, sem retry automático. `RepositoryImpl` grava na hora (flush), para constraint e versão serem conferidas ali e a instância devolvida já trazer a versão nova: ao gravar de novo na mesma requisição, siga com a instância devolvida. Transição que duas requisições disputam por natureza (quem perde recebe resposta de negócio, não 409) é um update condicional no contrato do repository.

**`created_at`/`updated_at` são só da persistência.** Toda JPA entity que estende `JpaEntity` os ganha, preenchidos por um listener com a hora do `Clock` (§6.3); o mapper nunca os leva ao domínio. **Armadilha conhecida:** `updated_at` nunca pode ser `NOT NULL` na migration (o primeiro INSERT o deixa nulo).

**Repository:** o contrato (`Repository<A, C>`) é do domínio e mora em `application.contracts.repositories`; a implementação estende `RepositoryImpl<A, C, P, R>` na infraestrutura, com um `EntityMapper<A, P>` (domínio ↔ JPA). Filtro de listagem é um `Criteria` (sempre um `record`) cujos componentes levam `@CriteriaField`, ligando-os ao atributo da entity.

---

## 9. Registro de beans

Sem wiring automático de lógica: **nada de `@Component`/`@Service`** em use case, repository impl ou policy.

- Cada BC registra os próprios beans numa `@Configuration` dentro da sua **`<bc>-infrastructure`** (ex. `IamBeans`): um arquivo por BC. Use case e Policy são instanciados ali, com `new`.
- O `common` expõe suas peças do mesmo jeito: uma `@Configuration` por mecanismo (`TimeConfig`, `OutboxConfig`, `MessagingConfig`...). A aplicação varre `com.crm`, então qualquer `@Configuration` é encontrada.
- Configuração de mecanismo vem de `@ConfigurationProperties` com prefixo `crm.*`, em `record` com `@DefaultValue`: **subir o app não exige configurar nada**. O processador de configuração gera os metadados.
- `boot` só registra o que cruza BCs (§4.1).

---

## 10. Nomenclatura

### 10.1 Contrato e implementação

Contrato tem **nome limpo**; implementação tem **prefixo tecnológico** e sufixo `Impl`:

```
EmailSenderService     →  SmtpEmailSenderServiceImpl
UserRepository         →  JpaUserRepositoryImpl
EventPublisher         →  SpringEventPublisherImpl
IntegrationEventSender →  RabbitIntegrationEventSenderImpl
Cache                  →  RedisCacheImpl
```

A tecnologia vai no início; `Impl` fecha o nome. A convenção só vale quando contrato e implementação estão em **camadas diferentes**. As bases genéricas de `common-infrastructure` (`RepositoryImpl`, `EntityMapper`, `JpaEntity`) mantêm nome neutro.

### 10.2 Idioma

- Identificadores em **inglês**.
- Comentário, Javadoc, `@DisplayName` de teste e texto de regra ArchUnit em **português (pt-BR)**.
- **Mensagem de exceção e de log em pt-BR, minúscula**, sem ponto final, direta e sem jargão: a de exceção pode chegar ao usuário pelo frontend. Estilo de código: `docs/rules/code-style.md`.
- Arquivo `.properties` é lido como ISO-8859-1: **texto sem acento** nos comentários e valores.

---

## 11. Enforcement

O Maven barra o import não declarado, mas um `pom.xml` pode ser editado, e regra de prompt é melhor esforço. O ArchUnit (`archunit-junit5`, em `edge/boot/src/test`, o único módulo que vê o classpath inteiro) torna o feedback determinístico. As regras existem antes do código (rodam vazias sem BC), e **afrouxar uma regra não é legítimo; adicionar é**.

| Teste | Trava |
|---|---|
| `BoundedContextRulesTest` | isolamento entre BCs, direção das camadas, framework fora de `domain` e `application`, fronteiras de `common` e `shared`, `web` só com `application` (e integrações) de um BC por classe, sem ciclos |
| `ClockRulesTest` | toda leitura de hora passa pelo `Clock` de `TimeConfig` |
| `AntiCorruptionLayerRulesTest` | terceiro só em `integrations`, DTO externo contido, `ServiceImpl` usa o Translator |
| `FlywayConfigTest` | a descoberta dos módulos de migration |

**O que nenhum teste trava** e vive na revisão: FK cross-BC no SQL; regra de negócio dentro de um use case; a escolha entre projeção e orquestração (§5).

Critério de aceite de toda mudança no backend: `mvn test` verde (o `BoundedContextRulesTest` roda ali, e pode ser pedido isoladamente com `-Dtest=BoundedContextRulesTest`).

---

## 12. Stack

- **Java 21**, Spring Boot 3.4.5. O `maven-enforcer-plugin` **recusa** outro JDK: em versões mais novas o Lombok e o Mockito falham com erros que não apontam a causa. Maven 3.9 ou superior.
- Versões de terceiros que o Spring Boot não gerencia ficam no `dependencyManagement` do `backend/pom.xml`; módulo folha declara sem `<version>`.
- **PostgreSQL** + Flyway (§8); **Redis** (cache e rate limit); **RabbitMQ** (integration events, §5).
- **Arquivos:** armazenamento compatível com S3 atrás da porta `FileStorage` (`common-application`), implementada com o AWS SDK v2 em `common-infrastructure` e ligada só quando `crm.storage.endpoint` existe. O banco guarda a chave do objeto, nunca a URL. Sem Translator, por ser infraestrutura nossa.
- **E-mail** por SMTP (`integrations/smtp`). **Resiliência:** Resilience4j (retry e circuit breaker, via `@UseCase`).
- **Documentação da API:** springdoc, **ligada só no perfil `dev`**: em produção é informação para quem ataca.
- **Perfis:** `dev` é o padrão por conveniência; **o deploy precisa definir `SPRING_PROFILES_ACTIVE=prod`**, senão a aplicação sobe em `dev`, apontando para localhost. O Actuator expõe só `health`.
- Hospedagem-alvo: uma VPS com todos os serviços no mesmo stack Docker. O que decorre disso (limites de memória, backup fora da VPS) é decisão de infraestrutura, em `infra/`.

### 12.3 Observabilidade

- **Dois ids no MDC** (`CorrelationContext`): `requestId` (uma unidade de trabalho: requisição, job ou mensagem) e `correlationId` (atravessa as fronteiras assíncronas). O filtro HTTP (`boot`) aceita `X-Correlation-Id` **válido** (`[A-Za-z0-9._-]{1,64}`, para ninguém injetar quebra de linha no log) e devolve `X-Request-Id` e `X-Correlation-Id`.
- O `correlationId` viaja no outbox (coluna `correlation_id`) e na mensagem do broker, e volta ao MDC no consumidor. Job agendado recebe um id novo por execução, e o executor assíncrono leva o MDC para a thread do pool.
- **Nunca** vai para o log dado pessoal, segredo, token nem payload de pagamento.
- `ERROR` significa "alguém precisa olhar": falha esperada de negócio nunca é `ERROR`.

---

## 13. Em aberto

Decisões ainda não tomadas ou peças ainda não escritas. Item resolvido sai daqui.

- **Segurança:** a cadeia de segurança (regra em §4.1) é escrita junto da feature `01-iam`, que ainda não tem código. Enquanto ela não existir, vale o padrão do Spring Security: **toda rota exige autenticação**.
- **Stack Docker de desenvolvimento** (`infra/`): ainda por escrever; o app espera Postgres, Redis e RabbitMQ.
- **Perfil `prod`:** `application-prod.properties` ainda não existe.
- **Heartbeat de jobs agendados** (outbox, e os que vierem): depende de onde ficará o monitor.
- **Mobile (Capacitor):** a empacotadora do app ainda não existe; a regra de sessão do app está no §14, e o resto vai para um plano próprio depois do esqueleto do frontend ([`frontend.md`](frontend.md)).

---

## 14. Sessão do cliente: navegador e aplicativo

O cliente web (navegador) e o aplicativo (Capacitor, que embrulha a mesma web) usam a **mesma sessão do servidor** (feature `01-iam`, ADR-1), apresentada de duas formas. O backend aceita as duas. **Hoje só o navegador está implementado**; o formato do aplicativo entra com o Capacitor, num plano próprio.

| | Navegador | Aplicativo |
|---|---|---|
| Credencial | cookie `crm_session`, `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/` | cabeçalho `Authorization: Bearer <token>` |
| Guarda | o JavaScript **nunca** vê o token | armazenamento seguro do aparelho |
| CSRF | **exigido** em toda escrita: cookie `XSRF-TOKEN` (legível) devolvido no cabeçalho `X-XSRF-TOKEN` | **dispensado**: a credencial não viaja sozinha |
| Origem | mesmo site da API (em dev, o proxy `/api`) | origem do webview, liberada por CORS (`crm.cors.allowed-origins`) |

- **Quem recebe o token no corpo.** Login e aceite de convite devolvem o token no corpo **só** quando a requisição traz `X-Client: app`. Sem esse cabeçalho (padrão, navegador) o servidor responde com `Set-Cookie` e o corpo não leva o token. Não há outro endpoint que devolva o token de uma sessão existente: um XSS no navegador não tem como obtê-lo.
- **Ordem na cadeia de segurança:** `Authorization` primeiro; sem ele, o cookie. Requisição com `Authorization` pula o CSRF; requisição só com cookie não pula.
- **`Max-Age` do cookie é o teto dos navegadores (400 dias).** A validade real e a renovação de 30 dias são do servidor, que é quem decide se a sessão vale ([`01-iam`](../system-design/01-iam/decisions.md) ADR-1 e ADR-6). Encerrar a sessão (logout ou revogação) vale nos dois formatos, porque o que se revoga é a sessão, não o cookie.
- **Por que dois formatos.** Cookie `HttpOnly` protege o navegador de roubo por XSS; no webview do aplicativo o cookie entre origens não é confiável (Android e iOS tratam como terceiro), e embrulhar o site remoto inteiro arrisca recusa na loja. Um token só em `localStorage` abriria mão da proteção do navegador, que é o canal principal.
