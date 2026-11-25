# design/: 03-customers

Artefatos visuais de validação de UX/UI desta feature.

**Regra 6** ([`docs/CLAUDE.md`](../../../CLAUDE.md)): antes de fechar `domain-model.md` e `usecases.md`, é obrigatório gerar um artefato visual aqui, com descrição do que ele representa e **quais decisões de UX ele ajudou a validar**.

**Regra 7**: nenhuma tela, nova ou redesenhada, é implementada sem mockup previamente aprovado aqui. A implementação final é **pixel perfect** em relação a ele, e termina com comparação explícita mockup × resultado.

## Artefatos

Canvas com 19 telas na identidade DUNAS (tema escuro, [`tokens.json`](../../../design-system/tokens.json)), no mesmo padrão do [catálogo](../../02-catalog/design/README.md) (mesmos valores de cor, fonte, espaçamento, campo, botão, tabela, selo de situação e casca): <https://claude.ai/artifact/NxExPYWS7kHNhmdB9jbyou>. O código-fonte das telas está em [`canvas/project/`](canvas/project/) (um `.dc.html` por tela e o `canvas.json`). Web em 1280 px, celular em 390 × 844; a ficha da web (1320 px) e a do celular (1640 px) estão na altura da página inteira. **Status:** telas 1 a 19 aprovadas pelo usuário em 2026-11-22; as derivadas (sem artboard) estão no fim da tabela e vão à comparação final.

