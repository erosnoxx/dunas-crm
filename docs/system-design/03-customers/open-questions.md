# Open questions: 03-customers

Pendências desta feature, cada uma com a minha recomendação. Os fechados pelo usuário viraram ADR-12 a ADR-16 em [`decisions.md`](decisions.md). Nenhuma das que restam exige decisão de arquitetura nova: o uso do catálogo pelo `customers` cabe na [`architecture.md`](../../rules/architecture.md) §4.4 como está (ADR-8).

## Mockups

As telas de [`design/`](design/README.md) estão pendentes de mockup e aprovação. `domain-model.md` e `usecases.md` só fecham depois delas (regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md)), e as colunas da lista, o conteúdo da ficha e a forma dos formulários podem mudar o retorno das queries.

**Fecha com:** o usuário desenhar e aprovar as telas.

## Como corrigir um documento digitado errado

O documento é imutável, sem command de correção, nem para o Administrador (ADR-12). Como a unicidade cobre os desativados (ADR-3), desativar e recadastrar não resolve: o cadastro errado segue dono do documento errado, e o cliente certo vira cliente novo, sem levar locais e equipamentos.

**Recomendação:** não decidir agora; o erro é raro (o `Document` confere os dígitos verificadores). Decidir quando o caso aparecer.

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

## Tamanho da ficha

A ficha traz todos os locais e todos os equipamentos, sem paginação. Um cliente com centenas de unidades deixaria a resposta e a tela pesadas.

**Recomendação:** sem paginação na primeira versão; revisar se algum cliente passar de 200 equipamentos.

## Importação em lote

A lista atual de clientes não está disponível ([`02-catalog` ADR-8](../02-catalog/decisions.md)).

**Recomendação:** cadastro à mão. Se a lista aparecer, uma carga única por seeder ou planilha, pelo domínio (as mesmas regras de documento e série), decidida com ela em mãos.

## Unir clientes duplicados

O documento único impede o duplicado pelo mesmo CPF ou CNPJ; sobra o duplicado por erro de digitação do documento.

**Recomendação:** não fazer agora. O caminho é desativar o errado (que continua dono do documento digitado: ADR-12); juntar locais e equipamentos de dois cadastros é uma feature própria se a frequência justificar.

## Navegação

O cliente é uma seção nova: item "Clientes" no menu da web e na barra inferior do celular (ao lado de Catálogo e Usuários; o Técnico passa a ter dois itens).

**Recomendação:** "Clientes" como primeiro item, por ser a tela mais usada; fecha com o mockup.
