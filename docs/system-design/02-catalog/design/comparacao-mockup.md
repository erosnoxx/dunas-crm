# Comparação mockup × resultado: 02-catalog

Etapa de comparação da regra 7 ([`docs/CLAUDE.md`](../../../CLAUDE.md)): cada tela do catálogo, em 390 px (celular) e 1280 px (web), contra os 13 artboards aprovados do canvas ([`README.md`](README.md)), na cobertura do plano ([`2026-11-21-catalog-frontend.md`](../../../plans/2026-11-21-catalog-frontend.md), "Cobertura dos mockups"). Feita em 2026-11-21, com o frontend servido pelo servidor de desenvolvimento do Vite e ligado ao backend real do catálogo (Postgres, Redis e um coletor de e-mail locais, descartáveis).

Nada aqui é aceito em silêncio: o que diverge está nas seções 4 a 7, **para o usuário decidir**. Os defeitos achados estão na seção 3, corrigidos e medidos de novo no navegador, com o valor de antes e o de depois.

## 1. Como foi comparado

- **Mockup: renderizado e medido no mesmo navegador.** Os artboards do catálogo (`canvas/project/*.dc.html`) são HTML estático, sem modelo `sc-for` nem lógica (o `renderVals` de todos devolve `{}`): abertos direto do arquivo no Chrome, com as fontes do próprio artboard (Chakra Petch 600/700 e IBM Plex Sans 400/600 carregadas, conferido em `document.fonts`), na largura do artboard (390×844 e 1280×820). Só o logo (arquivo interno do artefato) não carrega. Diferente do IAM, aqui o mockup **foi medido**, não só lido.
- **Resultado: medido num navegador real.** Chrome headless dirigido pelo DevTools protocol abriu cada tela, nos estados do mockup, em 390×844 (emulação de celular, barra de rolagem sobreposta) e 1280×800. A mesma rotina rodou nos dois lados: de cada elemento com texto próprio, campo, botão ou contêiner (`header`, `nav`, `form`, `aside`, `table`), `getComputedStyle` (fonte, tamanho, altura de linha, peso, cor, fundo, borda, respiro, espaço, `clip-path`) e `getBoundingClientRect`. Os elementos foram pareados pelo texto (ou pelo rótulo do campo) e as diferenças listadas propriedade a propriedade. As cores computadas foram lidas de volta para o nome do token (os artboards usam os mesmos hexadecimais do `tokens.json`).
- **Visual: as capturas do resultado foram olhadas**, e as do chip de situação foram ampliadas 6× lado a lado com o artboard. Nenhuma comparação pixel a pixel.
- **Teclado:** em cada tela e tamanho, `Tab` até dar a volta, anotando a ordem, o anel de foco computado e a altura do alvo.
- **Fluxos:** exercidos de verdade na tela (cliques e digitação reais), contra o backend, e conferidos pela API (seção 9).
- **Nova medição, depois das correções da seção 3:** a mesma rotina (Chrome headless pelo DevTools protocol, `getComputedStyle` e `getBoundingClientRect`, capturas ampliadas do chip), com o backend de `develop` e Postgres, Redis e coletor de e-mail descartáveis, os dados com os nomes do mockup, em 390, 768, 1024 e 1280. Cada item foi medido duas vezes na mesma sessão, com o código antes e depois da correção; os valores estão na seção 3 e na seção 10.

Dados de teste: criados pela API com os nomes do próprio mockup (Huawei, Fabricante B, Fabricante C desativado; Inversor com Potência em kW e Ar-condicionado com Capacidade em BTU/h; SUN2000-3KTL-L1, -5KTL-L1, -8KTL-M1 desativado, Split Inverter 12.000 e 18.000 BTU), para os textos coincidirem; os fluxos criaram outros (Sungrow, Painel solar, Bateria, SG5.0RS, ...). Usuários pelo fluxo do IAM: o Administrador semeado (troca a senha inicial) e um Técnico convidado, com o convite lido no coletor de e-mail e aceito. O avatar e o nome da conta diferem do mockup ("M", "Marina Costa").

Onde o mockup 1 e o 2 mostram um modelo desativado com o filtro "Ativos", o resultado foi medido com "Todos" (com "Ativos" a lista real não traz o desativado).

## 2. Corrigido depois da comparação

Os cinco defeitos da seção 3, o alvo de 18 px do "Editar" na tabela de tipos (seção 8) e as duas larguras de 768 px que rolavam a página inteira (seção 10). Todos foram medidos de novo no navegador; todos, menos o `relative` da lista de tipos (seção 10), têm teste de tela ou de componente. As divergências das seções 5 a 7 não mudaram.

## 3. Defeitos encontrados (corrigidos)

Medidos no navegador antes e depois da correção, na mesma sessão e com os mesmos dados.

