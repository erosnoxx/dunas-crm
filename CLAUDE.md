# crm

CRM de uma empresa de energia solar: app interno de **uma empresa só** (sem multi-tenant), com cadastro de clientes e usinas, monitoramento de geração (Huawei, Solis, Sungrow), agenda de manutenção, proposta e orçamento. Backend em DDD + Clean Architecture, com **um módulo Maven por bounded context**.

```
backend/   módulos Maven (kernel/, core/, edge/)
frontend/  web responsiva
mobile/    aplicativo
infra/     docker, envs, scripts
docs/      regras e design
```

## Arquitetura

**Fonte de verdade: [`docs/rules/architecture.md`](docs/rules/architecture.md).** Estrutura de módulos, regra de dependência, comunicação entre BCs, `@UseCase`, outbox, migrations, ACL e enforcement estão lá, e **não são repetidos em nenhum `CLAUDE.md`**.

Os pontos que mais causam erro, como atalho:

1. **Nenhum BC depende de outro BC**, em nenhuma camada. Referência cross-BC é UUID cru; comunicação é integration event via outbox. Travado pelo ArchUnit.
2. **Duas commons:** `kernel/common` é técnico (zero negócio), `kernel/shared` é negócio (zero framework). Nenhuma conhece um BC.
3. **`domain` e `application` são Java puro.** A única anotação de framework permitida em `application` é `@UseCase`, do próprio projeto.
4. **A hora vem do `Clock`**, nunca de `Instant.now()`. Terceiro só entra por uma integração com Translator.

## Regras

- Estilo de código: [`docs/rules/code-style.md`](docs/rules/code-style.md).
- Frontend (stack, estrutura, tema DUNAS, sessão): [`docs/rules/frontend.md`](docs/rules/frontend.md).
- Git (gitflow, commits, PR): [`docs/rules/git-workflow.md`](docs/rules/git-workflow.md). **Nunca adicione `Co-Authored-By` a um commit, nunca.**
- Domínio e casos de uso de cada feature: `docs/system-design/` (ver [`docs/CLAUDE.md`](docs/CLAUDE.md)).

## Spec e plano

- **A spec** (design aprovado, domínio, casos de uso) mora em `docs/system-design/NN-<feature>/`.
- **O plano de implementação** mora em `docs/plans/`.
- Isso **sobrepõe os caminhos padrão** das skills de brainstorming e de plano (`docs/superpowers/specs/` e `docs/superpowers/plans/`): nunca crie `docs/superpowers/`. Ordem de trabalho: skill `crm-new-feature`.

## Skills do projeto

Em `.claude/skills/`, só procedimentos; as regras ficam em `docs/rules/`.

| Skill | Quando |
|---|---|
| `crm-new-feature` | começar uma feature, modelar, escrever ou executar plano |
| `crm-add-bounded-context` | criar um BC novo |
| `crm-add-domain-model` | VO, entidade, aggregate, invariante |
| `crm-domain-rules` | onde mora uma regra: Specification, Policy, Domain Service, estados |
| `crm-add-use-case` | command, query, endpoint |
| `crm-add-persistence` | migration, entidade JPA, mapper, repository |
| `crm-domain-events` | domain event, integration event, consumidor |
| `crm-integration-acl` | falar com qualquer sistema de terceiro |
| `crm-build-and-test` | compilar, testar, build quebrado |
| `crm-frontend-add-feature` | telas de uma feature no frontend: rota, formulário, teste por tela |

## Regras operacionais

- **Plano fechado antes de implementar.** Se a tarefa não vier com spec sem ambiguidade de domínio ou UX, ou com referência clara a `docs/system-design/<feature>/` cobrindo o que falta, **não decida sozinho**: sinalize a lacuna antes de escrever código.
- **JDK 21**, sempre; o build recusa outro. Detalhes em `crm-build-and-test`.
- Dúvida de arquitetura: **pergunte, não assuma.** Regra de arquitetura nova entra em `architecture.md`, não numa skill nem num `CLAUDE.md`.
