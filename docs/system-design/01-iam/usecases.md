# Use cases: 01-iam

Forma dos use cases (sem interface, sem sufixo, `@UseCase`, `commands` e `queries`): [`architecture.md`](../../rules/architecture.md) §7. Domínio: [`domain-model.md`](domain-model.md). Telas: [`design/`](design/README.md).

> Proposta para aprovação. Os números marcados *proposta* não foram decididos pelo usuário.

## Convenções desta feature

**Quem age.** Todo use case autenticado recebe um `Actor` (`record Actor(UUID userId, Role role, UUID sessionId)`, em `iam-application`), montado pela `web` a partir da sessão. Endpoint "self" nunca aceita o id do próprio usuário no corpo: vem do `Actor`. A checagem de papel é **no use case**, nunca no controller (ADR-5).

**Configuração** (`crm.iam.*`, `@ConfigurationProperties` com `@DefaultValue`, [`architecture.md`](../../rules/architecture.md) §9):

| Propriedade | Valor | Origem |
|---|---|---|
| `session-validity` | 30 dias | ADR-1 |
| `session-renew-interval` | 5 minutos | *proposta* (ADR-6) |
| `session-cache-ttl` | 60 segundos | *proposta* (rede de segurança; a invalidação é por evento) |
| `invitation-validity` | 48 horas | *proposta* |
| `password-reset-validity` | 1 hora | *proposta* |
| `login-limit-per-email` | 5 tentativas em 15 minutos | ADR-4 |
| `login-limit-per-origin` | 20 tentativas em 15 minutos | *proposta* |
| `password-reset-limit-per-email` | 3 pedidos em 1 hora | *proposta* |

**Limite de tentativas** (`RateLimiter`): `consume` antes de checar a credencial e `refund` no acerto, como o contrato recomenda. O escopo é string do IAM (`iam.login.email`, `iam.login.origin`, `iam.password-reset.email`); o e-mail do contador é o normalizado e **conta também e-mail inexistente** (ADR-4). Contador fora do ar: **recusa** (`failClosed`), porque liberar login sem freio é pior do que recusá-lo.

**Segredos nunca em log.** Senha, token de sessão e segredo de link não aparecem em log, em mensagem de exceção nem em evento que não seja o de emissão do link (o aspect de `@UseCase` não loga argumentos, [`architecture.md`](../../rules/architecture.md) §7.2).

## Commands

### `Login`

- **Pacote:** `com.crm.iam.application.usecases.commands`
- **Anotação:** `@UseCase`.
- **Entrada:** `email`, `password`, `device` (rótulo do aparelho), `origin` (IP, do `web`). Sem `Actor`: é público.
- **Pré-condições:** o e-mail e a senha batem com um usuário `ACTIVE`. Falha, de **qualquer** motivo (e-mail inexistente, senha errada, usuário `INVITED` ou `DEACTIVATED`): `UnauthorizedException` com `invalid_credentials`. A resposta e o tempo são os mesmos: para e-mail inexistente o `PasswordHasher` roda contra um hash fixo, para não revelar quem tem conta pelo tempo de resposta.
- **Fluxo:** 1. `consume` dos limites por e-mail e por origem (estourou: `RateLimitExceededException`, código `rate_limited`). 2. Carrega o `User` por e-mail. 3. `PasswordHasher.matches`. 4. No acerto, `refund` dos dois limites. 5. `Session.start` com `TokenSecret.generate()`. 6. Salva. 7. Devolve o segredo **uma única vez**.
- **Pós-condições:** sessão ativa por 30 dias. Retorna `SessionResponse(token, expiresAt, mustChangePassword)`.
- **Endpoint:** `POST /auth/login`, público. O controller decide a forma da credencial pelo cabeçalho `X-Client` ([`architecture.md`](../../rules/architecture.md) §14): navegador recebe o cookie `crm_session` e o corpo **sem** token; `X-Client: app` recebe o token no corpo.

### `Logout`

- **Entrada:** `Actor`. Encerra a sessão do próprio `Actor`.
- **Fluxo:** carrega a `Session` por id, `revoke(actor.userId, now)`, salva, publica.
- **Pós-condições:** `SessionRevoked` publicado; o cache da sessão sai na hora. Idempotente.
- **Endpoint:** `POST /auth/logout`, autenticado. Limpa o cookie `crm_session`, se a requisição o usou.

