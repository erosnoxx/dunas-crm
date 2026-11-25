# Comparação mockup × resultado: 03-customers

Etapa de comparação da regra 7 ([`docs/CLAUDE.md`](../../../CLAUDE.md)) e Tarefa 10 do plano ([`2026-11-22-customers-frontend.md`](../../../plans/2026-11-22-customers-frontend.md)): cada tela de clientes, locais e equipamentos instalados, em 390 px (celular) e 1280 px (web), contra os 19 artboards aprovados ([`README.md`](README.md)); a lista, a ficha e os formulários também em 768 e 1024 px. Feita em 2026-11-25, com o frontend servido por `npm run dev` e ligado ao backend real do mesmo ciclo (branch `feature/customers-backend`), sobre Postgres, Redis e um coletor de e-mail próprios da comparação.

Nada aqui é aceito em silêncio. A seção 3 traz as decisões que o usuário delegou ao controlador em 2026-11-25: estão registradas e podem ser revistas. As seções 5 a 7 listam aproximações, interface sem mockup e pendências; os achados novos estão em "Para o usuário decidir", no fim. A comparação só fecha com essas decisões.

## 1. Como foi comparado

- **Mockup: leitura estrutural do markup.** Os artboards (`canvas/project/*.dc.html`) foram lidos campo a campo, com os valores do estilo de cada um (cor, tamanho, peso, altura, espaçamento, corte). Os artboards **não foram renderizados** fora do canvas.
- **Resultado: medido num navegador real.** Um Chrome headless, dirigido pelo DevTools protocol, abriu cada tela nos estados do mockup, com o tema escuro emulado. Os tamanhos foram 390×844, 768×1024, 1024×768 e 1280×800. De cada elemento-chave saíram `getComputedStyle` (fonte, tamanho, altura de linha, peso, cor, fundo, borda, respiro, espaço, `clip-path`, `letter-spacing`) e `getBoundingClientRect`. Os números foram confrontados com os do markup, e as cores computadas foram lidas de volta para o nome do token (`ground`, `panel`, `line-strong`, ...). Em cada tamanho também se mediu o estouro horizontal da página (`scrollWidth` contra a largura da janela) e a rolagem dentro das tabelas.
- **Visual: só do resultado.** As capturas de tela do resultado foram olhadas: alinhamento, quebra de linha, corte e foco. Elas **não** foram postas lado a lado com o mockup renderizado, e não houve comparação pixel a pixel. As capturas ficaram fora do repositório, como na comparação do IAM.
- **Teclado:** em cada tela e tamanho, `Tab` do topo até o fim, anotando a ordem, o anel de foco computado e a altura do alvo.
- **Rede:** os pedidos à API foram registrados em cada tela, para a regra do ADR-28 e a consulta que muda a cada tecla. O erro de carregamento foi provocado bloqueando o pedido no navegador (`Network.setBlockedURLs`), que é a mesma falha de rede de quando a API cai.

Dados de teste foram criados pela própria API, em cinco passos:

1. O Administrador semeado trocou a senha inicial. O Técnico e o Comercial foram convidados, e o convite chegou pelo coletor de e-mail.
2. Com o banco ainda sem clientes, foram tiradas as telas de "nenhum cadastrado".
3. Um cliente foi cadastrado e desativado, e com ele foi tirado o D2.
4. Entrou o resto dos dados, que espelha os mockups:
   - "J. Silva Comércio de Alimentos Ltda", com Loja Centro, Fábrica e o local desativado Loja Santa Mônica, e os mesmos modelos e séries da tela 9;
   - "Ana Paula Ribeiro" (CPF 318.457.260-90), com a série HV9910042;
   - "Mariana Tavares Lopes", sem locais e só com telefone;
   - "Transportes Cerrado Ltda", com três cidades e 15 equipamentos;
   - um cliente com telefone estrangeiro (`+14155552671`);
   - um CNPJ alfanumérico (`12.ABC.345/01DE-35`);
   - 16 clientes de enchimento, para a lista ter paginação.
5. Os nomes de exemplo do mockup ("Marina Costa") só aparecem onde o dado criado é igual.

## 2. Corrigido durante a comparação

| Achado | Onde | Correção |
|---|---|---|
| **A página inteira rolava 251 px na horizontal em 768 px.** O "Ações" `sr-only` do cabeçalho é absoluto, e o invólucro com `overflow-x-auto` não era o bloco que o contém. O texto escapava da rolagem da tabela e alargava o documento para 1019 px. É o mesmo defeito que o catálogo corrigiu na lista de tipos | lista de clientes (web) e ficha (as três tabelas de equipamentos) | invólucro `relative overflow-x-auto` em `screens/customers.tsx` e `components/equipment-table.tsx`, com os testes de classe dos dois ajustados. Medido de novo: documento de 768 px em 768, de 1024 em 1024 e de 1280 em 1280, na lista e na ficha |
| **Linhas da tabela da lista com 61 px.** O mockup tem `td` de 48 px (`Main.dc.html:36`), com 8 px de respiro. O alvo de 44 px do "Abrir" (`min-h-11`) mais esse respiro empurrava toda linha para 61 px, 13 px a mais por linha | lista de clientes (web) | `-my-1.5` no "Abrir": o alvo continua com 44 px e cabe no `td` de 48. Medido de novo: linha de 49 px com a borda (mockup 48 + 1), 55 px com o nome fantasia (mockup 8 + 20 + 18 + 8 + 1) e "Abrir" com 44 px dentro do `td`. O teste de classe foi ajustado |

Commit `9c210c7 fix(frontend): keep the customers tables inside their scroll and the list rows at 48 px`. As linhas da seção 4 que citam a lista e a ficha já trazem os valores medidos depois da correção.