| # | Defeito | Onde | Antes (medido) | Correção | Depois (medido) |
|---|---|---|---|---|---|
| D1 | O círculo **cheio** do chip "Ativo" saía como um **quadrado de cantos arredondados** | todo `StatusChip` de tom `ok` (catálogo **e** IAM: "Ativo" da lista de usuários) | `circle r=5` com traço de 1,6 numa caixa de 10×10: o traço saía da caixa e o SVG cortava as bordas; captura ampliada 6×: quadrado | raio 4 em todas as formas (o vazado e o cortado já usavam 4); teste confere que raio e traço cabem na caixa | `r=4` em 390 e 1280 (modelos, fabricantes, tipos, modelo, usuários do IAM); capturas ampliadas: **círculo**, no catálogo e no IAM. O vazado e o cortado não mudaram |
| D2 | Botões principais da **web** em peso **400**; o mockup é 600 | "Salvar modelo", "Salvar alterações" (modelo, fabricante, tipo), "Salvar fabricante", "Salvar tipo", "Reativar modelo", "Reativar fabricante", "Reativar tipo" | 14/20 **400** em todos os nove (o "Reativar tipo" medido agora: 400 também) | `md:font-semibold` no `WEB_BUTTON` de `panel-frame.tsx` e de `model-form.tsx`: o `md:text-body-sm` é variante de mídia, sai depois do `font-semibold` e trazia o peso 400 do token | 14/20 **600** nos nove; altura 44 mantida |
| D3 | Nome na linha da **tabela web** em peso **400**; o mockup é 600 | lista de modelos (coluna Modelo) e de fabricantes (coluna Fabricante) | 14/20 **400** | a mesma causa de D2: `md:font-semibold` em `model-row.tsx` e `manufacturer-row.tsx` | 14/20 **600**; no celular o nome do cartão segue 16/24 600 |
| D4 | Na tabela web de **fabricantes**, o chip de situação **esticava** até a largura da coluna e o "Desativado" ficava **quebrado** (bloco `line-strong` sobrando à direita) | `/catalogo/fabricantes` na web | chip de 158,8 px nos dois ("Ativo" e "Desativado") | o chip ganha um `span` com `md:justify-self-start`, como na `ModelRow` | "Ativo" **73** px, "Desativado" **111,4** px com o rótulo de 109,4 dentro (contorno de 1 px em volta, sem sobra); o mockup tem 65 e 105 (diferença do respiro do `StatusChip`, seção 5). Cartões do celular iguais (74 px, chip à direita) |
| D5 | O **cabeçalho** das tabelas web de modelos e de fabricantes **não alinhava** com as colunas das linhas | `/catalogo` e `/catalogo/fabricantes` na web | fabricantes: desvios de 0, 17,7 e 26,6 px ("Modelos" 541,5 × número 523,8; "Situação" 679,8 × chip 653,2). Modelos: 0, 5,3, 11,2, 19,8 e 27,4 px ("Situação" 1072,4 × chip 1045) | o cabeçalho leva na última coluna (`auto`) o "Abrir"/"Editar" da linha, invisível e sem altura: a coluna e as frações ficam iguais nos dois (o Técnico, sem "Editar", fica com a coluna vazia nos dois) | **0 px** em todas as colunas das duas tabelas; as linhas também coincidem entre si (cinco modelos, três fabricantes); cabeçalho com os mesmos 43 px |

D1 também afeta o IAM (componente compartilhado): a lista de usuários, que a comparação do IAM deu como igual, passa a ter o círculo.

## 4. Tela a tela

Legenda do status: ✅ os valores medidos coincidem com os do artboard; ⚠️ coincidem, salvo divergências aprovadas, aproximações sem token ou divergências para o usuário decidir (seções 5 a 7); ❌ tem defeito da seção 3. "Derivada" = sem artboard no tamanho: comparada com o artboard mais próximo e listada na seção 7. As linhas que eram ❌ só por D1 a D5 passaram a ⚠️ com a nova medição; "(era ❌: D…)" diz qual defeito saiu.

Valores comuns, medidos iguais ao mockup em todas as telas onde aparecem, e não repetidos nas linhas: título "Catálogo" 36/40 peso 700 (web); barra superior do celular 64 px `nav` com borda `line` e título 24/28; abas da web 14 peso 600 `text-muted`, ativa em `brand-text` com traço de 3 px, faixa de 47 px com linha `line`; controle segmentado do celular 3 × 44 px com contorno `line-strong`, ativa em `chip-ok`; rótulos 13/18 peso 600; busca e filtros 44 px `panel` com borda `line-strong` nas listas; campos dos formulários 44 px `ground` na web e 48 px `panel` no celular (variante `compact`, aprovada); cartões do celular `panel` com borda `line`, respiro 14/16, nome 16 peso 600, apoio 14/20 `text-muted`, característica 16 peso 600 Chakra Petch; botão principal da web 44 px corte 10, do celular 48 px corte 12 no rodapé; barra inferior `nav` com borda `line`, rótulo 12/16 peso 600, ativo em `brand-text`.