### `InviteUser`

- **Entrada:** `Actor`, `name`, `email`, `role`.
- **Pré-condições:** `Actor` é Administrador (`ForbiddenException`). E-mail ainda não existe (`ConflictException`, `email_already_registered`).
- **Fluxo:** 1. `User.invite`. 2. Salva. 3. `OneTimeToken.issue` (`INVITATION`, `invitation-validity`) com `TokenSecret.generate()`. 4. Salva. 5. Publica os eventos dos dois.
- **Pós-condições:** usuário `INVITED`. O listener de `OneTimeTokenIssued` envia o e-mail com o link, **depois do commit**: e-mail que falha não desfaz o convite, e o Administrador reenvia.
- **Retorno:** `UUIDResponse` com o id do usuário.
- **Endpoint:** `POST /users`, Administrador.

### `ResendInvitation`

- **Entrada:** `Actor`, `userId`.
- **Pré-condições:** Administrador. Usuário existe (`NotFoundException`, `user_not_found`) e está `INVITED` (`user_not_invited`, 422).
- **Fluxo:** invalida os tokens `INVITATION` pendentes do usuário, emite um novo e publica.
- **Endpoint:** `POST /users/{userId}/invitation`, Administrador.

### `AcceptInvitation`

- **Entrada:** `token` (o segredo do link), `password`, `device`. Público.
- **Pré-condições:** o token existe, não expirou e não foi usado (`link_expired`, `link_used`, 422). A senha cumpre a regra (`invalid_password`, 400).
- **Fluxo:** 1. Procura o `OneTimeToken` pelo `TokenHash` do segredo; não existe: `UnprocessableEntityException` com `link_used` (não distingue de usado, para não revelar quais links existem). 2. `consume` como update condicional. 3. `PasswordHasher.hash`. 4. `User.acceptInvitation`. 5. Salva. 6. Abre uma `Session`, como o `Login`.
- **Pós-condições:** usuário `ACTIVE` e já conectado. Retorna `SessionResponse`.
- **Endpoint:** `POST /auth/invitation/accept`, público. Mesma regra de credencial do login.

### `RequestPasswordReset`

- **Entrada:** `email`, `origin`. Público.
- **Pré-condições:** nenhuma que apareça na resposta: **sempre responde igual** (204), exista o e-mail ou não.
- **Fluxo:** `consume` do limite `iam.password-reset.email`. Se o usuário existe e está `ACTIVE`: invalida os tokens `PASSWORD_RESET` pendentes, emite um novo e publica. Se não: não faz nada.
- **Endpoint:** `POST /auth/password/forgot`, público.

### `ResetPassword`

- **Entrada:** `token`, `password`. Público.
- **Pré-condições:** como `AcceptInvitation`, para `PASSWORD_RESET`; usuário `ACTIVE` (`user_not_active`).
- **Fluxo:** consome o token, `User.resetPassword`, salva, **revoga todas as sessões do usuário** (quem esqueceu a senha pode estar com a conta comprometida) e publica `SessionRevoked` de cada uma.
- **Pós-condições:** nenhuma sessão ativa; o usuário entra de novo pelo login. Não conecta sozinho.
- **Endpoint:** `POST /auth/password/reset`, público.

### `ChangePassword`

- **Entrada:** `Actor`, `currentPassword`, `newPassword`.
- **Pré-condições:** a senha atual bate (`UnprocessableEntityException`, `wrong_current_password`; conta no limite `iam.login.email` do próprio usuário). A nova cumpre a regra.
- **Fluxo:** `User.changePassword` (zera `mustChangePassword`), salva, **revoga as outras sessões do usuário**, mantém a atual.
- **Endpoint:** `PUT /auth/password`, autenticado. É, com `Logout` e `GetCurrentUser` (o cliente precisa saber que a troca é obrigatória depois de recarregar), o que uma sessão de conta com `mustChangePassword` pode chamar (ADR-2).

### `ChangeUserRole`

- **Entrada:** `Actor`, `userId`, `role`.
- **Pré-condições:** Administrador; usuário existe; `cannot_change_own_role`; usuário `ACTIVE`.
- **Fluxo:** `User.changeRole(actor.userId, role, now)`, salva, publica `UserRoleChanged`.
- **Endpoint:** `PUT /users/{userId}/role`, Administrador.