## 3. Decisões já tomadas (por delegação do usuário, revisáveis)

Em 2026-11-25 o usuário delegou ("decida vc, sem tempo") as divergências encontradas durante a execução. Cada linha abaixo foi **decidida por delegação** e foi conferida no navegador nesta comparação. O usuário pode rever qualquer uma.

### 3.1 Decisões da Tarefa 1 (aprovadas em 2026-11-24 e confirmadas na delegação)

| Ponto | Mockup | Resultado conferido |
|---|---|---|
| CNPJ alfanumérico | dica "Digite só os números" e `inputmode="numeric"` no documento | o campo não tem `inputmode` (teclado de texto). A dica diz "Digite o CPF ou o CNPJ. O tipo (CPF ou CNPJ) e a pontuação aparecem ao digitar; os dígitos verificadores são conferidos antes de salvar." (só a primeira frase da tela 7 mudou). Ao digitar `12abc34501de35` com o teclado, aparece `12.ABC.345/01DE-35` e o tipo "CNPJ"; o CNPJ repetido dá 409 com "Já existe um cliente com este documento." |
| Valor da característica | "12.000 BTU/h", com milhar, na ficha e na opção do modelo | "12000 BTU/h" na ficha (web e celular) e na opção "Split Inverter 12.000 BTU · Fabricante B · Ar-condicionado · 12000 BTU/h". O "12.000" do nome do modelo é texto do próprio modelo |
| Ordem do filtro de situação | tela 1: "Todos, Ativos, Desativados"; telas 2 a 6: "Ativos, Desativados, Todos" | "Ativos, Desativados, Todos" em todas, e "Ativos" é o padrão |
| Telefone estrangeiro | não desenhado | na edição, o campo fica desligado (`nav`, borda `line`, `text-muted`) com o número guardado como está (`+14155552671`) e a dica "Número de fora do Brasil: não se edita nesta tela". A dica não tem ponto final, como foi decidido. Salvar uma edição do nome mantém o telefone: depois de salvar, a ficha mostra `+14155552671` |
| Selo de situação | caractere ●/○ dentro do selo, 13 px peso 600, respiro 3/10, sem corte | `StatusChip` do catálogo: 30 px de altura, respiro 0/12, círculo em SVG, corte de 10 px. O desativado é vazado. Igual ao catálogo |
| Navegação, página inicial, `<table>`, `<details>`, máscaras sem biblioteca | — | conferido: "Clientes" é o primeiro item da barra lateral e da inferior nos três papéis; `/` vai para `/clientes`; a lista e a ficha da web usam `<table>`; a ficha do celular usa `<details>` |

### 3.2 Decisões da execução

| Onde | Mockup | Resultado |
|---|---|---|
| Lista vazia, tela 4 | legenda "Estado da primeira vez: … A busca e o filtro continuam na tela …" (`Clientes-web-vazio.dc.html:93`) | **não renderizada**. Ela foi lida como nota para quem revisa, como as `.cap` "Dados de exemplo". O comportamento que ela descreve existe: busca e filtro ficam na tela |
| Títulos dos estados vazio, sem resultado e erro; "Locais"; vazio dos locais; nome do cliente no celular | 20/24 peso 600 (22/28 no nome do celular) | `text-title` 24/28 peso 600, porque não há token de 20/24 nem de 22/28. O catálogo fez a mesma escolha |
| Linha da série casada (D1), na web | a linha de apoio da mesma célula (nome fantasia) é 13/18 | 14/20 `text-muted` (a linha de documento da tela 2) na web e no celular |
| Valores fora da escala | série com `letter-spacing:.02em`; título do local 20/28; "+N" 12/18 | `tracking-[0.02em]` (medido 0,28 px), `text-xl leading-7` (medido 20/28 peso 600 Chakra Petch) e `text-caption leading-4.5` (medido 12/18 peso 600). Seguem o precedente do catálogo |
| Local sem equipamento | não desenhado | sem tabela (web) e sem lista (celular), só "0 equipamentos ativos". Conferido no local "Sede" do cliente desativado |
| "Editar local" sozinho no celular (cliente desativado) | não desenhado | fica à direita, onde está na tela 10 |
| Cartões de equipamento do Técnico, celular | `Ficha-celular-tecnico.dc.html:91-94` desenha os cartões como `<a href="#">` | texto, sem link. O plano manda nenhuma escrita para o Técnico; conferido: a ficha do Técnico tem 0 botões e um só link (`/clientes`) |
| Documento na lista e na ficha | três desenhos do tipo: tela 1 `caption` 600 `text-muted`; tela 2 "CPF 000…" corrido; tela 9 selo `chip-ok` | `DocumentText` com os três desenhos (prop `badge` na ficha) |
| Barra de filtros entre 768 e 1023 px | não desenhada | quebra a linha quando a busca ficaria abaixo de 240 px (`md:min-w-60`, do catálogo). Medido: em 768 a busca tem 280 px e a situação 180, e "Novo cliente" desce para a linha seguinte. Em 1024 e 1280 fica tudo numa linha |
| Formulário do cliente | dica e cadeado herdam o peso 400; o selo do documento na edição herda a linha de 24 | `text-caption font-normal` (medido 12/16 peso 400) e `text-label leading-6` (medido 13/24 peso 600) |
| Voltar do celular | "Voltar para Clientes", "Voltar para a ficha do cliente" (44 px) | `MobileHeader` compartilhado: 48×48 com nome "Voltar", como no catálogo |
| Edição do cliente | "Cancelar" da tela 7 aponta para a lista | na edição, "Cancelar" e voltar vão para a ficha |
| `customer_deactivated` ao salvar a edição | não desenhado | aviso no `Alert`, sem redirecionar. O redirecionamento só acontece quando o cliente já chega desativado |
| Ficha | "Não pode ser alterado." (cadeado) | só para quem edita e com cliente ativo. As telas 12 e 13 não o têm |
| Ficha, títulos no celular | o mockup tem "Cliente" como `<p>` e o nome como `h1` | `MobileHeader` faz "Cliente" ser o `h1`, e o nome é `h2`. O vazio dos locais é `h3` (no mockup, `h2`) |
| Ficha, erro de carregamento | só a tela 6 (lista) | "Não foi possível carregar o cliente" e "Sem resposta do servidor. Confira a conexão e tente de novo.". Medido: o mesmo quadro da tela 6, com ícone 40 `danger`, texto 14/20 `danger-text` e "Tentar de novo" 44 `chip-ok` corte 10. `customer_not_found` vai para o `Alert` sem "Tentar de novo" |
| Técnico com cliente desativado | não desenhado | só o aviso do papel, sem o aviso da tela 12; o selo diz "Desativado" |
| D3, painel "Situação" | sem texto aprovado | "Desativar marca o local na ficha: ele não recebe equipamento novo e não se edita até ser reativado. Os equipamentos dele ficam como estão. Dá para reativar a qualquer momento." |
| D4 | — | sem texto de ajuda acima de "Desativar local", como no estado ativo da tela 19 |
| UF vazia no local novo | os mockups já mostram uma UF escolhida | opção vazia e desligada, sem texto ("Escolha" não cabe no campo de 92/96 px) |
| `location_deactivated` ao salvar (corrida) | não desenhado | aviso no `Alert`; a página continua no formulário, sem passar sozinha para o D6 |
| Novo equipamento no celular | a tela 18 não desenha "Modelo e local não mudam depois de salvos…" | o aviso fica escondido no celular (`hidden md:flex`), só no cadastro |
| "Local" no formulário de equipamento | três formatos: 17:82 com o endereço, 18:44 só a cidade, 19:59 "local · cliente" | cada tela segue o seu mockup. Conferido: "Loja Centro · Av. João Pinheiro, 1450 · Uberlândia/MG" na web, "Loja Centro · Uberlândia/MG" no celular e "Loja Centro · Padaria Boa Vista" na edição |
| D5 | a tela 17 tem Local antes de Modelo | Modelo antes de Local, como na tela 19. O texto do painel "Situação" deriva de 19:91 |
| Desativar e reativar | — | "Desativar" e "Reativar" de cliente, local e equipamento funcionam sem confirmação (ADR-6). Conferido no navegador: nenhum diálogo, e a página fica onde está |

