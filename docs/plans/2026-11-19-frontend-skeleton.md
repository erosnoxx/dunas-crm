# Esqueleto do frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ter `frontend/` rodando (build, lint, teste, formatação) com a identidade DUNAS, o cliente HTTP com os dois formatos de sessão e os componentes base que as telas do IAM usam, partindo do esqueleto do Lume.

**Architecture:** Copia o esqueleto do Lume em `a158f13` e as melhorias que o Lume fez depois na camada `shared/`; troca o tema pelo DUNAS gerado dos tokens; nenhuma feature ainda. React Router framework mode (SPA), TanStack Query + axios, React Hook Form + Zod, Tailwind v4 + shadcn/ui sobre Base UI.

**Tech Stack:** Node 22 (o `.nvmrc`), npm, React 19, TypeScript strict, Vite, Vitest + Testing Library + jsdom, ESLint + Prettier, `@fontsource/chakra-petch`, `@fontsource/ibm-plex-sans`.

**Spec:** [`docs/rules/frontend.md`](../rules/frontend.md) (arquitetura), [`docs/design-system/`](../design-system/README.md) (identidade), [`architecture.md`](../rules/architecture.md) §14 (sessão). O plano não redefine essas regras.

## Global Constraints

- Tudo roda a partir de `frontend/`. Aceite de toda tarefa: `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check` verde.
- Origem do código: repositório `C:\Develop\Projects\lume-ticketing`, commit `a158f13` (esqueleto) e `HEAD` (melhorias de `shared/`). Leia com `git -C C:\Develop\Projects\lume-ticketing show <commit>:frontend/<caminho>`. **Nada do domínio do Lume entra**: tema Mulekera/Lume, logo, features, textos, rotas de marca.
- Nomes de token e de classe Tailwind são os do `docs/design-system/tokens.json` (inglês), nunca os do Lume.
- Texto de interface e comentário em pt-BR; identificadores em inglês. Sem cor literal fora de `theme.css`. Sem `axios` fora de `shared/api/`.
- Commit: Conventional Commits, escopo `frontend`, em inglês, **sem `Co-Authored-By`**. Branch `feature/frontend-skeleton`; a base é `develop`, que ainda não existe: **pergunte a base antes da Tarefa 1**. Não misture o trabalho não commitado do `main` (docs, skills, kernel) nos commits.
- Nenhum `package.json` sobe versão além das do esqueleto do Lume. `package-lock.json` é commitado.

## Review Focus

1. **Tema claro divergindo em silêncio do `tokens.json`** (só o escuro é desenhado nos mockups): o teste confere os dois. → Tarefa 3.
2. **Cancelamento de requisição virando aviso de erro para a pessoa:** passa como veio. → Tarefa 2.
3. **Escrita no navegador sem cookie de CSRF** (aba nova): `ensureCsrfCookie` busca antes e ignora o 401. → Tarefa 2.
4. **Token vazando para a web:** nenhum código do navegador lê, guarda ou envia `Authorization`; só um provedor de token explícito, vazio na web. → Tarefa 2.
5. **Cor literal em tela contornando o tema:** um teste varre o código-fonte. → Tarefa 3.

---

### Tarefa 1: Importar o esqueleto do Lume

**Files:** criar `frontend/` com o conteúdo de `a158f13:frontend/` **exceto** `package-lock.json` (regenerado), `public/favicon.ico` (Lume), `src/styles/theme.css`, `src/shared/ui/*` e `src/app/routes/home.tsx` (reescritos nas tarefas seguintes); ajustar `.gitignore` da raiz (`frontend/node_modules`, `frontend/build`, `frontend/.react-router`).

**Interfaces:** produz a estrutura de `docs/rules/frontend.md` §3, com `appDirectory: "src/app"`, alias `@/*` → `src/*`, scripts `dev`, `build`, `typecheck`, `lint`, `format`, `format:check`, `test`.

