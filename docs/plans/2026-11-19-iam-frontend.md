# IAM (frontend) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar no frontend as telas do IAM: entrar, convite, redefinição e troca de senha, sessões, e a gestão de usuários do Administrador, fiéis aos mockups aprovados.

**Architecture:** Uma feature `features/iam/` (serviço, hooks, schemas, mensagens por código de erro, componentes e telas) com rotas finas em `app/routes/`. A sessão do navegador é cookie `HttpOnly` que o código nunca vê; a guarda de rota lê `GET /auth/me`. Testes por tela contra o serviço mockado.

**Tech Stack:** o esqueleto de [`2026-11-19-frontend-skeleton.md`](2026-11-19-frontend-skeleton.md): React 19, React Router (framework mode, SPA), TanStack Query + axios, React Hook Form + Zod, Tailwind v4 + shadcn/ui sobre Base UI, Vitest + Testing Library.

**Spec:** [`docs/system-design/01-iam/`](../system-design/01-iam/README.md): `usecases.md` (rotas, erros, mensagens), `domain-model.md` (limites dos campos), `decisions.md` (ADR-1 a ADR-11), `design/` (os 12 mockups aprovados). Regras do frontend: [`docs/rules/frontend.md`](../rules/frontend.md). Backend: [`2026-11-19-iam-backend.md`](2026-11-19-iam-backend.md). **O plano não redefine a spec.**

## Global Constraints

- Tudo roda a partir de `frontend/`, **pelo PowerShell**. Aceite de toda tarefa: `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check` verde.
- **Nenhuma tela sem mockup aprovado** ([`docs/CLAUDE.md`](../CLAUDE.md) regras 6 e 7). Cada tarefa de tela diz quais artboards do canvas `https://claude.ai/artifact/CvVeA26TCvv9dBdXWwZ4eU` ela segue. Onde o mockup é só de um tamanho de tela, a tarefa nomeia o outro como **derivado** e ele entra na comparação final para aprovação. Tela sem mockup nenhum só começa depois da Tarefa 1.
- Fidelidade: cores, espaçamento, tipografia, estrutura e comportamento como no mockup, **só pelos tokens** do tema (`bg-panel`, `text-text-muted`, `cut-lg`, ...), nunca valor avulso.
- Erro decide a tela pelo **`code`**, nunca pelo texto ([`architecture.md`](../rules/architecture.md) §7.3). As mensagens exibidas são as da tabela de erros de `usecases.md`.
- Campo respeita o limite do domínio: senha de **8 a 30** caracteres com maiúscula, minúscula, número e especial (ADR-3); nome de **2 a 80**; e-mail até **254**. Senha **nunca** é aparada (`trim`) nem logada.
- Sem `localStorage`/`sessionStorage`, sem `Authorization`, sem ler o cookie de sessão (o de CSRF é a única exceção, lido por `ensureCsrfCookie` e pelo axios).
- Sem confirmação ao encerrar sessão e desativar usuário (ADR-11).
- Identificadores em inglês; texto de interface em pt-BR, frase com inicial minúscula de frase; verbos diretos nos botões. Ícone é SVG de traço (`lucide-react`), nunca emoji.
- Commit: Conventional Commits, escopo `frontend`, em inglês, **sem `Co-Authored-By`**. Branch `feature/iam-frontend`; a base é `develop`, que ainda não existe: **pergunte a base antes da Tarefa 2**. Não misture o trabalho não commitado do `main`.

## Review Focus

1. **Token ou senha vazando:** nenhum código do app usa `localStorage`, `sessionStorage` nem `Authorization`, e nenhum log recebe senha. Um teste varre o código-fonte. → Tarefa 2.
2. **Login revelando quem tem conta:** e-mail inexistente, senha errada e usuário desativado mostram a **mesma** mensagem; bloqueio desliga os campos e não tenta de novo sozinho. → Tarefa 5.
3. **Senha alterada pelo formulário:** espaço nas pontas não é aparado, o limite de 30 vale ao colar texto maior, e o checklist concorda com o backend nos mesmos casos (`ç` é minúscula, `€` é especial, espaço não é especial). → Tarefa 3.
4. **Sessão que cai no meio do uso (`session_invalid`) e conta com troca obrigatória:** volta para o login sem laço de redirecionamento; quem precisa trocar a senha não consegue abrir outra rota por URL. → Tarefa 4.
5. **Link de convite ou de redefinição sem `token`, ou com `token` lixo:** cai no estado de link inválido, sem tela quebrada. → Tarefas 6 e 7.
6. **Ação do Administrador sobre si mesmo:** trocar o próprio papel e desativar a própria conta nunca aparecem como ações; se o backend ainda recusar (`cannot_*`), a mensagem sai. → Tarefa 12.

