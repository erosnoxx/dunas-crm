# Comparação mockup × resultado: 01-iam

Etapa de comparação da regra 7 ([`docs/CLAUDE.md`](../../../CLAUDE.md)): cada tela do IAM, em 390 px (celular) e 1280 px (web), contra os artboards aprovados do canvas ([`README.md`](README.md) e [`telas-complementares.md`](telas-complementares.md)). Feita em 2026-11-20, com o frontend servido por `npm run dev` e ligado ao backend real (Postgres, Redis e um coletor de e-mail locais).

Nada aqui é aceito em silêncio: o que diverge e não foi corrigido está nas seções 3 a 6, **para o usuário decidir**.

## 1. Como foi comparado

- **Mockup: leitura estrutural do markup.** Os artboards (`project/*.dc.html` do canvas) foram lidos campo a campo: os valores estão escritos em estilo inline (cor, tamanho, peso, altura, espaçamento, corte). Os artboards **não foram renderizados** fora do canvas: as listas (regras da senha, sessões, linhas de usuário) são modelos `sc-for` que só o runtime do artefato expande, e o logo é um arquivo interno do artefato.
- **Resultado: medido num navegador real.** Chrome headless dirigido pelo DevTools protocol abriu cada tela, nos estados do mockup, em 390×844 e 1280×800, e registrou de cada elemento visível `getComputedStyle` (fonte, tamanho, altura de linha, peso, cor, fundo, borda, espaçamento, `clip-path`) e `getBoundingClientRect` (posição e tamanho). Os números foram confrontados com os do markup. As cores computadas foram lidas de volta para o nome do token (`ground`, `panel`, `line-strong`, ...).
- **Visual: só do resultado.** As capturas de tela do resultado foram olhadas (alinhamento, quebra de linha, corte, foco), mas **não lado a lado com o mockup renderizado**. Nenhuma comparação pixel a pixel foi feita.
- Teclado: em cada tela e tamanho, `Tab` até dar a volta, anotando a ordem, o anel de foco computado e o tamanho do alvo.

Dados de teste: usuários criados pelo próprio fluxo (Administrador semeado, convites, aceite, desativação). Os textos de exemplo do mockup ("Marcos Silva", "Galaxy A54") não existem no resultado.

## 2. Corrigido durante a comparação

| Achado | Onde | Correção |
|---|---|---|
| O `cn` (regras do tailwind-merge) lia `text-label`, `text-body-sm` e o resto da escala do tema como **cor**, e a cor seguinte (`text-text`, `text-danger-text`) apagava o tamanho. Todo rótulo de campo saía 16/24 peso 400 em vez de 13/18 peso 600, e o texto do `Alert` 16 em vez de 14 | todas as telas com formulário ou aviso | `shared/lib/cn.ts` registra a escala como tamanho de fonte; teste em `cn.test.ts`. Depois disso, rótulos, legenda "Papel" e corpo do aviso conferem com o mockup |
| Aviso de erro com 12 px de respiro em volta; o mockup tem 12 em cima e embaixo e 14 nas laterais | `Alert` de erro (Entrar, convite, sessões, ...) | `px-3.5 py-3` |
| Item atual da conta na barra lateral ("Minhas sessões") em peso normal; o mockup "Minhas sessões · web" o marca em seminegrito | casca, web | `aria-[current=page]:font-semibold` |

As duas últimas correções vieram depois das medições: as linhas da seção 3 que as citam não foram medidas de novo no navegador; o teste de classe de cada uma cobre a mudança.

Corrigido antes, durante a execução, e conferido aqui:

| Achado | Onde | Correção |
|---|---|---|
| O `Button` de contorno compartilhado tinha respiro lateral na camada externa (o anel saía mais largo que o mockup) e ficava 2 px mais alto | todos os botões de contorno; mudou também "Esqueci minha senha" (link enviado) e o link inválido | camada externa `px-px` e camada interna `self-stretch` com `min-h-[inherit]`; medido aqui: 52 px e 1 px de anel, como o mockup. O corte interno fica 1 px maior que o do mockup |

## 3. Tela a tela

Iguais ao mockup, sem ressalva: Entrar (390 e 1280, vazio), Usuários web com o painel aberto (rolagem da lista), detalhe pendente, desativado e o próprio na web, o próprio no celular, Início na web, cópia do link de redefinição inválido. As demais linhas são iguais com as ressalvas que cada uma diz.