- [x] **Step 1:** Exportar o esqueleto: `git -C C:\Develop\Projects\lume-ticketing archive a158f13 frontend | tar -x -C <pasta temporária>` e copiar para `frontend/` os arquivos listados acima como "ficam". Trocar `"name"` por `"crm-frontend"`; remover `@fontsource/manrope` e `@fontsource/space-mono`; acrescentar `@fontsource/chakra-petch` e `@fontsource/ibm-plex-sans`; acrescentar o script `"format:check": "prettier --check ."`.
- [x] **Step 2:** `npm install` (gera o `package-lock.json`). **Step 3: Verificar a linha de base:** `npm run typecheck && npm run lint && npm run build` verde com `home.tsx` mínimo (um `<main>` vazio) e os testes do esqueleto que ainda se aplicam (`api-error.test.ts`, `root.test.tsx` ajustado ao novo `root.tsx`).
- [ ] **Step 4: Commit:** `chore(frontend): import the lume frontend skeleton`.

### Tarefa 2: Melhorias da camada `shared/` e o cliente HTTP

**Files:** `src/shared/api/{http-client,api-error,csrf}.ts` e testes; `eslint.config.js`, `tsconfig.json`, `vite.config.ts`, `.prettierrc.json`.

**Interfaces:**
- `export const httpClient` (axios; `baseURL: "/api"`, `withCredentials: true`, `xsrfCookieName: "XSRF-TOKEN"`, `xsrfHeaderName: "X-XSRF-TOKEN"`).
- `export function setTokenProvider(provider: (() => string | null) | null): void`: sem provedor (web), nenhuma requisição leva `Authorization` nem `X-Client`; com provedor que devolve um token, leva `Authorization: Bearer <token>`.
- `export type ApiError = { status: number; code?: string; message: string; invalidParams?: { name: string; reason: string }[] }`; `normalizeRejection(error: unknown): unknown` (cancelamento passa como veio).
- `export function ensureCsrfCookie(): Promise<void>`: se o cookie `XSRF-TOKEN` existe, não faz nada; senão `GET /auth/me` e ignora o erro.

- [x] **Step 1: Testes que falham:**
  - `api-error.test.ts`: `ProblemDetail` com `code` e `invalidParams` vira `ApiError`; sem resposta → `status 0` e `"não foi possível conectar ao servidor"`; **`axios.isCancel` passa inalterado** (Review Focus 2).
  - `csrf.test.ts` (copiar a ideia do `csrf.test.ts` do Lume, com `/auth/me`): com cookie, nenhuma requisição; sem cookie, uma `GET /auth/me`, e um `401` não propaga (Review Focus 3).
  - `http-client.test.ts`: por padrão a requisição não leva `Authorization`; com `setTokenProvider(() => "abc")` leva `Authorization: Bearer abc`; com o provedor devolvendo `null`, não leva (Review Focus 4); toda requisição tem `withCredentials`.
- [x] **Step 2:** rodar `npm test` e ver falhar. **Step 3: Implementar**, copiando de `HEAD` do Lume `api-error.ts` (com o tratamento de cancelamento), `csrf.ts` (trocando `/users/me` por `/auth/me`) e as correções de `eslint.config.js`/`tsconfig.json`/`vite.config.ts`; o proxy fica `"/api"` → `http://localhost:8080` com o prefixo removido. **Step 4:** rodar e ver passar; `npm run format:check`.
- [ ] **Step 5: Commit:** `feat(frontend): add http client with cookie and bearer credentials`.

### Tarefa 3: Tema DUNAS

**Files:** `src/styles/theme.css`, `src/styles/fonts.ts`, `src/styles/theme.test.ts`, `src/app/root.tsx` (importa fontes e tema).

**Interfaces:** variáveis `--ground`, `--panel`, `--nav`, `--line`, `--line-strong`, `--text`, `--text-muted`, `--brand`, `--on-brand`, `--brand-text`, `--highlight`, `--on-highlight`, `--chip-ok`, `--danger`, `--danger-text`, com os valores **escuros em `:root`** e os claros em `@media (prefers-color-scheme: light)`; no bloco `@theme inline`, cada uma vira `--color-<nome>`. Fontes `--font-display` e `--font-sans`; escala de texto como utilidades (`text-display-xl`, `text-display-lg`, `text-title`, `text-number`, `text-body`, `text-body-sm`, `text-label`, `text-caption`); utilidades `cut-sm`, `cut-md`, `cut-lg` (`clip-path` com o corte superior direito de 10, 12 e 16 px); foco global `outline: 2px solid var(--highlight)` com `outline-offset: 2px`. As variáveis do shadcn (`--background`, `--primary`, `--destructive`...) apontam para estes tokens.

