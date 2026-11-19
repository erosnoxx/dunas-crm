# Decisions: 01-iam

ADRs **locais desta feature**. Decisão global ou de arquitetura do sistema não entra aqui: arquitetura vai em [`architecture.md`](../../rules/architecture.md), produto/global vai em [`00-overview.md`](../00-overview.md).

## ADR-1: sessão longa, revogável pelo Administrador

**Contexto:** o técnico usa o app no telhado, com a mão ocupada e sol na tela. Digitar senha toda hora é um custo real de uso. Em contrapartida, um celular perdido não pode manter acesso aberto.

**Decisão:** a sessão é **longa** e **revogável a qualquer momento pelo Administrador**, de qualquer lugar. A sessão tem um registro no servidor (não é um token que vale sozinho até expirar), então encerrá-la tem efeito imediato. O Administrador vê as sessões ativas de cada usuário (aparelho e último uso) e encerra uma ou todas. O próprio usuário também encerra as suas.

**Consequência:** o técnico quase não digita senha. Cada requisição confere a sessão no servidor (com cache, para não pesar no banco). A sessão dura **30 dias e renova a cada uso**: quem usa toda semana nunca digita senha, e quem some por um mês volta pelo login.

## ADR-2: o primeiro Administrador nasce por seeder

**Contexto:** convite exige alguém que convide, então o primeiro usuário vem de fora do fluxo normal.

**Decisão:** um `Seeder` do BC `iam` cria o Administrador inicial no boot. Nome, e-mail e senha vêm de variáveis de ambiente, resolvidas no JSON do seeder (`${...}`, como o `DatabaseSeeder` já faz): **nenhuma credencial vai para o repositório**. O seeder só cria quando **não existe nenhum usuário**, então reiniciar o app não recria nem sobrescreve. A conta nasce com **troca de senha obrigatória** no primeiro acesso.

**Consequência:** sem comando manual no servidor, e o deploy sobe com acesso. Depende do redesenho do seeder pendente em [`architecture.md`](../../rules/architecture.md) §13 (idempotente, transacional e seguro para produção): ele é pré-requisito do plano de implementação. A senha inicial fica no ambiente do deploy até a primeira troca, então a variável deve ser descartada depois. Variável ausente em produção deve falhar o boot, não criar conta sem senha.

## ADR-3: regra de senha

**Contexto:** a equipe define a própria senha no convite e ao redefinir. É preciso uma regra única para os dois caminhos.

**Decisão:** mínimo de **8 caracteres**, com pelo menos **uma maiúscula, uma minúscula, um número e um caractere especial**. O limite máximo é **30 caracteres**, regra do domínio. A coluna do banco guarda só o hash Argon2id, de tamanho fixo e independente da senha, então o limite do banco não acompanha o do domínio. A senha é guardada só como hash Argon2id e nunca aparece em log nem em mensagem de erro. A regra vale também para a senha inicial do Administrador (ADR-2).

**Consequência:** a regra mora num Value Object do domínio (`Password`, em `iam-domain`), então todo caminho que define senha a aplica. A mensagem de erro diz qual critério faltou, sem repetir a senha. Mudar a regra depois não invalida senhas já guardadas: vale só para as novas.

## ADR-4: bloqueio temporário por tentativas erradas de login

**Contexto:** senha de 8 a 30 caracteres, com regra de composição, é adivinhável por tentativa em massa se o login não tiver freio.

**Decisão:** tentativas de login erradas levam a **bloqueio temporário**, contado por e-mail e por origem, usando o `RateLimiter` que já existe. A resposta é a **mesma** para e-mail inexistente e senha errada, e o contador por e-mail conta também e-mail que não existe: o sistema não revela quem tem conta. **5 tentativas erradas em 15 minutos bloqueiam por 15 minutos.**

**Consequência:** protege contra tentativa em massa sem trancar a conta de ninguém de forma permanente, o que daria a um atacante um jeito de bloquear o Administrador. O Administrador não precisa desbloquear ninguém: o bloqueio expira sozinho. Pelo mesmo motivo, o bloqueio por origem existe ao lado do por e-mail.

## ADR-5: a 01-iam fixa só os papéis; cada feature define o que eles fazem

**Contexto:** as permissões por tela dependem de features que ainda não existem (clientes, agenda, propostas). Defini-las agora seria inventar.

**Decisão:** o IAM define três papéis, **Administrador, Comercial e Técnico**, e que **só o Administrador gerencia usuários** (convidar, mudar papel, desativar, encerrar sessões de outros). Cada feature futura declara no próprio `usecases.md` quais papéis podem cada caso de uso. Um usuário tem **um** papel.

**Consequência:** o IAM não conhece regra de nenhuma outra feature. O custo é que a lista completa de permissões fica espalhada pelos `usecases.md`; a verificação é feita no use case (papel exigido), nunca no controller. Acrescentar um papel é uma decisão nova, não uma extensão deste ADR.

## ADR-6: sessão renova com intervalo mínimo; o banco é a fonte de verdade, o cache é otimização

**Contexto:** a sessão renova a cada uso (ADR-1). Gravar `last_used_at` em toda requisição transformaria cada leitura numa escrita. Ao mesmo tempo, encerrar uma sessão tem de valer na hora.

