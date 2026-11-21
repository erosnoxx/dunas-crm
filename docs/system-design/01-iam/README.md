# 01: IAM

## Objetivo

Identidade e acesso da **equipe** da empresa: quem entra no sistema, como entra, o que pode fazer e de onde está conectado. É a primeira feature porque todas as outras dependem de saber quem está agindo.

## Escopo desta feature

> Proposta em discussão: nada aqui é decisão até o usuário aprovar a spec.

**Dentro:**
- Conta de funcionário, com papel (Administrador, Comercial, Técnico).
- Convite por e-mail e definição da primeira senha.
- Entrar, sair e sessões ativas (ver e encerrar).
- Redefinir senha.
- Gestão de usuários pelo Administrador (convidar, mudar papel, desativar).
- Cadeia de segurança do `boot` ligada ao IAM (hoje o Spring exige autenticação em toda rota: [`architecture.md`](../../rules/architecture.md) §13).

**Fora:**
- **Cliente final e link mágico** do portal: depende de existir cliente cadastrado, e a sessão da equipe já se valida sozinha. Vira feature própria depois do cadastro de clientes.
- **MFA do Administrador:** segunda fase, depois da sessão básica.
- Login social e SSO: sem demanda (uma empresa só).

## Bounded contexts envolvidos

Cria o BC `iam` (módulos `iam-domain`, `iam-application`, `iam-infrastructure`; passo a passo na skill `crm-add-bounded-context`). Nenhuma referência a outro BC por enquanto.

Não publica nem consome integration event: o e-mail de convite e de redefinição sai por domain event dentro do BC (ADR-7).

## Dependências de outras features

Nenhuma. É a primeira.

## Base de código já existente

Nenhuma de domínio: `core/iam` só tem os `pom.xml`. Existem as peças técnicas do `kernel` que a feature usa (`Email` em `shared-domain`, `EmailSenderService`, `RateLimiter`, `IdempotencyGuard`, `Cache`).

## Como este documento foi produzido

Brainstorming com o usuário em 2026-11-19 (decisões em `decisions.md`, ADR-1 a ADR-5). Mockups aprovados em 2026-11-19. `domain-model.md` e `usecases.md` escritos em seguida e **aprovados pelo usuário com "pode continuar" em 2026-11-19, inclusive os números marcados como proposta**; as escolhas que não vieram do usuário estão em ADR-6 a ADR-11.

## Critério de aceite

- [x] `mvn test` passa, a partir de `backend/` (inclui os testes de arquitetura e o `IamEndToEndTest`, com Docker; backend fechado em 2026-11-20)
- [x] Artefato visual em `design/` gerado e aprovado antes de fechar `domain-model.md`/`usecases.md` (telas de entrar, convite, sessões e usuários)
- [x] Plano de implementação em `docs/plans/` e executado (frontend fechado em 2026-11-20; a comparação com o mockup e o que fica para o usuário decidir: [`design/comparacao-mockup.md`](design/comparacao-mockup.md))