### Modelos: lista, vazio, sem resultado

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 1280 | 1 `Main` | filtros (busca, Tipo 200, Situação 180, "Novo modelo" 44 corte 10 peso 600); tabela `panel`, cabeçalho 13/18 600 `text-muted`, linhas com borda `line`, "Abrir" 14 peso 600 `brand-text` | ⚠️ (era ❌: D1, D3, D5) | nome do modelo 600, círculo, cabeçalho alinhado (0 px); característica 16 (mockup 14), "12000" (mockup "12.000"), chip com corte, seta do lucide, linha 56 px (mockup 57 com a borda), rodapé de paginação no lugar da nota "5 modelos." (seção 5) |
| 390 | 2 `Modelos-celular` | controle segmentado, busca 44, filtros 44 lado a lado em 14 px, cartões 112 px, "Novo modelo" 48 fixo acima da barra inferior, barra com 2 itens | ⚠️ (era ❌: D1) | chip com corte, "12000", rótulo acessível da barra "Principal no celular", item da barra 63 px (mockup 64), "Mostrando 5 de 5 modelos" e "Carregar mais" sob os cartões (seção 5) |
| 390 | 13 `Modelos-sem-resultado-celular` | busca "XPTO-9000"; painel `panel` com ícone, título, texto 14/20 `text-muted` com a busca entre aspas, "Limpar busca e filtros" 44 px `brand-text`; "Novo modelo"; barra inferior | ⚠️ | título 24/28 (mockup 20/24, sem token) |
| 1280 | derivada (13) | sem resultado na web | ⚠️ | o painel do 13 centralizado na largura da lista, título 24/28 |
| 390 e 1280 | derivada (13) | catálogo sem modelos | ⚠️ | "Nenhum modelo encontrado" e "O catálogo ainda não tem modelos ativos. Cadastre o primeiro.", sem "Limpar" (seção 6) |

### Modelos: visão do Técnico

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 390 | 12 `Modelos-tecnico-celular` | aviso `chip-ok` 14/20 com ícone "Seu papel (Técnico) consulta o catálogo..."; cartões; sem "Novo modelo"; barra inferior com um item na largura toda (390 px) | ⚠️ (era ❌: D1) | os filtros Tipo e Situação aparecem (o 12 não os tem); avatar da casca (seção 5) |
| 1280 | derivada (1, 12) | lista com o aviso, sem "Novo modelo" | ⚠️ (era ❌: D1, D3, D5) | as divergências da lista do Administrador (seção 5) |

### Novo modelo

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 1280 | 3 `Novo-modelo-web` | "Modelos" de volta 14 peso 600, 44 px; título 36/40; painel `panel` até 640, respiro 24, espaço 20; Fabricante, Tipo e Nome 44 `ground`; seção "Características do tipo Inversor" com linha `line` e título 13/18 `text-muted`; "Potência *" com sufixo "kW" `chip-ok` 14 peso 600; "Cancelar" `chip-ok` e "Salvar modelo" 44 corte 10 à direita | ⚠️ (era ❌: D2) | "Salvar modelo" 600; dicas 12/16 peso 500 (mockup 400), seta do lucide |
| 390 | 4 `Novo-modelo-celular` | Fabricante B, Ar-condicionado e nome repetido, enviado ao backend real: campo com borda 2 px `danger` e respiro 11, mensagem 14/20 `danger-text` com ícone; "Capacidade *" com "BTU/h"; "Salvar modelo" 48 no rodapé `nav` | ⚠️ | texto do erro: a frase da tabela de erros, minúscula e sem "Abra o existente ou use outro nome." (seção 5) |
| 390 | derivada (4) | inversor: "Potência *" e "kW" | ⚠️ | valores do 4 |
| 1280 | derivada (3, 4) | o erro do nome na web | ⚠️ (era ❌: D2) | o erro do 4 no painel de 640 |

### Modelo ativo (editar, desativar)

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 1280 | 5 `Modelo-web` | formulário 640 e situação 340 lado a lado (espaço 24); Fabricante e Tipo só leitura 44 `nav` com borda `line` e texto `text-muted`; dica "Fabricante e tipo não mudam..."; painel "Situação" com chip, texto 14/20 e "Desativar modelo" 14 peso 600 `danger-text` 44 px | ⚠️ (era ❌: D1, D2) | "Salvar alterações" 600, círculo; a dica "O campo e a unidade vêm do tipo escolhido..." aparece sob a Potência (o 5 não a tem; formulário 22 px mais alto) |
| 390 | derivada (4) | Fabricante e Tipo só leitura 48 px, painel de situação `panel` respiro 16 com "Desativar modelo", "Salvar alterações" no rodapé | ⚠️ (era ❌: D1) | derivada (seção 7) |
| 390 e 1280 | derivada (5, 6) | visão do Técnico: só leitura, sem "Salvar", "Cancelar" nem "Desativar" | ⚠️ (era ❌: D1) | derivada (seção 7) |

