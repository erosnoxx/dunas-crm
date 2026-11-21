# design/: 02-catalog

Artefatos visuais de validação de UX/UI desta feature.

**Regra 6** ([`docs/CLAUDE.md`](../../../CLAUDE.md)): antes de fechar `domain-model.md` e `usecases.md`, é obrigatório gerar um artefato visual aqui, com descrição do que ele representa e **quais decisões de UX ele ajudou a validar**.

**Regra 7**: nenhuma tela, nova ou redesenhada, é implementada sem mockup previamente aprovado aqui. A implementação final é **pixel perfect** em relação a ele, e termina com comparação explícita mockup × resultado.

## Artefatos

Canvas com 13 telas na identidade DUNAS (tema escuro, [`tokens.json`](../../../design-system/tokens.json)): <https://claude.ai/artifact/DvPCMAmQrPqX995ZFdc44U>. O código-fonte das telas está em [`canvas/project/`](canvas/project/) (um `.dc.html` por tela e o `canvas.json`). **Status:** telas 1 a 13 aprovadas pelo usuário em 2026-11-21.

| # | Tela | O que valida | Status |
|---|---|---|---|
| 1 | Modelos, web | Menu "Catálogo" com três abas (Modelos, Fabricantes, Tipos); lista com busca, filtro por tipo e por situação; característica na tabela com a unidade (kW, BTU/h); situação por forma (círculo cheio ou vazado), além da cor | aprovado |
| 2 | Modelos, celular | Os mesmos dados em cartões; abas em controle segmentado; botão "Novo modelo" fixo acima da barra inferior | aprovado |
| 3 | Novo modelo, web | Fabricante, tipo e nome; o campo da característica e a unidade vêm do tipo escolhido | aprovado |
| 4 | Novo modelo, celular, com erro | O campo muda para Capacidade (BTU/h) no ar-condicionado; erro "este fabricante já tem um modelo com esse nome" com ícone e texto, não só cor | aprovado |
| 5 | Modelo, web | Fabricante e tipo somente leitura depois de salvo (ADR-4); painel de situação com "Desativar modelo", sem confirmação (ADR-5) | aprovado |
| 6 | Modelo desativado, celular | Estado desativado, dados somente leitura e ação única "Reativar modelo" | aprovado |
| 7 | Fabricantes, web | Lista com número de modelos e painel lateral "Novo fabricante" só com o nome | aprovado |
| 8 | Fabricantes, celular | Cartões com número de modelos e situação | aprovado |
| 9 | Tipos, web | Lista com característica e unidade; painel "Editar tipo" com a unidade travada e o aviso do motivo (ADR-4) | aprovado |
| 10 | Tipos, celular | Cartões de tipo com a característica e a unidade | aprovado |
| 11 | Novo tipo, celular | Nome do tipo, nome da característica, unidade (lista fechada) e obrigatoriedade | aprovado |
| 12 | Modelos, celular, Técnico | Sem botões de edição e com o aviso de que o papel só consulta (ADR-6); barra inferior com um item | aprovado |
| 13 | Modelos, celular, sem resultado | Estado vazio da busca, com "Limpar busca e filtros" e o botão de cadastrar | aprovado |

## Decisões de UX que as telas deixam aprovadas

- Seção **Catálogo** com três abas, e não três itens separados no menu.
- **Barra de navegação inferior** no celular (Catálogo e Usuários). É mudança na casca do app, que hoje só tem barra superior: ver [`open-questions.md`](../open-questions.md).
- Fabricante e tipo do modelo **não mudam** depois de salvos; a **unidade da característica trava** quando o tipo já tem modelos.
- Desativar e reativar **sem confirmação**; desativar não propaga (ADR-5).
- Administrador e Comercial editam, Técnico só consulta (ADR-6).
- Fabricante só tem nome. **Sem** ficha técnica, foto, contagem de unidades instaladas nem histórico (ADR-8).
- Unidades disponíveis: kW, W, Wp e BTU/h.

## O que o mockup não mostra e a implementação precisa decidir sem inventar

- **Obrigatória travada:** o mockup só mostra a unidade travada; a caixa "Obrigatória" também trava quando liga de `false` para `true` com modelos (ADR-4). A tela deve desabilitá-la com o mesmo aviso, e isso entra na comparação como divergência sinalizada.
- Só o tema escuro foi desenhado; o tema claro de campo segue a confirmar ([`README`](../../../design-system/README.md)).
- Dados de exemplo: o modelo SUN2000-3KTL-L1 é o do cadastro real; os demais nomes e valores são ilustrativos. O design system DUNAS não foi instalado no canvas: os tokens foram aplicados à mão a partir do `tokens.json`.