### 3.3 Telas derivadas aceitas como construídas (D1 a D7)

| Derivada | Forma no resultado, conferida no navegador |
|---|---|
| D1, série casada | `?q=HV9910042`: a linha "Série HV9910042 · SUN2000-5KTL-L1" fica sob o nome. Na web vem dentro da célula Cliente; no celular vem depois do nome e antes do documento. É 14/20 `text-muted` (seção 3.2). "1 cliente encontrado" no celular |
| D2, nenhum cliente ativo | o quadro da tela 4 com "Nenhum cliente ativo", o texto do ADR-28 e "Ver desativados" (botão de 44 px), que muda a situação para Desativados na URL e no seletor. O "Novo cliente" é o da tela: no topo na web, fixo no celular |
| D3, editar local na web | a página da tela 14 preenchida, com título igual ao nome do local, voltar com o nome do cliente e "Salvar alterações". Ao lado vai o painel "Situação" de 340 px com o selo, a ajuda e "Desativar local" `danger-text` 44 px. Os problemas abaixo de 1280 px estão em "Para o usuário decidir" |
| D4, editar local no celular | a tela 15 preenchida, com o selo no topo, "Desativar local" abaixo do formulário e "Salvar alterações" no rodapé fixo |
| D5, editar equipamento na web | a página da tela 17, com Modelo e Local só leitura, série e data editáveis e o painel "Situação" com "Desativar equipamento". O 409 de série repetida na edição só traz a frase da tabela (sem o "Confira a etiqueta…") |
| D6, local e equipamento desativados | página só leitura. Na web, os dados ficam à esquerda e o painel à direita com o selo, a nota e "Reativar". No celular, a ordem é selo, dados e nota, com "Reativar" no rodapé fixo. A data ausente aparece como "Não informado" |
| D7, Comercial no celular | barra inferior com Clientes e Catálogo (dois itens de 195 px). A ficha e os formulários não têm barra inferior |

Também foram decididas e aceitas na execução estas formas derivadas:

- **Lista, Técnico na web:** é a tela 1 sem "Novo cliente", com o aviso da tela 5 sob o título.
- **Sem resultado na web:** o conteúdo da tela 3 no quadro web das telas 4 e 6, com respiro 48/24 e ícone 40.
- **Vazio e erro no celular:** o conteúdo das telas 4 e 6 no quadro da tela 3, com 32 px acima, respiro 24/16 e ícone 32.
- **Textos da tela 3 fora do padrão:** o sujeito acompanha a situação ("Nenhum cliente desativado tem …"), e a frase final junta só as opções que valem.
- **Tela 4 para o Técnico:** só o título.
- **Paginação:**
  - web: "Mostrando 1 a 20 de 23 clientes", com Anterior e Próxima de 44 px;
  - celular: "Mostrando N de M clientes" e "Carregar mais", de contorno, 52 px. Ele aparece sempre e fica desligado na última página.