### Modelo desativado

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 390 | 6 `Modelo-desativado-celular` | chip vazado; painel `panel` respiro 16 espaço 14, rótulos 13/18 `text-muted`, valores 16 (Potência em Chakra Petch 600); nota 14/20; "Reativar modelo" 48 no rodapé | ⚠️ | título 24/28 (mockup 22/28, sem token); chip com corte |
| 1280 | derivada (5, 6) | dados em lista (rótulo `text-muted`, valor `text`), painel de situação com o chip, a nota e "Reativar modelo" | ⚠️ (era ❌: D2) | "Reativar modelo" 600; derivada (seção 7) |

### Fabricantes

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 1280 | 7 `Fabricantes-web` | lista + painel 340 "Novo fabricante" (título 20/24 Chakra Petch 600, "Nome *" 44 `ground`, dica, "Salvar fabricante" 44 corte 10 à esquerda); colunas Fabricante, Modelos, Situação | ⚠️ (era ❌: D1 a D5) | nome 600, chip de 73 e 111,4 px sem esticar, cabeçalho alinhado (0 px), "Salvar fabricante" 600; número 16 (mockup 14), chip com corte e respiro do `StatusChip` (mockup 65 e 105), dica peso 500, ordem alfabética (seção 5) |
| 390 | 8 `Fabricantes-celular` | cartões 74 px (64 de mínimo) com nome, "3 modelos" / "Nenhum modelo" e chip à direita; "Novo fabricante" 48 | ⚠️ (era ❌: D1) | chip com corte |
| 390 | derivada (11) | "Novo fabricante" em tela cheia: campo 48, dica, "Salvar fabricante" no rodapé | ⚠️ | dica peso 500 |
| 1280 | derivada (7) | "Editar fabricante" no painel, "Cancelar", seção "Situação" com "Desativar fabricante" `danger-text`; desativado: nome só leitura `nav`, nota e "Reativar fabricante" | ⚠️ (era ❌: D1 a D5) | "Salvar alterações" e "Reativar fabricante" 600; a lista ao lado como a do 7 |
| 390 | derivada (11) | editar e desativado em tela cheia | ⚠️ (era ❌: D1) | derivada (seção 7) |

### Tipos

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 1280 | 9 `Tipos-web` (editar Inversor) | `table` de verdade, respiro 12/14, linhas 56; painel 360 "Editar tipo: Inversor"; seção "Característica"; Unidade desligada `nav` com borda `line` e aviso com cadeado 12/16; caixa 20 px; "Salvar alterações" e "Desativar tipo" `danger-text` | ⚠️ (era ❌: D1, D2) | "Salvar alterações" 600, círculo, "Editando" com alvo de 44 px (seção 8); linha em edição sem o fundo `chip-ok`, caixa "Obrigatória" desligada, dicas peso 500, "Ar-condicionado" quebra em duas linhas (seção 5) |
| 1280 | derivada (9) | `/catalogo/tipos`: painel "Novo tipo" vazio, "Escolha a unidade", caixa marcada | ⚠️ (era ❌: D1, D2) | "Salvar tipo" 600, círculo na tabela; "Reativar tipo" (tipo desativado) 600 |
| 390 | 10 `Tipos-celular` | cartões com tipo, "Característica: Potência (kW)", "3 modelos" em Chakra Petch e chip; "Novo tipo" 48 | ⚠️ (era ❌: D1) | chip com corte |
| 390 | 11 `Novo-tipo-celular` | Painel solar, Potência de pico, Wp: campos 48 `panel`, dicas, linha da seção, caixa 22 px com rótulo 16, "Salvar tipo" 48 no rodapé | ⚠️ | dicas peso 500, seta do lucide |
| 390 | derivada (11) | tipo com modelos: Unidade desligada com o aviso, "Obrigatória" desligada, "Desativar tipo", "Salvar alterações" no rodapé | ⚠️ | "Obrigatória" travada (seção 5) |

### Navegação