- [x] **Step 1: Testes que falham** (`theme.test.ts`, no formato do teste do Lume, adaptado ao `tokens.json` do DUNAS, que tem `value: { dark, light }`):
  - cada token de `color.tokens` tem o mesmo valor, sem diferenciar caixa, em `:root` (escuro) e no bloco `prefers-color-scheme: light` (**os dois temas**, Review Focus 1);
  - cada estilo de `type.groups[*].styles` tem utilidade com o mesmo `fontSize`, `lineHeight` e `fontWeight`;
  - cada `radius.tokens` `cut-*` tem a utilidade com o tamanho certo;
  - **nenhum arquivo `.ts`/`.tsx` fora de testes contém cor literal** (`#[0-9a-f]{3,8}` ou `rgb(`), Review Focus 5;
  - `fonts.ts` importa só pacotes `@fontsource/chakra-petch` e `@fontsource/ibm-plex-sans` (nenhuma URL do Google Fonts).
- [x] **Step 2:** ver falhar. **Step 3: Implementar** `theme.css` a partir do `tokens.json` e `fonts.ts` (pesos 500, 600, 700 do Chakra Petch; 400, 500, 600 do IBM Plex Sans). **Step 4:** passar.
- [ ] **Step 5: Commit:** `feat(frontend): add the dunas theme from the design tokens`.

### Tarefa 4: Componentes base

**Files:** `src/shared/ui/{button,input,label,form,panel,status-chip,alert,logo,sonner}.tsx` e `*.test.tsx`; `public/logo.jpeg` (cópia de `docs/assets/logo.jpeg`); `public/favicon.ico` a partir do logo.

**Interfaces (produz; o IAM consome):**
- `Button({ variant, size, loading, ...buttonProps })`: `variant: "primary" | "outline" | "danger-text" | "link"` (padrão `primary`), `size: "md" | "lg"` (altura 44 e 52 px). `primary` = `bg-brand text-on-brand` com `cut-sm`/`cut-md`; `outline` = recurso de duas camadas na cor `line-strong`; `danger-text` = texto `text-danger-text`; `link` = `text-brand-text` sublinhado. `loading` desabilita, mostra o indicador e põe `aria-busy`.
- `Input` (altura 48 e 52, `bg-panel` ou `bg-ground`, borda `line-strong`, texto 16 px), `Label` (`text-label`), `Field`/`form.tsx` ligando `@base-ui/react/field` a `react-hook-form` (copiado do Lume e adaptado).
- `Panel({ as?, className, children })`: `bg-panel` com `cut-lg`.
- `StatusChip({ tone: "ok" | "pending" | "off", children })`: `ok` = fundo `chip-ok` com círculo cheio; `pending` = contorno `line-strong` com círculo vazado; `off` = contorno com círculo cortado e texto `text-muted`. O glifo carrega `data-shape="filled" | "ring" | "slashed"`.
- `Alert({ tone: "danger" | "warning", title?, children })`: `danger` = contorno `danger` com ícone e texto `text-danger-text`, `role="alert"`; `warning` = fundo `highlight`, texto `on-highlight`, `cut-lg`, `role="alert"`.
- `Logo({ size })`: `<img>` do ícone com `alt="DUNAS"` e raio de 8 px.

- [x] **Step 1: Testes que falham** (Testing Library, um por linha):
  - `Button`: cada variante tem as classes do token certo; `primary` nunca tem `text-white`; `loading` desabilita, tem `aria-busy="true"` e não dispara `onClick`; alvo de toque `md` ≥ 44 px (classe de altura).
  - `StatusChip`: os três tons têm `data-shape` **diferentes** (situação legível sem cor) e exibem o texto.
  - `Alert`: `danger` e `warning` têm `role="alert"`; `warning` usa `bg-highlight` e `text-on-highlight`.
  - `Input`/`Field`: label associado ao campo; mensagem de erro do Zod aparece e o campo fica `aria-invalid`; `maxLength` é repassado.
  - `Logo`: `alt="DUNAS"`; `Panel`: classe `cut-lg`.