- **Carregando:** `Spinner`.
- **Ficha sem locais na web:** o vazio da tela 11 no quadro web, com "Cadastrar o primeiro local" (44 px) no vazio e "Novo local" na linha "Locais".
- **Cliente desativado no celular:** é a tela 10. O aviso da tela 12 vem depois do nome, e "Reativar cliente" ocupa o rodapé fixo.
- **Ficha do Técnico na web:** é a tela 9 sem ações, com o aviso do papel depois do cabeçalho.
- **Formulários:**
  - celular sem erro: as dicas ficam escondidas, como nos estados das telas 8, 15 e 18;
  - web com erro: as peças de erro das telas 8, 15 e 18 entram nas telas 7, 14 e 17;
  - edição do cliente no celular: a caixa só leitura do documento tem 48 px;
  - o tipo do documento ao lado do campo só aparece com o documento completo;
  - a dica da data só aparece na web, na edição;
  - enquanto a lista de modelos carrega, o seletor e "Salvar" ficam desligados.

## 4. Tela a tela

Legenda do resultado:

- **igual**: os valores do markup e os medidos coincidem.
- **aproximação**: não há token para o valor do mockup (seção 5).
- **sem mockup**: seção 6.
- **derivada**: seção 3.3.
- **decidido**: seção 3.

Valores comuns, medidos iguais ao mockup em todas as telas onde aparecem e não repetidos nas linhas:

- **Casca:**
  - barra lateral de 232 px com "Clientes" em primeiro, item atual de 44 px em `chip-ok` com corte 10;
  - barra superior do celular de 64 px `nav` com borda `line` e título 24/28 peso 600;
  - barra inferior de 64 px.
- **Títulos e voltar:** título da página 36/40 peso 700 Chakra Petch na web; voltar da web 14 peso 600 `text-muted`, 44 px, espaço 6.
- **Campos e texto:**
  - rótulos 13/18 peso 600;
  - campos de 44 px na web (`ground` nos formulários, `panel` nos filtros) e de 48 px `panel` no celular, com borda `line-strong` e respiro 12;
  - campo com erro: borda de 2 px `danger` e respiro 11;
  - erro sob o campo 14/20 `danger-text` com ícone e espaço 6;
  - dicas 12/16 peso 400 `text-muted`.
- **Botões:**
  - botão principal da web de 44 px, `brand`, 14 peso 600, respiro 20 e corte 10;
  - "Cancelar" e o botão secundário de 44 px `chip-ok`, corte 10;
  - botão do rodapé do celular de 48 px, 16 peso 600, corte 12, num rodapé `nav` com borda `line` e respiro 12/16.
- **Painéis e tabelas:**
  - painéis `panel` com borda `line`;
  - `th` 13/18 peso 600 `text-muted`, respiro 12/16, borda `line`;
  - links de texto 14 peso 600 `brand-text` com alvo de 44 px;
  - "Desativar …" em `danger-text` 14 peso 600, 44 px.

### Lista de clientes

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | 1 `Main` | `main` com respiro 32 e espaço 20; título; barra com espaço 12 e alinhada embaixo; busca flexível; "Situação" de 180; "Novo cliente" de 146×44; dica 12/16 8 px sob a barra (−12 + 20); painel; `td` 14/20 com respiro 8/16 e linha de 48 + 1; nome 600; nome fantasia 13/18 `text-muted`; documento sem quebra; contagens Chakra Petch 600; "+1" 12/18 600 `chip-ok` com respiro 0/6; "Abrir" | igual após a seção 2 | ordem do filtro, selo e paginação: seção 3 |
| 1280 | 1 | cidade buscada primeiro (`?q=Uberlândia`: "Uberlândia/MG, Araguari/MG") | igual | — |
| 1280 | 1 | linha desativada (`?situacao=ALL&q=Serra`) com selo vazado `text-muted` | igual | selo: seção 3.1 |
| 1024 | sem artboard | barra numa linha (busca 378); tabela de 802 px num invólucro de 726: rola **76 px dentro da tabela**, e "Abrir" só aparece rolando; página sem estouro | derivada | seção 4.1 |
| 768 | sem artboard | a barra quebra (busca 280 e situação 180; "Novo cliente" na segunda linha; dica em 2 linhas); a tabela rola 332 px por dentro; a página **não** estoura depois da seção 2 | derivada | seção 4.1 |
| 390 | 2 `Clientes-celular` | busca 44 `panel` 16 px; dica 12/16 6 px abaixo; seletor 44 com respiro 10, 14 px; cartões `panel` com respiro 14/16 e espaço 12; nome 16/24 600; apoio 14/20 `text-muted`; "2 locais · 5 equip." Chakra Petch 16 600; selo à direita; "Novo cliente" de 48 com corte 12, fixo 64 px acima da base sobre `ground` com respiro 12/16; barra inferior de 3 itens | igual | — |
| 390 | 3 `Clientes-celular-sem-resultado` | `?q=HV0000000`: quadro 32 px abaixo do seletor, respiro 24/16, espaço 10; ícone 32 `text-muted`; texto 14/20 `text-muted` com o termo; "Limpar busca e filtros" de 164×44 | igual, exceto o título 24/28 (mockup 20/24) | decidido (seção 3.2) |
| 1280 | sem artboard | sem resultado na web | derivada | seção 3.3 |
| 1280 | 4 `Clientes-web-vazio` | banco vazio: quadro com respiro 48/24 e espaço 10; ícone 40; texto 14/20 `text-muted` até 520; "Cadastrar o primeiro cliente" 44 com respiro 0/12; busca e filtro na tela | igual, exceto o título 24/28 e a legenda não renderizada | decidido (seção 3.2) |
| 390 | sem artboard | vazio no celular | derivada | seção 3.3 |
| 390 | 5 `Clientes-celular-tecnico` | `?q=HV99` como Técnico: aviso `chip-ok` com respiro 10/12, 14/20, ícone 18 e espaço 8; sem "Novo cliente" e sem a folga de baixo; "2 clientes encontrados" 13/18 `text-muted`; barra com Clientes e Catálogo | igual | — |
| 1280 | sem artboard | Técnico na web (aviso sob o título, sem "Novo cliente") | derivada | seção 3.3 |
| 1280 | 6 `Clientes-web-erro` | pedido bloqueado: quadro com respiro 48/24; ícone 40 `danger`; texto 14/20 `danger-text` até 560 com espaço 6; "Tentar de novo" 162×44 `chip-ok` corte 10; busca, filtro e "Novo cliente" usáveis; "Tentar de novo" refaz a consulta e a lista volta | igual, exceto o título 24/28 | decidido (seção 3.2) |
| 390 | sem artboard | erro no celular (título em 2 linhas) | derivada | seção 3.3 |
| ambos | D1, D2 | seção 3.3 | derivada | — |

