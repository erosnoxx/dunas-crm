# Clientes (frontend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar no frontend as telas de clientes, locais e equipamentos instalados, na web e no celular, fiéis aos 19 mockups aprovados em 2026-11-22 e às telas derivadas da spec, com "Clientes" como primeiro item da navegação.

**Architecture:** Uma feature `features/customers/` (serviço, hooks, schemas, mensagens por `code`, formatação de documento, telefone, CEP e data, componentes e telas) com rotas finas em `app/routes/`, no padrão de `features/catalog/`. A casca ganha o item "Clientes" em primeiro (ADR-26). Quem pode editar chega às telas como propriedade `canEdit`, calculada em `app/routes/clientes.access.ts` (`app/` compõe o papel do IAM; uma feature não importa outra). A lista de modelos do formulário de equipamento vem da API do catálogo, chamada pelo próprio serviço da feature (ADR-8), sem importar `features/catalog`. Testes por tela contra o serviço mockado.

**Tech Stack:** o do esqueleto e do catálogo: React 19, React Router (framework mode, SPA), TanStack Query + axios, React Hook Form + Zod, Tailwind v4 + shadcn/ui sobre Base UI, `lucide-react`, Vitest + Testing Library. **Nenhuma dependência nova** (máscaras e dígitos verificadores são funções da feature).