Legenda do resultado: **igual** (os valores do markup e os medidos coincidem), **aproximação** (sem token para o valor do mockup, seção 4), **sem mockup** (seção 5), **derivada** (seção 6).

### Entrar

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Entrar-erro` | respiros 32/24/24 e 32 do cabeçalho; título 36/40 peso 700; apoio 14/20 `text-muted`; rótulos 13/18 peso 600 a 6 px do campo; campos 52 px `panel` com borda `line-strong`; "Mostrar" 72×52 `brand`; botão 52 px corte 12; link 44 px; nota no pé | igual | — |
| 390 | `Entrar-erro` | aviso: contorno `danger`, ícone 20, texto 14/20 `danger-text`, mesmo lugar | igual após a seção 2 (o respiro 12×14 não foi medido de novo) | texto com inicial maiúscula, como o mockup (seção 5) |
| 390 | `Entrar-bloqueado` | painel `highlight` corte 16, ícone de relógio 24, título e corpo; rótulos e campos em `text-muted`/`line`; botão `line` com texto `text-muted` | igual, exceto: título do aviso 16/24 (mockup 16/22, aproximação) e o "Mostrar" continua visível e desligado (o mockup não o desenha) | "Mostrar" desligado: decisão do usuário |
| 390 | sem artboard | erros de validação no cliente (e-mail vazio ou inválido, senha vazia) | sem mockup | seção 5 |
| 1280 | `Main` (Entrar · web) | cabeçalho 24/32; painel 440 `panel` corte 16 com respiro 40/36 e espaço 24; campos 48 px `ground`; botão 48 px corte 10; nota centralizada | igual | — |
| 1280 | sem artboard | erro e bloqueio na web | derivada (o `Alert` no mesmo lugar do celular) | seção 6 |

### Convite: definir senha

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Convite-senha` | respiros 24 do cabeçalho e 20 do título; contador "9 de 30" 12/16 tabular ao lado do rótulo; checklist de 5 regras 14/20 com espaço 8, atendida em `text`, pendente em `text-muted`, estado lido por leitor de tela; "Repita a senha"; botão desligado `line` | igual, exceto o título 36/40 (mockup 32/36) e o ícone de regra atendida (lucide, não o círculo preenchido `chip-ok` do mockup) | aproximações (seção 4) |
| 390 | sem artboard | aviso "as senhas não conferem" ao vivo, enquanto se digita a repetição | sem mockup | seção 5 |
| 1280 | sem artboard | o mesmo formulário no painel de 440 do Entrar | derivada | seção 6 |

### Link de convite ou de redefinição inválido

A tela não consulta o link antes do envio (ADR-12): o estado aparece depois de enviar a senha com um link usado, substituído ou inexistente, e direto quando a URL não tem `token`.

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Convite-expirado` | painel `panel` corte 16, respiro 28/24, espaço 16; ícone 32 `highlight`; texto 16/24; botão de contorno 52 px corte 12 "Ir para o login" fora do painel | igual, exceto o título 24/28 peso 600 (mockup 28/32 peso 700) | aproximação (seção 4) |
| 390 | `Convite-expirado` (cópia de `telas-complementares.md`) | redefinição: "Este link já foi usado ou foi substituído", "Pedir novo link" | igual ao texto aprovado | — |
| 1280 | sem artboard | coluna de 440 centralizada, botão fora do painel | derivada | seção 6 |

### Esqueci minha senha e link enviado

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Esqueci-celular` | respiros 24/20; campo 52 `panel`; "Enviar link" 52 corte 12; "Voltar para o login" 44 px | igual, exceto o título 36/40 (mockup 32/36) | aproximação |
| 390 | `Esqueci-enviado-celular` | painel corte 16 respiro 24 espaço 16; ícone de envelope 32 `highlight`; texto 16/24; "Voltar para o login" de contorno 52; "Pedir o link de novo" 44 px | igual, exceto o título 24/28 peso 600 (mockup 28/32 peso 700) | aproximação |
| 390 | sem artboard | `rate_limited` no pedido: aviso amarelo com a frase da tabela de erros, sem título, botão ligado | sem mockup | seção 5 |
| 1280 | sem artboard | painel de 440 do Entrar | derivada | seção 6 |