### `DeactivateUser`

- **Entrada:** `Actor`, `userId`.
- **Pré-condições:** Administrador; usuário existe; `cannot_deactivate_self`; ainda não desativado (`user_deactivated`).
- **Fluxo:** `User.deactivate`, salva, publica `UserDeactivated`. O listener revoga todas as sessões do usuário (efeito imediato).
- **Endpoint:** `POST /users/{userId}/deactivation`, Administrador.

### `ReactivateUser`

- **Entrada:** `Actor`, `userId`.
- **Pré-condições:** Administrador; usuário existe; está `DEACTIVATED` (`user_not_deactivated`, 422).
- **Fluxo:** `User.reactivate`, salva. Volta a `ACTIVE` se tem senha, ou a `INVITED` se nunca definiu uma: nesse caso o Administrador usa `ResendInvitation` em seguida.
- **Pós-condições:** nenhuma sessão volta (as revogadas na desativação continuam revogadas); o usuário entra de novo pelo login. Sem evento.
- **Endpoint:** `POST /users/{userId}/reactivation`, Administrador.

### `RevokeSession`

- **Entrada:** `Actor`, `sessionId`.
- **Pré-condições:** a sessão existe (`NotFoundException`, `session_not_found`) e é **do próprio `Actor` ou o `Actor` é Administrador** (`ForbiddenException`).
- **Fluxo:** `Session.revoke(actor.userId, now)`, salva, publica `SessionRevoked`. Idempotente.
- **Endpoint:** `DELETE /sessions/{sessionId}`, autenticado.

### `RevokeAllSessions`

- **Entrada:** `Actor`, `userId`, `keepCurrent` (`true` mantém a sessão do `Actor`).
- **Pré-condições:** `userId` é o do `Actor` ou o `Actor` é Administrador. `keepCurrent` só vale para o próprio usuário.
- **Fluxo:** carrega as sessões ativas, revoga cada uma e publica.
- **Endpoint:** `DELETE /users/{userId}/sessions?keepCurrent=`, autenticado. "Encerrar todas as outras" (celular) usa o próprio `userId` com `keepCurrent=true`; o botão do Administrador usa o `userId` do outro com `keepCurrent=false`.

### `AuthenticateSession`

Chamado pela cadeia de segurança do `boot` a cada requisição ([`architecture.md`](../../rules/architecture.md) §4.1).

- **Entrada:** o token opaco do cabeçalho.
- **Fluxo:** 1. `TokenSecret` → `TokenHash`. 2. `Cache.getOrLoad("iam:session:<hash>", ttl = session-cache-ttl)` com o carregamento do banco (sessão + papel do usuário). 3. Sessão inexistente, revogada ou expirada, ou usuário não `ACTIVE`: `UnauthorizedException`, `session_invalid`. 4. `renewIfDue`; só grava quando devolve `true`. 5. Devolve o `Actor` e `mustChangePassword`.
- **Cache:** chave `iam:session:<tokenHash>`, TTL de segurança de 60 segundos. **Invalidação por evento**, que é o mecanismo principal ([`architecture.md`](../../rules/architecture.md) §6.2): `SessionRevoked` remove a chave; `UserDeactivated` e `UserRoleChanged` removem a chave de cada sessão do usuário. O cache nunca decide: o banco é a fonte de verdade.
- **Sem endpoint:** consumido pela cadeia de segurança.

### Listeners (adapters de entrada em `iam-infrastructure`)

Só traduzem e chamam um use case; nenhuma regra neles. Rodam **depois do commit**.

| Evento (domain event) | O que faz |
|---|---|
| `OneTimeTokenIssued` | monta o e-mail (convite ou redefinição) com o link e chama `EmailSenderService` |
| `SessionRevoked` | remove a sessão do cache |
| `UserDeactivated` | revoga as sessões ativas do usuário (use case interno `RevokeUserSessions`, sem `Actor`) |
| `UserRoleChanged` | remove do cache as sessões do usuário |

## Queries

### `GetCurrentUser`

- **Anotação:** `@UseCase(transactional = false)`.
- **Entrada:** `Actor`. **Retorno:** `id`, `name`, `email`, `role`, `mustChangePassword`.
- **Endpoint:** `GET /auth/me`, autenticado.

