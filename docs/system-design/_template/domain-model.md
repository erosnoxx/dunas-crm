# Domain model — <Nome da feature>

> Template — apague estas linhas de instrução ao preencher.
>
> **Este arquivo é a fonte de verdade do domínio desta feature.** Nenhum `CLAUDE.md`, skill ou código o duplica: todos o referenciam.
>
> Não fechar este documento antes de existir um artefato visual aprovado em `design/` (regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md)), nas features com tela.

## Nota de proveniência

Se este modelo revisa ou substitui um sketch/código anterior, diga o que mudou e por quê. Se nasceu do zero, escreva "modelado do zero" e siga.

## Aggregates

### `<Aggregate>` (aggregate root)

**Fronteira de consistência:** o que muda junto, na mesma transação, e por quê. Ver [`architecture.md`](../../rules/architecture.md) §6.1.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | — |
| `<campo>` | `<VO>` | invariante que o VO garante |

**Entidades internas:** quais entidades só existem dentro deste aggregate e nunca são carregadas ou salvas por fora dele.

**Referências cross-BC:** toda referência a outro BC é `UUID` cru, sem relação JPA e sem FK no banco.

**Invariantes:**
- O que precisa ser verdade em **todo** momento, garantido pelo próprio aggregate: não pelo use case, não pelo controller, não pelo banco.

**Métodos de domínio:** a mutação acontece por método nomeado pelo negócio (`cancel()`, `publish()`), nunca por setter. Método que depende de tempo recebe `Instant now`.

**Domain events registrados:** qual evento cada transição registra (`registerEvent`). Quem publica é a `application`, depois do save confirmado: ver [`architecture.md`](../../rules/architecture.md) §5.1.

## Value Objects

| VO | Base | Regra de validação |
|---|---|---|
| `<Nome>` | `StringValueObject` | o que rejeita, e com qual mensagem pt-BR minúscula |

VO usado por **mais de um BC** vai para `shared-domain`, não para o BC. VO de um BC só fica no `<bc>-domain`. Na dúvida, começa no BC: ver [`architecture.md`](../../rules/architecture.md) §3.1. Confira antes se o VO já existe em `shared-domain`.

## Specifications, Policies e Domain Services

Toda regra que não mora numa entidade ou num VO: uma linha por classe, com a regra em uma frase. Onde decidir entre os três: [`architecture.md`](../../rules/architecture.md) §6.4.

| Nome | Tipo | Regra |
|---|---|---|
| `<Nome>Specification` | Specification | a condição, em uma frase |
| `<Nome>Policy` | Policy | a decisão e as variantes |
| `<Nome>DomainService` | Domain Service | os aggregates que liga e a regra entre eles |

Se não houver nenhum, escreva "nenhum" e diga em uma linha por quê: a ausência também é decisão.

## Ciclo de vida

Se o aggregate tem status com transições e regras, desenhe a máquina de estados: estados, transições permitidas, e o que cada transição trava (base `StateMachine` de `common-domain`).

Se não tem, escreva "sem ciclo de vida" e siga: não invente máquina de estados para um campo booleano.

## Integration events

| Evento | Direção | Quem consome / de quem vem |
|---|---|---|
| `<NomeDoEvento>` | publicado / consumido | BC do outro lado |

Publicação é via outbox; consumo entra pela `<bc>-infrastructure` deste BC. Entrega é at-least-once, então **todo consumidor é idempotente**: diga aqui como a idempotência é garantida. Ver [`architecture.md`](../../rules/architecture.md) §5.

## Persistência

Tabelas criadas, e a referência cross-BC como coluna `uuid` **sem** `REFERENCES`. Migrations em `db/migration/<bc>/`, numeradas de `V1__` neste BC: ver [`architecture.md`](../../rules/architecture.md) §8.