#### 4.1 Larguras intermediárias (sem mockup)

| Tela | 768 | 1024 | 1280 |
|---|---|---|---|
| Lista | documento de 768 (era 1019 antes da seção 2); barra em duas linhas; a tabela rola por dentro | cabe; a tabela rola 76 px por dentro | cabe |
| Ficha | documento de 768 (era 1019 antes da seção 2); o cabeçalho não quebra: o nome fica numa coluna de **200 px**, uma palavra por linha; os dados ficam em 3 colunas de cerca de 140 px, o documento quebra no meio ("34.218.7 / 65/0001-14") e o e-mail também; as tabelas rolam por dentro (301 a 339 px) | cabe; o nome ocupa 2 linhas; as tabelas rolam 9 a 83 px por dentro | cabe; nada rola |
| Novo cliente, novo local, novo equipamento | cabe (formulário de 472) | cabe | cabe |
| Editar local (D3) | **a página estoura para 1099 px** | **a página estoura para 1099 px** | cabe (620 + 24 + 340) |
| Editar equipamento (D5) | **a página estoura para 940 px** | cabe | cabe |
| Equipamento desativado (D6) | **a página estoura para 795 px** | cabe | cabe |
| Local desativado (D6) | cabe | cabe | cabe |

Os estouros da edição e do D6 estão em "Para o usuário decidir", item 1; a ficha em 768, no item 2.

### Ficha do cliente

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | 9 `Ficha-web` | voltar "Clientes"; nome 36/40; nome fantasia 16 `text-muted` e selo 12 px ao lado; "Desativar cliente" e "Editar cliente" 127×44 `chip-ok` corte 10; dados com respiro 20/24, 3 colunas e espaço 24; rótulo 13/18; valor 16 a 4 px; selo do tipo 12/16 600 `chip-ok` com respiro 2/8; cadeado 12/16 400 com espaço 6; "Locais" e "2 ativos, 1 desativado"; "Novo local" 133×44 `brand` | igual, exceto o título "Locais" 24/28 (mockup 20/24) | decidido |
| 1280 | 9 | cartão do local: cabeçalho com respiro 16/20, espaço 16 e borda `line`; título 20/28 600 com espaço 12; endereço e "N equipamentos ativos" 14/20; "Editar local" 74×44 e "Novo equipamento" 188×44 `chip-ok` com espaço 16; tabela com `td` de 56 e respiro 0/16; modelo 600; característica Chakra Petch 600; série com `letter-spacing` 0,28 px; desativado em `text-muted` com "Abrir"; ativo com "Editar" 44 | igual (título do local e série: seção 3.2) | — |
| 1280 | 9 (último cartão) | local desativado: título `text-muted`, selo vazado, "Reativar local" 44 `brand-text`, aviso com respiro 10/20 `nav` 14/20 `text-muted` e ícone; equipamento ativo dentro dele com "Editar" (ADR-30) | igual | — |
| 1280 | 9 | ordem dos locais | Fábrica, Loja Centro, Loja Santa Mônica (ativos primeiro, depois por nome, como diz a legenda do mockup e a API). O desenho da tela 9 põe Loja Centro antes de Fábrica, o que contradiz a própria legenda | segue a spec, sem divergência a decidir |
| 1024, 768 | sem artboard | seção 4.1 | derivada | 768: "Para o usuário decidir", item 2 |
| 390 | 10 `Ficha-celular` | cabeçalho "Cliente" com voltar; corpo com respiro 20/16 e espaço 16; nome; painel com respiro 16 e espaço 14; linha "Editar cliente" / "Desativar cliente" de 44 px com borda; `summary` com respiro 14/16, espaço 12 e mínimo 44; título 16 600 com espaço 8; cartões de equipamento com respiro 10/16 e espaço 10; ativos abertos e desativado fechado; "Novo local" 48 no rodapé `nav` | igual, exceto o nome 24/28 (mockup 22/28) e o "Locais" 24/28 | decidido |
| 390 | 16 `Local-desativado-celular` | seção do local desativado aberta: título `text-muted`, aviso `nav` com respiro 10/16 e borda em cima, "Reativar local" 48 `chip-ok` corte 12 num bloco com respiro 12/16 e borda `line` | igual | — |
| 390 | 11 `Ficha-celular-sem-locais` | "Não informado" `text-muted`; vazio com respiro 24/16, espaço 10 e ícone 32; "Cadastrar o primeiro local" no rodapé fixo (48) | igual, exceto o título 24/28 | decidido |
| 1280 | sem artboard | sem locais na web (vazio com respiro 48/24 e o link de 44 px) | derivada | seção 3.3 |
| 1280 | 12 `Ficha-web-cliente-desativado` | aviso `chip-ok` com respiro 10/12 e 14/20; ação única "Reativar cliente" 142×44 `brand` corte 10; dados sem cadeado; local ativo com "Editar local" (ADR-30) e sem "Novo local" nem "Novo equipamento" | igual | — |
| 390 | sem artboard | cliente desativado no celular | derivada | seção 3.3 |
| 390 | 13 `Ficha-celular-tecnico` | aviso do papel primeiro; nenhum botão (0 na página, inclusive dentro de `<details>` fechado); um link só (`/clientes`); sem cadeado; sem rodapé | igual, exceto os cartões de equipamento em texto | decidido (seção 3.2) |
| 1280 | sem artboard | Técnico na web | derivada | seção 3.3 |
| ambos | sem artboard | erro de carregamento (pedido bloqueado) | sem mockup | decidido (seção 3.2) |