| # | Tela | O que valida | Status |
|---|---|---|---|
| 1 | Clientes, web (`Main.dc.html`) | "Clientes" primeiro no menu (ADR-26); busca única com placeholder e dica de que aceita o número de série (ADR-13); filtro de situação; colunas Cliente (nome fantasia abaixo), Documento (tipo + número formatado), Cidades (até duas e "+N"), Locais, Equipamentos (só ativos), Situação por forma | aprovado (2026-11-22) |
| 2 | Clientes, celular (`Clientes-celular.dc.html`) | Os mesmos dados em cartões; "Novo cliente" fixo acima da barra inferior; barra com Clientes, Catálogo e Usuários | aprovado (2026-11-22) |
| 3 | Clientes, celular, sem resultado (`Clientes-celular-sem-resultado.dc.html`) | Busca por série sem resultado, com "Limpar busca e filtros" e "Novo cliente" | aprovado (2026-11-22) |
| 4 | Clientes, web, nenhum cadastrado (`Clientes-web-vazio.dc.html`) | Primeira vez, com o convite "Cadastrar o primeiro cliente"; busca e filtro continuam na tela | aprovado (2026-11-22) |
| 5 | Clientes, celular, Técnico (`Clientes-celular-tecnico.dc.html`) | Sem "Novo cliente", aviso de que o papel só consulta (ADR-7); barra com Clientes e Catálogo (ADR-26); busca pela série da etiqueta | aprovado (2026-11-22) |
| 6 | Clientes, web, erro de carregamento (`Clientes-web-erro.dc.html`) | Falha de rede no lugar da lista, com "Tentar de novo"; menu, busca e "Novo cliente" usáveis | aprovado (2026-11-22) |
| 7 | Novo cliente, web (`Cliente-formulario-web.dc.html`) | Tipo reconhecido ao lado do documento formatado e o aviso "o documento não pode ser alterado depois de salvo" (ADR-24); "Razão social" e "Nome fantasia (opcional)" com CNPJ (ADR-21); seção "Contato" que diz que um dos dois basta (ADR-16); telefone com máscara (ADR-18). No *Tweak* `modo = editar`: o mesmo formulário com o documento somente leitura e o mesmo aviso | aprovado (2026-11-22) |
| 8 | Novo cliente, celular, com erro (`Novo-cliente-celular-erros.dc.html`) | Três estados no *Tweak* `erro`: documento repetido com "Ver cliente com este documento" (ADR-23); CPF inválido com o formulário sem contato (`contact_required`); telefone inválido ("informe DDD e número"). Ícone e texto, não só cor | aprovado (2026-11-22) |
| 9 | Ficha do cliente, web (`Ficha-web.dc.html`) | Dados com o documento travado (ADR-3); "Editar cliente" e "Desativar cliente"; um cartão por local, todos abertos, com nome, endereço, situação, equipamentos ativos, "Editar local" e "Novo equipamento"; tabela com modelo, fabricante, tipo e característica, série, data e situação, ativos primeiro (ADR-22); local desativado no fim, marcado, com "Reativar local" | aprovado (2026-11-22) |
| 10 | Ficha do cliente, celular (`Ficha-celular.dc.html`) | A mesma ficha empilhada; cada local é uma seção recolhível, aberta se ativa e fechada se desativada (ADR-22); "Novo local" fixo embaixo | aprovado (2026-11-22) |
| 11 | Ficha do cliente, celular, sem locais (`Ficha-celular-sem-locais.dc.html`) | Estado vazio dos locais, com "Cadastrar o primeiro local" (ADR-4); contato só com telefone | aprovado (2026-11-22) |
| 12 | Ficha do cliente desativado, web (`Ficha-web-cliente-desativado.dc.html`) | Ação única "Reativar cliente"; dados somente leitura; local e equipamentos como estão (ADR-6), sem "Novo local" nem "Novo equipamento" | aprovado (2026-11-22) |
| 13 | Ficha do cliente, celular, Técnico (`Ficha-celular-tecnico.dc.html`) | Nenhum botão de escrita (ADR-7) e o aviso do papel | aprovado (2026-11-22) |
| 14 | Novo local, web (`Novo-local-web.dc.html`) | Página própria; nome do local e endereço em campos, CEP com máscara `00000-000` só no formato (ADR-19), UF em lista fechada, sem consulta de CEP (ADR-4) | aprovado (2026-11-22) |
| 15 | Novo local, celular, com erro (`Novo-local-celular-erros.dc.html`) | CEP incompleto e bairro obrigatório | aprovado (2026-11-22) |
| 16 | Local desativado na ficha, celular (`Local-desativado-celular.dc.html`) | Seção do local desativado aberta, com o aviso, o equipamento que continua ativo, sem "Novo equipamento" e com "Reativar local" | aprovado (2026-11-22) |
| 17 | Novo equipamento, web (`Novo-equipamento-web.dc.html`) | Local vindo da ficha, somente leitura; modelo ativo do catálogo com fabricante, tipo e característica no texto da opção; série; data opcional | aprovado (2026-11-22) |
| 18 | Novo equipamento, celular, com erro (`Novo-equipamento-celular-erros.dc.html`) | "Este modelo já tem um equipamento com esse número de série" e data no futuro | aprovado (2026-11-22) |
| 19 | Equipamento, celular (`Equipamento-celular.dc.html`) | Modelo e local somente leitura (ADR-5), série e data editáveis com o erro de série repetida (ADR-17), "Desativar equipamento" sem confirmação; o estado desativado com "Reativar equipamento" (clicar alterna; o *Tweak* `situacao` também) | aprovado (2026-11-22) |
| D1 | Clientes, web e celular, linha com a série casada | A linha "Série … · modelo" abaixo do nome quando a busca casou série (ADR-27), no estilo da linha de documento das telas 1 e 2 | derivado |
| D2 | Clientes, web e celular, nenhum cliente ativo | A tela 4 com "Nenhum cliente ativo", o texto do ADR-28 e "Ver desativados" (ADR-28) | derivado |
| D3 | Editar local, web | A página da tela 14 preenchida, título com o nome do local, "Salvar alterações" e o painel "Situação" com "Desativar local" (ADR-32) | derivado |
| D4 | Editar local, celular | A tela 15 preenchida, selo no topo e "Desativar local" abaixo do formulário, como a tela 19 (ADR-32) | derivado |
| D5 | Editar equipamento, web | A página da tela 17 com modelo e local somente leitura e o painel "Situação" com "Desativar equipamento" (ADR-32) | derivado |
| D6 | Local e equipamento desativados, web | A página só leitura com "Reativar local" ou "Reativar equipamento", como o estado desativado da tela 19 (ADR-32) | derivado |
| D7 | Celular do Comercial | A barra inferior com Clientes e Catálogo (sem Usuários), e as telas de detalhe e de formulário sem barra inferior | derivado |

