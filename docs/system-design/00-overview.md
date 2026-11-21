# 00: Visão geral do sistema

## O que é

`crm` é o sistema interno de uma empresa de energia solar: **uma empresa só**, sem multi-tenant. Cadastro de clientes e usinas, monitoramento de geração, agenda de manutenção, proposta e orçamento.

- **Arquitetura** (módulos, regra de dependência, comunicação entre BCs, `@UseCase`, outbox, migrations): [`../rules/architecture.md`](../rules/architecture.md). **Não é reexplicada aqui.**
- **Este documento** é a visão geral de **domínio e produto**: quais features existem, em que ordem, com que dependências, e os princípios que valem para todas.

## Como esta pasta está organizada

```
docs/system-design/
  00-overview.md           este arquivo
  _template/               modelo a copiar para cada feature nova
  NN-<feature>/            uma pasta por feature, na ordem de construção
    README.md              objetivo, escopo, BCs, dependências
    domain-model.md        aggregates, VOs, invariantes (fonte de verdade do domínio)
    usecases.md            commands, queries, erros
    decisions.md           ADRs locais
    open-questions.md      pendências vivas
    design/                mockups aprovados e o que cada um valida
```

As regras de uso desta estrutura estão em [`../CLAUDE.md`](../CLAUDE.md). O passo a passo de uma feature nova está na skill `crm-new-feature`.

---

## Bounded contexts

| Contexto | Responsabilidade |
|---|---|
| `iam` | identidade e acesso: usuários, credenciais, sessões e papéis |
| `registry` | cadastro-base: catálogo (fabricante, tipo, modelo) e, depois, clientes, locais e equipamentos instalados. Cada área é um subdomínio, em pacote dentro do BC ([`02-catalog` ADR-1](02-catalog/decisions.md)) |

A lista completa de aggregates de cada BC mora no `domain-model.md` da feature dona dele. A atribuição de cada feature a um BC é **hipótese** até a modelagem dela.

---

## Mapa de features

| # | Feature | Cobre | Depende de | BC (hipótese) |
|---|---|---|---|---|
| 01 | [`01-iam`](01-iam/) | conta de funcionário, entrar, sessões, papéis | | `iam` |
| 02 | [`02-catalog`](02-catalog/) | fabricante, tipo, modelo e a característica de cada tipo | 01 (papéis) | `registry` (subdomínio `catalog`) |

### Previstas, sem número nem pasta

Ideias de produto, sem ordem decidida. Ganham número e pasta quando entrarem em modelagem.

- `03-customers`: cadastro de clientes, locais (endereços onde há equipamento) e equipamentos instalados, no BC `registry`, depois do catálogo. A usina das nuvens dos fabricantes se liga a um local quando o monitoramento existir;
- monitoramento de geração, com integração às nuvens dos fabricantes de inversor;
- agenda de manutenção e portal de agendamento do cliente;
- proposta e orçamento.

---

## Decisões globais de produto

Valem para toda feature. Decisão de arquitetura fica em `architecture.md`; o que não é arquitetura e vale para o sistema todo fica aqui.

- **Uma empresa só.** Sem tenant: o sistema é o da empresa dela, e a necessidade de atender outras vira instância própria, não código multi-tenant.
- **O sistema não custodia dinheiro de terceiros.** Cobrança e repasse de valores de outras pessoas estão fora do produto.
- **O produto não é só solar.** A empresa instala energia solar e ar-condicionado, e o cliente é um só. Nenhum nome de produto, tipo ou fabricante fica fixo no código: entra por cadastro.
- **O domínio é neutro em relação a terceiros.** Fabricantes de inversor, provedor de e-mail e qualquer API externa entram só por Translator ([`architecture.md`](../rules/architecture.md) §4.3).

## Pendências globais

- Canais do aplicativo (web responsiva e mobile): arquitetura e regras de `frontend/` e `mobile/` ainda por definir.
- Regra de segurança e IAM em `docs/rules/`: depende do desenho da feature 01.
