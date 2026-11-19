# Frontend: crm

Fonte de verdade da arquitetura do frontend (web responsiva). O `frontend/CLAUDE.md` e as skills **referenciam** este documento e não o repetem. Regra de backend: [`architecture.md`](architecture.md). Estilo de código Java: [`code-style.md`](code-style.md) (não vale aqui).

Se uma regra deste documento conflitar com o que uma tarefa pede, pare e pergunte.

## 1. Origem

O esqueleto parte do frontend do projeto Lume (`C:\Develop\Projects\lume-ticketing\frontend`), no commit `a158f13` ("scaffold the frontend skeleton"), **mais** as melhorias da camada `shared/` que o Lume fez depois (tratamento de cancelamento no `ApiError`, botão com estado de carregamento, formatação checada no `format:check`, o teste que confere o tema contra o `tokens.json`). Sai tudo o que é do domínio do Lume: tema, logo, telas, features, textos. A identidade vem de [`design-system/`](../design-system/README.md).

Escolhas do Lume que **não** vêm: sessão por cookie para todos os clientes (aqui há dois formatos, [`architecture.md`](architecture.md) §14) e o prefixo `mk-`/nomes em português dos tokens (aqui valem os nomes do `tokens.json`, em inglês).

## 2. Stack

| Camada | Escolha |
|---|---|
| Base | React 19 + TypeScript (`strict`) + Vite, Node 22, npm |
| Rotas | React Router em **framework mode**, `ssr: false` (SPA). Não há página pública que precise de SEO; o caminho para pré-render por rota fica aberto |
| Dados | TanStack Query + axios (axios só em `shared/api/`) |
| Formulários | React Hook Form + Zod |
| UI | Tailwind CSS v4 + shadcn/ui na variante **Base UI** (`@base-ui/react`); `lucide-react`; `sonner` |
| Fontes | **Chakra Petch** (títulos e números) e **IBM Plex Sans** (texto), auto-hospedadas por `@fontsource` (nenhuma chamada ao Google Fonts) |
| Qualidade | ESLint (flat config) + Prettier; Vitest + Testing Library + jsdom |
| App | Capacitor embrulhando esta mesma web, num plano próprio depois do esqueleto (§8) |

Versões: as do `package-lock.json` do Lume (`vite` 8.3.1, `vitest` 5.0.2, `jsdom` 27.4): versões mais novas do `vitest` e do `jest-dom` quebraram a suíte. Subir versão é decisão à parte, rodando a suíte inteira.

## 3. Estrutura de `frontend/src/`

```
app/       React Router (appDirectory = src/app): root.tsx, providers.tsx, routes.ts, routes/, layout/
features/  uma pasta por feature; nasce com a spec em docs/system-design/
shared/
  ui/      componentes shadcn/ui e composições cross-feature sobre eles
  api/     cliente HTTP único, ApiError, CSRF
  lib/     utilitário genérico sem domínio (cn, query-client)
styles/    theme.css (Tailwind v4 @theme), gerado a partir de tokens.json
test/      setup global de teste
```

- **`app/`** compõe: layout HTML, providers globais, rotas e o que junta mais de uma feature. Nenhuma lógica de domínio.
- **`features/<dominio>/`** só existe se a feature existe em `docs/system-design/`: nunca especulativa. **Uma feature não importa outra**: o que as duas precisam sobe para `shared/`. Arquivos por feature: `<dominio>.service.ts` (chamadas à API), `<dominio>.hooks.ts` (TanStack Query), `<dominio>.schemas.ts` (Zod), componentes e telas.
- **`shared/ui/`** não tem domínio.
- **`shared/api/`**: `http-client.ts` é a única instância de axios; nenhum outro lugar importa `axios`.
- A estrutura de `features/iam/` é fixada pelo plano da feature, e a skill `crm-frontend-add-feature` nasce dela, como as skills de backend nasceram do primeiro BC.

## 4. Tema e identidade

Fonte de verdade: [`docs/design-system/tokens.json`](../design-system/tokens.json) e o [`README`](../design-system/README.md).