### Redefinir senha

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Redefinir-celular` | igual ao convite, com título, apoio e botão próprios | igual, exceto o título 36/40 (mockup 32/36) e o ícone da regra | aproximação |
| 390 | sem artboard | aviso "Senha alterada" depois de redefinir (toast, e vai para `/entrar`) | sem mockup de forma (o texto é aprovado) | seção 5 |
| 1280 | sem artboard | painel de 440 do Entrar | derivada | seção 6 |

### Trocar a senha inicial (obrigatória)

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Trocar-obrigatoria-celular` | sem navegação nem voltar; "Senha atual" com "Mostrar", nova senha com contador e checklist, repetição; "Salvar e continuar"; "Sair" 14 peso 600 `brand` no pé | igual, exceto o título 36/40 (mockup 32/36) e o ícone da regra | aproximação |
| 1280 | sem artboard | o painel de 440 do Entrar, sem casca | derivada; em 1280×800 a página rola e o "Sair" fica abaixo da dobra | seção 6 |
| ambos | sem artboard | transição para a casca depois de salvar: medida quadro a quadro, a casca aparece em `/` com o título "Trocar senha" por um quadro e com a página inicial vazia por ~70 ms, antes de ir para `/usuarios` (de 95 a 125 ms no total, em duas medições) | piscada visível | decisão do usuário (seção 5) |

### Trocar senha (voluntária)

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Trocar-celular` | cabeçalho próprio 64 px `nav` com "Voltar" 48×48 e título 24/28; respiro 20/16/24; campos e checklist; "Salvar nova senha" desligado | igual, exceto o ícone da regra | aproximação |
| 1280 | sem artboard | dentro da casca, com a barra lateral; o cabeçalho "Voltar" + título 24/28 no topo da área de conteúdo, e o formulário no painel de 440 | derivada | seção 6 |

### Casca, menu da conta e página inicial

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Usuarios-celular`, `Conta-celular` | barra superior 64 `nav` com borda `line`, logo 36, título 24/28, avatar 44 `brand` corte 10 com uma letra | igual; o título da barra é `<p>` e o `h1` da página existe só para leitor de tela (um `h1` por tamanho de tela) | — |
| 390 | `Conta-celular` | folha do menu: fundo `ground` translúcido, painel `panel` corte 16 com respiro 20/20/28, avatar 48, "Fechar" 44; itens 52 px 16 peso 500 com borda `line` em cima e ícone 20; "Sair" em `danger-text` | igual, exceto o fundo translúcido (`bg-ground/80`; mockup 78%) | aproximação |
| 1280 | `Usuarios-web`, `Navegacao-web` | barra lateral 232 `nav` com respiro 24/16 e espaço 32; item atual 44 px `chip-ok` corte 10 com ícone 20; bloco da conta com avatar 40, nome 14 peso 600, papel 13; links de 44 px 14 `text-muted` | igual, exceto o texto do item ("Usuários") 14 (mockup 15) e o wordmark | aproximações |
| 1280 | `Navegacao-web` | Comercial e Técnico: sem itens de trabalho; "Início" 36/40; painel "Em construção" respiro 24 espaço 8, texto até 520 px | igual | — |
| 1280 | `Detalhe-pendente-web` | "Trocar senha" também na barra do Administrador | igual (derivada aprovada) | — |
| 390 | sem artboard | página inicial no celular (barra superior + painel "Em construção") | derivada | seção 6 |

### Minhas sessões

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 390 | `Sessoes` | cabeçalho próprio com "Voltar"; respiro 20/16/24 e espaço 16; cartões `panel` corte 16 respiro 16 espaço 12; ícone 24; aparelho 16/24 peso 600, último uso 14/20, entrada 12/16; chip "Esta sessão" 30 px; "Encerrar esta sessão" de contorno 44 corte 10; "Encerrar todas as outras" de contorno 52 em `danger-text`; "Sair deste aparelho" 52 `brand`; nota 12/16 | igual, exceto: rótulo do aparelho é o `User-Agent` cru (o mockup mostra "Chrome, Windows"); o rodapé não fica preso ao pé; "Dados de exemplo" não aparece (é do mockup) | rótulo do aparelho: pendência aberta em [`open-questions.md`](../open-questions.md) |
| 390 | sem artboard | carregando (indicador sem texto), lista só com a sessão atual ("Encerrar todas as outras" desligado) | sem mockup | seção 5 |
| 1280 | `Sessoes-web` | título 36/40; lista até 640 px; "Encerrar todas as outras" de contorno 48 corte 10 em `danger-text`; sem "Sair deste aparelho"; item atual da conta marcado | igual após a seção 2 (o peso do item atual não foi medido de novo); a nota "A sessão dura 30 dias..." é um segundo parágrafo sob o texto de apoio, no mockup é o mesmo parágrafo | decisão do usuário (diferença de marcação, mesma aparência em duas linhas) |

