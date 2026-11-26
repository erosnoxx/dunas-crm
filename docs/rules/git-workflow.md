# Convenção de git — crm

Gitflow simplificado. Fonte de verdade de branch, commit e PR.

---

## 1. Branches permanentes

| Branch | Ambiente | Regra |
|---|---|---|
| `main` | **produção** | só recebe merge de `release/*` ou `hotfix/*`. Nunca commit direto, nunca merge de `feature/*` |
| `develop` | **staging** (enquanto não houver servidor, valida-se no stack local) | base de todo trabalho novo. Nunca commit direto: sempre via PR |

---

## 2. Branches de trabalho

Saem **de `develop`** e voltam **para `develop`** via PR:

| Prefixo | Para que |
|---|---|
| `feature/*` | funcionalidade nova |
| `fix/*` | correção de bug em `develop` (não urgente em produção) |
| `chore/*` | build, dependência, config, tooling, faxina, sem mudança de comportamento |
| `docs/*` | só documentação |

Saem e voltam por caminho próprio:

| Prefixo | Sai de | Volta para | Para que |
|---|---|---|---|
| `release/*` | `develop` | `main` **e** `develop` | fechar uma versão: bump de versão, ajuste final, changelog |
| `hotfix/*` | `main` | `main` **e** `develop` | correção urgente em produção, sem esperar o ciclo de release |

**`release/*` e `hotfix/*` precisam voltar para os dois lados.** Se o merge de volta em `develop` for esquecido, a correção se perde no próximo release.

### Nome do branch

`<prefixo>/<escopo-curto-em-kebab-case>`, em inglês, sem número de issue no nome:

```
feature/iam-user-aggregate
feature/monitoring-huawei-adapter
fix/outbox-dead-letter-retry
chore/archunit-new-rule
docs/architecture-outbox
release/0.2.0
hotfix/session-revocation-cache
```

Um branch = um escopo. Escopo grande se quebra em vários PRs sequenciais, não num branch de vida longa.

**Em geral, um branch de feature toca um BC só.** Se estiver tocando dois, provavelmente há uma violação de fronteira, ou o recorte da feature está errado: confira antes de continuar (ver [`architecture.md`](architecture.md) §4).

---

## 3. Commits

**Conventional Commits.** O escopo é o **BC** ou o módulo afetado. Descrição em **inglês, minúscula, imperativa, sem ponto final**.

```
feat(iam): add User aggregate with credential
fix(common): keep the outbox event pending when the broker is down
chore(boot): add archunit rule for web composition
docs(architecture): document per-module flyway history
test(shared): cover alphanumeric cnpj in Document
refactor(iam): extract Session revocation into the aggregate
```

Tipos usados: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`.

Escopos usados: o BC (`iam`, e os que vierem), o módulo técnico (`common`, `shared`, `orchestration`, `web`, `boot`), ou a área (`architecture`, `infra`, `frontend`, `mobile`).

Um commit = uma mudança coerente. Não misture refactor com feature no mesmo commit.

**Nunca adicione `Co-Authored-By` (nem outra linha de atribuição a ferramenta ou a Claude) à mensagem de commit.** Vale para qualquer commit deste repositório, sem exceção.

Segredo (`.env`, chave, token, credencial) nunca entra no repositório. O que varia por ambiente vai em variável de ambiente; o `.gitignore` e o `gitguardian.yml` são a rede de segurança, não a regra.

---

## 4. Pull requests

- **Base é sempre `develop`**, nunca `main`. As únicas exceções são `release/*` e `hotfix/*`.
- Título no mesmo formato do commit (`feat(iam): ...`).
- O corpo diz **o que muda e por que**, e lista o critério de aceite verificado.
- **Todo PR que toca `backend/` cita `mvn test`, rodado a partir de `backend/` num JDK 21, como critério de aceite verificado.** O `mvn test` inclui os testes de arquitetura ([`architecture.md`](architecture.md) §11). Teste que depende de Docker e não pôde rodar é dito no corpo, não omitido.
- PR que toca `frontend/` ou `mobile/` cita o comando de verificação definido no `CLAUDE.md` da respectiva pasta.
- Nada de PR "shell": guard, rota, bean ou listener implementado mas não conectado a nada real não fecha PR.

---

## 5. Versão

`MAJOR.MINOR.PATCH` na `<version>` de `backend/pom.xml`, com `-SNAPSHOT` em `develop`. O bump acontece no `release/*`, não durante a feature.

Tag em `main` a cada release, no formato `v0.2.0`.