| Tamanho | Artboard | Comparado | Status | Resultado |
|---|---|---|---|---|
| 1280 | 1, 3, 5, 7, 9 | barra lateral: item "Catálogo" 44 px `chip-ok` corte 10, ícone de caixa 20, 14 peso 600, antes de "Usuários"; o Técnico vê só "Catálogo" | ✅ | texto 14/20 (o mockup herda 24: mesma altura de 44) |
| 390 | 2, 8, 10, 12, 13 | barra inferior: Administrador com Catálogo e Usuários (195 px cada), Técnico só Catálogo (390 px); ícone 22; rótulo 12/16 **peso 600** (medido: o `font-semibold` vence o peso 500 do `text-caption`); some nas telas com cabeçalho próprio (formulários, modelo) | ⚠️ | rótulo acessível "Principal no celular"; item 63 px (seção 5) |
| ambos | 1 a 13 | avatar e bloco da conta | ⚠️ | avatar da casca do IAM, não o redondo `chip-ok` do mockup do catálogo (seção 5) |

Contagem: 29 linhas (13 artboards no tamanho deles, 13 estados derivados, 3 de navegação), a partir de 40 combinações tela × tamanho medidas no navegador: **1 ✅, 28 ⚠️, 0 ❌** (na comparação, antes das correções: 1 ✅, 11 ⚠️, 17 ❌, todos os ❌ por D1 a D5). Os ⚠️ ficam para a decisão do usuário (seções 5 a 7).

## 5. Divergências (decisão do usuário)

Aprovadas antes da implementação ([`README.md`](README.md), "Divergências do mockup aprovadas"): campo `compact` de 44 px na web e 48 no celular (medido: confere), `NativeSelect` e `Checkbox` simples, `BottomNav` na casca, `CatalogTabs` com links e o `Page<T>` em `shared/api`. As de baixo **não** estão aprovadas.

### Comportamento

| Divergência | Mockup ou spec | Resultado |
|---|---|---|
| Caixa "Obrigatória" travada **nos dois sentidos** quando o tipo tem modelos | o mockup 9 só trava a unidade; o backend recusa só **ligar** `required` (ADR-4, `characteristic_locked`) | a tela desliga a caixa nos dois sentidos, com o aviso da unidade; desligar a obrigatoriedade de um tipo com modelos, que o backend aceita (seção 9), não é possível pela tela |
| Filtros na visão do Técnico | o mockup 12 tem só o aviso e a busca | o Técnico vê também Tipo e Situação |
| Valores sem separador de milhar | "Capacidade 12.000 BTU/h" (mockups 1, 2, 12) | "Capacidade 12000 BTU/h" (`formatAmount`, como o plano manda: o campo recusa o ponto, e a lista escreve como se digita) |
| Sem aviso de sucesso | — | salvar, desativar e reativar não mostram aviso (o IAM usa toast em "Senha alterada"); a tela muda sozinha (o chip, o botão, a lista) |
| Busca sem espera | — | cada tecla na busca de modelos pede ao servidor (sem *debounce*) |
| Listas de fabricantes e de tipos sem paginação | o mockup 7 mostra o desativado na mesma tabela | as duas listas pedem uma página de 100, com os desativados (`status=ALL`), e param aí, em silêncio |
| Seleções com até 100 opções | — | fabricante e tipo no "Novo modelo" e o filtro de tipo carregam os 100 primeiros; num tipo além deles, a edição do modelo perde o campo do valor (atalho registrado no plano) |
| Paginação da lista de modelos | o mockup 1 fecha a tabela com a nota "5 modelos."; o 2 não mostra rodapé | web: "Mostrando 1 a 5 de 5 modelos" com Anterior e Próxima, como o IAM; celular: "Mostrando 5 de 5 modelos" e "Carregar mais" |
| Linha em edição | o mockup 9 pinta a linha do tipo aberto em `chip-ok` (e o chip em `ground`) | só o texto "Editando" e `aria-current` (o `StatusChip` não tem a variante) |
| Novo tipo | o mockup 11 mostra exemplo preenchido, unidade "Wp" escolhida | abre vazio, com "Escolha a unidade" e a caixa marcada |
| Edição concorrente | — | `concurrent_update` sai no aviso com "tente de novo", sem botão "Recarregar" |
| Rótulo acessível da barra inferior | `aria-label="Principal"` (mockup e plano) | "Principal no celular": o jsdom dos testes não aplica `md:hidden`, e duas navegações "Principal" quebravam testes do IAM |
| Voltar do celular | 44 px, rótulo "Voltar para Modelos" / "Voltar para Tipos" | o cabeçalho compartilhado (`MobileHeader`, do IAM): 48 px, "Voltar" |
| Texto do erro de nome repetido | "Este fabricante já tem um modelo com esse nome. Abra o existente ou use outro nome." | "este fabricante já tem um modelo com esse nome" (a frase da tabela de erros de `usecases.md`), com o mesmo ícone e cor |
| Dica do campo da característica na edição | o mockup 5 não tem dica sob a Potência | "O campo e a unidade vêm do tipo escolhido..." aparece também na edição, onde o tipo não muda |