- **Cor.** Cada token de cor vira uma variável CSS em `theme.css` e uma cor do Tailwind **com o nome do token** (`bg-ground`, `bg-panel`, `text-text`, `text-text-muted`, `bg-brand`, `text-on-brand`, `text-brand-text`, `bg-highlight`, `text-on-highlight`, `bg-chip-ok`, `border-line`, `border-line-strong`, `text-danger-text`, `border-danger`). O tema **escuro é o padrão**; o claro (campo) entra por `prefers-color-scheme: light`. **Um teste confere cada valor contra o `tokens.json`**, nos dois temas, e falha se divergirem.
- **O laranja é ação.** `bg-brand` sempre com `text-on-brand`; branco sobre laranja não passa no contraste. Laranja como texto ou traço é `text-brand-text`.
- **Tipografia.** `font-display` (Chakra Petch) e `font-sans` (IBM Plex Sans), com a escala do `tokens.json` (`display-xl`, `display-lg`, `title`, `number`, `body`, `body-sm`, `label`, `caption`) como utilidades. Número com `tabular-nums`. Nada abaixo de 12 px. Tela nova não inventa tamanho fora da escala.
- **Forma.** O canto chanfrado é o desenho da marca: utilidades `cut-sm`, `cut-md`, `cut-lg` (tamanhos de `radius` no `tokens.json`), por `clip-path`. Contorno sobre canto cortado usa o recurso de duas camadas (camada externa na cor do contorno, interna recortada 1 px menor), porque `clip-path` corta a borda. Raio arredondado só no ícone do logo.
- **Sem decoração:** sem gradiente, sem sombra decorativa.
- **Foco visível** global em `theme.css`; componente novo não usa `outline-none`.
- **Nenhuma cor, tamanho, fonte ou raio avulso em tela**: um teste varre o código-fonte atrás de cor literal (`#...`, `rgb(`) fora de `theme.css`.
- **Tema claro:** os tokens existem e o teste os confere, mas os mockups aprovados são do tema escuro: a regra de fidelidade ao mockup vale para ele. O claro é proposta a confirmar com quem trabalha no campo.
- **Logo:** só o ícone (`docs/assets/logo.jpeg`) no tema escuro, como diz o `README` do design system.

## 5. Sessão e API

- O frontend fala só com `/api` (em dev o proxy do Vite reescreve para `http://localhost:8080`, mesmo site da página).
- **Navegador:** `withCredentials`, cookie `crm_session` **que o JavaScript nunca vê**, e o cabeçalho `X-XSRF-TOKEN` lido do cookie `XSRF-TOKEN` em toda escrita. Numa aba nova, antes da primeira escrita, `ensureCsrfCookie()` chama `GET /api/auth/me` e ignora o 401. Nada de `localStorage`/`sessionStorage` para sessão.
- **Aplicativo:** `Authorization: Bearer <token>`, com o token no armazenamento seguro do aparelho e `X-Client: app` no login. O esqueleto deixa um ponto de extensão no cliente HTTP (um provedor de token, vazio na web); a implementação vem com o Capacitor.
- **Erro decide a tela pelo `code`, nunca pelo texto** ([`architecture.md`](architecture.md) §7.3). `ApiError` é a única forma de erro que o resto do código vê (negócio, validação 400 com `invalidParams`, rede). A mensagem da API é mostrada como veio (já é pt-BR minúscula).
- Cancelamento de requisição (`AbortController` do TanStack Query) passa como veio: não é erro para o usuário.

## 6. Regras que nunca são quebradas

- **Nenhuma tela, nova ou redesenhada, sem mockup aprovado** em `docs/system-design/<feature>/design/`, e a tela é fiel a ele (regra 7 de [`docs/CLAUDE.md`](../CLAUDE.md)), com a comparação mockup × resultado ao fim da feature.
- **Mobile-first é responsividade real**: o componente parte do layout de celular e se adapta de propósito em cada ponto de quebra. Alvo de toque mínimo de 44 px.
- **Identificadores em inglês; texto de interface e comentário em pt-BR.** Voz da interface: [`design-system/README`](../design-system/README.md) ("Voz e conteúdo").
- **Campo de formulário respeita o limite do domínio** (ex. senha de 8 a 30, nome de 2 a 80: `usecases.md`/`domain-model.md` da feature) e mostra o estado de toda consulta assíncrona.
- **Ícone** é SVG de traço (`lucide-react`), nunca emoji.
- **Acessibilidade:** controle é `button`/`a`/`input` de verdade, rótulo associado, contraste ≥ 4,5:1 (os pares do design system já cumprem), foco visível.
- **Backend e frontend de uma feature são planejados juntos**: a spec cobre os dois, e o plano tem uma parte por lado (`docs/plans/<data>-<feature>-backend.md` e `-frontend.md`).

## 7. Verificação

Critério de aceite de toda mudança no frontend, a partir de `frontend/`:

```
npm run typecheck && npm run lint && npm test && npm run build && npm run format:check
```

Rode pelo **PowerShell**: pelo Git Bash o Vitest falhou de forma intermitente nesta máquina.

Hora, aleatoriedade e rede são sempre controladas no teste (nada de teste que depende do relógio ou do servidor real).

## 8. Aplicativo (Capacitor)

O app embrulha o build desta web. Entra num plano próprio, depois de existir pelo menos uma feature no frontend, e define: projeto Capacitor, armazenamento seguro do token, `X-Client: app`, `crm.cors.allowed-origins` com as origens do webview, ícones e splash. Até lá, nada na web depende dele além do ponto de extensão do cliente HTTP (§5).
