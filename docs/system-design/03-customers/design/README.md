# design/: 03-customers

Artefatos visuais de validação de UX/UI desta feature.

**Regra 6** ([`docs/CLAUDE.md`](../../../CLAUDE.md)): antes de fechar `domain-model.md` e `usecases.md`, é obrigatório gerar um artefato visual aqui, com descrição do que ele representa e **quais decisões de UX ele ajudou a validar**.

**Regra 7**: nenhuma tela, nova ou redesenhada, é implementada sem mockup previamente aprovado aqui. A implementação final é **pixel perfect** em relação a ele, e termina com comparação explícita mockup × resultado.

## Artefatos

Canvas com 19 telas na identidade DUNAS (tema escuro, [`tokens.json`](../../../design-system/tokens.json)), no mesmo padrão do [catálogo](../../02-catalog/design/README.md) (mesmos valores de cor, fonte, espaçamento, campo, botão, tabela, selo de situação e casca): <https://claude.ai/artifact/NxExPYWS7kHNhmdB9jbyou>. O código-fonte das telas está em [`canvas/project/`](canvas/project/) (um `.dc.html` por tela e o `canvas.json`). Web em 1280 px, celular em 390 × 844; a ficha da web (1320 px) e a do celular (1640 px) estão na altura da página inteira. **Status:** telas 1 a 19 pendentes de aprovação do usuário.