## Decisões de UX que as telas propõem

- **Formulários de local e de equipamento em página própria**, não em painel lateral (escolha deixada aberta; ver abaixo).
- **Várias cidades na lista: até duas, depois "+N"** (escolha deixada aberta; ver abaixo).
- Documento mostrado com o **tipo à parte** ("CNPJ" + número formatado) na lista, na ficha e no formulário; ao digitar, o tipo aparece ao lado do campo, como a unidade no modelo do catálogo.
- O aviso de **documento imutável** usa o ícone de cadeado, no cadastro, na edição e na ficha.
- A regra "pelo menos um contato" vira o **título da seção "Contato"**, e o erro `contact_required` aparece no topo da seção, com os dois campos marcados.
- **Desativar** cliente fica na ficha (link de perigo, como no modelo); desativar local e equipamento fica na página de edição deles; **reativar** é ação única no estado desativado (cliente, equipamento) ou no próprio bloco do local.
- No celular, a ficha não tem barra inferior (é tela de detalhe, como o modelo no catálogo); a lista tem.
- O equipamento desativado aparece na tabela com o selo vazado e o texto em cinza; o local desativado tem o título em cinza, o selo vazado e um aviso.

## Escolhas deixadas abertas pela spec

- **Local e equipamento em página, não em painel lateral.** O local tem oito campos de endereço que precisam de largura (CEP e logradouro lado a lado, cidade e UF); o painel de 340 px do "Novo fabricante" serve a um campo só. A ficha tem tabelas largas que o painel espremeria. A página é a mesma forma do "Novo modelo" já aprovado, tem rota própria para a edição e é igual no celular, onde o painel não existe.
- **Várias cidades: até duas, em ordem alfabética, e "+N" com o texto "e mais N cidades" para leitor de tela.** Todas cabem na ficha. Quando a busca casa uma cidade, ela vem primeiro (o frontend reordena a lista `cities` que já recebe), para o motivo do resultado ficar visível. Mostrar todas quebraria a linha da tabela de um cliente com dez filiais; mostrar só o número ("3 cidades") obrigaria a abrir a ficha para saber onde é.

## O que o mockup não mostra e a implementação precisa decidir sem inventar

