# DUNAS: identidade visual

Fonte de verdade da identidade visual. Os valores estão em [`tokens.json`](tokens.json). Toda tela referencia esta identidade e nunca reinventa token (regra 7 de [`docs/CLAUDE.md`](../CLAUDE.md)).

Identidade visual e design system do app da DUNAS, empresa de energia solar. Direção escolhida: **Operação**, uma interface escura de sala de controle, com o laranja da marca como cor de ação.

O app tem dois públicos: a **equipe** (administrador, comercial e o técnico no celular) e o **cliente**, que entra num portal para ver quanto a usina gerou.

## Princípios

1. **O laranja é ação, não texto sobre claro.** Branco sobre `#FF7E00` dá cerca de 2,5:1 e não passa. Sobre o laranja o texto é sempre `on-brand`. Como texto ou traço, use `brand-text`.
2. **Corte, não arredondamento.** O corte diagonal do D do logo vira o canto chanfrado de botões, chips e painéis (`cut-sm`, `cut-md`, `cut-lg`). Não há cantos arredondados grandes.
3. **O número é o herói.** A geração do dia, em Chakra Petch com algarismos tabulares, é o maior elemento da tela do cliente. Um número grande por tela.
4. **A situação se lê sem cor.** Cada situação de uma usina difere em luminosidade e em forma, além da cor: normal (`chip-ok`), alerta (`highlight`, claro) e falha (contorno `danger`, vazado).
5. **Sem decoração.** Sem gradiente, sem sombra decorativa. A curva de geração é um preenchimento chapado em `brand` com 20% de opacidade e um traço de 3px.

## Temas

- **Escuro** (padrão): `ground` `#0B1220`, painéis `#131C2E`.
- **Claro (campo)**: proposta ainda a confirmar com quem trabalha no telhado. Sob o sol, o tema escuro perde legibilidade, e o claro usa os mesmos tokens com valores de luz. A escolha segue o aparelho.

## Cor

Os valores e o uso de cada token estão em `tokens.json`. Pares aprovados:

| Texto | Fundo | Uso |
|---|---|---|
| `text` | `ground`, `panel` | corpo |
| `text-muted` | `ground`, `panel` | rótulos e apoio |
| `on-brand` | `brand` | botão primário |
| `on-highlight` | `highlight` | chip de alerta |
| `brand-text` | `ground`, `panel` | valor em destaque, traço de gráfico |
| `danger-text` | `ground`, `panel` | falha de comunicação |

## Tipografia

- **Chakra Petch** (títulos e números), pela afinidade com o chanfro do logo. **IBM Plex Sans** (texto), por legibilidade em dados.
- Ambas são do Google Fonts. Auto-hospedar as fontes no frontend é decisão pendente.
- Números sempre com `font-variant-numeric: tabular-nums`.
- Linhas de texto abaixo de 80 caracteres. Nada abaixo de 12px.

## Forma e espaço

- Escala de espaço de 4px (`space-1` a `space-6`).
- Painel principal com corte `cut-lg`; botão e chip com `cut-sm`.
- Alvo de toque mínimo de 44px.

## Logo

- Dois arquivos em `docs/assets/` (`logo.jpeg` e `logo-nome.PNG`): o **ícone** (quadrado laranja com o D branco) e o **nome**.
- Não recolorir, não distorcer, não aplicar efeito. Ícone mínimo de 24px.
- No tema escuro use o **ícone**. O arquivo do nome tem fundo branco opaco; falta uma versão com fundo transparente para usá-lo em qualquer fundo.

## Voz e conteúdo

- Português do Brasil, frases em minúscula inicial de frase, sem caixa alta decorativa.
- Verbos diretos nos botões: "Agendar visita", "Novo cliente".
- Valor em reais derivado da geração é sempre marcado como **estimativa**.
- Erro diz o que houve e o que fazer, sem pedir desculpa.

## Componentes

Botão, chip de situação e painel são as primeiras peças, com previews no design system publicado: <https://claude.ai/artifact/GTX4NXatx8Krd7FSqZRoxu>. Telas de referência (direção C, equipe e cliente): o canvas <https://claude.ai/artifact/UrnpFViMxmZjGWUdMfpsuj>. Quando o frontend existir, o código dos componentes passa a ser a fonte, e esta seção aponta para ele.
