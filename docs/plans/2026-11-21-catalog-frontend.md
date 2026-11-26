# Catálogo (frontend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar no frontend as telas do catálogo (modelos, fabricantes e tipos), na web e no celular, fiéis aos 13 mockups aprovados.

**Architecture:** Uma feature `features/catalog/` (serviço, hooks, schemas, mensagens por código de erro, componentes e telas) com rotas finas em `app/routes/`, no padrão de `features/iam/`. A casca ganha o item "Catálogo" e a barra de navegação inferior do celular. Quem pode editar chega às telas como propriedade `canEdit`, calculada na rota (`app/` compõe features; uma feature não importa outra). Testes por tela contra o serviço mockado.

**Tech Stack:** o esqueleto de [`2026-11-19-frontend-skeleton.md`](2026-11-19-frontend-skeleton.md): React 19, React Router (framework mode, SPA), TanStack Query + axios, React Hook Form + Zod, Tailwind v4 + shadcn/ui sobre Base UI, Vitest + Testing Library.

**Spec:** [`docs/system-design/02-catalog/`](../system-design/02-catalog/README.md): `usecases.md` (rotas da API, erros), `domain-model.md` (limites dos campos), `decisions.md` (ADR-1 a ADR-10), `design/` (13 mockups aprovados em 2026-11-21: <https://claude.ai/artifact/DvPCMAmQrPqX995ZFdc44U>, fontes em `design/canvas/project/`). Regras: [`frontend.md`](../rules/frontend.md). Backend: [`2026-11-21-catalog-backend.md`](2026-11-21-catalog-backend.md). **O plano não redefine a spec.**

## Global Constraints

- Tudo roda a partir de `frontend/`, **pelo PowerShell**. Aceite de toda tarefa: `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check` verde.
- **Nenhuma tela sem mockup aprovado.** A tabela "Cobertura dos mockups" diz que artboard cada tela segue em cada tamanho; tamanho sem artboard é **derivado** e vai à comparação final. Valor do mockup sem token do tema é **divergência**: sinalize ao usuário **antes** de escrever a tela (Tarefa 1).
- Só tokens do tema (`bg-panel`, `text-text-muted`, `cut-lg`, ...); nenhum valor avulso (um teste varre cor literal).
- Erro decide a tela pelo **`code`**, nunca pelo texto. As mensagens são as da tabela de erros de `usecases.md`. `session_invalid` não é tratado na tela (o cliente de consulta cuida).
- Limites dos campos: fabricante **2 a 80**, tipo **2 a 60**, modelo **1 a 80**, nome da característica **2 a 40**; valor da característica **maior que zero, até 3 casas decimais, até 9 dígitos inteiros** (ADR-9); unidades `KILOWATT`, `WATT`, `WATT_PEAK`, `BTU_PER_HOUR` com os símbolos **kW**, **W**, **Wp**, **BTU/h**.
- Desativar e reativar **sem confirmação** (ADR-5). Situação lida por forma (círculo cheio ou vazado) além da cor (`StatusChip`).
- Identificadores em inglês; texto de interface em pt-BR, frase com inicial minúscula de frase, verbos diretos nos botões. Ícone é SVG de traço (`lucide-react`).
- Commit: Conventional Commits, escopo `frontend`, em inglês, **sem `Co-Authored-By`**. Branch `feature/catalog-frontend`; **pergunte a base** antes da Tarefa 1 (a mesma do backend). As telas precisam do backend de pé para o fechamento (Tarefa 8), mas as tarefas 2 a 7 rodam só com serviço mockado.

## Cobertura dos mockups

Artboards numerados como em [`design/README.md`](../system-design/02-catalog/design/README.md). **Derivado** = sem artboard no tamanho, vai à comparação final.

| Tela | Web (1280) | Celular (390) |
|---|---|---|
| Modelos, lista | 1 | 2 |
| Modelos, vazio e sem resultado | derivado | 13 |
| Modelos, visão do Técnico | derivado (sem botões de escrita) | 12 |
| Novo modelo | 3 (inversor) | 4 (ar-condicionado, com erro); inversor derivado |
| Modelo ativo (editar, desativar) | 5 | derivado (tela 4 com os dados e a ação "Desativar modelo") |
| Modelo desativado | derivado (tela 5 só leitura e "Reativar modelo") | 6 |
| Fabricantes, lista e painel "Novo fabricante" | 7 | 8; formulário do celular derivado (tela cheia como a 11) |
| Editar fabricante e desativar | derivado (painel da 7 com o nome e a ação) | derivado |
| Tipos, lista | 9 | 10 |
| Novo tipo | derivado (painel da 9 vazio) | 11 |
| Editar tipo (unidade travada) | 9 | derivado (tela 11 com o aviso) |
| Barra de navegação inferior | n/a | nas telas 2, 8, 10, 12 e 13 |
| Item "Catálogo" na barra lateral | em todas as telas web | n/a |

## Review Focus

1. **Valor com ponto ou milhar:** `12.000` digitado em pt-BR **não** vira 12 nem 12000 em silêncio: o campo aceita só dígitos e **vírgula** como separador decimal e recusa o ponto ("use só números e vírgula"); o envio manda ponto. → Tarefa 2.
2. **Técnico por URL:** abrir `/catalogo/modelos/novo` (ou os de fabricante e tipo) sem poder editar volta para a lista, e nenhuma tela mostra botão de escrita para ele. → Tarefas 5 a 7.
3. **Trocar o tipo no formulário de modelo:** o rótulo e a unidade da característica trocam e o valor digitado **não** vaza para o tipo novo. → Tarefa 5.
4. **Tipo que já tem modelos:** a unidade e a caixa "Obrigatória" ficam desabilitadas com o aviso; se o backend ainda responder `characteristic_locked`, a mensagem sai. → Tarefa 7.
5. **Nome repetido (409) e edição concorrente:** a mensagem do `code` aparece no campo ou no alerta **sem perder o que foi digitado**; `concurrent_update` pede para recarregar. → Tarefas 5 a 7.

---

### Tarefa 1: Portão de divergências e dependências

Sem código. Leva ao usuário, com a recomendação, o que o mockup traz e o esqueleto ainda não tem. **As tarefas 3 em diante não começam antes da resposta.**

| Ponto | O que o mockup mostra | O que existe hoje | Recomendação |
|---|---|---|---|
| Altura do campo | 44 px na web e 48 px no celular | `Input` do `shared/ui`: 52 px no celular e 48 px a partir de `md` | seguir o mockup, com uma variante `size` no `Input` (o IAM não muda) |
| Campo de seleção e caixa de marcar | `select` e checkbox nativos estilizados | não existem em `shared/ui` | criar `NativeSelect` e `Checkbox` simples em `shared/ui`, sobre os tokens |
| Barra inferior no celular | Catálogo e Usuários (um item para o Técnico) | só barra superior e menu da conta | implementar na casca, junto desta feature |
| Abas do catálogo | links `Modelos`, `Fabricantes`, `Tipos` (aba ativa em `brand-text` com traço) e controle segmentado no celular | nenhum componente de abas | componente da feature (`CatalogTabs`), links de verdade |
| `Page<T>` | usado pelo catálogo e pelo IAM | mora em `iam.service.ts` | subir para `shared/api/page.ts` (feature não importa outra) |

- [ ] **Step 1:** levar a tabela ao usuário e registrar a resposta em `docs/system-design/02-catalog/design/README.md`. **Step 2:** nenhuma divergência nova aprovada além da tabela → seguir; qualquer outra descoberta depois entra na comparação final.

### Tarefa 2: Fundação da feature

**Files:** `src/shared/api/page.ts` (e `iam.service.ts` passa a importá-lo); `src/features/catalog/{catalog.service.ts,catalog.messages.ts,catalog.schemas.ts,amount.ts,catalog.hooks.ts}` e testes ao lado.

**Interfaces (produz):**
- `catalog.service.ts` (usa `httpClient`; rotas de `usecases.md`): tipos `CatalogStatus = "ACTIVE" | "DEACTIVATED"`, `StatusFilter = CatalogStatus | "ALL"`, `Unit = "KILOWATT" | "WATT" | "WATT_PEAK" | "BTU_PER_HOUR"`, `ManufacturerSummary`, `EquipmentTypeSummary { id; name; status; modelCount; characteristics: { id; name; unit: Unit; required: boolean }[] }`, `EquipmentModelSummary { id; name; status; manufacturer: { id; name }; type: { id; name }; values: { definitionId; name; unit: Unit; amount: string }[] }` (o `amount` chega como texto ou número JSON; a tela o converte). Funções: `listManufacturers(input: { text?: string; status?: StatusFilter; page: number; size: number }): Promise<Page<ManufacturerSummary>>`; `registerManufacturer(input: { name: string }): Promise<{ id: string }>`; `renameManufacturer(id: string, input: { name: string }): Promise<void>`; `deactivateManufacturer(id)`, `reactivateManufacturer(id)`; `listEquipmentTypes(input: { status?: StatusFilter; page: number; size: number })`; `registerEquipmentType(input: { name: string; characteristic: { name: string; unit: Unit; required: boolean } })`; `updateEquipmentType(id, input)` (mesmo corpo); `deactivateEquipmentType(id)`, `reactivateEquipmentType(id)`; `listEquipmentModels(input: { text?: string; typeId?: string; status?: StatusFilter; page: number; size: number })`; `getEquipmentModel(id)`; `registerEquipmentModel(input: { manufacturerId: string; typeId: string; name: string; values: { definitionId: string; amount: string }[] })`; `updateEquipmentModel(id, input: { name: string; values: ... })`; `deactivateEquipmentModel(id)`, `reactivateEquipmentModel(id)`. Toda escrita chama `ensureCsrfCookie()` antes, como o IAM.
- `catalog.messages.ts`: `messageFor(code, fallback)` com **todos** os códigos da tabela de erros de `usecases.md`, e `LABELS`: `unit` (`kW`, `W`, `Wp`, `BTU/h`) e `status` (`Ativo`, `Desativado`).
- `amount.ts`: `parseAmount(input: string): { ok: true; value: string } | { ok: false; reason: string }` (aceita dígitos e **uma vírgula**, até 9 inteiros e 3 decimais, maior que zero; devolve o texto com **ponto** para a API; recusa ponto, espaço, sinal, milhar e vazio, com a razão em pt-BR); `formatAmount(value: string | number): string` (ponto vira vírgula, sem separador de milhar, sem zeros à direita: `3` → `"3"`, `0.5` → `"0,5"`, `12000` → `"12000"`).
- `catalog.schemas.ts` (Zod): `manufacturerSchema { name }`, `equipmentTypeSchema { name; characteristicName; unit; required }`, `equipmentModelSchema { manufacturerId; typeId; name; amount }` (`amount` validado por `parseAmount`), com os `.max()` dos limites; nome apara as pontas (não é senha).
- `catalog.hooks.ts`: chaves `["catalog", "manufacturers" | "types" | "models", ...]`; uma consulta por leitura (`useManufacturers`, `useEquipmentTypes`, `useEquipmentModels`, `useEquipmentModel(id)`, com `keepPreviousData` na lista) e uma mutation por escrita, cada uma invalidando as chaves que mudou (mudar tipo invalida também modelos; mudar fabricante, também modelos).

- [ ] **Step 1: Testes que falham:** `amount.test.ts` (`"3"`, `"0,5"`, `"12000"`, `"999999999,999"` aceitos; **`"12.000"`, `"1,2345"`, `"0"`, `"-1"`, `"1e3"`, `"1 000"`, `""` recusados**; `formatAmount` dos exemplos acima); `catalog.service.test.ts` (rota, verbo e corpo de cada função, CSRF antes da escrita, `httpClient` mockado); `catalog.messages.test.ts` (cada código da spec tem mensagem; código desconhecido cai no `fallback`); `catalog.schemas.test.ts` (limites inferior e superior de cada nome); `catalog.hooks.test.tsx` (a mutation de tipo invalida modelos).
- [ ] **Step 2: Rodar e ver falhar:** `npm test -- src/features/catalog src/shared/api` → FAIL.
- [ ] **Step 3: Implementar**; mover o `Page` e ajustar o import do IAM. **Step 4: Rodar a suíte inteira** (o IAM segue verde). **Step 5: Commit:** `feat(frontend): add the catalog service, hooks and schemas`.

### Tarefa 3: Peças de UI

**Files:** `src/shared/ui/{native-select,checkbox}.tsx` e testes; `src/shared/ui/input.tsx` (variante `size`); `src/features/catalog/components/{catalog-tabs,model-values-fields}.tsx` e testes.

**Interfaces (produz):**
- `NativeSelect` (`React.ComponentProps<"select">`; `<select>` estilizado com os tokens do `Input`, seta de `ChevronDownIcon`, `aria-invalid`); `Checkbox` (`input type="checkbox"` com `accent-brand`, alvo de toque de 44 px no rótulo).
- `Input` ganha `size?: "default" | "compact"`: `compact` = 44 px na web e 48 px no celular (mockup); o padrão não muda.
- `CatalogTabs({ active }: { active: "models" | "manufacturers" | "types" })`: `nav` com três `Link` (`/catalogo`, `/catalogo/fabricantes`, `/catalogo/tipos`), `aria-current="page"` na ativa; abas na web (traço de 3 px em `brand-text`) e controle segmentado no celular.
- `ModelValuesFields({ type, value, onChange, error })`: a seção "Características do tipo `<nome>`" com um campo por definição do tipo (`label` com o nome da característica e `*` se obrigatória, o símbolo da unidade como sufixo do campo, a dica "O campo e a unidade vêm do tipo escolhido").

- [ ] **Step 1: Testes que falham:** `native-select.test.tsx` e `checkbox.test.tsx` (rótulo associado, `aria-invalid`, foco por `Tab`); `catalog-tabs.test.tsx` (`aria-current` só na ativa; os três `href`); `model-values-fields.test.tsx` (inversor mostra "Potência *" e o sufixo `kW`; ar-condicionado mostra "Capacidade *" e `BTU/h`; **trocar o `type` zera o valor**, Review Focus 3); `input.test.tsx` ganha a variante `compact` (`toHaveClass("h-12", "md:h-11")`, ou as classes que o mockup pedir).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar a suíte** (inclui o teste de cor literal). **Step 5: Commit:** `feat(frontend): add the select, checkbox and catalog tabs`.

### Tarefa 4: Casca (item "Catálogo", barra inferior, títulos) e acesso

**Files:** `src/app/layout/app-shell.tsx` (e teste), `src/app/layout/bottom-nav.tsx`, `src/app/routes/catalogo.access.ts`. O `src/app/routes.ts` é editado em cada tarefa de tela (5 a 7), junto das rotas que ela cria.

**Interfaces:**
- `NAV_ITEMS` ganha o ícone por item (hoje `UsersIcon` fixo) e o item `{ to: "/catalogo", label: "Catálogo", roles: [ADMINISTRATOR, COMMERCIAL, TECHNICIAN] }` (ícone `PackageIcon`); a barra lateral o mostra como no mockup 1.
- `BottomNav` (`md:hidden`, 64 px, `nav` com `aria-label="Principal"`, um link por item que o papel vê, ativo em `text-brand-text`): aparece nas rotas com a barra superior e **não** nas de `OWN_HEADER`.
- `TITLES`: o título "Catálogo" casa por **prefixo** (`/catalogo`, `/catalogo/fabricantes`, `/catalogo/tipos`); `OWN_HEADER` (por prefixo) cobre `/catalogo/modelos/` e os formulários de fabricante e tipo no celular.
- `useCanEditCatalog(): boolean` (`catalogo.access.ts`): `true` para `ADMINISTRATOR` e `COMMERCIAL` (usa `useCurrentUser` do IAM; `app/` pode compor features).
- Rotas (registradas nas tarefas 5 a 7, dentro do `layout("routes/authenticated.tsx")`): `catalogo` (índice = modelos), `catalogo/modelos/novo`, `catalogo/modelos/:id`, `catalogo/fabricantes` (layout com `novo` e `:id` filhos, como `usuarios`), `catalogo/tipos` (idem). Estáticas antes de `:id`.

- [ ] **Step 1: Testes que falham** (`app-shell.test.tsx`): Administrador vê "Catálogo" e "Usuários" na barra lateral e na inferior; **Técnico vê só "Catálogo"** nas duas; `/catalogo/modelos/novo` no celular não tem a barra superior nem a inferior; `/catalogo/fabricantes` tem o título "Catálogo"; `useCanEditCatalog` é falso para o Técnico.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the catalog entry to the shell`.

### Tarefa 5: Modelos (lista, novo, editar, desativado)

Segue os artboards **1, 2, 3, 4, 5, 6, 12, 13** (derivados: ver a tabela).

**Files:** `src/features/catalog/screens/{models,model-form}.tsx` e testes; `src/features/catalog/components/model-row.tsx`; `src/app/routes/{catalogo._index,catalogo.modelos.novo,catalogo.modelos.$id}.tsx`.

**Interfaces:**
- `ModelsScreen({ canEdit }: { canEdit: boolean })`: aba `models`; busca (texto), filtro de tipo (`NativeSelect` alimentado por `useEquipmentTypes`), filtro de situação (`Ativos`, `Desativados`, `Todos` → `ACTIVE`, `DEACTIVATED`, `ALL`), tabela na web e cartões no celular; "Novo modelo" só com `canEdit`; vazio e sem resultado (mockup 13: "Nenhum modelo encontrado", "Limpar busca e filtros"); carregando e erro com `Alert`; "Carregar mais" no celular e paginação na web, como o IAM.
- `ModelFormScreen({ mode, canEdit }: { mode: "new" | "edit"; canEdit: boolean })`: `new` (fabricante e tipo editáveis, só as opções `ACTIVE`; o campo da característica vem de `ModelValuesFields`) e `edit` (fabricante e tipo **só leitura**, nome e valor editáveis, painel de situação com "Desativar modelo"; modelo desativado fica só leitura com "Reativar modelo", como o mockup 6). Sem `canEdit`, a rota de `new` e `edit` volta para `/catalogo` (Review Focus 2) e a de `edit` abre só leitura.
- Erros: `model_name_taken` no campo do nome; `characteristic_required`, `invalid_amount` no campo do valor; `manufacturer_deactivated`, `type_deactivated`, `model_deactivated`, `concurrent_update` e o resto em `Alert`.

- [ ] **Step 1: Testes que falham** (um por comportamento, serviço mockado, `createRoutesStub`): lista mostra "Potência 3 kW" e "Capacidade 12000 BTU/h"; filtro de tipo e situação chamam o serviço com os parâmetros certos; busca sem resultado mostra o estado vazio e "Limpar busca e filtros" refaz a consulta; **Técnico não vê "Novo modelo" nem "Abrir" de escrita** e a rota `new` redireciona; formulário novo: trocar o tipo troca o rótulo e a unidade e **zera o valor**; `"12.000"` mostra "use só números e vírgula" e não envia; `"12000"` envia `amount: "12000"`; `model_name_taken` aparece sob o nome **mantendo o texto digitado**; editar mostra fabricante e tipo como texto e não como campo; "Desativar modelo" chama o serviço e a tela passa a mostrar "Reativar modelo"; contratos de classe do mockup (`text-display-lg` no título, `max-md:sr-only` no `h1` da barra superior) onde o teste de IAM faz o mesmo.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** os componentes e as rotas finas (guardas só de autenticação; `canEdit` vem de `useCanEditCatalog`). **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the catalog model screens`.

### Tarefa 6: Fabricantes

Segue os artboards **7 e 8** (formulário do celular e edição: derivados).

**Files:** `src/features/catalog/screens/{manufacturers,manufacturer-panel}.tsx` e testes; `src/app/routes/{catalogo.fabricantes,catalogo.fabricantes.novo,catalogo.fabricantes.$id}.tsx`.

**Interfaces:**
- `ManufacturersScreen({ canEdit })`: aba `manufacturers`; lista (nome, nº de modelos, situação); na web o `Outlet` mostra o painel à direita (como `usuarios`), no celular os filhos `novo` e `:id` são telas cheias com cabeçalho próprio ("Voltar").
- `ManufacturerPanel({ mode, canEdit })`: "Novo fabricante" (nome, 2 a 80) e edição (renomear, "Desativar fabricante" ou "Reativar fabricante"). Sem `canEdit`, nem painel nem botões.
- Erros: `manufacturer_name_taken` no campo; `manufacturer_deactivated` e `concurrent_update` em `Alert`.

- [ ] **Step 1: Testes que falham:** lista com contagem de modelos e situação por forma; "Novo fabricante" envia o nome; `manufacturer_name_taken` mantém o texto; editar renomeia e desativar/reativar alternam o botão; **Técnico: sem botão e a rota `novo` redireciona**; celular: `novo` abre tela cheia com "Voltar" levando a `/catalogo/fabricantes`.
- [ ] **Step 2 a 4:** falhar, implementar, passar a suíte. **Step 5: Commit:** `feat(frontend): add the catalog manufacturer screens`.

### Tarefa 7: Tipos

Segue os artboards **9, 10 e 11** (novo tipo na web e edição no celular: derivados).

**Files:** `src/features/catalog/screens/{types,type-panel}.tsx` e testes; `src/app/routes/{catalogo.tipos,catalogo.tipos.novo,catalogo.tipos.$id}.tsx`.

**Interfaces:**
- `TypesScreen({ canEdit })`: aba `types`; lista (tipo, característica, unidade, nº de modelos, situação); painel à direita na web, telas cheias no celular.
- `TypePanel({ mode, canEdit })`: nome do tipo, nome da característica, unidade (`NativeSelect` com as quatro), "Obrigatória no cadastro do modelo" (`Checkbox`), "Desativar tipo" ou "Reativar tipo". **Tipo com `modelCount > 0`:** a unidade e a caixa ficam `disabled` com o aviso "A unidade não muda porque o tipo já tem modelos cadastrados" (mockup 9). A caixa "Obrigatória" também fica travada com modelos: é mais restritiva que o backend (que aceita desligar) e é a ampliação do ADR-4, **divergência a sinalizar** na comparação.

- [ ] **Step 1: Testes que falham:** lista mostra "Potência" e `kW`; novo tipo envia `characteristic: { name, unit: "WATT_PEAK", required: true }`; **com `modelCount > 0` a unidade e a caixa estão desabilitadas** e o aviso aparece; sem modelos estão habilitadas; `characteristic_locked` do backend aparece em `Alert`; `type_name_taken` no campo; Técnico sem botões e rotas `novo` redirecionando.
- [ ] **Step 2 a 4:** falhar, implementar, passar a suíte. **Step 5: Commit:** `feat(frontend): add the catalog type screens`.

### Tarefa 8: Fechamento

**Files:** `docs/system-design/02-catalog/design/comparacao-mockup.md`; `docs/system-design/02-catalog/README.md` (critério de aceite).

- [ ] **Step 1:** com o backend de pé (Postgres, Redis, API em `http://localhost:8080`) e o `npm run dev`, abrir cada tela **em 390 e em 1280** num navegador real e comparar os estilos computados com os valores dos artboards, no formato de [`01-iam/design/comparacao-mockup.md`](../system-design/01-iam/design/comparacao-mockup.md): separar o que foi conferido visualmente do que foi estrutural, e listar **cada divergência, estado sem mockup e tela derivada** para o usuário decidir (incluindo a caixa "Obrigatória" travada, a barra inferior e as alturas do `Input`).
- [ ] **Step 2:** `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check`, pelo PowerShell: verde (o aviso conhecido de `react-refresh` em `shared/ui/form.tsx` é o único tolerado).
- [ ] **Step 3:** marcar o critério de aceite do frontend no `README` da feature e **não** declarar a comparação encerrada: ela fecha com a decisão do usuário sobre as divergências.
- [ ] **Step 4: Commit:** `docs(catalog): record the mockup comparison`.