### Usuários: lista

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | `Usuarios-web`, `Detalhe-proprio-web` | título 36/40 e "Convidar usuário" 44 px corte 10; painel da lista corte 16, cabeçalho 13/18 peso 600 `text-muted`, grade 2.2/1.1/1.6/1, linhas 64 px com borda `line`, linha escolhida em `line`; chips Ativo (cheio), Convite pendente (vazado), Desativado (cortado); rodapé "Mostrando 1 a 5 de N usuários" 13/18 com Anterior e Próxima de contorno 44 | igual, exceto: nome e papel 14 (mockup 15) e o texto do botão "Convidar usuário" 16 (mockup 15) | aproximações |
| 1280 | `Usuarios-web` | com o painel de detalhe aberto a lista tem 604 px e rola na horizontal (largura mínima 620), o mesmo cálculo do markup do mockup | igual | — |
| 1280 | sem artboard | `/usuarios` sem ninguém escolhido: a lista ocupa a largura toda, sem painel de detalhe | derivada | seção 6 |
| 390 | `Usuarios-celular`, `Paginacao-celular` | cartões `panel` corte 16 com nome 16/22, "papel, último uso" 13/18 e chip à direita; "Mostrando N de M usuários" 13/18 centralizado; "Carregar mais" de contorno 52; "Convidar usuário" 52 no rodapé fixo `nav` | igual, exceto o tamanho de página (5; o mockup do celular mostra "4 de 54") | decisão do usuário (seção 5) |

### Usuário: detalhe (ativo, convite pendente, desativado, o próprio)

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | `Usuarios-web` (painel) | painel 340 a 400 px, respiro 24, espaço 20, corte 16; nome 24/28 peso 600; e-mail 14/20; "Sessões ativas" 13/18 com contagem; linha de sessão com "Encerrar" de contorno 44; "Encerrar todas as sessões" 44 em `danger-text`; "Papel" com seletor 48 `ground`; "Desativar usuário" texto `danger-text` sublinhado 44 px | igual, exceto o rótulo do aparelho (`User-Agent` cru, que quebra em várias linhas) | rótulo do aparelho: [`open-questions.md`](../open-questions.md) |
| 390 | `Usuario-celular` | cabeçalho com "Voltar" e nome; seção com e-mail, chip e "Papel" (seletor 52); seção "Sessões ativas" 16 peso 600; "Encerrar todas as sessões" 52; "Desativar usuário" | igual, exceto o título da seção 16/24 (mockup 16/22) | aproximação |
| 1280 | `Detalhe-pendente-web` | "e-mail, papel", chip vazado, frase com o primeiro nome, "Reenviar convite" 48 `brand`, "Desativar usuário" | igual | — |
| 390 | `Detalhe-pendente-celular` | papel só leitura, seção "Convite não aceito", "Desativar usuário", "Reenviar convite" no rodapé fixo | igual, exceto o título da seção 16/24 (mockup 16/22) | aproximação |
| 1280 | `Desativado-web` | chip cortado, texto de quem não consegue entrar, ação única "Reativar usuário" 48 | igual | — |
| 390 | `Desativado-celular` | papel só leitura, seção "Sem acesso", "Reativar usuário" no rodapé fixo | igual, exceto o título da seção 16/24 | aproximação |
| 1280 | `Detalhe-proprio-web` | "(você)" no nome, frase "Você não pode mudar o seu papel nem desativar a sua conta.", sessões com "Esta sessão" e sem "Encerrar" na atual, "Encerrar todas as outras" 44; sem papel editável nem desativar | igual | — |
| 390 | `Detalhe-proprio-celular` | o mesmo no celular; "Encerrar todas as outras" 52 | igual | — |
| ambos | sem artboard | carregando (indicador sem texto), erro de rede (mensagem da API), id que não é UUID ("Usuário não encontrado", sem pedir ao servidor), usuário sem sessões (contagem 0 e "Encerrar todas as sessões" desligado) | sem mockup | seção 5 |