---

## Lacunas de mockup

A spec exige telas que os 12 mockups aprovados **não** desenham. Elas esperam a Tarefa 1; o resto do plano não depende delas.

| Tela | Por que falta | Tarefa que depende |
|---|---|---|
| Esqueci minha senha (pedir) e o aviso "se o e-mail existir, enviamos" | só o link "Esqueci minha senha" está desenhado | 7 |
| Redefinir senha (pelo link) | só se disse que reaproveita a tela de definir senha | 7 |
| Trocar senha (voluntária e a obrigatória do primeiro Administrador) | precisa do campo "senha atual", que a tela de convite não tem | 8 |
| Menu da conta no celular (o avatar do topo leva a quê) e o menu na web para Comercial e Técnico (a navegação só mostra "Usuários") | desenhado só para o Administrador na web | 9 |
| Minhas sessões na web | só existe a tela do celular | 10 |
| Convidar usuário no celular | só o diálogo da web | 12 |
| Detalhe do usuário em **convite pendente** (ação "Reenviar convite") e do **próprio Administrador** (sem trocar papel nem desativar) | só estão desenhados o ativo e o desativado | 12 |
| Lista de usuários com muita gente (paginação) | a lista mostra 5 linhas, sem navegação de página | 11 |

---

### Tarefa 1: Mockups complementares (portão de aprovação)

Sem código. Gera os artboards que faltam no **mesmo canvas** (arquivos `.dc.html` novos, na mesma identidade, nas mesmas dimensões: celular 390×844, web 1280 fluida), os registra em [`design/README.md`](../system-design/01-iam/design/README.md) com o que cada um valida, e espera a aprovação do usuário.

**Files:** novos artboards no canvas; `docs/system-design/01-iam/design/README.md` (linhas 13 em diante); `docs/system-design/01-iam/open-questions.md` (remover "Telas sem mockup" ao fechar).

- [x] **Step 1:** Ler `design/README.md` e a lista "Lacunas de mockup" acima. (Os artboards e as cópias estão em [`design/telas-complementares.md`](../system-design/01-iam/design/telas-complementares.md).) **Step 2:** Gerar, nesta ordem: `Esqueci-senha` (web e celular, estados inicial e "enviamos o link"); `Redefinir-senha` e `Trocar-senha` (celular e web; a de troca com "senha atual", e a variante obrigatória com o título "Troque a senha inicial" e sem botão de voltar); `Conta-celular` (menu do avatar: Minhas sessões, Trocar senha, Sair) e `Navegacao-web` (a barra lateral para Comercial e Técnico); `Sessoes-web`; `Convidar-celular`; `Detalhe-convite-pendente` e `Detalhe-proprio` (web e celular); decisão de paginação (uma linha de "mostrando N de M" ou "carregar mais", a escolher pelo usuário). **Step 3:** Cada texto novo sai com a voz do design system e fica numa tabela de cópias no `design/README.md` para aprovação (inclusive "Convite enviado" e "Enviamos o link, se o e-mail existir").
- [x] **Step 4: Portão:** o usuário aprova os artboards e as cópias (aprovados em 2026-11-19). **As tarefas 7 a 12 não começam antes disso**; as tarefas 2 a 6 podem seguir em paralelo.

### Tarefa 2: Fundação da feature

**Files:** `src/features/iam/{iam.service.ts,iam.schemas.ts,iam.messages.ts,password-rules.ts,iam.hooks.ts}`, testes ao lado; `src/shared/lib/no-secrets.test.ts`.