### Forma (valores sem token ou diferentes do mockup)

| Valor do mockup | Onde | Usado |
|---|---|---|
| chip de situação sem corte, respiro 3/10, espaço 6, glifo de texto (● ○) | listas, modelo, fabricante | `StatusChip` do IAM: corte 10, respiro 0/12, espaço 8, glifo SVG (círculo de raio 4 desde D1); desativado em duas camadas (contorno `line-strong` + fundo `panel`, 28 px; o mockup 30 com borda e fundo transparente). Larguras medidas na tabela de fabricantes: 73 e 111,4 px (mockup 65 e 105) |
| característica e número 14 px Chakra Petch 600 | coluna Característica (modelos) e Modelos (fabricantes) | `text-number` 16/24 |
| título do vazio 20/24 Chakra Petch 600 | sem resultado (13) | `text-title` 24/28 |
| título 22/28 | modelo desativado no celular (6) | `text-title` 24/28 |
| título do painel 20/24 Chakra Petch 600 | painéis de fabricante e tipo na web | `font-display text-xl leading-6 font-semibold` (fora da escala; pedir um token) |
| texto 14 peso 600 | abas, controle segmentado, "Abrir", "Editar", sufixo da unidade, botões da web | `text-body-sm` + `font-semibold` (altura de linha 20, mockup 24 herdado; abas com respiro 12 para manter 47 px). Nos botões e no nome da tabela, `md:text-body-sm` com `md:font-semibold` (D2, D3) |
| dicas 12/16 peso 400 | todos os formulários | `text-caption` 12/16 **peso 500** |
| seta nativa da seleção | filtros e formulários | `ChevronDownIcon` do lucide, respiro direito 40 |
| filtros do celular 14 px | Tipo e Situação no celular (2, 13) | `text-body-sm` 14/20 em campo de 44 (o mesmo valor do mockup, fora das alturas de campo) |
| barra inferior de 64 px + 1 de borda | celular | 64 px com a borda: o item fica 63 |
| linha da tabela 56 px + 1 de borda | tabelas de modelos e fabricantes | 56 px com a borda |
| avatar redondo `chip-ok`, inicial 18 peso 600 IBM Plex Sans | barra superior e barra lateral | o da casca do IAM, aprovado lá: quadrado `brand` com corte, inicial 18 peso 700 Chakra Petch; links da conta todos de 44 px (o mockup tem 44, 34 e 34) |
| larguras e frações sem origem no mockup | listas web de fabricantes e tipos | `md:min-w-120` no invólucro das duas listas, `gap-6` entre lista e painel, grade `2fr 1fr 1.4fr auto` (fabricantes) e `1fr 1.1fr 1.6fr 1.4fr 1fr auto` (modelos), com o texto da última coluna invisível no cabeçalho (D5); o mockup usa `table` com colunas automáticas. Barra de filtros dos modelos: a busca tem mínimo de 240 px e a barra quebra linha abaixo disso (seção 10) |
| ordem da lista | o mockup 7 põe Huawei primeiro | ordem alfabética do backend |

## 6. Estados sem mockup (decisão do usuário)

Cada linha foi decidida na implementação, sem mockup; nenhuma está aprovada.

| Onde | O que a tela faz |
|---|---|
| Listas | carregando: indicador sem texto; falha: o aviso de erro de sempre |
| Modelos, catálogo vazio | "Nenhum modelo encontrado" e "O catálogo ainda não tem modelos ativos. Cadastre o primeiro.", sem "Limpar busca e filtros" |
| Fabricantes e tipos, lista vazia | "Nenhum tipo cadastrado" e "Cadastre o primeiro." (e o equivalente de fabricante), com o painel "Novo" ao lado na web |
| Novo modelo | só fabricantes e tipos ativos nas opções; `manufacturer_deactivated` e `type_deactivated` no aviso, se um deles for desativado no meio |
| Novo modelo | valor com ponto: "use só números e vírgula" sob o campo, no cliente, sem pedir ao servidor |
| Tipo com modelos | `characteristic_locked` vindo do servidor (a tela estava desatualizada): a mensagem no aviso, a lista recarrega e a unidade e a caixa voltam ao valor salvo e travam |
| Fabricante, tipo e modelo desativados | campos só leitura, a nota e a ação única de reativar |
| Visão do Técnico | aviso de que o papel só consulta; linhas de fabricante e tipo sem link e sem a coluna "Editar"; modelo abre só leitura; `/novo` e `/:id` de fabricante e tipo voltam para a lista |
| Tipo sem modelos | unidade e caixa livres, com a dica "Depois que o tipo tiver modelos, a unidade não muda." |

## 7. Telas derivadas para aprovação