| # | Tela | O que valida | Status |
|---|---|---|---|
| 1 | Clientes, web (`Main.dc.html`) | "Clientes" primeiro no menu (ADR-26); busca única com placeholder e dica de que aceita o número de série (ADR-13); filtro de situação; colunas Cliente (nome fantasia abaixo), Documento (tipo + número formatado), Cidades (até duas e "+N"), Locais, Equipamentos (só ativos), Situação por forma | pendente de aprovação |
| 2 | Clientes, celular (`Clientes-celular.dc.html`) | Os mesmos dados em cartões; "Novo cliente" fixo acima da barra inferior; barra com Clientes, Catálogo e Usuários | pendente de aprovação |
| 3 | Clientes, celular, sem resultado (`Clientes-celular-sem-resultado.dc.html`) | Busca por série sem resultado, com "Limpar busca e filtros" e "Novo cliente" | pendente de aprovação |
| 4 | Clientes, web, nenhum cadastrado (`Clientes-web-vazio.dc.html`) | Primeira vez, com o convite "Cadastrar o primeiro cliente"; busca e filtro continuam na tela | pendente de aprovação |
| 5 | Clientes, celular, Técnico (`Clientes-celular-tecnico.dc.html`) | Sem "Novo cliente", aviso de que o papel só consulta (ADR-7); barra com Clientes e Catálogo (ADR-26); busca pela série da etiqueta | pendente de aprovação |
| 6 | Clientes, web, erro de carregamento (`Clientes-web-erro.dc.html`) | Falha de rede no lugar da lista, com "Tentar de novo"; menu, busca e "Novo cliente" usáveis | pendente de aprovação |
| 7 | Novo cliente, web (`Cliente-formulario-web.dc.html`) | Tipo reconhecido ao lado do documento formatado e o aviso "o documento não pode ser alterado depois de salvo" (ADR-24); "Razão social" e "Nome fantasia (opcional)" com CNPJ (ADR-21); seção "Contato" que diz que um dos dois basta (ADR-16); telefone com máscara (ADR-18). No *Tweak* `modo = editar`: o mesmo formulário com o documento somente leitura e o mesmo aviso | pendente de aprovação |
| 8 | Novo cliente, celular, com erro (`Novo-cliente-celular-erros.dc.html`) | Três estados no *Tweak* `erro`: documento repetido com "Ver cliente com este documento" (ADR-23); CPF inválido com o formulário sem contato (`contact_required`); telefone inválido ("informe DDD e número"). Ícone e texto, não só cor | pendente de aprovação |
| 9 | Ficha do cliente, web (`Ficha-web.dc.html`) | Dados com o documento travado (ADR-3); "Editar cliente" e "Desativar cliente"; um cartão por local, todos abertos, com nome, endereço, situação, equipamentos ativos, "Editar local" e "Novo equipamento"; tabela com modelo, fabricante, tipo e característica, série, data e situação, ativos primeiro (ADR-22); local desativado no fim, marcado, com "Reativar local" | pendente de aprovação |
| 10 | Ficha do cliente, celular (`Ficha-celular.dc.html`) | A mesma ficha empilhada; cada local é uma seção recolhível, aberta se ativa e fechada se desativada (ADR-22); "Novo local" fixo embaixo | pendente de aprovação |
| 11 | Ficha do cliente, celular, sem locais (`Ficha-celular-sem-locais.dc.html`) | Estado vazio dos locais, com "Cadastrar o primeiro local" (ADR-4); contato só com telefone | pendente de aprovação |
| 12 | Ficha do cliente desativado, web (`Ficha-web-cliente-desativado.dc.html`) | Ação única "Reativar cliente"; dados somente leitura; local e equipamentos como estão (ADR-6), sem "Novo local" nem "Novo equipamento" | pendente de aprovação |
| 13 | Ficha do cliente, celular, Técnico (`Ficha-celular-tecnico.dc.html`) | Nenhum botão de escrita (ADR-7) e o aviso do papel | pendente de aprovação |
| 14 | Novo local, web (`Novo-local-web.dc.html`) | Página própria; nome do local e endereço em campos, CEP com máscara `00000-000` só no formato (ADR-19), UF em lista fechada, sem consulta de CEP (ADR-4) | pendente de aprovação |
| 15 | Novo local, celular, com erro (`Novo-local-celular-erros.dc.html`) | CEP incompleto e bairro obrigatório | pendente de aprovação |
| 16 | Local desativado na ficha, celular (`Local-desativado-celular.dc.html`) | Seção do local desativado aberta, com o aviso, o equipamento que continua ativo, sem "Novo equipamento" e com "Reativar local" | pendente de aprovação |
| 17 | Novo equipamento, web (`Novo-equipamento-web.dc.html`) | Local vindo da ficha, somente leitura; modelo ativo do catálogo com fabricante, tipo e característica no texto da opção; série; data opcional | pendente de aprovação |
| 18 | Novo equipamento, celular, com erro (`Novo-equipamento-celular-erros.dc.html`) | "Este modelo já tem um equipamento com esse número de série" e data no futuro | pendente de aprovação |
| 19 | Equipamento, celular (`Equipamento-celular.dc.html`) | Modelo e local somente leitura (ADR-5), série e data editáveis com o erro de série repetida (ADR-17), "Desativar equipamento" sem confirmação; o estado desativado com "Reativar equipamento" (clicar alterna; o *Tweak* `situacao` também) | pendente de aprovação |

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

- **A edição de local e de equipamento na web** não foi desenhada: é a página das telas 14 e 17 preenchida, com "Salvar alterações" e o painel "Situação" com "Desativar", como a página do modelo no catálogo (escrito na legenda das telas).
- **Busca pela série sem pista do motivo:** o cliente achado pela série aparece sem dizer qual equipamento casou (tela 5), porque o `CustomerSummary` não traz a série. Ver a pendência em [`open-questions.md`](../open-questions.md).
- O **estado de carregamento** (esqueleto ou indicador) e a **paginação** da lista não foram desenhados; seguem o catálogo.
- O **celular** sem barra inferior nas telas de detalhe e de formulário, e a barra do Comercial (que não vê Usuários) não foram desenhados.
- Mensagens sem texto na spec, escritas aqui: "CEP incompleto: informe os 8 dígitos.", "Informe o bairro.", "CPF inválido: confira os números digitados." e o texto do erro de rede.
- Só o tema escuro. Dados de exemplo ilustrativos (CPF e CNPJ com dígitos verificadores válidos, inventados); os modelos Huawei vêm do catálogo, os da Solis e o "Fabricante B" são ilustrativos. O design system DUNAS não foi instalado no canvas: os tokens foram aplicados à mão a partir do `tokens.json`, como no catálogo.
