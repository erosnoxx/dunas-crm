# design/: 03-customers

Artefatos visuais de validação de UX/UI desta feature.

**Regra 6** ([`docs/CLAUDE.md`](../../../CLAUDE.md)): antes de fechar `domain-model.md` e `usecases.md`, é obrigatório gerar um artefato visual aqui, com descrição do que ele representa e **quais decisões de UX ele ajudou a validar**.

**Regra 7**: nenhuma tela, nova ou redesenhada, é implementada sem mockup previamente aprovado aqui. A implementação final é **pixel perfect** em relação a ele, e termina com comparação explícita mockup × resultado.

## Artefatos

As telas abaixo são a **lista do que precisa ser desenhado**, na identidade DUNAS (tema escuro, [`tokens.json`](../../../design-system/tokens.json)), na largura da web (1280 px) e do celular (390 px), seguindo o padrão do [catálogo](../../02-catalog/design/README.md). **Status de todas: pendente de mockup e aprovação.**

| # | Tela | O que precisa validar | Status |
|---|---|---|---|
| 1 | Clientes, web | Item "Clientes" no menu; lista com busca única (nome, documento, cidade) e filtro de situação; colunas (nome, documento formatado com o tipo, cidades, número de locais e de equipamentos, situação por forma além da cor); botão "Novo cliente" | pendente de mockup e aprovação |
| 2 | Clientes, celular | Os mesmos dados em cartões; "Novo cliente" fixo acima da barra inferior; item "Clientes" na barra inferior | pendente de mockup e aprovação |
| 3 | Clientes, celular, sem resultado | Estado vazio da busca, com "Limpar busca e filtros" | pendente de mockup e aprovação |
| 4 | Clientes, web, nenhum cadastrado | Estado vazio da primeira vez, com o convite a cadastrar | pendente de mockup e aprovação |
| 5 | Clientes, celular, Técnico | Sem botões de escrita e com o aviso de que o papel só consulta (ADR-7) | pendente de mockup e aprovação |
| 6 | Clientes, web, erro de carregamento | Falha de rede ao listar, com "Tentar de novo" | pendente de mockup e aprovação |
| 7 | Novo cliente, web | Documento (CPF ou CNPJ, o tipo reconhecido ao digitar), rótulo "Nome" ou "Razão social" conforme o tipo, e-mail e telefone opcionais, máscara brasileira no telefone | pendente de mockup e aprovação |
| 8 | Novo cliente, celular, com erro | Erro "já existe um cliente com este documento" e erro de CPF inválido no campo, com ícone e texto, não só cor; se a tela leva ao cliente existente | pendente de mockup e aprovação |
| 9 | Ficha do cliente, web | Dados do cliente (documento somente leitura, ADR-3), editar e desativar sem confirmação; os locais com endereço e situação; os equipamentos de cada local (modelo, fabricante, tipo e característica, série, data); ações "Novo local" e "Novo equipamento" | pendente de mockup e aprovação |
| 10 | Ficha do cliente, celular | A mesma ficha empilhada; como os locais e os equipamentos se expandem | pendente de mockup e aprovação |
| 11 | Ficha do cliente, celular, sem locais | Estado vazio dos locais, com o convite a cadastrar o primeiro (ADR-4) | pendente de mockup e aprovação |
| 12 | Ficha do cliente desativado, web | Dados somente leitura, ação única "Reativar cliente"; os locais e os equipamentos aparecem como estão (sem propagação, ADR-6), sem "Novo local" nem "Novo equipamento" | pendente de mockup e aprovação |
| 13 | Ficha do cliente, celular, Técnico | Ficha sem nenhum botão de escrita (ADR-7) | pendente de mockup e aprovação |
| 14 | Novo local, web | Nome do local e endereço em campos (CEP, logradouro, número, complemento, bairro, cidade, UF em lista fechada), sem consulta de CEP (ADR-4); editar local no mesmo formulário | pendente de mockup e aprovação |
| 15 | Novo local, celular, com erro | Erro de CEP inválido e de campo obrigatório | pendente de mockup e aprovação |
| 16 | Local desativado na ficha, celular | Local desativado com os equipamentos visíveis, sem "Novo equipamento", ação "Reativar local" | pendente de mockup e aprovação |
| 17 | Novo equipamento, web | Escolha do modelo ativo do catálogo (com fabricante, tipo e característica para distinguir), número de série, data de instalação opcional; local já definido pela ficha | pendente de mockup e aprovação |
| 18 | Novo equipamento, celular, com erro | Erro "este modelo já tem um equipamento com esse número de série" e data no futuro | pendente de mockup e aprovação |
| 19 | Equipamento, celular | Modelo e local somente leitura (ADR-5), série e data editáveis; desativar sem confirmação; estado desativado com "Reativar equipamento" | pendente de mockup e aprovação |

## Decisões de UX que as telas precisam fechar

- Ficha do cliente como **uma tela** com locais e equipamentos, ou locais em telas próprias.
- Formulários em **painel lateral** (como "Novo fabricante" no catálogo) ou em **página**.
- O que a lista mostra quando o cliente tem locais em várias cidades.
- Se o erro de documento repetido oferece **abrir o cliente existente** (a resposta de `customer_document_taken` não traz o id dele: incluí-lo muda o `usecases.md`).
- Ordem do item "Clientes" no menu e na barra inferior ([`open-questions.md`](../open-questions.md)).
