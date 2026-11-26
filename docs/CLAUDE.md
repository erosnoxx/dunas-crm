# docs

```
rules/           regras do projeto inteiro (fonte de verdade)
design-system/   identidade visual: tokens e brand book (fonte de verdade das telas)
assets/          logo e imagens da marca
brainstorming/   ideias, ainda não aprovadas (não é spec)
system-design/   a spec: domínio e casos de uso, uma pasta por feature
plans/           planos de implementação, gerados a partir da spec
diagrams/        diagramas
market-research/ pesquisa de mercado
```

## `rules/`

[`architecture.md`](rules/architecture.md), [`frontend.md`](rules/frontend.md), [`code-style.md`](rules/code-style.md), [`git-workflow.md`](rules/git-workflow.md). **Uma convenção tem uma única casa.** Skill, `CLAUDE.md` e código **referenciam** a seção; nunca copiam o texto. Regra que o ArchUnit trava aparece em uma linha, com o nome do teste.

A numeração das seções do `architecture.md` é citada por comentários no código: **não renumere**.

## `brainstorming/`: ideias

Um arquivo por tema, `YYYY-MM-DD-<tema>.md`, copiado de [`_template.md`](brainstorming/_template.md). **Ideia não é requisito**:

- Nenhum código, skill, plano ou documento de `system-design/` cita um brainstorming como fonte. Implementação parte só da spec aprovada.
- Cada ideia tem status (`ideia`, `em avaliação`, `aceita`, `adiada`, `descartada`). **Promover** uma ideia é criar a feature em `system-design/`, com aprovação do dono, e registrar o link no brainstorming.
- Ao anotar, sem filtro de custo ou viabilidade. Ideia que fere uma decisão global de `00-overview.md` recebe a marca `fere: <restrição>` e **não é apagada**.
- **Fato e hipótese ficam em colunas separadas.** O que não foi verificado nunca aparece como certeza em outro documento.

## `system-design/`: a spec

`00-overview.md` guarda a visão de domínio e o mapa de features. Cada feature tem uma pasta numerada pela ordem de construção, copiada de `_template/`:

```
NN-<feature>/
  README.md          objetivo, escopo, BCs envolvidos, dependência de outras features (link relativo)
  domain-model.md    aggregates, entidades, VOs e invariantes: fonte de verdade do domínio da feature
  usecases.md        commands, queries, pré e pós-condições, códigos de erro
  decisions.md       ADRs locais (contexto → decisão → consequência)
  open-questions.md  pendências ainda não fechadas
  design/            mockups/protótipos + descrição do que cada um valida
```

Regras:

1. Cada feature é única e fechada na própria pasta. Dependência de domínio entre features é só **link relativo**, nunca definição duplicada.
2. Os `CLAUDE.md` de código **referenciam** o `domain-model.md` da feature equivalente, nunca duplicam seu conteúdo.
3. Decisão **de arquitetura** vai em [`rules/architecture.md`](rules/architecture.md). Decisão **global de produto** vai em `00-overview.md`. Nunca no `decisions.md` de uma feature específica.
4. `open-questions.md` é mantido vivo por feature, atualizado conforme pendências surgem e fecham. Pendência que vira decisão migra para `decisions.md` como ADR.
5. O escopo de cada feature cobre **backend e frontend juntos**: nunca planejados separadamente.
6. Antes de fechar `domain-model.md` e `usecases.md` de uma feature **com tela**, é obrigatório gerar um artefato visual de validação de UX/UI, salvo em `design/`, com a descrição do que ele representa e quais decisões de UX ele ajudou a validar.
7. **Nenhuma tela, nova ou redesenhada, é implementada sem mockup previamente aprovado**, salvo em `design/` da feature. A implementação é fiel ao mockup em nível **pixel perfect** (cores, espaçamento, tipografia, hierarquia, estrutura de componentes, comportamento). Divergência necessária (limitação técnica, troca de biblioteca) é **sinalizada ao usuário antes de implementar**, nunca decidida em silêncio. Ao final, a etapa explícita de **comparação mockup × resultado**, campo a campo, reportando qualquer divergência.
8. Sem "estado de implementação" ("ainda vazio", "falta fazer") em nenhum documento: fica errado no commit seguinte.

## `plans/`: o plano

Plano de implementação gerado a partir da spec de uma feature: `YYYY-MM-DD-<feature>[-<parte>].md`. É aqui, e **não** em `docs/superpowers/`, que as skills de escrever e de executar plano gravam e leem. A spec fica em `system-design/`; o plano só descreve como implementá-la, sem redefinir o domínio.