### Novo cliente e editar cliente

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | 7 `Cliente-formulario-web` | painel `panel` de 640 com respiro 24 e espaço 20; rótulos na cor `text`; campos 44 `ground` 16 px; cadeado 12/16 com ícone 14; seção "Contato" com borda em cima, respiro 20 e espaço 12, título 13/18 `text-muted`; e-mail e telefone lado a lado; "Cancelar" 97×44 e "Salvar cliente" 128×44 à direita com espaço 12 | igual | dica do documento: seção 3.1 |
| 1280 | 7 | CNPJ digitado: tipo "CNPJ" ao lado com respiro 14, `chip-ok` e borda `line-strong` sem a da esquerda, 14 600; rótulo "Razão social *"; "Nome fantasia (opcional)" com a dica | igual | — |
| 1280 | 7 (*Tweak* editar) | documento só leitura 44 `nav` com borda `line` `text-muted`; selo do tipo 13/24 600 `chip-ok` com respiro 2/8; "O documento não pode ser alterado depois de salvo."; valores do próprio cliente (`(34) 3212-4580`); "Salvar alterações" | igual | — |
| 390 | 8 `Novo-cliente-celular-erros`, documento repetido | 409 real: campo com borda 2 `danger`; tipo ao lado 64×48; "Já existe um cliente com este documento."; "Ver cliente com este documento" 230×44 14 600 com espaço 6, que abre `/clientes?q=34218765000114&situacao=ALL` com o cliente achado; o digitado fica; foco no documento | igual | — |
| 390 | 8, CPF inválido sem contato | "CPF inválido: confira os números digitados." sob o documento e "Informe pelo menos um contato: e-mail ou telefone." no topo da seção; nada é enviado; foco no documento | igual | — |
| 390 | 8, telefone inválido | `119999999` dá "Telefone inválido: informe DDD e número." | igual | — |
| 390 | sem artboard | sem erro (as dicas ficam escondidas no celular) | derivada | seção 3.3 |
| 1280 | sem artboard | os três erros na web | derivada | seção 3.3 |
| ambos | sem artboard | telefone estrangeiro na edição | decidido | seção 3.1 |
| 768, 1024 | sem artboard | formulário de 472 e de 640 px; nada estoura | derivada | — |

### Novo local e editar local

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | 14 `Novo-local-web` | voltar com o nome do cliente; painel de 640 com respiro 24 e espaço 18; "Cliente" só leitura 44 `nav` `text-muted`; dica do nome; seção "Endereço" com borda, respiro 18 e espaço 12; CEP 160 + logradouro; número 160 + complemento; bairro, cidade e UF de 96; dicas do CEP e do número; "Salvar local" 115×44 | igual | — |
| 390 | 15 `Novo-local-celular-erros` | "Cliente: …" 14/20 `text-muted`; CEP `01310-10` com `inputmode="numeric"` dá "CEP incompleto: informe os 8 dígitos."; bairro vazio dá "Informe o bairro."; nada é enviado; foco no CEP; número e complemento lado a lado com o placeholder "Opcional"; cidade + UF de 92 | igual | — |
| 1280 | D3 | seção 3.3 | derivada | 768 e 1024: "Para o usuário decidir", item 1 |
| 390 | D4 | seção 3.3 | derivada | — |
| ambos | D6 (Loja Santa Mônica) | página só leitura com os dados (respiro 24 e espaço 20 na web; 16 e 14 no celular), a nota da tela 16 e "Reativar local". Desativar pela página volta a ela mesma, já desativada, sem confirmação | derivada | — |

### Novo equipamento e editar equipamento

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | 17 `Novo-equipamento-web` | "Local" só leitura com o endereço; "Modelo *" 590×44 com as opções "modelo · fabricante · tipo · valor unidade" e a dica; série e data lado a lado (1fr + 220); dicas; "Modelo e local não mudam depois de salvos…" com cadeado; "Salvar equipamento" 170×44 | igual | "12000": seção 3.1 |
| 390 | 18 `Novo-equipamento-celular-erros` | data de amanhã (2026-11-26, com `max` = 2026-11-25) dá "A data de instalação não pode ser depois de hoje.", nada é enviado e o foco vai para a data. Série `HV2240187563` do mesmo modelo dá o 409 real "Este modelo já tem um equipamento com esse número de série. Confira a etiqueta ou busque a série na lista de clientes.", com foco na série e o digitado mantido | igual. O mockup mostra os dois erros juntos; no resultado eles saem um de cada vez, porque o erro da data impede o envio que traria o 409 | — |
| 390 | 19 `Equipamento-celular` (ativo) | selo no topo; Modelo e Local só leitura 48 `nav`; nota com cadeado; série e data; "Desativar equipamento" `danger-text` 44 com borda em cima e respiro 6; "Salvar alterações" no rodapé. 409 na edição: só a frase da tabela | igual | — |
| 390 | 19 (desativado) | painel de dados com respiro 16 e espaço 14; rótulo 13/18; valor 16; nota 14/20; "Reativar equipamento" 48 no rodapé. Desativar e reativar alternam na mesma página, sem diálogo | igual | — |
| 1280 | D5, D6 | seção 3.3 | derivada | 768: "Para o usuário decidir", item 1 |
| 1280 | sem artboard | equipamento ativo de local desativado: edita (ADR-30) | sem mockup | — |

