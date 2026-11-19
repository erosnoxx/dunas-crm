# <Tema> — brainstorming

> Template. Copie para `YYYY-MM-DD-<tema>.md` e apague estas linhas de instrução ao preencher.
>
> **Nada aqui é spec nem requisito.** Ideia não é verdade: código, skill e plano nunca citam este documento como fonte. Uma ideia só vira trabalho quando é **promovida** para uma feature em [`docs/system-design/`](../system-design/00-overview.md), com aprovação do dono. As regras desta pasta estão em [`docs/CLAUDE.md`](../CLAUDE.md).

**Data:** AAAA-MM-DD · **Origem:** quem trouxe o tema (dono, cliente, outra pessoa, pesquisa) · **Estado do documento:** aberto / arquivado

## Tema

Uma ou duas frases: o que estamos pensando e por quê agora.

## Restrições que valem para tudo

Não repita as decisões globais: referencie [`00-overview.md`](../system-design/00-overview.md), seção "Decisões globais". Liste aqui só o que é **específico deste tema** (prazo, orçamento, integração que já existe, dependência de terceiro).

Ideia que fere uma restrição **não é apagada**: recebe a marca `fere: <restrição>` na tabela, e reabrir a restrição é decisão do dono.

## Ideias

Sem filtro de custo ou de viabilidade ao anotar: o filtro vem depois, nas colunas e no status.

| ID | Ideia | Origem | Valor esperado | Custo | Risco principal | Status |
|---|---|---|---|---|---|---|
| I1 | uma frase, no vocabulário do negócio | quem sugeriu | o que melhora e para quem | baixo / médio / alto | o que mais provavelmente dá errado | ideia |

**Status:** `ideia` (anotada, ninguém avaliou) → `em avaliação` → `aceita` (vira feature: link para `system-design/NN-...`) · `adiada` (boa, não agora, com o gatilho para voltar) · `descartada` (com o motivo, para não ser proposta de novo).

## Fatos e hipóteses

Separar o que se **sabe** (e de onde) do que se **supõe**. Hipótese nunca é escrita como certeza em nenhum outro documento.

| ID | Afirmação | Tipo | Fonte / como verificar |
|---|---|---|---|
| I1 | o que se afirma | fato / hipótese | de onde veio, ou o que fazer para confirmar |

## Perguntas abertas

O que precisa ser respondido para decidir, e **quem** ou **o que** destrava cada uma. Pergunta sem dono e sem gatilho é só uma anotação.

## Descartadas

Ideia, motivo, data. Fica registrada para a mesma ideia não voltar sem resposta ao motivo.

## Promovidas

Ideia → feature criada (link para a pasta em `system-design/`). A spec passa a ser a fonte de verdade: este documento deixa de ser atualizado para aquela ideia.