| Derivada | Forma no resultado |
|---|---|
| Modelos, vazio e sem resultado na web | o painel do mockup 13, centralizado na largura da lista |
| Modelos, Técnico na web | a lista sem "Novo modelo", com o aviso do mockup 12 acima dos filtros |
| Novo modelo, inversor no celular; erro do nome na web | a forma do mockup 4 com "Potência" e "kW"; na web, o erro do mockup 4 no painel de 640 |
| Modelo ativo no celular | a tela 4 com Fabricante e Tipo só leitura, o painel "Situação" e "Salvar alterações" no rodapé |
| Modelo desativado na web | a tela 5 só leitura (rótulo e valor em lista), com o chip, a nota e "Reativar modelo" no painel da direita |
| Novo fabricante no celular | tela cheia, como a 11: "Nome *", a dica e "Salvar fabricante" no rodapé |
| Editar fabricante (web e celular) | o painel da 7 (ou a tela cheia) "Editar fabricante", com "Salvar alterações", "Cancelar" (web) e a seção "Situação" com "Desativar fabricante" |
| Fabricante desativado | nome só leitura, a nota e "Reativar fabricante" |
| Novo tipo na web | o painel da 9 vazio, "Novo tipo", "Salvar tipo" |
| Editar tipo no celular | a tela 11 com a unidade e a caixa travadas, o aviso com cadeado e "Desativar tipo" (sem "Cancelar", como o painel da 9; o de fabricante tem "Cancelar") |
| Tipo desativado | os campos só leitura e "Reativar tipo" |

## 8. Teclado e foco

Medido nas listas, formulários, modelo ativo e desativado, fabricante e tipo em edição, nos dois tamanhos, e nas listas do Técnico. Em todas, o `Tab` passa pelos controles na ordem da tela, o anel de foco computado é o do tema (2 px sólido `highlight`, afastado 2 px; o mesmo valor do IAM) e todo alvo tem pelo menos 44 px de altura, com uma exceção:

| Onde | O que |
|---|---|
| Caixa "Obrigatória" | 20 px na web e 22 no celular, como o mockup, dentro do rótulo clicável de 44 px |

Corrigido: na tabela web de tipos, "Editar" e "Editando" eram links de texto de **18 px** de altura. Agora o alvo tem **44 px**, dentro da linha de 56; o texto fica no mesmo lugar (medido: x 809,9, y 221,5, 18 px de texto, antes e depois), como o link de texto do mockup 9. Nas tabelas de modelos e fabricantes a linha inteira é o link (56 px).

Ordem: na lista de modelos do celular, "Novo modelo" (rodapé fixo) vem **antes** dos cartões, embora apareça depois deles (o mesmo caso do "Convidar usuário" do IAM); em fabricantes e tipos, "Novo fabricante" e "Novo tipo" vêm depois dos cartões, como aparecem. Campos desligados (unidade e caixa travadas) ficam fora do `Tab`. Linhas do Técnico em fabricantes e tipos não recebem foco (não há link).

## 9. Fluxos com o backend real

Rodados num Chrome de verdade (headless), com cliques e digitação reais, contra o backend do catálogo (`feature/catalog-backend`) e conferidos pela API. Nenhum erro nem aviso no console, nenhum diálogo de confirmação.

| Fluxo | Resultado |
|---|---|
| Cadastrar "Sungrow" pelo painel padrão da web | entra na lista e o campo **volta vazio** |
| "SUNGROW" em seguida | `manufacturer_name_taken`: "já existe um fabricante com este nome" sob o campo, o campo marcado inválido e **com o texto digitado** |
| Renomear no painel de edição | salva e volta à lista com o painel "Novo fabricante" vazio |
| Cadastrar tipos pelo painel (Painel solar em Wp, obrigatória; Bateria em W, opcional) | entram na lista; a API confirma unidade e obrigatoriedade |
| Novo modelo com "12.000" (web e celular) | recusado no cliente: "use só números e vírgula, com até 9 inteiros e 3 decimais", sem pedir ao servidor |
| Depois "12,5" (web) e "3" (celular) | salvos; a API guarda `12.5` e `3`; a lista mostra "Potência 12,5 kW" |
| Tipo com modelos | Unidade e "Obrigatória" desligadas, com "A unidade não muda porque o tipo já tem modelos cadastrados." |
| `PUT` forçado pela API com outra unidade | **422 `characteristic_locked`** |
| `PUT` forçado: obrigatória de ligada para desligada, com modelos | **204: o backend aceita** (a tela não deixa: seção 5) |
| `PUT` forçado: de desligada para ligada, com modelos | 422 `characteristic_locked` |
| Tela desatualizada: tipo aberto sem modelos, modelo criado por fora, unidade trocada e salva | aviso "O tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar"; a unidade volta ao valor salvo e trava |
| Desativar e reativar fabricante, tipo e modelo | sem confirmação; o chip e o botão trocam na hora |
| Tipo desativado e fabricante desativado | somem das opções do "Novo modelo" |
| Técnico abre `/catalogo/modelos/novo`, `/catalogo/fabricantes/novo`, `/catalogo/tipos/novo`, `/catalogo/fabricantes/:id`, `/catalogo/tipos/:id` (390 e 1280) | volta para a lista de cada um |
| Técnico nas listas e no modelo (390 e 1280) | nenhum botão nem campo de escrita; aviso só na lista de modelos; barra inferior com um item |
| Técnico tenta `POST` pela API | 403 `forbidden` |

