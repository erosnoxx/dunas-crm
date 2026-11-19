---
name: crm-new-feature
description: Use ao começar uma feature nova no crm, ao modelar o domínio dela, ao escrever ou executar o plano de implementação — define onde a spec e o plano moram (docs/system-design/NN-feature/ e docs/plans/), a ordem das etapas e os portões de aprovação. Invoque antes de brainstorming, de writing-plans e de executing-plans, e sempre que uma tarefa pedir código sem spec fechada.
---

# Feature nova

As regras de `docs/system-design/` (estrutura, fonte de verdade do domínio, mockup antes de tela) estão em [`docs/CLAUDE.md`](../../../docs/CLAUDE.md). Esta skill é a ordem de trabalho.

## Onde as coisas moram (sobrepõe o padrão das skills de brainstorming e de plano)

| O quê | Onde |
|---|---|
| **Spec** (design aprovado, domínio, casos de uso) | `docs/system-design/NN-<feature>/` |
| **Plano de implementação** | `docs/plans/YYYY-MM-DD-<feature>[-<parte>].md` |

As skills `superpowers:brainstorming`, `superpowers:writing-plans` e `superpowers:executing-plans` têm padrões próprios (`docs/superpowers/specs/` e `docs/superpowers/plans/`). **Neste projeto valem os caminhos acima**: grave e leia sempre lá. Nunca crie `docs/superpowers/`.

## Ordem

1. **Entenda antes de escrever.** Leia o `00-overview.md`, as features de que esta depende e o código do BC (se existir). Pergunte o que o projeto não responde.
2. **Brainstorming** (`superpowers:brainstorming`) com o usuário: objetivo, escopo, o que fica de fora, abordagens. Ideias soltas ficam em `docs/brainstorming/` (modelo em `_template.md`) e **não são spec**. O resultado **não** vira um arquivo em `docs/superpowers/specs/`: o que for aprovado alimenta a pasta da feature (passo 3), e a ideia promovida é registrada como tal no brainstorming.
3. **Crie a pasta da feature:** copie `docs/system-design/_template/` para `NN-<nome-da-feature>/`, com `NN` pela ordem de construção, e preencha `README.md`. Adicione a feature ao mapa do `00-overview.md`.
4. **Feature com tela: mockup antes de fechar o domínio.** Gere o artefato visual em `design/`, descreva o que ele valida e obtenha a aprovação do usuário (regras 6 e 7 de `docs/CLAUDE.md`).
5. **Modele:** `domain-model.md` e `usecases.md`, conferindo contra [`architecture.md`](../../../docs/rules/architecture.md) §5 a §8. Decisões vão para `decisions.md`; dúvidas abertas para `open-questions.md`.
6. **Portão: o usuário aprova a spec.** Nada de plano nem de código antes disso. Se uma tarefa pede implementação **sem** spec fechada nem referência clara a `docs/system-design/<feature>/` cobrindo o que falta, **não decida sozinho**: sinalize a lacuna.
7. **Plano** (`superpowers:writing-plans`), gravado em `docs/plans/`. O plano referencia a spec e **não redefine o domínio**: se o plano precisa mudar o domínio, volte ao passo 5.
8. **Execução** (`superpowers:executing-plans` ou `superpowers:subagent-driven-development`), lendo o plano de `docs/plans/`. Em cada passo de código valem `crm-add-bounded-context` (BC novo), `crm-add-domain-model` e `crm-domain-rules` (domínio), `crm-add-use-case`, `crm-add-persistence`, `crm-domain-events`, `crm-integration-acl` (terceiro) e `crm-build-and-test`.
9. **Fechamento:** o critério de aceite do `README.md` da feature, `mvn test` verde, e (feature com tela) a comparação mockup × resultado. `open-questions.md` revisado: o que virou decisão migrou para `decisions.md`.

## Armadilhas

- Spec e plano são coisas diferentes: a **spec** diz o quê e por quê (e vive); o **plano** diz como, em passos (e é executado).
- Domínio duplicado entre pastas de feature: use link relativo.
- Decisão de arquitetura escondida num `decisions.md` de feature: ela vai para `architecture.md`.
