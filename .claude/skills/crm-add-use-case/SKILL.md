---
name: crm-add-use-case
description: Use ao escrever um caso de uso (command ou query) no crm — classe `@UseCase`, `Params`, uso do Clock, salvar e publicar eventos, erros, registro do bean na `<Bc>Beans`, e o que o controller de `web` pode fazer. Invoque antes de criar qualquer classe em `application.usecases` e antes de expor um endpoint.
---

# Caso de uso

Regras e motivo: [`architecture.md`](../../../docs/rules/architecture.md) §5.5 (quando um use case basta), §7 (forma, `@UseCase`, erros) e §9 (beans). O **quê** vem de `docs/system-design/<feature>/usecases.md` (pré/pós-condições e códigos de erro): sem ele fechado, sinalize a lacuna.

## Forma

- **Classe concreta, sem interface e sem sufixo `UseCase`**: `CancelVisit`. Pacote `application.usecases.commands` (muda estado) ou `application.usecases.queries` (só lê).
- `@UseCase` do projeto, **sem Spring**. Nunca `@Transactional`, `@Service`, `@Retryable`.
- Um método público, `execute(...)`. Mais de um argumento → `record Params` aninhado. `@RequiredArgsConstructor`.
- **Sem regra de negócio**: carrega, chama o domínio, salva, publica. Um `if` de negócio vai para entidade, Specification, Policy ou Domain Service ([`crm-domain-rules`](../crm-domain-rules/SKILL.md)).
- Hora: o `Clock` vem por construtor e o use case passa `Instant.now(clock)` ao domínio.

## Command

```java
@UseCase
@RequiredArgsConstructor
public class CancelVisit {

    private final VisitRepository repository;
    private final EventPublisher publisher;
    private final Clock clock;

    public void execute(UUID visitId) {
        var visit = repository.findById(visitId)
            .orElseThrow(() -> new NotFoundException("visita não encontrada: " + visitId));

        visit.cancel(Instant.now(clock));

        repository.save(visit);

        publisher.publishAll(visit.pullEvents());
    }
}
```

- **Publica depois do `save`**, puxando os eventos da **instância que você alterou**, não da devolvida pelo `save` (a devolvida vem do banco, sem eventos). Quem publica é a application: o aggregate só registra, o repository não publica (§5.1).
- Se for salvar de novo na mesma requisição, siga com a instância **devolvida** (traz a versão nova).
- Criação devolve `UUIDResponse` (`shared-application`); as demais mutações, nada.
- Command que cria algo único trata duplicidade com `ConflictException`; o `RepositoryImpl` já converte violação de constraint.

## Query

`@UseCase(transactional = false)`, em `queries`. **Não carrega aggregate**: devolve DTO/projeção. Listagem usa `Page`/`Pageable`.

**Contrato de leitura.** A query não usa o `Repository` do aggregate: lê por um `<Nome>Reader` em `application.contracts.readers`, interface que devolve **projeção** (record em `application.schemas`), nunca o aggregate. A implementação `Jpa<Nome>ReaderImpl` mora na persistência do BC e monta a projeção numa consulta só, com o `EntityManager`. Cada método atende uma tela, com nome do que devolve:

```java
public interface UserReader {

    Page<UserSummary> search(UserFilter filter, Pageable pageable);

    Optional<UserSummary> findSummary(UUID id);

    /** Quem está logado, com a flag de troca de senha que o resumo não traz. */
    Optional<CurrentUser> findCurrent(UUID id);
}
```

A query só confere quem pode ler e repassa: `ListUsers` (`actor.requireAdministrator()` e `userReader.search(...)`), `GetCurrentUser` (`findCurrent(...)` ou `NotFoundException` com código). O filtro da tela é um record de `schemas` (`UserFilter`), não um `Criteria`: o `Criteria` é do repository.

Referência viva: command `Login` (limite de tentativa, hasher, erro com código), queries `ListUsers` e `GetCurrentUser`, contratos `UserReader` e `SessionReader`, todos em `core/iam`.

## Erros