### Convidar usuário

| Tamanho | Artboard | Comparado | Resultado | Feito / decisão |
|---|---|---|---|---|
| 1280 | `Convidar-web` | diálogo 480 px `panel` respiro 32 espaço 24 corte 16 sobre fundo translúcido; apoio 14/20; campos 48 `ground`; legenda "Papel" 13/18 peso 600; cartões de papel 48 com borda `line-strong`, escolhido em `chip-ok` com borda `brand`; "Cancelar" de contorno e "Enviar convite" `brand`, 48 px corte 10, à direita | igual, exceto: título 24/28 peso 600 (mockup 28/32 peso 700), nome do papel 14 (mockup 15), fundo `bg-ground/80` | aproximações |
| 1280 | sem artboard | "Enviar convite" desligado com o formulário incompleto (cinza dos botões de senha) | sem mockup | seção 5 |
| 390 | `Convidar-celular` | cabeçalho com "Voltar"; respiro 20/16 espaço 16; campos 52 `panel`; cartões de papel 52 `panel`; "Enviar convite" 52 no rodapé fixo | igual, exceto o nome do papel 14 (mockup 15) | aproximação |
| janelas baixas | sem artboard | 1366×768: o diálogo cabe (de 86 a 682 px); 1024×400: o diálogo ocupa a altura e o conteúdo rola por dentro; 683×384 (1366×768 com zoom de 200%): abaixo de `md`, o convite vira a tela cheia do celular e a página rola | rola onde precisa | — |

## 4. Divergências por falta de token (aproximação mais próxima)

Valores do mockup que não existem na escala do [`tokens.json`](../../../design-system/tokens.json). Em cada um, ou o design ganha o token, ou o usuário aceita a aproximação:

| Valor do mockup | Onde | Usado |
|---|---|---|
| título 32/36 peso 700 | convite, redefinir, esqueci, troca obrigatória (celular) | `display-lg` 36/40 |
| título 28/32 peso 700 | link inválido, link enviado, diálogo de convite | `title` 24/28 peso 600 |
| wordmark "DUNAS" 20 px peso 600, espaçamento 0.04em | cabeçalho das telas de acesso e barra lateral | `text-xl` e `tracking-[0.04em]` (fora da escala; pedir um token `wordmark`) |
| texto 15 px | item da barra lateral, nome e papel nas linhas da lista web, nome do papel no convite, botão "Convidar usuário" da web | 14 (`body-sm`) ou 16 (`body`, no botão) |
| 16/22 peso 600 | título do aviso de bloqueio, títulos de seção do detalhe no celular | `body` 16/24 peso 600 |
| fundo do diálogo e da folha, `ground` a 78% | diálogo de convite, menu da conta | `bg-ground/80` |
| ícone de regra atendida: círculo preenchido `chip-ok` com visto | checklist da senha | `CircleCheck` do lucide (contorno) |
| 13 px peso 400 | nota do Entrar, papel na barra lateral, entrada da sessão, e-mail e papel nas linhas da lista, contagem e "Mostrando N de M", nota do próprio usuário | `text-label font-normal` (o token `label` é 13/18 peso 600; o peso é sobrescrito) |
| espaços do celular: 24/20 no convite contra 32/24 no Entrar | telas de acesso | seguido cada mockup (prop `compact`); os mockups discordam entre si |

## 5. Interface sem mockup nem texto aprovado (decisão do usuário)

Cada linha foi decidida na implementação, sem mockup; nenhuma está aprovada.

