# Domain model: 01-iam

**Este arquivo é a fonte de verdade do domínio desta feature.** Nenhum `CLAUDE.md`, skill ou código o duplica: todos o referenciam. Casos de uso e códigos de erro: [`usecases.md`](usecases.md). Decisões: [`decisions.md`](decisions.md).

> Proposta para aprovação. Os mockups estão aprovados ([`design/`](design/README.md)); este modelo e o `usecases.md` ainda não.

## Nota de proveniência

Modelado do zero, a partir das decisões do usuário em [`decisions.md`](decisions.md) (ADR-1 a ADR-5) e dos mockups aprovados. Os pontos que **eu** escolhi, sem decisão prévia do usuário, estão marcados como *proposta* e listados em [`decisions.md`](decisions.md) (ADR-6 a ADR-9).

## Aggregates

Três aggregates, todos do BC `iam`. Entre si se referenciam por `UUID` (mesmo BC, mas cada um é fronteira de consistência própria: ver [`architecture.md`](../../rules/architecture.md) §6.1).

### `User` (aggregate root)

**Fronteira de consistência:** a conta e o que a define: identidade, papel, situação e credencial. Muda sozinho, na própria transação. A unicidade do e-mail é garantida pelo banco (`UNIQUE`), traduzida em `ConflictException` pelo repository.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `name` | `UserName` | de 2 a 80 caracteres |
| `email` | `Email` (shared) | normalizado em minúsculas; identidade de login |
| `role` | `Role` | um dos três papéis (ADR-5) |
| `status` | `UserStatus` | ver ciclo de vida |
| `passwordHash` | `PasswordHash` | ausente enquanto `INVITED` |
| `mustChangePassword` | `boolean` | `true` só na conta criada pelo seeder (ADR-2) até a primeira troca |

**Invariantes:**
- `ACTIVE` sempre tem `passwordHash`. `INVITED` nunca tem.
- Quem age **não altera o próprio papel nem se desativa** (*proposta*, ADR-8). Com isso, sempre sobra o Administrador que agiu: o sistema nunca fica sem Administrador por uma ação isolada.
- Reativar devolve o usuário ao estado de antes da desativação: `ACTIVE` se ele tem senha, `INVITED` se nunca definiu uma (ADR-10).
- A senha nunca é guardada nem comparada em texto: o aggregate só vê `PasswordHash`.

**Métodos de domínio:**

| Método | Efeito | Erros de domínio (`code`) |
|---|---|---|
| `invite(name, email, role, now)` | cria em `INVITED`, sem senha | — |
| `acceptInvitation(passwordHash)` | `INVITED` → `ACTIVE` | `user_not_invited` |
| `changePassword(newHash)` | troca o hash; zera `mustChangePassword` | `user_not_active` |
| `resetPassword(newHash)` | troca o hash de quem esqueceu | `user_not_active` |
| `changeRole(actorId, newRole, now)` | muda o papel; registra `UserRoleChanged` com `now` | `cannot_change_own_role`, `user_not_active` |
| `deactivate(actorId, now)` | `INVITED` ou `ACTIVE` → `DEACTIVATED` | `cannot_deactivate_self`, `user_deactivated` |
| `reactivate()` | `DEACTIVATED` → `ACTIVE` (se tem senha) ou `INVITED` (se nunca definiu) | `user_not_deactivated` |
| `markPasswordChangeRequired()` | só usado pelo seeder do primeiro Administrador | — |

**Domain events registrados** (todos **domain event**, in-process, depois do commit):
- `UserDeactivated(aggregateId, occurredOn)`: o listener revoga as sessões do usuário.
- `UserRoleChanged(aggregateId, occurredOn)`: o listener invalida o cache de sessão do usuário, porque o papel vai junto na sessão em cache.

### `OneTimeToken` (aggregate root)

Link de uso único: o convite e a redefinição de senha. Aggregate próprio porque tem ciclo de vida independente do usuário (emitido, usado, expirado, substituído) e porque duas requisições disputam o mesmo link.

**Fronteira de consistência:** o token e o seu consumo. Usar um link é um update condicional no banco (`WHERE used_at IS NULL`), não um `save` com `409`: quem perde a corrida recebe `link_used`, resposta de negócio (ver [`architecture.md`](../../rules/architecture.md) §8).

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio |
| `userId` | `UUID` | usuário dono do link |
| `purpose` | `TokenPurpose` | `INVITATION` ou `PASSWORD_RESET` |
| `tokenHash` | `TokenHash` | SHA-256 do segredo; o segredo nunca é guardado |
| `expiresAt` | `Instant` | emissão + validade da finalidade |
| `usedAt` | `Instant?` | preenchido ao consumir |
| `invalidatedAt` | `Instant?` | preenchido quando um link mais novo o substitui |