**Decisão:** *proposta.* A renovação só grava quando passou o **intervalo mínimo de 5 minutos** desde o último uso (`Session.renewIfDue`). A conferência de cada requisição passa pelo cache (Redis, TTL de segurança de 60 segundos), e **a invalidação é por evento**: `SessionRevoked`, `UserDeactivated` e `UserRoleChanged` removem as chaves afetadas depois do commit ([`architecture.md`](../../rules/architecture.md) §6.2).

**Consequência:** a maior parte das requisições não escreve no banco. O "último uso" mostrado nas telas pode estar até 5 minutos atrasado. Se o Redis estiver fora, o `Cache` cai no banco: mais lento, nunca errado. Mudar o intervalo é só configuração.

## ADR-7: o e-mail de convite e de redefinição sai por domain event, depois do commit

**Contexto:** convite e redefinição enviam um link por e-mail. Enviar dentro do `@UseCase` mandaria e-mail de um token cuja gravação ainda pode ser desfeita; e o contrato `EmailSenderService` foi desenhado para ser chamado fora da thread do request ([`architecture.md`](../../rules/architecture.md) §5.1).

**Decisão:** *proposta.* `OneTimeToken.issue` registra o domain event `OneTimeTokenIssued`, que um listener de `iam-infrastructure` consome **depois do commit** e transforma em e-mail. Não é integration event: nenhum outro BC se interessa, e ele carrega o segredo do link, que nunca pode ir ao outbox nem ao broker.

**Consequência:** e-mail que falha não desfaz o convite: o Administrador reenvia (`ResendInvitation`), e quem esqueceu a senha pede de novo. O segredo existe em memória, só dentro desse evento, e nunca é logado. Isto fecha a pendência "o convite vira integration event?" (não vira).

## ADR-8: quem age não muda o próprio papel nem se desativa

**Contexto:** o sistema não pode ficar sem Administrador. Garantir isso entre aggregates (contar Administradores ativos) exigiria uma consulta no meio da transação e ainda assim teria corrida.

**Decisão:** *proposta.* O Administrador **não altera o próprio papel nem desativa a si mesmo**. A regra mora no `User` (`cannot_change_own_role`, `cannot_deactivate_self`). Como quem age é sempre um Administrador ativo que não é o alvo, sempre sobra pelo menos ele.

**Consequência:** a invariante "há pelo menos um Administrador" fica garantida sem consulta entre aggregates. Resta uma corrida rara: dois Administradores se desativando ao mesmo tempo, cada um pela sessão do outro. Fica registrada como risco aceito; se ocorrer, o acesso volta pelo banco (o seeder só cria quando não existe usuário nenhum, então não o recria). Reativar usuário fica fora desta rodada.

## ADR-9: ciclo de vida simples é enum com guardas, sem `StateMachine`

**Contexto:** o `User` tem três estados e transições poucas. A base `StateMachine` de `common-domain` só guarda valor e evento, e nenhum BC a usa ainda, então não há padrão a seguir.

**Decisão:** *proposta.* O `User` usa um **enum** (`UserStatus`) com guardas nos métodos do aggregate, e cada transição inválida é uma `DomainException` com código. A base `StateMachine` fica reservada a ciclos com muitas transições, regras por transição ou um evento por transição.

**Consequência:** o desenho mais simples que cobre o caso. Quando a primeira feature com ciclo complexo aparecer (proposta, visita), ela decide se usa a base ou a substitui, e a skill `crm-domain-rules` registra o que for escolhido.

## ADR-10: usuário desativado pode ser reativado

**Contexto:** o e-mail é único, então quem foi desativado não podia ser convidado de novo. Sem reativação, desativar por engano seria irreversível.

**Decisão:** o Administrador **reativa** um usuário desativado (`ReactivateUser`). O usuário volta ao estado de antes: `ACTIVE` se já tinha senha, `INVITED` se nunca definiu uma (*regra proposta por mim*; nesse caso o Administrador reenvia o convite). As sessões revogadas na desativação **não voltam**: a pessoa entra de novo pelo login.

**Consequência:** `DEACTIVATED` deixa de ser terminal. A tela de reativar não estava nos mockups aprovados e entra em `design/` antes da implementação (regra 7).

## ADR-11: encerrar sessão e desativar usuário não pedem confirmação

**Contexto:** as duas ações são sentidas na hora pela pessoa afetada, e no celular um toque acidental as dispara.

**Decisão:** decisão do usuário: **sem confirmação**. A ação executa ao toque.

**Consequência:** as telas aprovadas valem como estão. Como ambas são reversíveis (a sessão se refaz entrando de novo; o usuário se reativa, ADR-10), o custo de um toque acidental é baixo. Reabrir é um ADR novo.

## ADR-12: a tela de convite não mostra o e-mail nem o papel do convidado

**Contexto:** o mockup mostrava "Você foi convidado como Técnico. Defina uma senha para entrar com rafael@exemplo.com.br". O link do convite traz só o segredo, e `AcceptInvitation` não tem consulta que devolva e-mail e papel.

**Decisão:** decisão do usuário (opção A): o texto passa a ser **"Defina uma senha para entrar."** A tela não consulta o token antes de enviar a senha. O mockup aprovado foi ajustado.

**Consequência:** nenhuma consulta nova no backend, e o link, sozinho, não revela a ninguém quem foi convidado. O convidado só vê um link expirado ou usado depois de enviar a senha. Mostrar e-mail e papel exigiria uma consulta `GetInvitation(token)`: ADR novo.