**Interfaces (produz):**
- `iam.service.ts` (único lugar que chama a API do IAM; usa `httpClient`): `login(input: { email: string; password: string }): Promise<SessionView>`; `logout(): Promise<void>`; `acceptInvitation(input: { token: string; password: string }): Promise<SessionView>`; `requestPasswordReset(input: { email: string }): Promise<void>`; `resetPassword(input: { token: string; password: string }): Promise<void>`; `changePassword(input: { currentPassword: string; newPassword: string }): Promise<void>`; `getCurrentUser(): Promise<CurrentUser>`; `listUsers(input: { page: number; size: number }): Promise<Page<UserSummary>>`; `getUser(id: string): Promise<UserDetail>`; `inviteUser(input: { name: string; email: string; role: Role }): Promise<{ id: string }>`; `resendInvitation(id)`, `changeUserRole(id, role)`, `deactivateUser(id)`, `reactivateUser(id)`, `listSessions(userId): Promise<SessionSummary[]>`, `revokeSession(id)`, `revokeOtherSessions(userId)`, `revokeAllSessions(userId)`. Os tipos `SessionView { expiresAt: string; mustChangePassword: boolean }`, `CurrentUser`, `UserSummary`, `UserDetail`, `SessionSummary`, `Role = "ADMINISTRATOR" | "COMMERCIAL" | "TECHNICIAN"`, `UserStatus = "INVITED" | "ACTIVE" | "DEACTIVATED"`, `Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }` espelham os schemas de `iam-application` ([plano de backend](2026-11-19-iam-backend.md), Tarefa 5); datas chegam como texto ISO e viram `Date` só na tela.
- `iam.messages.ts`: `messageFor(code: string | undefined, fallback: string): string` com a tabela de erros de `usecases.md` (pt-BR), e `LABELS` de papel e de situação ("Administrador", "Comercial", "Técnico"; "Ativo", "Convite pendente", "Desativado").
- `password-rules.ts`: `type PasswordRule = { id: "length" | "upper" | "lower" | "digit" | "special"; label: string; met: boolean }`; `checkPassword(password: string): PasswordRule[]` (as 5 regras na ordem do mockup: "De 8 a 30 caracteres", "Uma letra maiúscula", "Uma letra minúscula", "Um número", "Um caractere especial, como ! ou #"); `isPasswordValid(password: string): boolean`; `PASSWORD_MAX = 30`.
- `iam.schemas.ts` (Zod): um schema por formulário (`signInSchema`, `acceptInvitationSchema`, `forgotPasswordSchema`, `resetPasswordSchema`, `changePasswordSchema`, `inviteUserSchema`), com `.max()` dos limites acima; **nenhum `.trim()` em campo de senha**; `repeat` igual a `password` por `.refine`.

- [ ] **Step 1: Testes que falham:**
  - `no-secrets.test.ts` (varre o código-fonte, como o teste de cor literal): nenhum arquivo de produção contém `localStorage`, `sessionStorage` nem `"Authorization"` fora de `shared/api/http-client.ts` (Review Focus 1).
  - `password-rules.test.ts`, **os mesmos casos do `PasswordTest` do backend**: `"Dunas#2026"` cumpre as 5; 7 e 31 caracteres falham `length`; sem maiúscula/minúscula/número/especial falham a regra certa; `"Açúcar1€"` cumpre as 5 (`ç` minúscula, `€` especial); `"Aa1     "` não cumpre `special`; `" Dunas#2026 "` não é aparada: `checkPassword` mede o texto como veio (Review Focus 3).
  - `iam.messages.test.ts`: cada `code` da tabela de `usecases.md` tem a mensagem exata; código desconhecido devolve o `fallback`.
  - `iam.service.test.ts` (com `httpClient` mockado): cada função chama o verbo e a rota de `usecases.md` com o corpo certo; `login`, `acceptInvitation`, `requestPasswordReset` e `resetPassword` chamam `ensureCsrfCookie()` antes; nenhuma lê `token` da resposta.
  - `iam.schemas.test.ts`: senha de 31 caracteres é recusada; espaços nas pontas **permanecem** no valor validado; `repeat` diferente é recusado; nome de 1 e de 81 caracteres são recusados; e-mail sem `@` é recusado.
- [ ] **Step 2:** rodar e ver falhar. **Step 3: Implementar.** `iam.hooks.ts` expõe `useCurrentUser()` (query `["me"]`, `staleTime` longo, sem nova tentativa em 401) e uma mutation por ação (invalidando `["me"]`, `["users"]`, `["sessions", userId]` quando cabe). **Step 4:** rodar e ver passar.
- [ ] **Step 5: Commit:** `feat(frontend): add the iam service, schemas and password rules`.