**Contrato:** nada diverge entre o serviço simulado dos testes e o backend real. As listas chegam como `{ content, page, size, totalElements, totalPages }` (o `Page<T>`); o `amount` chega como número JSON (`3`, `12.5`) e a tela o escreve com vírgula; os códigos de erro (`manufacturer_name_taken`, `model_name_taken`, `characteristic_locked`, `forbidden`) são os da tabela de `usecases.md`.

## 10. Larguras intermediárias (sem mockup)

O plano compara em 390 e 1280; entre 768 e 1279 a web não tem artboard. Medido a 768 e 1024 (altura 800), com o painel de 340/360 px aberto ao lado das listas, antes e depois das correções:

| Tela | 768 | 1024 | 1280 |
|---|---|---|---|
| Modelos (sem painel) | **corrigido**: antes a página rolava na horizontal (documento 876 px; a barra de filtros não quebrava, a busca encolhia para 45 px e "Novo modelo" ficava fora da tela). Agora o documento tem 768: busca (260) e Tipo (200) na primeira linha, Situação (180) e "Novo modelo" na segunda; a tabela (720) rola dentro de 470 px | cabe. Antes, tudo numa linha com a busca de 161 px; agora a busca tem 324, com Tipo e Situação na mesma linha, e "Novo modelo" passa para a segunda (sem mockup nessa largura) | igual ao de antes: busca 417, Tipo 200, Situação 180, "Novo modelo" 151, tudo numa linha de 44 px |
| Fabricantes + painel | a lista (mínimo 480, `md:min-w-120`) mostra **108 px** e rola dentro dela; "Situação" e "Editar" só rolando; documento 768 | **364 px** visíveis; "Editar" só rolando | cabe (618) |
| Tipos + painel | **88 px** visíveis, com ou sem o tipo aberto. **Corrigido**: antes o documento tinha 802 px (a página rolava 34 px na horizontal): o "Ações" `sr-only` (absoluto) da última coluna escapava da rolagem da lista; com a seção da lista `relative`, o documento tem 768 | **344 px**; documento 1024 | cabe (598; "Ar-condicionado" quebra em duas linhas) |
| Modelo (editar) | formulário 307 e situação ao lado | formulário 455 | 627 (o do mockup) |

Nada se sobrepõe (medido antes e depois): as listas rolam dentro do próprio invólucro (`overflow-x-auto`), atrás do painel, e em 768, 1024 e 1280 nenhuma das três listas alarga a página. Decisão do usuário: empilhar o painel abaixo da lista (ou escondê-la) abaixo de uma largura, já que fabricantes e tipos mostram só 108 e 88 px em 768.

## 11. Pendências menores

| Onde | Pendência |
|---|---|
| Edição de modelo | com o tipo além dos 100 primeiros, o campo do valor some e "Salvar" desliga sem explicar (atalho do plano) |
| Edição de tipo | enquanto a lista de tipos carrega ou falha, a característica salva é tratada como obrigatória (não dá para limpar um valor opcional até carregar) |
| Edição de modelo | limpar uma característica opcional manda `values: []` (o que `usecases.md` pede), sem teste próprio |
| Fabricante | `manufacturer_deactivated` recarrega o painel e perde o aviso |

## 12. Não verificado

- Nenhuma comparação pixel a pixel nem sobreposição de capturas; o logo do mockup não carrega fora do artefato.
- A nova medição cobriu os itens corrigidos (seções 3, 8 e 10) e o que eles tocam (cartões do celular, chips, cabeçalhos, barra de filtros em 1280); as demais linhas da seção 4 mantêm os valores da comparação, em código que as correções não mudaram.
- Estados que exigem falha do servidor: carregando, erro de rede, `concurrent_update`, `manufacturer_deactivated` e `type_deactivated` no meio do cadastro; cobertos só pelos testes de tela.
- O papel Comercial (mesmas permissões do Administrador no catálogo); só Administrador e Técnico foram usados.
- Tema claro, Safari, Firefox, aparelho físico, toque, leitor de tela real e o aplicativo (Capacitor). O celular foi emulado (390×844, `mobile: true`).
- Zoom de 200%.
