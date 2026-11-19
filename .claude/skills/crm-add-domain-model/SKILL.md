---
name: crm-add-domain-model
description: Use ao escrever código de domínio no crm — Value Object, entidade interna, aggregate root, invariante, domain exception com código de erro, factory create/reconstitute. Invoque antes de criar qualquer classe em `<bc>-domain` ou em `shared-domain`, e antes de decidir se um VO é do BC ou do shared.
---

# Modelo de domínio

As regras e o motivo estão em [`architecture.md`](../../../docs/rules/architecture.md) §6.1 (entidade e aggregate) e §6.3 (hora), travadas por `BoundedContextRulesTest` (`domain` é Java puro) e `ClockRulesTest`. Esta skill é como se escreve. O **quê** modelar vem de `docs/system-design/<feature>/domain-model.md`: sem ele fechado, sinalize a lacuna e não modele ([`crm-new-feature`](../crm-new-feature/SKILL.md)).

Referências vivas e testadas: `Email`, `Money`, `BirthDate` em `shared-domain`; `AggregateRoot` e `DomainEntity` em `common-domain`.

## Onde mora

1. Já existe em `shared-domain`? (`Email`, `Phone`, `Document`, `Address`, `Money`, `Percentage`, `Quantity`, `BirthDate`, `Observations`...) Use. Não duplique dentro do BC.
2. Só um BC usa → o VO nasce **dentro do BC**. Mais de um → `shared-domain`. Na dúvida, dentro do BC: promover é barato, despromover não é (§3.1).

## Value Object

- `final class` que estende a base do tipo: `StringValueObject`, `BigDecimalValueObject<Self>`, `LocalDateValueObject` ou `ValueObject<T>`. Construtor **privado** e factory estática `of(...)`.
- Imutável, igual por valor, **nunca nulo** (a base já recusa com `ValidationException`, 400).
- `type()` (e `scale()`, `roundingMode()`, `allowNegative()` nos numéricos) devolve **constante**: é chamado de dentro do construtor da base, antes de os campos da subclasse existirem.
- Regra própria em `customValidate` (texto, já com trim) com `checkLength(...)` e `ValidationException` de mensagem específica. Normalização que muda o valor (minúsculas, só dígitos) sobrescreve `validate` e chama `super.validate(...)` primeiro, como `Email`.
- Dependeu de "hoje"? Recebe `Clock` em `of(valor, clock)`, como `BirthDate`. **Nunca** `now()` sem relógio.
- Operação de domínio tem nome de domínio e devolve o próprio VO (`Money.add`, `applyRate`). Não exponha `BigDecimal` solto, e não crie `divide` ingênuo: rateio decide a sobra.
- Entrada com casas a mais é **recusada**, nunca arredondada; arredonda só a aritmética.

## Entidade interna e aggregate root

| Pergunta | Resposta |
|---|---|
| Só existe acessada por outra entidade? | `DomainEntity`, sem repository: o root a carrega e salva |
| Algum use case a busca ou salva sozinha? | `AggregateRoot`, com `Repository` próprio |

```java
public final class Visit extends AggregateRoot {

    private VisitStatus status;

    private Visit(UUID id, Long version, /* VOs */ VisitStatus status) {
        super(id, version);
        this.status = status;
    }

    public static Visit schedule(UUID customerId, VisitDate date, Instant now) {
        var visit = new Visit(UUID.randomUUID(), null, ..., VisitStatus.SCHEDULED);

        visit.registerEvent(new VisitScheduled(visit.getId(), now));

        return visit;
    }

    /** Reconstrução do banco: não valida de novo, não registra evento. */
    public static Visit reconstitute(UUID id, Long version, /* VOs */ VisitStatus status) {
        return new Visit(id, version, status);
    }

    public void cancel(Instant now) {
        if (status == VisitStatus.DONE)
            throw new DomainException("visit_already_done", "não é possível cancelar uma visita já concluída");

        this.status = VisitStatus.CANCELED;

        registerEvent(new VisitCanceled(getId(), now));
    }
}
```

- **Id é `UUID` gerado pelo domínio** (`UUID.randomUUID()` no `create`). Nunca o banco.
- **Sem setter público.** Mutação só por método nomeado pela intenção. Coleção interna sai como `List.copyOf`/`unmodifiableList`.
- **Sem `createdAt`/`updatedAt`** (são da persistência). Data de negócio tem nome de negócio (`scheduledFor`, `expiresAt`).
- **Hora entra por parâmetro** (`Instant now`): o domínio nunca lê o relógio.
- `create` ganha o nome do negócio quando faz sentido (`schedule`, `invite`); `reconstitute` recebe a **versão** lida e é o que o mapper da persistência chama.
- Invariante violada: `DomainException` (422). Quando o frontend precisa distinguir erros de mesmo status, use `(code, message)`: `snake_case`, inglês, **único dentro do BC**, e liste no `usecases.md` da feature. O código é contrato público.
- Evento: o aggregate só **registra** (`registerEvent`); quem publica é o use case. Ver [`crm-domain-events`](../crm-domain-events/SKILL.md).
- Regra que não cabe numa entidade (condição reutilizável, decisão por contexto, ciclo de vida): [`crm-domain-rules`](../crm-domain-rules/SKILL.md).

## Proibido em `domain`

Spring, JPA, Jackson, Bean Validation (o ArchUnit barra). Lombok (`@Getter`) é permitido. Mensagem de exceção em pt-BR, minúscula, sem ponto final e sem valor sensível ([`code-style.md`](../../../docs/rules/code-style.md) §5).

## Teste

JUnit puro, sem Spring, `@DisplayName` em pt-BR, hora fixa ([`code-style.md`](../../../docs/rules/code-style.md) §8).

- **VO:** nulo (a base recusa), inválido, válido, normalização, e cada operação. Veja `StringValueObjectTest`.
- **Aggregate:** cada transição feliz, cada invariante com a mensagem/código esperado, os eventos de `pullEvents()` (e que a lista esvazia), e que `reconstitute` não registra evento. Veja `AggregateRootTest`.

## Checklist

- [ ] `domain-model.md` da feature cobre o que está sendo escrito
- [ ] VO existente reaproveitado; VO novo no lugar certo (BC ou shared)
- [ ] Construtor privado, factory, sem setter, id gerado pelo domínio
- [ ] Hora por parâmetro; nenhum `Instant.now()`
- [ ] Erro de domínio com mensagem pt-BR (e código, se o frontend distingue)
- [ ] Testes de VO e de aggregate; `mvn test` verde