### Tarefa 3: Componentes de formulário do IAM

**Files:** `src/features/iam/components/{password-field,password-checklist,auth-shell,role-picker}.tsx`, testes ao lado.

**Interfaces (produz):**
- `PasswordField({ id, label, error, ...inputProps })`: campo `type=password` com `maxLength={30}`, botão "Mostrar/Ocultar" (alvo ≥ 44 px, `aria-pressed`), nunca aparado.
- `PasswordChecklist({ password }: { password: string })`: lista `aria-label="Regras da senha"` com as 5 regras de `checkPassword`; cada item atendido tem círculo cheio com visto e texto `text-text`, o pendente tem círculo vazado e texto `text-text-muted`, e cada um carrega texto só para leitor de tela (", atendida" / ", ainda não atendida"); contador "N de 30" acima.
- `AuthShell({ title, description, children })`: cabeçalho com `Logo` e "DUNAS" + conteúdo. No celular, tela cheia com `p-6`; a partir de `md`, `Panel` centralizado de 440 px com `cut-lg` e `p-10` (como o mockup `Entrar · web`).
- `RolePicker({ value, onChange })`: três opções de rádio (Administrador, Comercial, Técnico), a escolhida com fundo `chip-ok` e borda `brand`.

- [ ] **Step 1: Testes que falham:** `PasswordField` mostra e oculta o texto, repassa `maxLength=30` e **colar 31 caracteres deixa 30**; `PasswordChecklist` com `"Dunasolar"` marca as 3 primeiras e deixa 2 pendentes, com os dois estados diferentes **pela forma** (`data-shape`) e pelo texto para leitor de tela (Review Focus 3); `AuthShell` usa o painel `cut-lg` e o logo; `RolePicker` é um grupo de rádio navegável por teclado com uma opção marcada.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add iam form components`.

### Tarefa 4: Sessão, guardas e redirecionamentos

**Files:** `src/features/iam/guards.tsx`, `src/app/routes.ts`, `src/shared/lib/query-client.ts` (tratamento global de 401), testes.

**Interfaces:** `RequireAuth({ children })` (sem `me` válido → `<Navigate to="/entrar" replace />`); `RequireRole({ role, children })` (papel diferente → `/`); `RequireSettledPassword({ children })`: se `me.mustChangePassword` e a rota **não** é `/conta/trocar-senha`, redireciona para ela. O `QueryCache` global, ao receber `ApiError` com `status 401` e `code === "session_invalid"` de uma query ou mutation autenticada, limpa o cache e navega para `/entrar` **uma única vez**.

- [ ] **Step 1: Testes que falham:** sem sessão (`getCurrentUser` rejeita 401) a rota protegida vai para `/entrar`; com sessão passa; `RequireRole` manda Comercial de `/usuarios` para `/`; **conta com `mustChangePassword` abrindo `/usuarios` por URL vai para `/conta/trocar-senha`, e continua lá** (Review Focus 4); `session_invalid` no meio do uso leva ao login e **não** dispara outro redirecionamento ao limpar o cache (sem laço); `GET /auth/me` na tela de login não redireciona (a rota pública só consulta a sessão para mandar quem já entrou para `/`).
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add session guards`.

### Tarefa 5: Tela Entrar

**Mockups:** `Entrar · web`, `Entrar · celular, senha errada`, `Entrar · celular, bloqueado`. **Derivados:** celular sem aviso (o das duas variações sem o `Alert`) e web com aviso (o `Alert` no mesmo lugar, entre o texto de abertura e os campos).

**Files:** `src/features/iam/screens/sign-in.tsx`, `src/app/routes/entrar.tsx` (rota `/entrar`), testes.

**Interfaces:** consome `AuthShell`, `PasswordField`, `Alert`, `Button`, `Input`, `iam.service.login`, `messageFor`. Após sucesso: `mustChangePassword` → `/conta/trocar-senha`; senão `/`.

