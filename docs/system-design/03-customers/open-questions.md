# Open questions: 03-customers

Pendências desta feature, cada uma com a minha recomendação. Nenhuma exige decisão de arquitetura nova: o uso do catálogo pelo `customers` cabe na [`architecture.md`](../../rules/architecture.md) §4.4 como está (ADR-8).

## Mockups

As telas de [`design/`](design/README.md) estão pendentes de mockup e aprovação. `domain-model.md` e `usecases.md` só fecham depois delas (regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md)), e as colunas da lista, o conteúdo da ficha e a forma dos formulários podem mudar o retorno das queries.

**Fecha com:** o usuário desenhar e aprovar as telas.

## O documento pode mudar depois do cadastro?

É imutável (ADR-3): errou, desativa e cadastra de novo. O efeito colateral: o cadastro errado continua dono do documento digitado. Como o `Document` confere os dígitos verificadores, a chance de um erro de digitação formar o CPF válido de outra pessoa que depois vire cliente é muito pequena.

**Recomendação:** manter imutável. Se o caso aparecer, um command próprio de correção, só para o Administrador, com a checagem de unicidade.

## A série pode mudar depois do cadastro?

O modelo propõe que sim (ADR-5), para corrigir digitação, com a checagem de unicidade. A alternativa (imutável, como o modelo e o local) obrigaria desativar e recadastrar por um caractere errado, e a série desativada continuaria ocupada.

**Recomendação:** editável, como está.

## Formato do telefone na tela

O `Phone` do shared aceita só E.164 (`+5511999999999`). A equipe digita "(11) 99999-9999".

**Recomendação:** o formulário tem máscara brasileira (DDD e número, fixo ou celular) e o frontend envia `+55` + DDD + número; número estrangeiro não é digitável nesta versão. A exibição formata de volta. Nenhuma mudança no VO.

## CEP: só formato

O `ZipCode` remove o que não é dígito e exige 8, sem saber se o CEP existe. "00000-000" passa.

**Recomendação:** aceitar assim nesta versão (decisão do usuário: sem consulta de CEP). A consulta, quando vier, preenche o formulário pela integração com Translator, sem mudar o domínio.

## Limites propostos

`CustomerName` de 2 a 120, `LocationName` de 2 a 60, `SerialNumber` de 1 a 60 caracteres. Razões sociais longas passam de 100 caracteres; série de inversor costuma ter de 10 a 20.

**Recomendação:** aprovar os números; ajustar se a lista do dono trouxer série maior.

## Nome fantasia e pessoa de contato

Pessoa jurídica tem razão social e nome fantasia, e a equipe conhece a empresa pelo fantasia. A decisão do usuário é um nome e um contato só.

**Recomendação:** não incluir agora. Se a busca por fantasia fizer falta, um campo `tradeName` opcional, que entra na busca de texto.

## Pelo menos um contato?

Os dois são opcionais (ADR-2).

**Recomendação:** manter opcionais até a agenda de manutenção existir; ela é que precisa avisar o cliente e pode pedir a regra.

## Tamanho da ficha

A ficha traz todos os locais e todos os equipamentos, sem paginação. Um cliente com centenas de unidades deixaria a resposta e a tela pesadas.

**Recomendação:** sem paginação na primeira versão; revisar se algum cliente passar de 200 equipamentos.

## Busca por número de série

O técnico em campo tem a etiqueta do inversor e quer achar o cliente. A busca da lista não olha a série.

**Recomendação:** incluir a série na busca de texto da lista de clientes (casa a série de um equipamento ativo). Custo baixo; fica fora até o usuário aprovar, porque muda a tela.

## Mover equipamento e reaproveitar série

O local e o modelo do equipamento não mudam, e a série é única mesmo entre desativados (ADR-5). Uma unidade retirada de um cliente e instalada em outro, ou trocada de local no mesmo cliente, não tem caminho: o cadastro novo bate na série.

**Recomendação:** quando acontecer, um command `MoveInstalledEquipment` (troca o `locationId`, exige o novo local e o cliente dele ativos). Não abrir a unicidade para só os ativos: perderia a garantia de que a série identifica uma unidade.

## Importação em lote

A lista atual de clientes não está disponível ([`02-catalog` ADR-8](../02-catalog/decisions.md)).

**Recomendação:** cadastro à mão. Se a lista aparecer, uma carga única por seeder ou planilha, pelo domínio (as mesmas regras de documento e série), decidida com ela em mãos.

## Unir clientes duplicados

O documento único impede o duplicado pelo mesmo CPF ou CNPJ; sobra o duplicado por erro de digitação do documento.

**Recomendação:** não fazer agora. O caminho é desativar o errado; juntar locais e equipamentos de dois cadastros é uma feature própria se a frequência justificar.

## Busca sem acento

Mesma pendência do catálogo ([`02-catalog` open-questions](../02-catalog/open-questions.md)): "sao paulo" não acha "São Paulo". Na busca por cidade isso pesa mais que no catálogo.

**Recomendação:** resolver junto com o catálogo (extensão `unaccent` ou normalização), numa mudança só.

## Navegação

O cliente é uma seção nova: item "Clientes" no menu da web e na barra inferior do celular (ao lado de Catálogo e Usuários; o Técnico passa a ter dois itens).

**Recomendação:** "Clientes" como primeiro item, por ser a tela mais usada; fecha com o mockup.