- **A edição de local e de equipamento na web** e o lugar de "Desativar local": decididos no ADR-32 (telas D3 a D6).
- **A série que casou com a busca:** decidida no ADR-27 (tela D1); **a lista vazia com todos desativados:** decidida no ADR-28 (tela D2).
- O **estado de carregamento** (esqueleto ou indicador) e a **paginação** da lista não foram desenhados; seguem o catálogo.
- O **celular** sem barra inferior nas telas de detalhe e de formulário, e a barra do Comercial (que não vê Usuários): tela D7.
- **Mensagens:** os textos que o mockup escreveu ("CEP incompleto: informe os 8 dígitos.", "Informe o bairro.", "CPF inválido: confira os números digitados.") são da tela e estão, com os demais, em [Mensagens da tela](../usecases.md#mensagens-da-tela) (ADR-31). O texto do erro de rede é o da tela 6.
- Só o tema escuro. Dados de exemplo ilustrativos (CPF e CNPJ com dígitos verificadores válidos, inventados); os modelos Huawei vêm do catálogo, os da Solis e o "Fabricante B" são ilustrativos. O design system DUNAS não foi instalado no canvas: os tokens foram aplicados à mão a partir do `tokens.json`, como no catálogo.

## Decisões da implementação (Tarefa 1 do plano do frontend)

Pontos em que o mockup e o que existe hoje divergem, ou que o mockup não desenha. Linhas marcadas como **divergência** levam à comparação final com o mockup.

| Ponto | O que o mockup mostra | O que existe hoje | Decisão |
|---|---|---|---|
| Ordem da navegação | "Clientes" primeiro, ícone de cartão de contato (o `ContactIcon` do `lucide-react`) | `NAV_ITEMS` com Catálogo e Usuários | Decidido: `NAV_ITEMS` com Clientes primeiro (ADR-26); a barra inferior herda a ordem (três itens para o Administrador, dois para Comercial e Técnico: D7) |
| Página inicial | não desenhada | `/` manda o Administrador para `/usuarios` e mostra "Em construção" aos demais | Decidido: `/` redireciona **todos os papéis, o Técnico inclusive,** para `/clientes` (ADR-26; o comentário do `home.tsx` já previa que a primeira feature de negócio o substitui) |
| Filtro de situação | tela 1: "Todos, Ativos, Desativados"; telas 2 a 6: "Ativos, Desativados, Todos" | o catálogo usa "Ativos, Desativados, Todos" | Decidido: "Ativos, Desativados, Todos" em todas (as telas 2 a 6, a maioria, e o catálogo); a tela 1 do mockup ("Todos, Ativos, Desativados") é **divergência** da comparação final; o padrão é `ACTIVE` |
| Tabelas da web | `<table>` de verdade na lista e na ficha | o catálogo usa linhas em `grid` | Decidido: `<table>` com `thead` e uma definição de colunas só (cabeçalho e linhas alinham por construção); cartões no celular |
| Seção recolhível | `<details>`/`<summary>` nativos na ficha do celular | nenhum componente | Decidido: o elemento nativo, estilizado na feature; nenhum componente novo |
| Erro sob o campo e carregando | ícone + texto; indicador | `FieldError` e `Spinner` moram em `features/catalog` (uma feature não importa outra) | Decidido: mover os dois para `shared/ui/{field-error,spinner}.tsx` na Tarefa 3, e o catálogo passa a importá-los de lá (mecânico, coberto pelos testes existentes) |
| Máscaras | CPF/CNPJ, `(00) 00000-0000`, `00000-000`, com `inputmode="numeric"` (no documento, ver a linha seguinte) | nada | Decidido: funções da feature sobre o `Input` (`size="compact"`), sem biblioteca; `inputmode="numeric"` só no telefone e no CEP |
| CNPJ alfanumérico | dica "Digite só os números" e `inputmode="numeric"` no documento | o `Document` aceita CNPJ com letras (2026) | Decidido: o campo **não é só numérico**: sem `inputmode="numeric"` (teclado de texto), dica "Digite o CPF ou o CNPJ" (sem "só os números"). O `Document` normaliza tirando tudo que não é letra ou dígito e pondo em maiúsculas; CPF tem 11 dígitos; CNPJ tem 14 caracteres `[A-Z0-9]`, com letras só nas 12 primeiras posições (as duas últimas, dígitos verificadores, são números). A máscara aceita letras (a-z viram A-Z) nas posições do CNPJ e só dígitos no CPF (11 caracteres). **Muda texto aprovado do mockup: divergência** da comparação final |
| Valor da característica | "12.000 BTU/h" (com milhar) na ficha e na opção do modelo | `formatAmount` do catálogo mostra "12000" | Decidido: "12000 BTU/h", como as telas do catálogo (`formatAmount`); o "12.000" do mockup é separador de milhar ilustrativo: **divergência** da comparação final |
| Selo de situação | caractere ●/○ dentro do selo | `StatusChip` com o círculo em SVG fixo | Decidido: `StatusChip` (tom `ok`; `off` com forma `ring`), como o catálogo |
| Telefone estrangeiro na edição | não desenhado | o `Phone` aceita qualquer E.164 (ADR-18) | Decidido: o campo mostra o número guardado como está, desabilitado, com a dica "Número de fora do Brasil: não se edita nesta tela", e o valor é reenviado sem mudança |
| Linha da série casada, nenhum ativo, edições derivadas | D1 a D7 | — | Decidido: implementar como a spec descreve e levar à comparação final |

O usuário aprovou estas decisões em 2026-11-24 ("pode partir pro front") e pode rever qualquer linha durante a execução; qualquer outra descoberta depois entra na comparação final.