**Spec:** [`docs/system-design/03-customers/`](../system-design/03-customers/README.md): `usecases.md` (rotas, retornos, tabela de erros, **Mensagens da tela**), `domain-model.md` (limites), `decisions.md` (ADR-1 a ADR-32), `design/` (19 mockups aprovados em 2026-11-22: <https://claude.ai/artifact/NxExPYWS7kHNhmdB9jbyou>, fontes em `design/canvas/project/`, e as telas derivadas D1 a D7). Regras: [`frontend.md`](../rules/frontend.md). Backend: [`2026-11-22-customers-backend.md`](2026-11-22-customers-backend.md). **O plano não redefine a spec**; onde o texto daqui e a spec divergirem, vale a spec.

## Global Constraints

- Tudo roda a partir de `frontend/`, **pelo PowerShell**. Aceite de toda tarefa: `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check` verde. O `configure({ asyncUtilTimeout: 5000 })` **já está** em `src/test/setup.ts`: não repita.
- **Nenhuma tela sem mockup aprovado.** A tabela "Cobertura dos mockups" diz que artboard cada tela segue em cada tamanho; tamanho sem artboard é **derivado** e vai à comparação final. **Todo valor de leiaute (altura, espaço, fonte, cor) aponta para uma linha do mockup**; valor sem linha no mockup, ou sem token do tema, é **divergência**: sinalize ao usuário **antes** de escrever a tela (Tarefa 1), nunca decida em silêncio.
- Só tokens do tema (`bg-panel`, `text-text-muted`, `cut-lg`...); o teste de cor literal varre a feature. Quando houver utilitário de tamanho responsivo (`md:text-body-sm`), o peso vem com o mesmo prefixo (`md:font-semibold`), porque o token de tamanho traz o peso 400 (armadilha D2/D3 do catálogo).
- Erro decide a tela pelo **`code`**, nunca pelo texto. Mensagens: as da tabela de erros de `usecases.md`, como frase (inicial maiúscula e ponto final), e as de **Mensagens da tela** para a conferência do formulário (ADR-31). Erro com campo na tela vai **sob o campo** (ícone e texto, como o `FieldError` do catálogo) e o foco vai para ele (`setFocus`, com o `field.ref` ligado ao input em campo controlado); erro sem campo renderizado vai ao `Alert`. Nunca um erro invisível.
- **Formulário:** a conferência roda antes de enviar e nenhum envio sai com campo que a tabela recusa; campo opcional vazio vai como `null` (ADR-31); telefone vai como `+55` + DDD + número (ADR-18); documento vai normalizado. **Edição** monta os valores iniciais só do próprio dado carregado (a ficha), e o formulário não é desenhado nem "Salvar" habilitado enquanto ele não chegou: nunca enviar vazio por informação que faltava. Depois do sucesso, navega (novo) ou volta à ficha, e o formulário não guarda o estado anterior.
- Limites (domínio): nome e nome fantasia **2 a 150**; nome do local **2 a 60**; série **1 a 60**; logradouro 2 a 150; número 1 a 15; complemento 1 a 50; bairro 2 a 100; cidade 1 a 100; e-mail até 254; data de instalação não depois de hoje.
- Desativar e reativar **sem confirmação** (ADR-6). Situação por forma além da cor (`StatusChip`, círculo cheio no ativo e vazado no desativado, como o catálogo).
- **Técnico** (ADR-7): nenhuma tela mostra botão ou link de escrita; toda rota de escrita (`novo`, `editar`, página de local e de equipamento) redireciona para a lista ou para a ficha.
- Listas da web ao lado de outra coluna têm guarda `overflow-x-auto`; barras de busca e filtro quebram linha a 768 px; alvos de toque de no mínimo 44 px; cabeçalho e linhas de tabela vêm da **mesma** definição de colunas.
- Identificadores em inglês; texto de interface em pt-BR. Ícone é SVG de traço (`lucide-react`).
- Commit: Conventional Commits, escopo `frontend`, em inglês, **sem `Co-Authored-By`**. Branch `feature/customers-frontend` num **git worktree separado** do backend; base `develop` depois do merge do PR da spec: **pergunte a base** antes da Tarefa 1. As Tarefas 2 a 9 rodam só com serviço mockado, em paralelo ao backend; a Tarefa 10 precisa do backend de pé.

## Cobertura dos mockups

Artboards numerados como em [`design/README.md`](../system-design/03-customers/design/README.md). **Derivado** = sem artboard no tamanho, vai à comparação final.

| Tela | Web (1280) | Celular (390) |
|---|---|---|
| Clientes, lista | 1 | 2 |
| Lista: sem resultado | derivado (3 na web) | 3 |
| Lista: nenhum cadastrado / nenhum ativo | 4 / D2 | derivado / D2 |
| Lista, Técnico | derivado (1 sem "Novo cliente", com o aviso) | 5 |
| Lista: erro de carregamento | 6 | derivado |
| Linha com a série casada | D1 | D1 |
| Novo cliente / editar cliente | 7 (*Tweak* `modo`) | 8 (erros); sem erro derivado |
| Ficha | 9 | 10 |
| Ficha sem locais | derivado | 11 |
| Ficha do cliente desativado | 12 | derivado |
| Ficha, Técnico | derivado | 13 |
| Local desativado na ficha | 9 (último cartão) | 16 |
| Novo local / editar local | 14 / D3 | 15 / D4 |
| Novo equipamento | 17 | 18 |
| Editar equipamento / desativado | D5 / D6 | 19 (os dois estados) |
| Barra inferior do Comercial | n/a | D7 |
| "Clientes" primeiro na barra lateral | em todas as telas web | n/a |

## Review Focus

1. **Documento:** o tipo aparece ao digitar; CPF e CNPJ (inclusive o alfanumérico) com dígito errado não enviam; `customer_document_taken` mostra "Ver cliente com este documento", que abre a lista buscando o documento com "Todos". → Tarefas 2 e 6.
2. **Telefone:** máscara de fixo e de celular, envio em `+55`, exibição de volta formatada; telefone estrangeiro guardado aparece como está e **não se perde** ao salvar a edição. → Tarefas 2 e 6.
3. **Edição sem dado:** nenhuma edição envia campo vazio porque a ficha ainda carregava; "Salvar" fica desligado até lá. → Tarefas 6, 8 e 9.
4. **Técnico por URL:** `/clientes/novo`, `/clientes/:id/editar`, as páginas de local e de equipamento redirecionam; nenhuma tela mostra escrita. → Tarefas 4 a 9.
5. **Pai desativado:** ficha de cliente desativado sem "Novo local"/"Novo equipamento"/"Editar cliente", com "Editar local" e "Editar" (ADR-30); local desativado com "Reativar local" e sem "Editar local". → Tarefa 7.
6. **Lista vazia e série casada:** "Nenhum cliente cadastrado" × "Nenhum cliente ativo" pela segunda consulta só no vazio (ADR-28); a linha "Série … · modelo" (ADR-27). → Tarefa 5.

---

### Tarefa 1: Portão de divergências e dependências

Sem código. Leva ao usuário, com a recomendação, o que os mockups trazem e o código ainda não tem ou faz diferente. **As Tarefas 3 em diante não começam antes da resposta**; a Tarefa 2 (sem tela) pode começar.

| Ponto | O que o mockup mostra | O que existe hoje | Recomendação |
|---|---|---|---|
| Ordem da navegação | "Clientes" primeiro, ícone de cartão de contato (o `ContactIcon` do `lucide-react`) | `NAV_ITEMS` com Catálogo e Usuários | `NAV_ITEMS` com Clientes primeiro (ADR-26); a barra inferior herda a ordem (três itens para o Administrador, dois para Comercial e Técnico: D7) |
| Página inicial | não desenhada | `/` manda o Administrador para `/usuarios` e mostra "Em construção" aos demais | `/` redireciona os três papéis para `/clientes` (o comentário do `home.tsx` já previa que a primeira feature de negócio o substitui) |
| Filtro de situação | tela 1: "Todos, Ativos, Desativados"; telas 2 a 6: "Ativos, Desativados, Todos" | o catálogo usa "Ativos, Desativados, Todos" | "Ativos, Desativados, Todos" em todas (o mockup se contradiz; o padrão é `ACTIVE`) |
| Tabelas da web | `<table>` de verdade na lista e na ficha | o catálogo usa linhas em `grid` | `<table>` com `thead` e uma definição de colunas só (cabeçalho e linhas alinham por construção); cartões no celular |
| Seção recolhível | `<details>`/`<summary>` nativos na ficha do celular | nenhum componente | o elemento nativo, estilizado na feature; nenhum componente novo |
| Erro sob o campo e carregando | ícone + texto; indicador | `FieldError` e `Spinner` moram em `features/catalog` (uma feature não importa outra) | mover os dois para `shared/ui/{field-error,spinner}.tsx`, e o catálogo passa a importá-los de lá |
| Máscaras | CPF/CNPJ, `(00) 00000-0000`, `00000-000`, com `inputmode="numeric"` | nada | funções da feature sobre o `Input` (`size="compact"`), sem biblioteca |
| CNPJ alfanumérico | dica "Digite só os números" e `inputmode="numeric"` no documento | o `Document` aceita CNPJ com letras (2026) | aceitar letras (maiúsculas) no campo, teclado de texto no celular e dica "Digite só letras e números" — **muda texto aprovado**: o usuário decide |
| Valor da característica | "12.000 BTU/h" (com milhar) na ficha e na opção do modelo | `formatAmount` do catálogo mostra "12000" | seguir o mockup nesta feature (`Intl.NumberFormat("pt-BR")`); a diferença para as telas do catálogo vai à comparação |
| Selo de situação | caractere ●/○ dentro do selo | `StatusChip` com o círculo em SVG fixo | `StatusChip` (tom `ok`; `off` com forma `ring`), como o catálogo |
| Telefone estrangeiro na edição | não desenhado | o `Phone` aceita qualquer E.164 (ADR-18) | o campo mostra o número guardado como está, desabilitado, com a dica "Número de fora do Brasil: não se edita nesta tela", e o valor é reenviado sem mudança |
| Linha da série casada, nenhum ativo, edições derivadas | D1 a D7 | — | implementar como a spec descreve e levar à comparação final |

- [ ] **Step 1:** levar a tabela ao usuário e registrar a resposta em `docs/system-design/03-customers/design/README.md`. **Step 2:** nenhuma divergência nova aprovada além da tabela → seguir; qualquer outra descoberta depois entra na comparação final.

### Tarefa 2: Fundação da feature

**Files:** `src/features/customers/{customers.service.ts,customers.messages.ts,customers.schemas.ts,customers.hooks.ts,document.ts,phone.ts,zip-code.ts,display.ts}` e testes ao lado.

**Interfaces (produz):**
- `customers.service.ts` (usa `httpClient`, `ensureCsrfCookie()` antes de toda escrita, `Page` de `@/shared/api/page`; rotas de `usecases.md`): tipos `CustomerStatus = "ACTIVE" | "DEACTIVATED"`, `StatusFilter = CustomerStatus | "ALL"`, `DocumentType = "CPF" | "CNPJ"`, `UF` (as 27 siglas), `CustomerSummary { id; name; tradeName: string | null; document; documentType; status; locationCount; equipmentCount; cities: { city: string; state: UF }[]; matchedSerials: { serialNumber: string; modelName: string }[] }`, `CustomerDetail { id; document; documentType; name; tradeName; email; phone; status; locations: LocationView[] }`, `LocationView { id; name; status; address: Address; equipment: EquipmentView[] }`, `Address { zipCode; street; number; complement: string | null; neighborhood; city; state: UF }`, `EquipmentView { id; serialNumber; installedOn: string | null; status; model: { id; name; status }; manufacturer: { id; name }; type: { id; name }; characteristic: { name; unit: Unit; amount: string | number } | null }`, `Unit` (as quatro do catálogo), `ModelOption` (o recorte do `EquipmentModelSummary` do catálogo que o formulário usa). Funções: `listCustomers({ text?, status?, page, size })`; `getCustomer(id)`; `registerCustomer(input: CustomerInput & { document: string })` → `{ id }`; `updateCustomer(id, input: CustomerInput)` com `CustomerInput { name; tradeName: string | null; email: string | null; phone: string | null }`; `deactivateCustomer(id)`, `reactivateCustomer(id)`; `registerLocation(customerId, input: { name; address: Address })`; `updateLocation(id, input)`; `deactivateLocation(id)`, `reactivateLocation(id)`; `installEquipment(locationId, input: { modelId; serialNumber; installedOn: string | null })`; `updateInstalledEquipment(id, input: { serialNumber; installedOn: string | null })`; `deactivateInstalledEquipment(id)`, `reactivateInstalledEquipment(id)`; `listModelOptions()` = `GET /catalog/equipment-models?status=ACTIVE&page=0&size=100` (shortcut como o do catálogo: as 100 primeiras; paginar a escolha se o catálogo passar disso).
- `customers.messages.ts`: `messageFor(code, fallback)` com **todos** os códigos da tabela de erros (texto exato da spec); `sentence(text)` (inicial maiúscula e ponto final); `errorMessage(error)` para o `Alert` (pelo `code`, senão o texto da API, como frase); `FORM_MESSAGES` com **cada** texto de "Mensagens da tela"; `LABELS.status` (`Ativo`, `Desativado`), `LABELS.unit` (`kW`, `W`, `Wp`, `BTU/h`); contagens: `locationsSummary(active, deactivated)` ("2 ativos, 1 desativado"), `activeEquipmentText(n)` ("3 equipamentos ativos", "1 equipamento ativo"), `listCounts(locais, equip)` ("2 locais · 5 equip.", "1 local · 3 equip.").
- `document.ts`: `normalizeDocument(text)` (só letras e dígitos, maiúsculas); `documentTypeOf(text): DocumentType | undefined` (11 → CPF, 14 → CNPJ); `formatDocument(value)` (`000.000.000-00`, `00.000.000/0000-00`, parcial ao digitar); `checkDocument(text): { ok: true; value: string } | { ok: false; message: string }` com os dígitos verificadores do CPF e do CNPJ **alfanumérico** (mesmo algoritmo do `CnpjValidator`: letra vale `código - 48`; sequência repetida recusada) e as mensagens de `FORM_MESSAGES`.
- `phone.ts`: `maskPhone(text)` (`(00) 0000-0000` ou `(00) 00000-0000`); `toE164(masked): string | null` (`+55` + DDD + número; `null` se vazio); `checkPhone(masked)` (DDD de dois dígitos sem zero inicial; 8 dígitos para fixo ou 9 começando por 9 para celular, ADR-18); `formatPhone(stored)` (`+55…` volta formatado; outro E.164 como está); `isBrazilian(stored)`.
- `zip-code.ts`: `maskZipCode(text)`, `formatZipCode("01310100")` → `"01310-100"`. `display.ts`: `formatDate("2025-03-12")` → `"12/03/2025"`; `formatAmount(value)` conforme a resposta da Tarefa 1; `orderCities(cities, text)` (a que casa o texto primeiro, depois por nome: design/README).
- `customers.schemas.ts` (Zod, mensagens de `FORM_MESSAGES`): `customerSchema { document; name; tradeName; email; phone }` (documento por `checkDocument`; nome fantasia só com CNPJ; **pelo menos um contato**, com o erro em `contact`); `locationSchema { name; zipCode; street; number; complement; neighborhood; city; state }`; `equipmentSchema(today: string) { modelId; serialNumber; installedOn }` (data não depois de `today`, recebido de fora para o teste ser determinístico). Texto apara as pontas.
- `customers.hooks.ts`: chaves `["customers", "list", input]`, `["customers", "detail", id]`, `["customers", "model-options"]`; `useCustomers(input)` (com `keepPreviousData`), `useCustomersInfinite(input)` (celular, "Carregar mais"), `useDeactivatedCount(enabled)` (`status=DEACTIVATED&size=1`, só ligado no vazio: ADR-28), `useCustomer(id)`, `useModelOptions()`; uma mutation por escrita, cada uma invalidando `["customers"]`.

- [ ] **Step 1: Testes que falham:** `document.test.ts` (CPF e CNPJ válidos, inclusive `"12.ABC.345/01DE-35"`, passam e normalizam; dígito errado de cada um dá a mensagem certa; 10 e 12 caracteres dão "Documento incompleto…"; `"11111111111"` recusado; `formatDocument` parcial e completo); `phone.test.ts` (fixo `(34) 3212-4580` → `+553432124580`; celular `(11) 99999-9999` → `+5511999999999`; 9 dígitos sem o 9 inicial, DDD `01` e 7 dígitos recusados; `formatPhone("+14155552671")` igual ao guardado); `zip-code.test.ts`; `display.test.ts` (`orderCities` põe a cidade buscada primeiro); `customers.service.test.ts` (rota, verbo e corpo de cada função; CSRF antes da escrita; opcionais `null`); `customers.messages.test.ts` (todo código da spec tem mensagem; `sentence`; código desconhecido cai no fallback); `customers.schemas.test.ts` (**uma linha de teste por linha de "Mensagens da tela"**, com os limites inferior e superior); `customers.hooks.test.tsx` (mutation invalida `["customers"]`; `useDeactivatedCount(false)` não consulta).
- [ ] **Step 2: Rodar e ver falhar:** `npm test -- src/features/customers` → FAIL.
- [ ] **Step 3: Implementar.** **Step 4: Rodar a suíte inteira.** **Step 5: Commit:** `feat(frontend): add the customers service, hooks, schemas and formatters`.

### Tarefa 3: Peças de UI

**Files:** `src/shared/ui/{field-error,spinner}.tsx` e testes (movidos do catálogo, conforme a Tarefa 1); `src/features/catalog/**` (só os imports); `src/features/customers/components/{customer-status,document-text,city-list,matched-serials,location-block,equipment-table,equipment-card}.tsx` e testes.

**Interfaces (produz):**
- `FieldError({ id, children })` e `Spinner()` em `shared/ui`, iguais aos de hoje.
- `CustomerStatusChip({ status })` (como o `CatalogStatusChip`); `DocumentText({ type, value })` (selo do tipo + número formatado: telas 1, 2, 9); `CityList({ cities, text })` (até duas, depois "+N" com "e mais N cidades" para leitor de tela; "Nenhum local" sem cidade); `MatchedSerials({ items })` (D1: uma linha "Série X · modelo" por item, nada se vazio).
- `EquipmentTable({ equipment, canEdit, customerId })` (web, tela 9): **uma** constante de colunas para `thead` e `tbody` (Modelo, Fabricante, Tipo e característica, Número de série, Instalado em, Situação, Ações em `sr-only`); "Editar" no ativo e "Abrir" no desativado, só com `canEdit`; "Sem data" sem data; guarda `overflow-x-auto`.
- `EquipmentCard({ item, href })` (celular, telas 10 e 16): link inteiro quando há `href` (quem edita), texto quando não há (Técnico).
- `LocationBlock({ location, customerActive, canEdit, onReactivate, reactivating })`: na web o cartão da tela 9 (todos abertos), no celular o `<details>` da tela 10, **aberto se ativo e fechado se desativado** (ADR-22); título, selo, endereço `Logradouro, número[, complemento] · bairro · cidade/UF · CEP 00000-000`, `activeEquipmentText`; ações: "Editar local" e "Novo equipamento" só com `canEdit` e local ativo ("Novo equipamento" também exige `customerActive`); local desativado com o aviso da tela 9 e "Reativar local" (com `canEdit`).

- [ ] **Step 1: Testes que falham:** `field-error.test.tsx` e `spinner.test.tsx` (os do catálogo seguem verdes); `city-list.test.tsx` ("+1" com o texto de leitor de tela; a cidade buscada primeiro); `matched-serials.test.tsx`; **`equipment-table.test.tsx`** (cabeçalho e linhas com a mesma classe de colunas; Técnico sem "Editar"/"Abrir"; "Sem data"); `location-block.test.tsx` (desativado fechado no celular e aberto o ativo; **cliente desativado: sem "Novo equipamento", com "Editar local"**; local desativado: "Reativar local" e nada de "Editar local"); `customer-status.test.tsx`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar a suíte** (inclui cor literal). **Step 5: Commit:** `feat(frontend): add the customers ui pieces`.

### Tarefa 4: Casca ("Clientes" primeiro, títulos, página inicial) e acesso

**Files:** `src/app/layout/app-shell.tsx` e teste; `src/app/routes/home.tsx` e teste; `src/app/routes/clientes.access.ts` e teste. O `src/app/routes.ts` é editado em cada tarefa de tela (5 a 9), junto das rotas que ela cria.

**Interfaces:**
- `NAV_ITEMS`: `{ to: "/clientes", label: "Clientes", icon: ContactIcon, roles: [ADMINISTRATOR, COMMERCIAL, TECHNICIAN] }` **primeiro** (ADR-26); a `BottomNav` não muda de código.
- `OWN_HEADER_PREFIXES` ganha `"/clientes/"`: a ficha e os formulários têm cabeçalho próprio no celular, sem barra superior nem inferior (design/README); `/clientes` (a lista) mantém as barras e o título "Clientes" pelo item.
- `home.tsx`: `/` redireciona para `/clientes` (conforme a resposta da Tarefa 1).
- `useCanEditCustomers(): boolean` em `clientes.access.ts`: `true` para `ADMINISTRATOR` e `COMMERCIAL` (usa `useCurrentUser` do IAM, como o `useCanEditCatalog`).
- Rotas (registradas nas tarefas 5 a 9, dentro do `layout("routes/authenticated.tsx")`, estáticas antes de `:id`): `clientes` (`clientes._index.tsx`), `clientes/novo`, `clientes/:id` (ficha), `clientes/:id/editar`, `clientes/:id/locais/novo`, `clientes/:id/locais/:locationId`, `clientes/:id/locais/:locationId/equipamentos/novo`, `clientes/:id/equipamentos/:equipmentId`.

- [ ] **Step 1: Testes que falham** (`app-shell.test.tsx`): **"Clientes" é o primeiro item** na barra lateral e na inferior para os três papéis; Técnico vê Clientes e Catálogo; Comercial vê Clientes e Catálogo (D7); `/clientes/123` no celular não tem barra superior nem inferior; `/clientes` tem o título "Clientes"; `home.test.tsx`: os três papéis vão para `/clientes`; `useCanEditCustomers` é falso para o Técnico.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar.** **Step 4: Rodar a suíte** (os testes da casca do catálogo e do IAM seguem verdes). **Step 5: Commit:** `feat(frontend): put customers first in the shell`.

### Tarefa 5: Lista de clientes

Segue os artboards **1, 2, 3, 4, 5, 6** e os derivados **D1, D2**.

**Files:** `src/features/customers/screens/customers.tsx` e teste; `src/features/customers/components/customer-row.tsx`; `src/app/routes/clientes._index.tsx`.

**Interfaces:**
- `CustomersScreen({ canEdit }: { canEdit: boolean })`: busca e situação **na URL** (`?q=` e `?situacao=ACTIVE|DEACTIVATED|ALL`, padrão `ACTIVE`), para "Ver cliente com este documento" (ADR-23) abrir a lista já buscando; campo de busca com o placeholder e a dica das telas 1 e 2; filtro de situação (ordem da Tarefa 1); "Novo cliente" só com `canEdit` (na web no topo, no celular fixo acima da barra inferior); aviso do papel para o Técnico (tela 5); web: `<table>` com Cliente (nome, nome fantasia abaixo, `MatchedSerials`), Documento, Cidades, Locais, Equipamentos, Situação, "Abrir"; celular: cartões com `listCounts` e "Carregar mais"; paginação na web como o catálogo; "N clientes encontrados" no celular com busca (tela 5).
- Estados: carregando (`Spinner`); **erro** (tela 6: "Não foi possível carregar os clientes", o texto da tela e "Tentar de novo", com busca, filtro e "Novo cliente" usáveis); **sem resultado** com texto ou filtro (tela 3: título, o texto com o termo buscado, "Limpar busca e filtros" e, com `canEdit`, "Novo cliente"); **vazio padrão** (sem texto, `ACTIVE`): `useDeactivatedCount(true)` decide entre a tela 4 ("Nenhum cliente cadastrado", o texto, "Cadastrar o primeiro cliente" com `canEdit`, a nota de que busca e filtro continuam) e D2 ("Nenhum cliente ativo", o texto do ADR-28, "Ver desativados" que muda a situação, "Novo cliente" com `canEdit`).

- [ ] **Step 1: Testes que falham** (serviço mockado, `createRoutesStub`): a linha mostra nome, nome fantasia, `CPF 318.457.260-90`, cidades, contagens e situação; **busca e situação chegam ao serviço e à URL**; `?q=34218765000114&situacao=ALL` abre já buscando; **série casada** aparece como "Série HV9910042 · SUN2000-5KTL-L1"; a cidade buscada vem primeiro; sem resultado mostra o termo e "Limpar busca e filtros" refaz a consulta; **vazio padrão sem desativados** → "Nenhum cliente cadastrado"; **com desativados** → "Nenhum cliente ativo" e "Ver desativados" muda o filtro; a consulta de desativados **não sai** quando a lista tem cliente; erro mostra "Tentar de novo", que refaz a consulta; **Técnico sem "Novo cliente"** e com o aviso; contratos de classe (`text-display-lg` no h1 da web e `max-md:sr-only`, `overflow-x-auto` na tabela, filtros com `md:flex-wrap`).
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** e a rota fina (`canEdit` de `useCanEditCustomers`). **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the customers list`.

### Tarefa 6: Novo cliente e editar cliente

Segue os artboards **7** (novo e, no *Tweak*, editar) e **8** (erros); celular sem erro e web com erro derivados.

**Files:** `src/features/customers/screens/customer-form.tsx` e teste; `src/app/routes/{clientes.novo,clientes.$id.editar}.tsx`.

**Interfaces:**
- `CustomerFormScreen({ mode, canEdit }: { mode: "new" | "edit"; canEdit: boolean })`. Sem `canEdit`: `new` volta para `/clientes` e `edit` para a ficha.
- **Novo:** documento com máscara, o tipo reconhecido ao lado e o aviso do cadeado "Confira antes de salvar: o documento não pode ser alterado depois de salvo." (ADR-24); rótulo "Nome *" (CPF) ou "Razão social *" (CNPJ); "Nome fantasia (opcional)" só com CNPJ, com a dica da tela 7; seção "Contato: informe pelo menos um, e-mail ou telefone", e-mail e telefone com a dica "Fixo ou celular, com DDD."; "Cancelar" e "Salvar cliente" (web) / rodapé fixo (celular). Salvar navega para a ficha nova.
- **Editar:** só depois de `useCustomer(id)` trazer o cliente, com os valores **do próprio cliente** (telefone `+55` formatado; estrangeiro desabilitado e reenviado como está, conforme a Tarefa 1); documento somente leitura com o tipo e "O documento não pode ser alterado depois de salvo."; cliente desativado redireciona para a ficha; "Salvar alterações" volta para a ficha.
- Erros pelo `code` (usecases.md, "Onde aparece"): `customer_document_taken` sob o documento com o link "Ver cliente com este documento" (`/clientes?q=<documento normalizado>&situacao=ALL`); `contact_required` no topo da seção Contato, com e-mail e telefone em `aria-invalid`; `trade_name_not_allowed` sob o nome fantasia; o resto no `Alert`. Foco no campo do erro.

- [ ] **Step 1: Testes que falham:** digitar `34218765000114` mostra "CNPJ", `34.218.765/0001-14`, troca o rótulo para "Razão social *" e mostra o nome fantasia; trocar para CPF esconde o nome fantasia **e não envia o que estava nele**; CPF com dígito errado mostra "CPF inválido: confira os números digitados." e **não chama o serviço**; sem contato mostra "Informe pelo menos um contato: e-mail ou telefone." no topo da seção; telefone `(11) 9999-999` mostra "Telefone inválido: informe DDD e número."; envio manda `document` normalizado, `phone: "+5511999999999"` e `email: null`; **409 `customer_document_taken`** mostra a mensagem e o link, mantém o que foi digitado, e o foco vai ao documento; editar: o formulário só aparece com o cliente carregado e "Salvar" não envia antes disso; o corpo do `PUT` não tem `document`; telefone estrangeiro preservado; Técnico redirecionado nas duas rotas.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** e as rotas. **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the customer form`.

### Tarefa 7: Ficha do cliente

Segue os artboards **9, 10, 11, 12, 13, 16** (os outros tamanhos derivados).

**Files:** `src/features/customers/screens/customer-detail.tsx` e teste; `src/app/routes/clientes.$id.tsx`.

**Interfaces:**
- `CustomerDetailScreen({ canEdit }: { canEdit: boolean })`: cabeçalho com nome, nome fantasia e selo; dados (documento com "Não pode ser alterado." para quem edita, e-mail, telefone formatado, "Não informado" no ausente: tela 11); **ativo** com `canEdit`: "Editar cliente" e "Desativar cliente" (link de perigo, sem confirmação); **desativado** com `canEdit`: o aviso da tela 12 e a ação única "Reativar cliente"; Técnico: o aviso do papel (tela 13) e nenhuma ação; "Locais" com `locationsSummary`; "Novo local" (web no título da seção, celular fixo embaixo) só com `canEdit` e cliente ativo; um `LocationBlock` por local, na ordem da API; sem locais, o vazio da tela 11 ("Nenhum local cadastrado", o texto e "Cadastrar o primeiro local" com `canEdit` e cliente ativo).
- Erro de carregamento: o mesmo aviso da tela 6 com "Tentar de novo"; `customer_not_found` no `Alert`. Erro de desativar ou reativar (`customer_deactivated`, `location_not_deactivated`, `concurrent_update`) no `Alert` da ficha, que se recarrega.

- [ ] **Step 1: Testes que falham:** ficha com dois locais ativos e um desativado (ordem da API mantida, "2 ativos, 1 desativado"); "Desativar cliente" chama o serviço sem confirmar e a ficha passa a "Reativar cliente"; **cliente desativado: sem "Editar cliente", "Desativar cliente", "Novo local" e "Novo equipamento", com "Editar local" e "Editar"** (ADR-30); "Reativar local" chama o serviço; celular: local desativado fechado e o ativo aberto; sem locais, "Cadastrar o primeiro local"; **Técnico: nenhum botão ou link de escrita** e as linhas de equipamento não são links; erro com "Tentar de novo".
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** e a rota. **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the customer detail`.

### Tarefa 8: Novo local e editar local

Segue os artboards **14, 15** e os derivados **D3, D4, D6**.

**Files:** `src/features/customers/screens/location-form.tsx` e teste; `src/app/routes/{clientes.$id.locais.novo,clientes.$id.locais.$locationId}.tsx`.

**Interfaces:**
- `LocationFormScreen({ mode, canEdit }: { mode: "new" | "edit"; canEdit: boolean })`: lê a ficha (`useCustomer(id)`) para o nome do cliente e, na edição, o local (sem `GET` próprio: ADR-22); sem `canEdit` volta para a ficha; local de outro cliente ou inexistente → `Alert` "local não encontrado".
- Campos da tela 14: "Cliente" (texto), "Nome do local *" com a dica, seção "Endereço" com CEP (máscara) e a dica "O CEP não preenche o endereço: digite todos os campos.", logradouro, número com "Sem número, use \"S/N\".", complemento (opcional), bairro, cidade e UF (`NativeSelect` com as 27 siglas). Novo: só com o cliente ativo (desativado → volta à ficha); "Salvar local" volta à ficha.
- **Editar (D3/D4):** título com o nome do local, valores do próprio local, "Salvar alterações"; web: painel "Situação" ao lado com o selo, a ajuda e "Desativar local" (sem confirmação); celular: selo no topo e "Desativar local" abaixo do formulário. **Desativado (D6):** só leitura com "Reativar local". Desativar ou reativar volta a página ao estado novo sem sair dela.
- Erros: `customer_deactivated`, `location_deactivated`, `concurrent_update` e o 400 sem `code` no `Alert`.

- [ ] **Step 1: Testes que falham:** CEP `01310-10` mostra "CEP incompleto: informe os 8 dígitos." e não envia; bairro vazio mostra "Informe o bairro."; sem UF, "Escolha a UF."; envio manda `zipCode: "01310100"`, `complement: null`; editar mostra os valores do local e não renderiza o formulário antes da ficha; "Desativar local" troca para "Reativar local"; local desativado aberto por URL fica só leitura; **Técnico redirecionado** nas duas rotas; `location_deactivated` (corrida) no `Alert`.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** e as rotas. **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the location form`.

### Tarefa 9: Novo equipamento e editar equipamento

Segue os artboards **17, 18, 19** e os derivados **D5, D6**.

**Files:** `src/features/customers/screens/equipment-form.tsx` e teste; `src/app/routes/{clientes.$id.locais.$locationId.equipamentos.novo,clientes.$id.equipamentos.$equipmentId}.tsx`.

**Interfaces:**
- `EquipmentFormScreen({ mode, canEdit })`: lê a ficha para o local e, na edição, o equipamento; sem `canEdit` volta para a ficha.
- **Novo (17/18):** "Local" somente leitura ("Loja Centro · Av. João Pinheiro, 1450 · Uberlândia/MG"); "Modelo *" (`NativeSelect` de `useModelOptions`, opção "modelo · fabricante · tipo · valor unidade", com a dica da tela 17); "Número de série *" (`maxLength` 60, "Copie da etiqueta do equipamento."); "Data de instalação (opcional)" (`input type="date"` com `max` = hoje, "Não pode ser depois de hoje."); a nota "Modelo e local não mudam depois de salvos. A série e a data podem ser corrigidas."; "Salvar equipamento" volta à ficha. Só com local e cliente ativos.
- **Editar (19/D5):** modelo e local somente leitura, a nota da tela 19, série e data editáveis com os valores do próprio equipamento, "Salvar alterações"; web: painel "Situação" com "Desativar equipamento"; celular: como a tela 19. **Desativado (19 desligado/D6):** só leitura com o texto da tela 19 e "Reativar equipamento".
- Erros: `serial_number_taken` sob a série (no cadastro, com "Confira a etiqueta ou busque a série na lista de clientes."), `invalid_installation_date` sob a data, foco no campo; `model_deactivated`, `location_deactivated`, `customer_deactivated`, `installed_equipment_deactivated`, `concurrent_update` no `Alert`. Falha da lista de modelos: `Alert` e "Salvar" desligado.

- [ ] **Step 1: Testes que falham:** a opção mostra "SUN2000-5KTL-L1 · Huawei · Inversor · 5 kW"; sem modelo, "Escolha o modelo."; data de amanhã (relógio fixo do teste) mostra "A data de instalação não pode ser depois de hoje." e não envia; envio manda `installedOn: null` sem data; **409 `serial_number_taken`** sob a série, com o texto extra no cadastro e só a frase da tabela na edição, mantendo o digitado e com foco; editar não envia antes da ficha carregar e o corpo não tem modelo nem local; "Desativar equipamento" troca para "Reativar equipamento"; **equipamento de local desativado se edita** (ADR-30); **Técnico redirecionado**.
- [ ] **Step 2: Rodar e ver falhar.** **Step 3: Implementar** e as rotas. **Step 4: Rodar a suíte.** **Step 5: Commit:** `feat(frontend): add the installed equipment form`.

### Tarefa 10: Fechamento

**Files:** `docs/system-design/03-customers/design/comparacao-mockup.md`; `docs/system-design/03-customers/README.md` (critério de aceite).

- [ ] **Step 1:** com o backend de pé (Postgres, Redis, API em `http://localhost:8080`, o plano do backend executado) e o `npm run dev`, abrir cada tela **num navegador real em 390 e em 1280**, e a lista, a ficha e os formulários também **em 768 e 1024** (estouro horizontal, quebra da barra de filtros, tabela ao lado de outra coluna), e comparar os estilos computados com os valores dos artboards, no formato de [`01-iam/design/comparacao-mockup.md`](../system-design/01-iam/design/comparacao-mockup.md): separar o conferido visualmente do estrutural e listar **cada divergência, estado sem mockup e tela derivada (D1 a D7)** para o usuário decidir, inclusive as respostas da Tarefa 1 (milhar, CNPJ com letras, ordem do filtro, telefone estrangeiro).
- [ ] **Step 2:** `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check`, pelo PowerShell: verde (o aviso conhecido de `react-refresh` em `shared/ui/form.tsx` é o único tolerado).
- [ ] **Step 3:** marcar o critério de aceite do frontend no `README` da feature e **não** declarar a comparação encerrada: ela fecha com a decisão do usuário sobre as divergências.
- [ ] **Step 4: Commit:** `docs(customers): record the mockup comparison`.

---

## Notas de consistência com a spec

Conferido antes de gravar: cada tarefa consome só o que as anteriores produzem (serviço, mensagens, schemas e formatação na 2; peças de UI na 3; casca, acesso e a lista de rotas na 4; a ficha da 7 usa o `LocationBlock` da 3, e os formulários da 8 e da 9 só leem a ficha pelo hook da 2). Onde o plano segue a spec contra o costume ou contra o mockup:

- **Mensagens:** os textos sob os campos são os de "Mensagens da tela" e os da tabela de erros como frase (ADR-31), não o texto cru da API como o catálogo fazia no campo; o 400 sem `code` vai ao `Alert`, nunca a um campo pelo texto.
- **Lista vazia:** a segunda consulta (`status=DEACTIVATED&size=1`) só sai no vazio padrão (ADR-28); o mockup 4 só cobre o primeiro caso, e o segundo é D2.
- **Série casada e cidades:** a linha da série vem do `matchedSerials` da API (ADR-27); a ordem das cidades é do frontend (design/README); nenhuma das duas é inventada pela tela.
- **Pai desativado:** a ficha esconde só o que o domínio recusa (cadastro novo, edição do próprio desativado) e mantém "Editar local"/"Editar" nos filhos ativos de cliente desativado (ADR-30), como a tela 12 aprovada.
- **Busca na URL:** necessária para "Ver cliente com este documento" abrir a lista buscando com "Todos" (ADR-23); o catálogo guardava o filtro só no estado da tela.
- **Telefone estrangeiro:** o ADR-18 diz que ele não é digitável e é exibido como está; a edição o preserva em vez de apagá-lo, e o desenho do campo vai à Tarefa 1.
- **Modelos do formulário:** `GET /catalog/equipment-models?status=ACTIVE` (ADR-8), pelo serviço da própria feature, com o mesmo limite de 100 do catálogo, marcado como `shortcut`.
