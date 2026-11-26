# NN — <Nome da feature>

> Template. Copie a pasta `_template/` para `NN-<nome-da-feature>/`, com `NN` refletindo a **ordem de construção**, e apague estas linhas de instrução ao preencher.
>
> As regras de uso desta estrutura estão em [`docs/CLAUDE.md`](../../CLAUDE.md), seção "`system-design/`". Não as repita aqui. O passo a passo está na skill `crm-new-feature`.

## Objetivo

Uma a três frases: o que este bounded context/feature representa e por que existe. Sem detalhe de implementação.

## Escopo desta feature

**Dentro:**
- Aggregates, entidades internas e capacidades que esta feature entrega.

**Fora (features futuras, não modeladas nem mencionadas na implementação):**
- O que deliberadamente **não** entra, com uma linha dizendo por quê ou para onde foi.

Ser explícito aqui é o que impede que a implementação cresça por conta própria.

## Bounded contexts envolvidos

Qual BC esta feature cria ou altera, e quais outros ela **referencia por UUID cru** (nunca por relação JPA, nunca por import: ver [`architecture.md`](../../rules/architecture.md) §4.1).

Se a feature precisa reagir a algo de outro BC, diga qual **integration event** ela consome, e em qual `<bc>-infrastructure` o listener mora.

## Dependências de outras features

Só link relativo para a pasta da outra feature (ex. `[02-clientes](../02-clientes/)`). **Nunca duplicar definição de domínio entre pastas.**

## Base de código já existente

Se havia código, sketch ou modelo prévio, diga o que era e como foi tratado: ponto de partida para debate ou verdade fechada. Se `domain-model.md` substituiu pontos dele, registre aqui e detalhe em `decisions.md`.

Se não havia nada, escreva "nenhuma" e siga.

## Como este documento foi produzido

Método (brainstorming com o usuário, pesquisa, leitura crítica de código anterior), data, e a confirmação explícita de que o design foi aprovado antes da escrita dos artefatos.

## Critério de aceite

- [ ] `mvn test` passa, a partir de `backend/` (inclui os testes de arquitetura)
- [ ] Artefato visual em `design/` gerado e aprovado antes de fechar `domain-model.md`/`usecases.md` (feature com tela)
- [ ] Plano de implementação em `docs/plans/` e executado