- [ ] **Step 1: Testes que falham:** envia e-mail e senha e navega conforme `mustChangePassword`; `invalid_credentials` mostra o `Alert` `danger` "e-mail ou senha incorretos. Confira os dados e tente de novo." e **a mesma mensagem** aparece para qualquer `invalid_credentials`, sem variar por e-mail (Review Focus 2); `rate_limited` mostra o `Alert` `warning` "Muitas tentativas de entrada" + "Por segurança, o acesso fica pausado por 15 minutos. Tente de novo depois desse prazo.", **desliga e-mail, senha e botão** e não reenvia sozinho; erro de rede (`status 0`) mostra a mensagem de conexão e deixa tentar de novo; botão `loading` durante o envio (um clique só dispara um pedido); "Esqueci minha senha" vai para `/esqueci-senha`; o texto "O acesso é por convite. Se você ainda não tem conta, peça ao administrador." está presente; quem já tem sessão e abre `/entrar` vai para `/`.
- [ ] **Step 2 a 4:** falhar, implementar (celular parte do layout de celular, `md` adapta), passar. **Step 5: Commit:** `feat(frontend): add the sign in screen`.

### Tarefa 6: Convite: definir a senha e link inválido

**Mockups:** `Convite · definir senha` (celular), `Convite · link expirado` (celular). O texto de apoio é "Defina uma senha para entrar." (ADR-12): a tela **não** consulta o token antes de enviar a senha. **Derivados:** a web usa o `AuthShell` (painel central) com o mesmo conteúdo; `link usado` usa o desenho do expirado com o texto de `link_used`.

**Files:** `src/features/iam/screens/{accept-invitation,link-problem}.tsx`, `src/app/routes/convite.tsx` (rota `/convite?token=`), testes.

**Interfaces:** `LinkProblem({ kind: "expired" | "used" })`: ícone de relógio `highlight`, título ("Este convite expirou" / "Este convite já foi usado ou foi substituído"), texto ("Por segurança, o link do convite deixa de valer depois de um tempo. Peça ao administrador para enviar um novo convite." / "Peça ao administrador para enviar um novo convite."), botão "Ir para o login" (`outline`). `AcceptInvitationScreen` lê `token` de `useSearchParams`.

- [ ] **Step 1: Testes que falham:** título "Crie sua senha" e o texto de apoio "Defina uma senha para entrar." (nenhum e-mail nem papel na tela); nenhuma chamada à API antes do envio; checklist ao vivo; **botão "Criar senha e entrar" desligado** até as 5 regras e a repetição baterem; sucesso navega para `/` com a sessão aberta; `link_expired` e `link_used` trocam a tela por `LinkProblem` do tipo certo; `invalid_password` mostra a mensagem do primeiro critério que falta; **sem `token` na URL, ou `token` vazio, mostra `LinkProblem` `used` sem chamar a API** (Review Focus 5).
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the invitation screens`.

### Tarefa 7: Esqueci minha senha e redefinir senha *(depende da Tarefa 1)*

**Mockups:** os da Tarefa 1 (`Esqueci-senha`, `Redefinir-senha`). **Files:** `src/features/iam/screens/{forgot-password,reset-password}.tsx`, `src/app/routes/{esqueci-senha,redefinir-senha}.tsx`, testes.

**Interfaces:** `/esqueci-senha` pede o e-mail e **sempre** mostra o mesmo aviso de sucesso (a resposta do backend é a mesma exista o e-mail ou não); `/redefinir-senha?token=` reaproveita `PasswordField`, `PasswordChecklist` e `LinkProblem` (`kind` `expired`/`used`, com o texto de "link" no lugar de "convite").

- [ ] **Step 1: Testes que falham:** pedido com qualquer e-mail mostra o aviso aprovado e nunca diz se o e-mail existe; `rate_limited` mostra o aviso de espera; redefinir com sucesso vai para `/entrar` com o aviso aprovado (a redefinição **não** abre sessão); `link_expired`/`link_used` mostram `LinkProblem`; sem `token` mostra `LinkProblem` sem chamar a API (Review Focus 5).
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the password reset screens`.

### Tarefa 8: Trocar senha, voluntária e obrigatória *(depende da Tarefa 1)*

**Mockups:** `Trocar-senha` (Tarefa 1) e, para a obrigatória, o `Convite · definir senha` com o título "Troque a senha inicial" (já aprovado em ADR-2). **Files:** `src/features/iam/screens/change-password.tsx`, `src/app/routes/conta.trocar-senha.tsx`, testes.