- Domínio lança `DomainException`/`ValidationException`; o aspect converte (422/400). **Não escreva `try/catch`** só para converter.
- A decisão é do use case (não achou, conflito de estado): lance direto o tipo de application (`NotFoundException`, `ConflictException`, `ForbiddenException`...). Tabela em [`code-style.md`](../../../docs/rules/code-style.md) §6.
- Erro que o frontend precisa distinguir leva código: `new ConflictException("link_used", "este link já foi usado")`. Código `snake_case`, inglês, único no BC, listado no `usecases.md`.
- Mensagem em pt-BR, minúscula, sem ponto final, **sem valor sensível**: o aspect a escreve no log.

## `@UseCase` flags

`retriable` e `circuitBreaker` **só** para quem chama serviço externo diretamente **e** tem efeito idempotente: o retry reexecuta o use case inteiro (§7.2).

## Registro

Nada de `@Component`. Instancie com `new` na `@Configuration` do BC, em `<bc>-infrastructure` (`IamBeans`), um arquivo por BC (§9).

## O endpoint (`web`)

O controller **injeta use cases e nada mais**: nada de `domain`, repository nem infraestrutura. Uma classe de `web` fala com **um** BC; fluxo que precisa de dois é `orchestration` (§5.4). Erros saem pelo `RestExceptionHandler`, que só conhece tipos de application.

Referência viva: `AuthController`, `UsersController` e `SessionsController`, em `edge/web/src/main/java/com/crm/web/iam/`. O desenho deles (registrado aqui; a aprovação do usuário é a regra desta skill):

- **Quem age chega como parâmetro `Actor`.** A cadeia de segurança de `boot` deixa o `Actor` no atributo `ActorRequestAttribute.NAME`; o `ActorArgumentResolver` (registrado em `WebIamConfig`) o entrega ao método, e sem ele responde 401 `session_invalid`. Nenhum controller lê cookie, token nem cabeçalho de credencial, e nenhum confere papel: a checagem é do use case.
- **Corpo da requisição é um record de `web`** (`com.crm.web.<bc>.requests`), com Bean Validation e mensagem pt-BR (`@NotBlank(message = "é obrigatório")`). O record que carrega senha ou segredo de link sobrescreve o `toString()` sem eles. O controller monta o `Params` do use case a partir do record; papel e enum chegam como texto e o use case os interpreta.
- **Sem `try/catch`.** O aspect de `@UseCase` traduz e o `RestExceptionHandler` responde.
- **Criação devolve 201** com `UUIDResponse`; as demais mutações, 204 sem corpo; query devolve a projeção direto.
- **Credencial do navegador** (só no `AuthController`): o token da sessão sai em `Set-Cookie` montado pelo `SessionCookie` (`HttpOnly`, `SameSite=Lax`, `Secure` fora do perfil `dev`); o corpo leva `SessionView`, nunca o token (architecture.md §14). `logout` limpa o cookie.
- **Aparelho e origem** saem da requisição, nunca do corpo: o rótulo é `X-Device-Name` ou o `User-Agent`, cortado em 80 caracteres; a origem é `request.getRemoteAddr()`, que o Tomcat já resolve atrás do proxy (`server.forward-headers-strategy=native`).
- **Paginação** por `page` e `size`, com teto de tamanho no controller; o filtro é ligado direto de parâmetros (`@ModelAttribute`).

Tela nova só depois de mockup aprovado (regra 7 de [`docs/CLAUDE.md`](../../../docs/CLAUDE.md)).

## Teste

Unitário, sem Spring: repository e publisher com Mockito, `Clock.fixed(...)`, `@DisplayName` em pt-BR. Cubra o caminho feliz, **cada** erro do `usecases.md`, e a ordem `save` antes de `publishAll` (`InOrder`). O aspect já é testado em `UseCaseAspectTest`; não o reteste.

## Checklist

- [ ] Spec da feature cobre o caso de uso
- [ ] Classe concreta, sem sufixo, `@UseCase`, `execute`
- [ ] Nenhuma regra de negócio nem `Instant.now()` sem `Clock`
- [ ] `save` e depois `publishAll(instância.pullEvents())`
- [ ] Erros com mensagem pt-BR (e código, se o frontend distingue)
- [ ] Bean registrado na `<Bc>Beans`
- [ ] Testes de sucesso e de cada erro; `mvn test` verde