**Invariantes:**
- Só vale uma vez: consumido ou invalidado, nunca mais vale.
- Só vale antes de `expiresAt`.
- O segredo em texto nunca sai do domínio, a não ser dentro do evento de emissão (abaixo).

**Métodos de domínio:**

| Método | Efeito | Erros (`code`) |
|---|---|---|
| `issue(userId, purpose, secret, validity, now)` | cria o token e registra o evento | — |
| `consume(now)` | marca como usado | `link_expired`, `link_used` |
| `invalidate(now)` | substitui por um novo (reenvio de convite, novo pedido de redefinição) | — (idempotente) |

`link_used` vale para link já usado **e** para link substituído por outro mais novo.

**Domain event registrado:** `OneTimeTokenIssued(aggregateId, occurredOn, userId, purpose, secret)`. É o único evento que carrega um segredo: o listener (em `iam-infrastructure`) monta o e-mail com o link e o envia. **É domain event, nunca integration event**: nunca vai ao outbox nem ao broker, e nunca é logado nem serializado.

### `Session` (aggregate root)

Um aparelho conectado à conta (ADR-1).

**Fronteira de consistência:** a sessão sozinha. Revogar ou renovar uma sessão não depende de outra.

| Campo | Tipo | Regra |
|---|---|---|
| `id` | `UUID` | gerado pelo domínio; é o que a tela mostra e o Administrador encerra |
| `userId` | `UUID` | dono |
| `tokenHash` | `TokenHash` | SHA-256 do token opaco que o cliente guarda |
| `device` | `DeviceLabel` | de 1 a 80 caracteres, ex. "Galaxy A54, app DUNAS" |
| `startedAt` | `Instant` | quando entrou |
| `lastUsedAt` | `Instant` | último uso que renovou a sessão |
| `expiresAt` | `Instant` | `lastUsedAt` + 30 dias (ADR-1) |
| `revokedAt` | `Instant?` | preenchido ao encerrar |
| `revokedBy` | `UUID?` | quem encerrou (o próprio usuário ou um Administrador) |

**Invariantes:**
- Está **ativa** quando não foi revogada e `now < expiresAt`.
- Sessão revogada nunca volta a valer.
- A duração (30 dias) e o intervalo mínimo entre renovações entram **por parâmetro**: o domínio não lê configuração.

**Métodos de domínio:**

| Método | Efeito | Erros (`code`) |
|---|---|---|
| `start(userId, secret, device, validity, now)` | cria a sessão | — |
| `renewIfDue(now, validity, minInterval)` | se passou `minInterval` desde `lastUsedAt`, renova e devolve `true`; senão devolve `false` (ADR-6) | `session_invalid` se inativa |
| `revoke(by, now)` | encerra | — (idempotente: já revogada não muda nada) |
| `isActiveAt(now)` | `boolean` | — |

**Domain event registrado:** `SessionRevoked(aggregateId, occurredOn, tokenHash)`: o listener tira a sessão do cache **na hora**, e é o que torna o encerramento remoto imediato (ADR-1).

## Value Objects

| VO | Base | Regra de validação | Fica em |
|---|---|---|---|
| `Email` | `StringValueObject` | **já existe** | `shared-domain` |
| `UserName` | `StringValueObject` | de 2 a 80 caracteres | `iam-domain` |
| `Password` | `ValueObject<String>` (não `StringValueObject`: este apara os espaços das pontas, e aparar altera a senha) | senha em texto, só em trânsito: de 8 a 30 caracteres, com maiúscula, minúscula, número e caractere especial (ADR-3). Erro `invalid_password` diz o primeiro critério que falta. `toString()` e `equals` nunca expõem o valor; nunca é guardada | `iam-domain` |
| `PasswordHash` | `StringValueObject` | hash Argon2id já calculado (formato PHC); recusa texto que não tenha esse formato | `iam-domain` |
| `TokenSecret` | `StringValueObject` | segredo de 256 bits em base64 url-safe (43 caracteres). `generate()` usa `SecureRandom`; `hash()` devolve o `TokenHash`; `toString()` mascarado | `iam-domain` |
| `TokenHash` | `StringValueObject` | SHA-256 em hexadecimal (64 caracteres) | `iam-domain` |
| `DeviceLabel` | `StringValueObject` | de 1 a 80 caracteres | `iam-domain` |
| `Role` | enum | `ADMINISTRATOR`, `COMMERCIAL`, `TECHNICIAN` | `shared-domain` (todo BC o usa para checar permissão: [`architecture.md`](../../rules/architecture.md) §3.1; [`02-catalog`](../02-catalog/README.md)) |
| `UserStatus` | enum | `INVITED`, `ACTIVE`, `DEACTIVATED` | `iam-domain` |
| `TokenPurpose` | enum | `INVITATION`, `PASSWORD_RESET` | `iam-domain` |

