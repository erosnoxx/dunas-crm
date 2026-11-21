# 02: Catálogo

## Objetivo

O cadastro-base do que a DUNAS vende e instala: **fabricantes**, **tipos** de equipamento (inversor, ar-condicionado, o que vier) e **modelos**, cada modelo com a característica que o tipo pede (potência em kW no inversor, capacidade em BTU/h no ar-condicionado). É a base de que o cadastro de clientes e de equipamentos instalados ([03](../00-overview.md), ainda sem pasta) depende, e nenhum nome de produto fica fixo no código: a empresa trabalha com energia solar e com ar-condicionado, e o catálogo cresce por cadastro.

## Escopo desta feature

**Dentro:**
- Fabricante, tipo e modelo: cadastrar, editar, desativar e reativar.
- Tipo com **uma característica** (nome, unidade, obrigatória ou não); o modelo guarda o valor dela.
- Listagem com busca e filtros (modelos por tipo e situação), na web e no celular.
- Permissões: Administrador e Comercial cadastram e alteram; os três papéis consultam (ADR-6).
- Carga inicial por seeder, com a lista de modelos que a empresa já vende (ADR-7).
- As 13 telas do mockup aprovado ([`design/`](design/README.md)).
- **Pré-requisito no IAM:** mover o `Role` (de `iam-domain`) para `shared-domain` e o `Actor` (de `iam-application`) para `shared-application`, para o catálogo receber quem está agindo sem depender do IAM ([`architecture.md`](../../rules/architecture.md) §3.1). Refactor mecânico: o comportamento do IAM não muda.

**Fora (features futuras ou sem demanda):**
- **Cliente, local e equipamento instalado:** feature `03-customers`, no mesmo BC. É ela que aponta para o modelo, e o catálogo nunca enxerga cliente.
- **Integração com as nuvens dos fabricantes** e perfil de integração por marca ou modelo: o domínio é neutro ([`architecture.md`](../../rules/architecture.md) §4.3), e isso nasce com o monitoramento.
- **Histórico de alterações** (quem mudou o quê): decisão do usuário, esforço próprio quando fizer sentido (ADR-8).
- **Ficha técnica (PDF) e foto do modelo:** fora da primeira versão.
- **Mais de uma característica por tipo:** o modelo já comporta, e a segunda característica entra quando a demanda aparecer (ADR-3).
- **Importação por planilha:** a carga inicial vem do seeder; o resto é cadastro à mão.
- **Tema claro** ([`design-system`](../../design-system/README.md)): segue a confirmar.

## Bounded contexts envolvidos

Cria o BC **`registry`** (`registry-domain`, `registry-application`, `registry-infrastructure`; passo a passo na skill `crm-add-bounded-context`) com o subdomínio **`catalog`**, que é um pacote dentro de cada camada (ADR-1). O subdomínio `customers` entra depois, no mesmo BC, e enxerga `catalog` de mão única.

Nenhuma referência a outro BC. Não publica nem consome integration event: nenhum outro BC existe que precise de um fato do catálogo, e o evento nasce com a necessidade, como no IAM.

## Dependências de outras features

- [`01-iam`](../01-iam/): papéis e sessão. O catálogo recebe o `Actor` já montado pela `web`, pelo tipo de `shared-application`; não depende do IAM.

## Base de código já existente

Nenhuma de domínio. Existem as peças técnicas do `kernel` que a feature usa (`Repository`, `Criteria`, `Page`, `StringValueObject`, `BigDecimalValueObject`, o `Seeder`/`DatabaseSeeder` de `common-infrastructure`).

## Como este documento foi produzido

Brainstorming com o usuário em 2026-11-20 e 2026-11-21 (decisões em [`decisions.md`](decisions.md)). **Mockup aprovado em 2026-11-21**, incluindo as decisões de UX listadas em [`design/README.md`](design/README.md). `domain-model.md` e `usecases.md` escritos em seguida e **aprovados pelo usuário em 2026-11-21**, inclusive os pontos marcados como *proposta* (listados em `decisions.md`). Na mesma aprovação o usuário mandou mover `Role` e `Actor` para o `shared` e deixou a regra de subdomínio por minha conta: as duas entraram no [`architecture.md`](../../rules/architecture.md) (§3.1 e §4.4).

## Critério de aceite

- [x] `mvn test` passa, a partir de `backend/` (inclui os testes de arquitetura e o ponta a ponta do catálogo)
- [ ] Artefato visual em `design/` gerado e aprovado antes de fechar `domain-model.md`/`usecases.md` (13 telas aprovadas em 2026-11-21)
- [ ] Plano de implementação em `docs/plans/` e executado
- [x] Frontend: as telas dos 13 mockups e as derivadas da cobertura do plano ([`2026-11-21-catalog-frontend.md`](../../plans/2026-11-21-catalog-frontend.md)); `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check` passa, a partir de `frontend/`
- [ ] Comparação mockup × resultado, tela a tela, com as divergências reportadas ([`design/comparacao-mockup.md`](design/comparacao-mockup.md)): fecha com a decisão do usuário sobre os defeitos e as divergências de lá
- [x] Seeder aplicado uma vez e idempotente (reiniciar o app não recria nem sobrescreve)
