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

`@UseCase(transactional = false)`, em `queries`. **Não carrega aggregate**: devolve DTO/projeção. Listagem usa `Page`/`Pageable` e um `Criteria`.

> Ainda não há query de referência no repositório, nem o contrato de leitura que ela usa. O primeiro caso real (IAM) fixa o padrão; combine com o usuário antes de repetir um desenho e depois registre-o aqui.

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

O controller **injeta use cases e nada mais**: nada de `domain`, repository nem infraestrutura. Uma classe de `web` fala com **um** BC; fluxo que precisa de dois é `orchestration` (§5.4). Erros saem pelo `RestExceptionHandler`, que só conhece tipos de application. Request com Bean Validation (`jakarta.validation`) é permitido nos schemas da application.

> Ainda não há controller de referência. Mostre o desenho do primeiro ao usuário e registre-o aqui.

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