- [ ] **Step 1: Testes que falham:** campos "Senha atual", "Nova senha", "Repita a senha" e o checklist; `wrong_current_password` aparece no campo da senha atual; sucesso volta para `/` e, na variante obrigatória, `me.mustChangePassword` vira `false` sem recarregar a página; **na variante obrigatória não há "voltar" nem outro destino além de "Sair"**, e a voluntária tem "voltar"; nova senha igual à atual não é bloqueada pelo frontend (decisão do backend).
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the change password screen`.

### Tarefa 9: Casca autenticada e menu da conta *(depende da Tarefa 1 para o menu)*

**Mockups:** a barra lateral de `Usuários · web` e a barra superior de `Usuários · celular` (já aprovadas, para o Administrador); `Navegacao-web` e `Conta-celular` (Tarefa 1) para os demais papéis e para o menu do avatar.

**Files:** `src/app/layout/{app-shell,account-menu}.tsx`, `src/app/routes/_app.tsx` (layout de rotas autenticadas, com `RequireAuth` e `RequireSettledPassword`), substituir `src/app/routes/home.tsx`, testes.

**Interfaces:** `AppShell` mostra a barra lateral de 232 px (`nav`, `aria-label="Principal"`, item atual com `aria-current="page"`) a partir de `md`, e a barra superior de 64 px no celular; o item "Usuários" só aparece para `ADMINISTRATOR`; o bloco da conta mostra iniciais, nome e papel, com "Minhas sessões" e "Sair" (na web) e o menu do avatar (no celular). "Sair" chama `logout`, limpa o cache e vai para `/entrar`. A rota `/` vai para `/usuarios` quando Administrador e, para os demais, mostra uma página mínima com o nome e o papel (**provisória**: a primeira feature de negócio a substitui).

- [ ] **Step 1: Testes que falham:** Administrador vê "Usuários"; Comercial e Técnico não; "Sair" encerra a sessão e leva a `/entrar`; o nome e o papel vêm de `me`; o menu do celular abre por teclado e tem as três ações; navegação por teclado alcança tudo, com foco visível.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the authenticated shell`.

### Tarefa 10: Minhas sessões

**Mockups:** `Minhas sessões · celular`; web em `Sessoes-web` (Tarefa 1). **Files:** `src/features/iam/screens/sessions.tsx`, `components/session-item.tsx`, `src/app/routes/conta.sessoes.tsx`, testes.

- [ ] **Step 1: Testes que falham:** lista com aparelho, "Último uso" e "Entrou em"; a sessão do aparelho tem o chip "Esta sessão" (forma `filled`) e **sem** botão de encerrar; as outras têm "Encerrar esta sessão" que chama `revokeSession` **sem confirmação** (ADR-11) e a tira da lista; "Encerrar todas as outras" chama `revokeOtherSessions` e deixa só a atual; "Sair deste aparelho" faz logout; texto fixo "A sessão dura 30 dias e renova a cada uso."; erro de rede mantém a lista e mostra o aviso.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the sessions screen`.

### Tarefa 11: Lista de usuários

**Mockups:** `Usuários · web`, `Usuários · celular`; paginação conforme a Tarefa 1. **Files:** `src/features/iam/screens/users.tsx`, `components/user-row.tsx`, `src/app/routes/usuarios.tsx`, `usuarios.$id.tsx`, testes.

**Interfaces:** `/usuarios` lista com `Pageable` (tamanho de página e navegação a definir na Tarefa 1); a linha traz nome, e-mail, papel, `StatusChip` (`ACTIVE`→`ok`, `INVITED`→`pending`, `DEACTIVATED`→`off`) e "Último uso" ("agora", "há 3 horas", "nunca entrou"). Na web `/usuarios/:id` mostra a lista e o painel de detalhe lado a lado (a linha selecionada com fundo `line`); no celular, `/usuarios/:id` é a tela de detalhe e a linha é um link.

- [ ] **Step 1: Testes que falham:** a lista exibe as linhas com o chip certo (**cada situação com forma diferente**); "você" ao lado do nome do próprio usuário; `ListUsers` por Comercial/Técnico (403) mostra "você não tem permissão para isto"; lista vazia e erro de rede têm estado próprio; "Último uso" em texto relativo com a hora fixa do teste; clicar numa linha navega para `/usuarios/:id`.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the users list`.

### Tarefa 12: Detalhe do usuário, convidar e ações *(depende da Tarefa 1)*