| Onde | O que a tela faz |
|---|---|
| Guarda das rotas autenticadas | enquanto carrega `me`, e em erro de rede ou 5xx, a tela fica vazia |
| Entrar | mensagens de validação no cliente: "informe o e-mail", "informe um e-mail válido", "informe a senha" |
| Entrar | texto do aviso de credencial com inicial maiúscula, como o mockup ("E-mail ou senha incorretos. Confira os dados e tente de novo."), diferente da frase minúscula da tabela de erros |
| Entrar, bloqueado | a pausa vive só na tela: recarregar a página religa o formulário (o servidor segue recusando); depois dos 15 minutos não há "tentar de novo" sem recarregar; o "Mostrar" fica visível e desligado |
| Convite, redefinir, trocar | o aviso de repetição diferente aparece ao vivo, enquanto se digita |
| Redefinir e trocar senha | "Senha alterada" sai como aviso flutuante (toast); o texto é aprovado, a forma não; aparece também depois da troca obrigatória |
| Troca obrigatória | piscada da casca entre salvar e chegar a `/usuarios` (de 95 a 125 ms, seção 3) |
| Troca voluntária | título "Trocar senha", botão "Salvar nova senha" e rótulos vêm do texto do mockup, não da tabela de cópias de [`telas-complementares.md`](telas-complementares.md) |
| Troca voluntária | "Voltar" vai para `/`, não para a tela anterior; depois de `wrong_current_password` o botão fica desligado até um campo mudar |
| Esqueci minha senha | `rate_limited`: aviso amarelo com a frase da tabela de erros, sem título, botão ligado |
| Entrar aberto por quem já tem sessão | vai direto para o início |
| Convite aberto por quem já tem sessão | a tela de convite abre normalmente, sem redirecionar |
| Minhas sessões e detalhe do usuário | carregando: indicador sem texto (sem nome para leitor de tela); falha ao carregar: o aviso de erro de sempre; lista vazia: nenhuma lista; "Encerrar todas as outras" desligado quando só existe a sessão atual; usuário sem sessões: contagem 0 e "Encerrar todas as sessões" desligado |
| Detalhe do usuário | erro de rede mostra a mensagem da API; id que não é UUID mostra "Usuário não encontrado" sem pedir ao servidor |
| Usuários, celular | tamanho de página 5 (o mockup mostra "Mostrando 4 de 54"); com um usuário só, "1 usuários" |
| Usuários, web | falha da primeira página: sem rodapé nem botão de tentar de novo (recarregar) |
| Convidar | "Enviar convite" desligado com o cinza dos botões de senha (o mockup só mostra o ligado) |
| Rótulos | link de volta "Voltar" em todas as telas (os mockups misturam "Voltar" e "Voltar para usuários") |
| Todas as listas de sessões | último uso relativo: "agora" e "há N min" seguem o mockup; "há 3 horas" e "há 2 dias" saem do `Intl.RelativeTimeFormat` (`last-use.ts`), sem cópia aprovada |
| Todas as listas de sessões | o ícone (celular ou monitor) é deduzido do rótulo livre do aparelho por expressão regular |
| Avisos de erro | erro que não é `ApiError` (falha inesperada do código) mostra a própria mensagem, que pode vir em inglês; erro sem mensagem deixa o aviso vazio |
| Entrar | o aviso de um envio anterior continua na tela quando o envio seguinte falha na validação do cliente |
| Todas as listas de sessões | rótulo do aparelho é o `User-Agent` cru ([`open-questions.md`](../open-questions.md), "Como o rótulo do aparelho é obtido") |
| Usuários, celular | na ordem do teclado, "Convidar usuário" (rodapé fixo) vem antes da lista, embora apareça depois dela |

## 6. Telas derivadas para aprovação

As derivadas listadas em [`telas-complementares.md`](telas-complementares.md) seguiram o que lá está; estas são as formas finais, para aprovar:

| Derivada | Forma no resultado |
|---|---|
| Esqueci, link enviado, redefinir e troca obrigatória na web | painel de 440 px do `Entrar · web`, sem casca; na troca obrigatória a página rola em 1280×800 e o "Sair" fica abaixo da dobra |
| Link inválido (convite e redefinição) e link enviado na web | coluna central de 440 px com o painel e o botão de contorno **fora** do painel, como no celular |
| Troca voluntária na web | dentro da casca, com a barra lateral; no topo da área de conteúdo, o mesmo cabeçalho do celular ("Voltar" e título 24/28); o formulário no painel de 440 |
| Barra lateral do Administrador | "Minhas sessões", "Trocar senha" e "Sair" no bloco da conta |
| Erro e bloqueio do Entrar na web | o `Alert` do celular, no mesmo lugar, dentro do painel de 440 |
| Página inicial no celular | barra superior da casca ("Início") e o painel "Em construção" |
| Usuários na web sem ninguém escolhido | a lista na largura toda, sem painel de detalhe |
| Convidar na web | diálogo aberto pela rota `/usuarios/convidar` sobre a lista; fechar volta para `/usuarios` |

