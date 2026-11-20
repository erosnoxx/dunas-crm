# design/: 01-iam

Artefatos visuais de validação de UX/UI desta feature.

**Regra 6** ([`docs/CLAUDE.md`](../../../CLAUDE.md)): antes de fechar `domain-model.md` e `usecases.md`, é obrigatório gerar um artefato visual aqui, com descrição do que ele representa e **quais decisões de UX ele ajudou a validar**.

**Regra 7**: nenhuma tela, nova ou redesenhada, é implementada sem mockup previamente aprovado aqui. A implementação final é **pixel perfect** em relação a ele, e termina com comparação explícita mockup × resultado.

## Artefatos

Canvas com 12 telas na identidade DUNAS (tema escuro, [`tokens.json`](../../../design-system/tokens.json)): <https://claude.ai/artifact/CvVeA26TCvv9dBdXWwZ4eU>. **Status:** telas 1 a 12 aprovadas pelo usuário em 2026-11-19 (as telas 11 e 12, de usuário desativado, vêm do ADR-10).

| # | Tela | O que valida | Status |
|---|---|---|---|
| 1 | Entrar, web | Login só por e-mail e senha, sem "criar conta" (acesso é por convite) | aprovado |
| 2 | Entrar, celular, senha errada | Mensagem única "e-mail ou senha incorretos", igual para e-mail inexistente (ADR-4) | aprovado |
| 3 | Entrar, celular, bloqueado | Aviso de pausa de 15 minutos, campos e botão desligados, "Esqueci minha senha" continua disponível (ADR-4) | aprovado |
| 4 | Convite, definir senha | Texto "Defina uma senha para entrar." (ADR-12). Checklist das 5 regras da senha (ADR-3) com estado por forma e não só por cor; contador de 30; botão só libera com tudo atendido. A troca obrigatória do primeiro Administrador (ADR-2) reaproveita esta tela, com outro título | aprovado |
| 5 | Convite, link expirado | Estado de erro do link (`link_expired`); o mesmo desenho serve ao link já usado, com outro texto | aprovado |
| 6 | Minhas sessões, celular | Lista de aparelhos, "Esta sessão" marcada, encerrar uma ou todas as outras, sair deste aparelho (ADR-1) | aprovado |
| 7 | Usuários, web | Lista com papel, situação (Ativo, Convite pendente, Desativado) e último uso; painel do usuário com sessões e encerramento remoto (ADR-1, ADR-5) | aprovado |
| 8 | Convidar usuário, web | Nome, e-mail e um papel entre três (ADR-5) | aprovado |
| 9 | Usuários, celular | O Administrador gerencia pelo celular, com o convite sempre ao alcance | aprovado |
| 10 | Usuário e sessões, celular | Encerrar sessão de um celular perdido, de qualquer lugar (ADR-1) | aprovado |
| 11 | Usuário desativado, web | Estado de quem foi desativado no painel: sem sessões, texto dizendo que não consegue entrar, ação única "Reativar usuário" (ADR-10) | aprovado |
| 12 | Usuário desativado, celular | O mesmo, no celular, com a ação fixa no rodapé (ADR-10) | aprovado |

## Decisões de UX que as telas deixam para aprovar

- Situação do usuário se lê por **forma** (círculo cheio, vazado, cortado) além da cor.
- Encerrar sessão e desativar usuário usam texto `danger-text` e **não pedem confirmação** (ADR-11).
- Só o tema escuro foi desenhado; o tema claro de campo segue a confirmar ([`README`](../../../design-system/README.md)).
- Dados de exemplo; nenhum nome ou e-mail é real.
- As 14 telas complementares (13 a 26), aprovadas, com as cópias novas: [`telas-complementares.md`](telas-complementares.md).
- Comparação mockup × resultado, com as divergências e o que fica para o usuário decidir: [`comparacao-mockup.md`](comparacao-mockup.md).