**Mockups:** `Usuários · web` (painel), `Usuário e sessões · celular`, `Usuário desativado · web` e `· celular`, `Convidar usuário · web`; `Convidar-celular`, `Detalhe-convite-pendente` e `Detalhe-proprio` (Tarefa 1).

**Files:** `src/features/iam/screens/{user-detail,invite-user}.tsx`, `components/{user-panel,session-row}.tsx`, `src/app/routes/usuarios.convidar.tsx`, testes.

**Interfaces:** `UserPanel({ user, sessions, isSelf })` com, por situação: **ativo** (sessões ativas com "Encerrar" e "Encerrar todas as sessões", seletor de papel, "Desativar usuário"), **desativado** (texto "As sessões foram encerradas e este usuário não consegue entrar. Ao reativar, ele volta a entrar com a senha que já tem." e "Reativar usuário"), **convite pendente** ("Reenviar convite" e "Desativar usuário"), **o próprio** (sem seletor de papel e sem "Desativar"). `InviteUser`: diálogo na web (`role="dialog"`, `aria-modal`) e tela no celular, com nome, e-mail e `RolePicker`; "Enviar convite" desligado até o formulário ser válido.

- [ ] **Step 1: Testes que falham:** o painel de cada situação mostra **só** as ações dela; **para o próprio usuário não há seletor de papel nem "Desativar"** (Review Focus 6), e se o backend ainda devolver `cannot_change_own_role`/`cannot_deactivate_self` a mensagem aparece; trocar o papel chama `changeUserRole` e atualiza o painel; "Encerrar"/"Encerrar todas as sessões"/"Desativar"/"Reativar" chamam o serviço **sem confirmação** (ADR-11) e atualizam lista e painel; `email_already_registered` aparece **no campo e-mail** do convite; sucesso fecha o diálogo, atualiza a lista e mostra o aviso aprovado; "Reenviar convite" chama `resendInvitation` e mostra o aviso; cada botão de ação fica `loading` e não dispara duas vezes.
- [ ] **Step 2 a 4:** falhar, implementar, passar. **Step 5: Commit:** `feat(frontend): add the user detail and invitation`.

### Tarefa 13: Fechamento

**Files:** `docs/system-design/01-iam/design/comparacao-mockup.md`, `docs/rules/frontend.md` §3, `.claude/skills/crm-frontend-add-feature/SKILL.md`, `CLAUDE.md`.

- [ ] **Step 1: Comparação mockup × resultado**, campo a campo, para cada tela e tamanho: subir `npm run dev`, abrir o canvas e a tela lado a lado em 390 px e em 1280 px (navegador), e registrar em `comparacao-mockup.md` cada divergência (cor, espaçamento, tipografia, estrutura, comportamento) com o que foi feito. Divergência necessária vai **ao usuário antes de ser aceita**. Inclui as telas **derivadas** para aprovação.
- [ ] **Step 2: Acessibilidade:** teclado em todas as telas, foco visível, rótulos associados, anúncios de erro (`role="alert"`) e de carregamento (`aria-busy`).
- [ ] **Step 3: Ponta a ponta com o backend real** (quando o plano de backend estiver executado): login do Administrador semeado com troca obrigatória, convite, aceite pelo link do e-mail, encerramento remoto de sessão, desativar e reativar. Se o backend ainda não existe, registre aqui e deixe o passo aberto.
- [ ] **Step 4: Skill `crm-frontend-add-feature`** extraída da estrutura real de `features/iam/` (arquivos por feature, serviço, schemas, mensagens por código, guarda, rota fina, teste por tela); `CLAUDE.md` raiz ganha a linha da skill; `docs/rules/frontend.md` §3 passa a citar `features/iam/` como referência viva.
- [ ] **Step 5: Verificação completa** e commits: `docs(iam): add the mockup comparison`, `docs(frontend): add the frontend feature skill`.

---

## Fora deste plano

- **Capacitor e o token do app:** plano próprio ([`frontend.md`](../rules/frontend.md) §8). Aqui a web só usa o cookie.
- **Tema claro:** os tokens existem e são testados, mas nenhuma tela foi desenhada nele; a comparação mockup × resultado vale para o escuro.
- **Telas de negócio** (clientes, usinas, agenda, propostas): features futuras.
- **Busca e filtro na lista de usuários:** o backend aceita (`role`, `status`, texto), mas nenhum mockup os desenha.