## 7. Pendências menores

Comportamentos e débitos da execução, sem divergência visual de mockup; registrados para não se perderem:

| Onde | Pendência |
|---|---|
| Link inválido | "Ir para o login" e "Pedir novo link" são `Button` com `role="link"`: também disparam com Espaço |
| Aceite do convite e redefinição | o cache de outro usuário (`me`) não é limpo ao aceitar um convite nem ao redefinir a senha no mesmo navegador |
| Casca | `/usuarios/`, com barra no fim, perde a barra superior do celular; a barra lateral não rola em janelas muito baixas |
| Usuários, web | "Anterior" fica sem efeito depois de uma falha ao voltar de página; o aviso de erro do "Carregar mais" sai acima da lista; uma página exibida que fique vazia some com o rodapé |
| Detalhe do usuário, celular | carregando e erro não têm `h1` (só o "Voltar") |
| Detalhe do usuário | ação iniciada num usuário perde o aviso de sucesso ou de erro se o Administrador abrir outro antes de terminar; encerramentos seguidos perdem o erro de cada chamada |
| Minhas sessões | dois cliques rápidos em "Encerrar esta sessão" perdem o carregando do primeiro; um aviso de erro antigo fica depois de um "Encerrar todas as outras" bem-sucedido; os botões "Encerrar esta sessão" têm o mesmo nome acessível |
| Diálogo de convite | `clip-path` com `overflow-y-auto` pode cortar a barra de rolagem no chanfro |
| Sessão | `403 password_change_required` não é tratado (a guarda impede chegar lá); a trava do `session_invalid` volta a armar com qualquer sucesso |
| Testes | sem teste direto de `last-use`, `useMediaQuery`, `useInfiniteUsers` e da remoção do ouvinte de `matchMedia`; sem teste de `aria-current` em `/usuarios/:id`, do alvo `md:min-h-12` do Entrar, de `invalid_credentials` seguido de `rate_limited`, das classes `md:` do link inválido, do "Senha alterada" na troca obrigatória e do toast renderizado (o `sonner` é simulado) |
| Código | classes da casca de acesso repetidas entre o link inválido e `AuthShell`; o estilo de link de texto montado à mão três vezes; `new-password-form` importa `screens/link-problem`; `ResetPasswordValues` sem uso |

## 8. Teclado e foco

Em todas as telas e nos dois tamanhos, o `Tab` passa por todos os controles na ordem da tela (exceção: a lista de usuários no celular, seção 5), o anel de foco computado é o do tema, 2 px sólido `highlight` afastado 2 px, o mesmo valor escrito no estilo dos artboards (`outline:2px solid #FFC15A; outline-offset:2px`; comparação dos valores, não da imagem) e todo alvo tem pelo menos 44 px de altura, menos o rádio de 20 px do papel, que fica dentro do cartão de 48/52 px clicável. Campos de formulário têm rótulo associado; erros saem em `role="alert"`; botões em envio têm `aria-busy`.

## 9. Ponta a ponta com o backend real

Rodado num Chrome de verdade (headless), contra o backend do mesmo commit: o Administrador semeado entra e cai em "Troque a senha inicial", sem navegação; troca e chega a `/usuarios`; convida um Técnico pelo diálogo; o e-mail chega ao coletor com o link `/convite?token=...`; o convidado abre o link noutro navegador, define a senha e entra; o Administrador encerra a sessão dele no detalhe, e a ação seguinte do convidado o leva a `/entrar`; o Administrador desativa, e o login do desativado mostra a mesma mensagem única de credencial; o Administrador reativa, e o login volta a funcionar. Nenhum erro no console do navegador.

## 10. Não verificado

- Nenhum artboard renderizado; nenhuma comparação pixel a pixel nem lado a lado (seção 1).
- Estado "link expirado" (`link_expired`): exige um convite com mais de 48 h ou um link de redefinição com mais de 1 h; coberto só pelos testes de unidade da tela.
- Tema claro, Safari, Firefox, aparelho físico, toque, leitor de tela real (NVDA, VoiceOver) e o aplicativo (Capacitor).
- Zoom de 200% verdadeiro: emulado por uma janela de 683×384.
