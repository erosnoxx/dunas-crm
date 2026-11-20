---
name: crm-frontend-add-feature
description: Use ao construir as telas de uma feature no frontend do crm — pasta `features/<feature>/` (service, hooks, schemas, mensagens por `code`, components, screens), rota fina em `app/routes/`, guardas, casca e título, formulário com React Hook Form + Zod, erro pelo `code`, teste por tela e a comparação final com o mockup. Invoque antes de criar qualquer arquivo em `frontend/src/features/` ou `frontend/src/app/routes/`, e antes de começar qualquer tela.
---

# Feature no frontend

Regras e motivo: [`frontend.md`](../../../docs/rules/frontend.md) §3 (estrutura), §4 (tema), §5 (sessão e erro pelo `code`), §6 (o que nunca se quebra) e §7 (verificação); regra 7 de [`docs/CLAUDE.md`](../../../docs/CLAUDE.md) (mockup aprovado e comparação final). Referência viva, completa: `frontend/src/features/iam/` e `frontend/src/app/`.

## Antes de começar

1. A feature existe em `docs/system-design/<NN-feature>/`: `usecases.md` dá os campos, os limites e a **tabela de erros por `code`**; `design/` dá os mockups aprovados. Sem isso, sinalize a lacuna e pare.
2. Cada tela, em cada tamanho (390 e 1280), aponta o artboard que segue. Tamanho sem artboard é **derivado** e vai à comparação final; tela sem artboard nenhum não começa.
3. Leia o artboard campo a campo e traduza cada valor para um token do tema (`bg-panel`, `text-label`, `cut-lg`, ...). Valor sem token é divergência: sinalize ao usuário **antes** de escrever a tela.

## Arquivos da feature

Em `frontend/src/features/<feature>/`, como em `features/iam/`:

| Arquivo | O que tem | No IAM |
|---|---|---|
| `<feature>.service.ts` | uma função por chamada à API, pelo `httpClient` de `shared/api`, com os tipos de pedido e resposta | `iam.service.ts` |
| `<feature>.hooks.ts` | TanStack Query: chaves num objeto `keys`, uma consulta por leitura, uma mutation por escrita, que invalida as chaves que mudou | `iam.hooks.ts` |
| `<feature>.schemas.ts` | Zod dos formulários, com os limites do domínio; senha **nunca** com `.trim()` | `iam.schemas.ts` |
| `<feature>.messages.ts` | `MESSAGES` (a tabela de erros de `usecases.md`, por `code`), `messageFor(code, fallback)`, `errorMessage(error)` e os rótulos da feature (`LABELS`) | `iam.messages.ts` |
| `components/` | peças da feature usadas por mais de uma tela | `auth-shell`, `password-field`, `new-password-fields`, `mobile-header`, `session-item` |
| `screens/` | uma tela por arquivo, com o teste ao lado | `sign-in.tsx` e `sign-in.test.tsx` |

Peça que outra feature também precise sobe para `shared/ui/` (uma feature não importa outra).

## Rota

1. Arquivo fino em `app/routes/<rota>.tsx`: só compõe guarda e tela, com um comentário de uma linha dizendo o que a rota é.

   ```tsx
   /** `/usuarios`: a lista, só do Administrador (ADR-5). */
   export default function Usuarios() {
     return (
       <RequireRole role="ADMINISTRATOR">
         <UsersScreen />
       </RequireRole>
     )
   }
   ```

2. Registre em `app/routes.ts`. Rota autenticada entra no `layout("routes/authenticated.tsx", [...])`, que já aplica `RequireAuth` → `RequireSettledPassword` → `AppShell`. Pública fica fora; a que quem tem sessão não deve ver usa `RedirectIfAuthenticated`. Papel é `RequireRole` na rota.
3. Rota estática irmã de `:id` (ex. `usuarios/convidar`) vem declarada junto, para não virar um id.

## Casca e título

Um `h1` por tamanho de tela:

- **Rota com a barra superior da casca no celular:** entrada em `TITLES` (`app/layout/app-shell.tsx`), e a página desenha o próprio `h1` com `max-md:sr-only`.
- **Rota com cabeçalho próprio no celular** (com "Voltar"): entra em `OWN_HEADER` (ou no prefixo, como `/usuarios/`) e a página desenha o cabeçalho com o `h1` (`MobileHeader` no IAM).
- Item de trabalho da barra lateral: `NAV_ITEMS`, com os papéis que o veem.

## Formulário

- `useForm` com `zodResolver(<schema>)` e os componentes de `shared/ui/form` (`Form`, `FormField`, `FormItem`, `FormLabel`, `FormMessage`), `Input` e `Button`. O rótulo vem do `FormLabel`: o teste acha o campo por `getByLabelText`.
- `maxLength` no campo com o limite do domínio. Senha: `PasswordField` (mostrar/ocultar, contador com `showCount`), e para nova senha `NewPasswordFields`/`PasswordChecklist`; nunca aparada nem registrada.
- Envio: `Button` com `loading` (dá o `aria-busy` e segura o segundo clique). Erro em `Alert` (`role="alert"`). Sucesso com texto aprovado em `toast` do `sonner`.
- Toda consulta assíncrona mostra carregando, erro e vazio; estado sem mockup é divergência a sinalizar.

## Erro pelo `code`

- A tela decide pelo `code` do `ApiError`, nunca pelo texto: `messageFor(error.code, error.message)` ou `errorMessage(error)`; um `code` que muda a tela (ex. `link_expired` → `LinkProblem`) é um `if` no `code`.
- `session_invalid` **não** se trata na tela: o cliente de consulta (`createQueryClient` em `shared/lib/query-client.ts`, ligado em `app/providers.tsx`) limpa o cache e vai para `/entrar` uma vez. Consulta que decide sozinha o que fazer sem sessão leva `meta: { handlesOwnSession: true }`.

## Teste por tela

`screens/<tela>.test.tsx`, como `sign-in.test.tsx`:

- `vi.mock("../<feature>.service")` e respostas por `vi.mocked(service.x).mockResolvedValue(...)`; a tela roda num `createRoutesStub` com `QueryClientProvider` de `createQueryClient(() => undefined)` e as rotas de destino como `<p>`.
- Hora controlada: `vi.useFakeTimers({ toFake: ["Date"] })` e `vi.setSystemTime(...)`.
- Busca por papel e rótulo (`getByRole`, `getByLabelText`); cobre `role="alert"` nos erros por `code`, `aria-busy` no envio e a ordem do `Tab`.
- O jsdom não tem CSS: a geometria do mockup vira **contrato de classe** (`toHaveClass("text-display-lg", "max-md:sr-only")`), e a conferência visual é a do passo seguinte.
- Os testes de `shared/` já varrem o código atrás de cor literal, `localStorage`, `sessionStorage` e `Authorization`.

## Fechamento

1. Comparação mockup × resultado em `docs/system-design/<NN-feature>/design/comparacao-mockup.md`, no formato do [`01-iam`](../../../docs/system-design/01-iam/design/comparacao-mockup.md): cada tela em 390 e 1280 num navegador real (estilos computados contra os valores do artboard), dizendo o que foi visual e o que foi estrutural, e levando ao usuário cada divergência, estado sem mockup e tela derivada.
2. Verificação, a partir de `frontend/`, **pelo PowerShell**:

   ```
   npm run typecheck && npm run lint && npm test && npm run build && npm run format:check
   ```