- [x] **Step 2:** ver falhar. **Step 3: Implementar** (shadcn/ui na variante Base UI, `class-variance-authority`, `cn`); `form.tsx` e `sonner.tsx` vêm do Lume `HEAD`, trocando os tokens. **Step 4:** passar.
- [ ] **Step 5: Commit:** `feat(frontend): add the base components`.

### Tarefa 5: Casca do aplicativo

**Files:** `src/app/{root.tsx,providers.tsx,routes.ts}`, `src/app/routes/home.tsx`, `src/shared/lib/{cn,query-client}.ts`, `src/test/setup.ts`; testes `root.test.tsx`.

**Interfaces:** `Providers({ children })` com `QueryClientProvider` e o `Toaster`; `queryClient` sem nova tentativa para `ApiError` com `status` 400 a 499; `home.tsx` é uma página mínima (marca e o texto "Em construção") **provisória**, a ser substituída pela primeira tela aprovada. `root.tsx` define `lang="pt-BR"`, `<meta name="viewport">` e o tema.

- [x] **Step 1: Testes que falham:** `root.test.tsx` renderiza o layout com `lang="pt-BR"`; `query-client.test.ts`: erro 404 não é repetido, erro de rede (`status 0`) é repetido uma vez.
- [x] **Step 2 a 4:** falhar, implementar, passar; `npm run build` gera `build/client`.
- [ ] **Step 5: Commit:** `feat(frontend): add the app shell`.

### Tarefa 6: Documentação e fechamento

**Files:** `frontend/CLAUDE.md`, `CLAUDE.md` (raiz), `docs/CLAUDE.md`, `docs/rules/architecture.md` §13, `docs/plans/` (este arquivo).

- [x] **Step 1: `frontend/CLAUDE.md`** vira roteador curto: estrutura de `src/`, comandos de verificação e links para `docs/rules/frontend.md`, `docs/design-system/` e a regra 7. **Nada do que está em `frontend.md` é repetido.**
- [x] **Step 2:** `CLAUDE.md` raiz: acrescentar `docs/rules/frontend.md` à lista de regras; `docs/CLAUDE.md`: listar `frontend.md` em `rules/`.
- [x] **Step 3:** conferir os links relativos de `docs/` e `frontend/`; rodar a verificação completa (`npm run typecheck && npm run lint && npm test && npm run build && npm run format:check`).
- [ ] **Step 4: Commit:** `docs(frontend): document the frontend rules and the app shell`.

---

## Fora deste plano

- **As telas do IAM** (12 mockups aprovados): plano `docs/plans/<data>-iam-frontend.md`, depois deste esqueleto. A estrutura de `features/iam/` e a skill `crm-frontend-add-feature` nascem dele.
- **Capacitor** (app): plano próprio, depois da primeira feature no frontend ([`frontend.md`](../rules/frontend.md) §8).
- **Pipeline de CI** e hospedagem do build.

## Desvios da execução

- **Esqueleto por comando, não por cópia.** A base saiu de `npx create-react-router@latest` (template padrão, `--no-install --no-git-init`), reorganizada para `src/app` com `ssr: false`; as dependências entraram por `npm i`, e os componentes-base pelo `npx shadcn@latest add` (reescritos depois com os tokens DUNAS). Do Lume vieram só as configs (`eslint`, `prettier`, `vitest`, `components.json`, `.nvmrc`), o `form.tsx` e o `package-lock.json`.
- **Versões do Lume, não as mais novas.** Com as mais recentes (`vitest` 5.1.1, `@testing-library/jest-dom` 8, `jsdom` 31, `vite` 8.3.8) a suíte quebrou. O `package.json` usa as dependências do Lume e o `package-lock.json` é o dele (`vite` 8.3.1, `vitest` 5.0.2, `jsdom` 27.4).
- **Rode o npm pelo PowerShell.** Pelo Git Bash o Vitest falhou de forma intermitente (`Cannot read properties of undefined (reading 'config')`); pelo PowerShell passa. A causa não foi esclarecida.
- **Nada foi commitado**: o usuário não pediu. Os passos de commit seguem abertos.
