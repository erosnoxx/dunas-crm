# 03: Clientes, locais e equipamentos instalados

## Objetivo

O cadastro de **quem** a DUNAS atende e **do que** está instalado em cada lugar: o **cliente** (pessoa física ou jurídica), os **locais** dele (casa, filial, fazenda: os endereços onde há instalação) e os **equipamentos instalados** em cada local, cada um uma unidade de um modelo do [catálogo](../02-catalog/) com o seu número de série. É a base de que o monitoramento (a usina se liga a um local), a agenda de manutenção e a proposta vão depender.

## Escopo desta feature

**Dentro:**
- Cliente: cadastrar, editar, desativar e reativar. Pessoa física ou jurídica, pelo CPF ou CNPJ (o tipo vem do documento), com nome ou razão social, um e-mail e um telefone, os dois opcionais (ADR-2). Documento único no cadastro (ADR-3).
- Local: cadastrar no cliente, editar, desativar e reativar. Um nome curto ("Casa", "Fazenda Boa Vista") e o endereço em campos, digitado à mão (ADR-4).
- Equipamento instalado: cadastrar no local, editar, desativar e reativar. Um modelo do catálogo, o número de série (único por modelo) e a data de instalação opcional; uma linha por unidade (ADR-5).
- Lista de clientes com busca por texto (nome, documento, cidade de um local) e filtro de situação, paginada como o catálogo; ficha do cliente com os locais e os equipamentos de cada um.
- Permissões: Administrador e Comercial escrevem; os três papéis consultam (ADR-7).
- As telas listadas em [`design/`](design/README.md), **pendentes de mockup e aprovação**.

**Fora (features futuras ou sem demanda):**
- **Contatos múltiplos** por cliente (pessoa de contato, cargo, vários telefones): decisão do usuário, um e-mail e um telefone (ADR-2).
- **Consulta automática de CEP** (preencher o endereço a partir do CEP): seria integração com terceiro, com Translator ([`architecture.md`](../../rules/architecture.md) §4.3); fica para quando houver demanda (ADR-4).
- **Garantia, foto, nota fiscal e histórico do equipamento** (troca, manutenção feita): decisão do usuário (ADR-9).
- **Mover um equipamento de local ou trocar o modelo dele:** o local e o modelo não mudam depois do cadastro (ADR-5); o caso real de mudança está em [`open-questions.md`](open-questions.md).
- **Apagar** cliente, local ou equipamento: só desativa (ADR-6).
- **Usina e integração com as nuvens dos fabricantes:** nascem com o monitoramento, que vai apontar para o local.
- **Importação em lote, junção de clientes duplicados e histórico de alterações** ([`open-questions.md`](open-questions.md), ADR-9).
- **Tema claro** ([`design-system`](../../design-system/README.md)): segue a confirmar, como no catálogo.

## Bounded contexts envolvidos

Altera o BC **`registry`**: cria o subdomínio **`customers`**, pacote `com.crm.registry.<camada>.customers` nos três módulos que já existem (ADR-1; layout em [`architecture.md`](../../rules/architecture.md) §4.4). Migrations na mesma pasta do catálogo, `db/migration/registry/`, a partir de `V2__`.

`customers` enxerga `catalog` de mão única, como a regra `subdomainsDependOnlyInDeclaredDirection` já declara (`registry.customers → registry.catalog`). O que isso permite e o que esta feature usa está no ADR-8. Nenhuma referência a outro BC; não publica nem consome integration event.

## Dependências de outras features

- [`01-iam`](../01-iam/): papéis e sessão. Recebe o `Actor` de `shared-application`, como o catálogo; não depende do IAM.
- [`02-catalog`](../02-catalog/): o modelo de equipamento ([`domain-model.md`](../02-catalog/domain-model.md#equipmentmodel-aggregate-root)). O equipamento instalado aponta para ele; o catálogo nunca enxerga cliente.

## Base de código já existente

Nenhuma de domínio do `customers`. A feature reaproveita, de `shared-domain`, o `Document` (CPF e CNPJ), o `Email`, o `Phone` e o `Address` com os VOs de endereço e o enum `UF` (o que cada um valida está no [`domain-model.md`](domain-model.md#value-objects)), e do catálogo o aggregate `EquipmentModel` e o seu repository (ADR-8).

## Como este documento foi produzido

Decisões do usuário em 2026-11-21 (registradas em [`decisions.md`](decisions.md)); o restante é **proposta para aprovação**, marcada *proposta* onde aparece. Pela regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md), `domain-model.md` e `usecases.md` só fecham depois de as telas de [`design/`](design/README.md) serem desenhadas e aprovadas.

## Critério de aceite

- [ ] Telas de [`design/`](design/README.md) desenhadas e aprovadas antes de fechar `domain-model.md` e `usecases.md`
- [ ] `domain-model.md`, `usecases.md` e os pontos *proposta* aprovados pelo usuário; [`open-questions.md`](open-questions.md) sem pendência que mude domínio
- [ ] Plano de implementação em `docs/plans/`, backend e frontend juntos, e executado
- [ ] `mvn test` passa, a partir de `backend/` (inclui os testes de arquitetura e o ponta a ponta do `customers`)
- [ ] `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check` passa, a partir de `frontend/`
- [ ] Comparação mockup × resultado, tela a tela, com as divergências reportadas
- [ ] Segundo cadastro com o mesmo CPF ou CNPJ (com ou sem pontuação) recusado com 409; segundo equipamento com a mesma série no mesmo modelo recusado com 409