## 5. Divergências por falta de token (aproximação mais próxima)

| Valor do mockup | Onde | Usado |
|---|---|---|
| título 20/24 peso 600 | estados vazio, sem resultado e erro; "Locais"; vazio dos locais | `title` 24/28 (seção 3.2) |
| 22/28 peso 600 | nome do cliente na ficha do celular | `title` 24/28 |
| 20/28 peso 600 | título do cartão do local na web | `text-xl leading-7`, fora da escala, como o `PanelFrame` do catálogo |
| 12/18 peso 600 | "+N" das cidades | `text-caption` + `leading-4.5` |
| `letter-spacing:.02em` | número de série na tabela da ficha | `tracking-[0.02em]` |
| 13 px na linha de 24 | selo do documento na edição | `text-label leading-6` |
| 12/16 peso 400 | dicas e cadeados (o token `caption` é peso 500) | `text-caption font-normal` |
| selo ●/○ 13 px com respiro 3/10, sem corte | todas as telas | `StatusChip` (seção 3.1) |

## 6. Interface sem mockup nem texto aprovado

Decidido na execução e listado para o usuário ver. Nada aqui tem mockup.

| Onde | O que a tela faz |
|---|---|
| Lista | `?q=` e `?situacao=` vivem na URL e são escritos com `replace`. O "Voltar" do navegador, saindo da ficha, devolve a lista com a busca e o campo preenchidos (conferido em 1280 e 390). Dentro da lista, mudar a busca ou o filtro **não** cria passos no histórico: o "Voltar" sai da lista |
| Lista | o campo de busca tem estado próprio. Testado com teclado real: digitar "Uberlndia", voltar o cursor 4 posições e inserir "â" dá "Uberlândia" com o cursor no lugar (posição 6), e a URL vira `?q=Uberl%C3%A2ndia` |
| Lista | sai um pedido por tecla, sem espera entre teclas (como no catálogo) |
| Lista | depois de "Limpar busca e filtros" e de "Ver desativados", o foco cai no `body` (o botão some) |
| Lista, celular | "Mostrando 1 de 1 clientes" (plural com um) e "Carregar mais" desligado, junto de "1 cliente encontrado" |
| Lista, celular | na ordem do teclado, "Novo cliente" (rodapé fixo) vem antes dos cartões, embora apareça depois deles (como em Usuários) |
| Lista | a consulta de desativados (`status=DEACTIVATED&size=1`) só sai no vazio padrão. Conferido na rede: ela sai com o banco vazio e com o D2, e **nenhum** pedido `size=1` sai com clientes na lista (1280, 1024, 768, 390). Se essa consulta falha, o quadro de erro da tela 6 aparece com "Tentar de novo" |
| Formulário do cliente | o tipo só aparece com o documento completo; a máscara do telefone muda para celular com 11 dígitos (`34321245809` vira `(34) 32124-5809`, que a conferência recusa) |
| Formulários | a data de hoje vem do relógio do navegador (`toLocaleDateString("sv-SE")`), e o `max` da data é fixado ao abrir a página. **Não testado** perto da meia-noite: quem deixa a página aberta e passa da meia-noite tem o `max` de ontem até recarregar |
| Formulário de equipamento | a série fica como foi digitada (`ab-12000x` aparece em minúsculas na ficha); o 409 compara sem caixa (backend) |

## 7. Pendências menores

Comportamentos e débitos anotados pelas revisões da execução. Não divergem do mockup; ficam aqui para não se perderem.

| Onde | Pendência |
|---|---|
| `DocumentText` na web | sem espaço real entre o tipo e o número: o leitor de tela lê "CPF318…" |
| Lista | os links "Abrir" têm todos o mesmo nome acessível; busca só com espaços vai como texto; a seleção do filtro escreve `filters.q`, não o texto do campo |
| `Spinner` | `role="status"` sem nome acessível |
| Ficha | carregando e erro da web sem `h1`; "Tentar de novo" sem estado de espera; "Reativar local" dos outros locais ligado enquanto um reativa; `StateFrame`/`LoadError` duplicados entre a lista e a ficha |
| Formulários | `Frame`, `describedBy`, `Failure`, `Back` e `WEB_BUTTON` duplicados entre o formulário de cliente e o de local; o `aria-describedby` aponta para dicas escondidas no celular; `tabular-nums` faltando no campo do documento e na caixa só leitura; telefone `+55` antigo que não casa a regex brasileira obriga a mudar ao salvar; na edição, um documento guardado que falhe na conferência do cliente pode dar erro invisível |
| Equipamento | "Salvar" e "Desativar" podem se sobrepor na edição; a data de hoje usa o fuso do navegador, não o `America/Sao_Paulo`; o `max` congela ao abrir (seção 6) |
| Local | a corrida `location_deactivated` mantém o formulário editável; a opção vazia da UF é anunciada vazia |
| Cache | a lista de modelos do formulário (`["customers", "model-options"]`) não é invalidada por escritas no catálogo |
| Código | `formatAmount` duplicado em `customers/display.ts` e no catálogo (mover para `shared/` toca o catálogo) |
| Testes | o teste "Sair encerra a sessão sem confirmar…" de `app-shell.test.tsx` estourou o tempo (5,5 s) sob carga em duas execuções da suíte inteira, nas Tarefas 8 e 9, e passa sozinho. No portão desta tarefa a suíte inteira passou na primeira execução |