Nenhum outro VO é promovido a `shared-domain`: só o IAM os usa. A exceção é o `Role`, que sobe porque todo BC checa o papel de quem age. A tabela de mensagens de erro de cada VO vem na implementação, em pt-BR minúsculo ([`code-style.md`](../../rules/code-style.md) §5).

**Hash de senha e geração de segredo.** `Password` → `PasswordHash` exige Argon2id, que depende de biblioteca e portanto não cabe em `domain`: é um **contrato** em `iam-application` (`PasswordHasher`: `hash(Password)` e `matches(Password, PasswordHash)`), implementado em `iam-infrastructure`. É infraestrutura nossa, não terceiro: não passa por Translator. `TokenSecret` e `TokenHash` usam só o JDK (`SecureRandom`, `MessageDigest`) e ficam no domínio.

## Specifications, Policies e Domain Services

Nenhum. As regras desta feature (única utilização, validade, sessão ativa, não agir sobre si mesmo) pertencem a um único aggregate e moram nele. Não há decisão que varie por contexto nem regra que ligue aggregates sem dono natural. Se a regra de papéis crescer (permissões por papel), o primeiro candidato é uma Policy, e nasce junto da feature que a exigir (ADR-5).

## Ciclo de vida

Só o `User` tem status.

```
INVITED ──acceptInvitation──▶ ACTIVE
   │                            │
   └────────deactivate──────────┴──▶ DEACTIVATED ──reactivate──▶ ACTIVE (tinha senha)
                                                  └────────────▶ INVITED (nunca definiu)
```

| De | Para | Quem | Trava |
|---|---|---|---|
| `INVITED` | `ACTIVE` | o convidado, com o link e a senha | `user_not_invited` |
| `ACTIVE` | `DEACTIVATED` | Administrador, nunca em si mesmo | `cannot_deactivate_self`, `user_deactivated` |
| `INVITED` | `DEACTIVATED` | Administrador | idem |
| `DEACTIVATED` | `ACTIVE` ou `INVITED` | Administrador | `user_not_deactivated` |

Três estados e poucas transições: implementado como **enum com guardas no aggregate**, sem a base `StateMachine` de `common-domain` (ADR-9).

`Session` e `OneTimeToken` têm estado derivado de campos (`revokedAt`, `usedAt`, `expiresAt`), não de enum: "sem ciclo de vida" no sentido da máquina de estados.

## Integration events

**Nenhum publicado, nenhum consumido.** Nenhum outro BC existe ainda, e o e-mail de convite e de redefinição é decidido dentro do BC por domain event (ADR-7). Quando um BC precisar saber de um usuário (ex. o cadastro de clientes referenciando quem atendeu), um integration event nasce com a necessidade, e isto vira ADR.

## Persistência

Migrations em `iam-infrastructure/src/main/resources/db/migration/iam/`, numeradas de `V1__`. Referências entre as três tabelas são por `uuid`; `FK` entre elas é permitida (mesmo BC), mas **nenhuma** para tabela de outro BC.

| Tabela | Colunas (além de `id uuid PK`, `version`, `created_at`, `updated_at`) | Restrições e índices |
|---|---|---|
| `users` | `name`, `email`, `role`, `status`, `password_hash` (nulável), `must_change_password` | `UNIQUE (email)` |
| `one_time_tokens` | `user_id`, `purpose`, `token_hash`, `expires_at`, `used_at` (nulável), `invalidated_at` (nulável) | `UNIQUE (token_hash)`; índice em `(user_id, purpose)` para invalidar os pendentes |
| `sessions` | `user_id`, `token_hash`, `device`, `started_at`, `last_used_at`, `expires_at`, `revoked_at` (nulável), `revoked_by` (nulável) | `UNIQUE (token_hash)`; índice em `(user_id)` parcial onde `revoked_at IS NULL` |

- Hash de senha e de token são colunas de texto: o tamanho do banco não acompanha o limite de 30 caracteres da senha (ADR-3).
- Sessão revogada ou expirada **não é apagada** por esta feature: serve de trilha. Limpeza periódica fica para um plano posterior.