### `ListUsers`

- **Entrada:** `Actor` (Administrador), filtro opcional (`role`, `status`, texto em nome ou e-mail), `Pageable`. O filtro é o `UserFilter`, lido pelo contrato `UserReader`.
- **Retorno:** `Page<UserSummary>`: `id`, `name`, `email`, `role`, `status`, `lastUsedAt` (o último uso entre as sessões do usuário; vazio se nunca entrou).
- **Endpoint:** `GET /users`, Administrador.

### `GetUser`

- **Entrada:** `Actor` (Administrador), `userId`. **Retorno:** o resumo acima e as sessões ativas (como em `ListSessions`).
- **Endpoint:** `GET /users/{userId}`, Administrador.

### `ListSessions`

- **Entrada:** `Actor`, `userId` (o do `Actor`, ou outro se Administrador).
- **Retorno:** lista de `SessionSummary`: `id`, `device`, `startedAt`, `lastUsedAt`, `current` (só quando é a sessão do `Actor`). Só as ativas, da mais recente para a mais antiga.
- **Endpoint:** `GET /users/{userId}/sessions`, autenticado ("Minhas sessões" usa o `userId` do próprio `Actor`).

> Queries não carregam aggregate: leem por um contrato de leitura em `iam-application` (`application.contracts.readers`: `UserReader`, `SessionReader`), devolvendo projeção. O desenho está registrado na skill `crm-add-use-case`.

## Erros

Contrato público ([`architecture.md`](../../rules/architecture.md) §7.3). Códigos únicos no BC.

| `code` | Status | Mensagem (pt-BR) | Quando |
|---|---|---|---|
| `invalid_credentials` | 401 | e-mail ou senha incorretos | login com qualquer falha de credencial |
| `session_invalid` | 401 | sua sessão não vale mais; entre de novo | token desconhecido, expirado, revogado ou de usuário desativado |
| `rate_limited` | 429 | muitas tentativas; tente de novo em alguns minutos | limite estourado (código do kernel) |
| `password_change_required` | 403 | você precisa trocar a senha antes de continuar | sessão com `mustChangePassword` fora de `ChangePassword`, `Logout` e `GetCurrentUser` (ADR-2, [`architecture.md`](../../rules/architecture.md) §4.1) |
| `csrf_invalid` | 403 | requisição recusada; recarregue a página e tente de novo | escrita por cookie sem o cabeçalho de CSRF (*proposta*: código da cadeia de segurança) |
| `forbidden` | 403 | você não tem permissão para isto | papel insuficiente ou sessão de outro usuário |
| `invalid_password` | 400 | a senha precisa de `<critério que falta>` | regra do ADR-3 |
| `link_expired` | 422 | este link expirou | link vencido |
| `link_used` | 422 | este link já foi usado ou foi substituído | link usado, invalidado ou inexistente |
| `email_already_registered` | 409 | já existe um usuário com este e-mail | convite de e-mail repetido |
| `user_not_found` | 404 | usuário não encontrado | id inexistente |
| `session_not_found` | 404 | sessão não encontrada | id inexistente |
| `user_not_invited` | 422 | este usuário não está com convite pendente | reenviar convite de quem não está `INVITED` |
| `user_not_active` | 422 | este usuário não está ativo | trocar papel ou senha de quem não está `ACTIVE` |
| `user_deactivated` | 422 | este usuário já está desativado | desativar duas vezes |
| `user_not_deactivated` | 422 | este usuário não está desativado | reativar quem não está `DEACTIVATED` |
| `cannot_change_own_role` | 422 | você não pode mudar o seu próprio papel | `ChangeUserRole` sobre si mesmo |
| `cannot_deactivate_self` | 422 | você não pode desativar a sua própria conta | `DeactivateUser` sobre si mesmo |
| `wrong_current_password` | 422 | a senha atual está incorreta | `ChangePassword` |

## Consumo de integration event

Nenhum (ver [`domain-model.md`](domain-model.md)).

## Fora de escopo

- **MFA do Administrador, login social, SSO** (README da feature).
- **Cliente final e link mágico** (feature própria, depois do cadastro de clientes).
- **Editar o próprio nome ou e-mail:** sem demanda nem tela.
- **Limpeza de sessões e tokens antigos:** plano posterior.