## 8. Teclado e foco

Em todas as telas e nos quatro tamanhos:

- **Ordem:** o `Tab` passa por todos os controles na ordem da tela. A exceção é a lista do celular, na seção 6.
- **Anel de foco:** o computado é o do tema, 2 px sólido `highlight` afastado 2 px, inclusive nos `<summary>` da ficha do celular e na data (conferida na captura).
- **Alvos:** todo alvo tem pelo menos 44 px de altura (o voltar do celular, 48; os botões do rodapé, 48; "Carregar mais", 52).
- **Técnico:** a ordem só passa por navegação, busca, filtro e "Abrir" ou cartões. Nenhum controle de escrita.

## 9. Ponta a ponta com o backend real

O que foi rodado no Chrome headless contra o backend do mesmo ciclo, com o console do navegador sem erros em todas as execuções:

1. O Administrador semeado entra.
2. O Técnico e o Comercial chegam por convite.
3. O Comercial faz, pela tela:
   - tenta cadastrar o CNPJ alfanumérico digitado em minúsculas (`12abc34501de35`), que já existe, e recebe o 409 sob o documento;
   - cadastra `11.444.777/0001-61` só com telefone e cai na ficha nova;
   - cadastra o primeiro local (número "S/N") e volta à ficha;
   - cadastra um equipamento sem data ("Sem data" na tabela);
   - desativa e reativa o cliente, sem diálogo;
   - reativa a Loja Santa Mônica na ficha e a desativa de novo pela página do local.
4. **Edição com o usuário ainda carregando** (a revisão da Tarefa 7 apontou o risco). A recarga direta (F5) foi feita, com `/auth/me` segurado 2 s pelo navegador, nas oito rotas de escrita:
   - `/clientes/novo` e `/clientes/:id/editar`;
   - local novo, local ativo e local desativado;
   - equipamento novo, equipamento ativo e equipamento desativado.

   Resultado: o Administrador e o Comercial **ficam** na rota, em 1280 e em 390. A URL não muda nem por um instante (um caminho só, amostrado a cada 150 ms), e o formulário aparece. A guarda `RequireAuth` não desenha a rota antes de existir o usuário, por isso o `useCanEditCustomers` nunca é lido com `false` de carregamento. O Técnico é redirecionado em todas: `/clientes/novo` vai para `/clientes`, e as outras vão para a ficha.

## 10. Não verificado

- Nenhum artboard renderizado, nenhuma comparação pixel a pixel nem lado a lado (seção 1).
- A lógica do "hoje" perto da meia-noite local (seção 6). Não dá para testar sem mudar o relógio.
- Tema claro, Safari, Firefox, aparelho físico, toque real, leitor de tela real e o aplicativo (Capacitor).
- `concurrent_update` e as corridas (`location_deactivated`, `installed_equipment_deactivated` entre abrir e salvar): cobertos só pelos testes de unidade das telas.

## Para o usuário decidir

Achados novos desta comparação, que não estavam nas decisões delegadas:

| # | Achado | Severidade | Recomendação |
|---|---|---|---|
| 1 | **A página estoura na horizontal nas edições com o painel "Situação":** D3 em 1024 e 768 (documento de 1099 px), D5 em 768 (940) e D6 do equipamento em 768 (795). O painel tem 340 px e `shrink-0`, e o formulário não encolhe abaixo do conteúdo mínimo (cerca de 470 px). O catálogo, no mesmo desenho, não estoura em 1024 nem em 768, porque o painel dele (`md:w-85`) encolhe. Tirar o `shrink-0` resolve 1024, mas ainda deixa D3 e D5 estourando em 768 | média: 768 a 1023 px não têm mockup, mas o plano pede página sem estouro | empilhar o painel "Situação" sob o formulário abaixo de `xl` (`max-xl:flex-col`) nas três páginas. É mudança de leiaute de tela derivada, então fica para o usuário |
| 2 | **Ficha em 768:** o cabeçalho não quebra. O nome fica numa coluna de 200 px, uma palavra por linha, ao lado das ações. A grade de dados de 3 colunas (cerca de 140 px cada) quebra o documento no meio do número ("34.218.7 / 65/0001-14") e o e-mail no meio da palavra. O cabeçalho do cartão do local espreme o endereço em 4 linhas | média: tablet em pé | cabeçalho com `flex-wrap` (as ações descem sob o nome); dados em 1 ou 2 colunas abaixo de `lg`; documento sem quebra (`whitespace-nowrap`, como na lista) |
| 3 | **Tabelas em 1024:** a da lista (802 px de mínimo) e as da ficha (735 a 809) não cabem nos 726 px. "Abrir", "Editar" e "Situação" só aparecem rolando dentro da tabela. A guarda funciona e a página não estoura | baixa | aceitar (a guarda é a decisão do plano) ou esconder colunas menos usadas abaixo de `xl` |
| 4 | "Mostrando 1 de 1 clientes" e "Mostrando 1 a 1 de 1 clientes": plural com um. A mesma frase existe em Usuários ("1 usuários") | baixa | trocar por "Mostrando 1 cliente" quando o total for 1 |
| 5 | Depois de "Limpar busca e filtros" e de "Ver desativados", o foco vai para o `body`. Era uma pendência da revisão da Tarefa 5 e foi confirmado no navegador | baixa (teclado) | levar o foco para o campo de busca |
