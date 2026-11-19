# Ideias do sistema — brainstorming

> **Nada aqui é spec nem requisito.** Ideia não é verdade: só vira trabalho quando promovida para uma feature em [`docs/system-design/`](../system-design/00-overview.md), com aprovação do dono. Regras da pasta: [`docs/CLAUDE.md`](../CLAUDE.md).

**Data:** 2026-11-19 · **Origem:** dono do projeto e o irmão dele, dono da empresa de energia solar · **Estado do documento:** aberto

## Tema

O que o sistema da empresa de energia solar vai ter, além do cadastro básico. Ainda é uma lista de ideias.

## Restrições que valem para tudo

Decisões globais em [`00-overview.md`](../system-design/00-overview.md). Específico deste tema:

- As usinas já ficam cadastradas na conta da **empresa** nos portais de cada marca (Huawei, Solis, Sungrow), com acesso compartilhado ao cliente.
- O acesso à API de cada marca depende de credencial e, em alguns casos, de aprovação do fabricante.

## Ideias

| ID | Ideia | Origem | Valor esperado | Custo | Risco principal | Status |
|---|---|---|---|---|---|---|
| I1 | Cadastro central de clientes e usinas, de qualquer marca, com vínculo usina-cliente | dono | uma visão única da carteira, que os apps dos fabricantes não dão | médio | depende do cadastro nas contas da empresa | ideia |
| I2 | Dashboard de geração por cliente e por usina, atualizado a cada intervalo | dono | acompanhar a carteira e perceber queda de geração | médio | limites de chamada e acesso à API de cada marca | ideia |
| I3 | Relatório "no último período seu sistema gerou X, isso representa R$ Y no seu bolso" | irmão | o cliente enxerga o valor e a empresa retém o cliente | baixo | o valor depende da tarifa; sai como estimativa | ideia |
| I4 | Portal em que o cliente agenda uma visita | irmão | menos telefone, agenda organizada | baixo | exige acesso de cliente no `iam` | ideia |
| I5 | Calendário de manutenção ligado a alertas de queda de geração | dono | manutenção que vira receita recorrente | médio | alerta falso gera visita desnecessária | ideia |
| I6 | Alerta de créditos de energia prestes a vencer, como oportunidade de venda (ex. mais carga, ar-condicionado) | irmão (variação do dono) | o cliente não perde crédito, a empresa vende | médio | o saldo está na fatura, não na API do inversor | ideia |
| I7 | Vender os créditos vencendo e ganhar porcentagem | irmão | receita nova | alto | regulatório e de custódia, ver abaixo | ideia `fere: não custodia dinheiro de terceiros` |
| I8 | Proposta e orçamento simplificados | dono | agilizar a venda | médio | formato e regras tarifárias mudam | ideia |
| I9 | Técnico cadastra a usina no portal da marca pelo nosso app, sem entrar no portal | dono | simplifica o campo | alto | pode não existir criação de usina na API | ideia |
| I10 | Ler a fatura da distribuidora em PDF para obter o saldo de créditos | dono | alimenta o alerta de I6 | alto | formato varia por distribuidora | ideia |

## Fatos e hipóteses

| ID | Afirmação | Tipo | Fonte / como verificar |
|---|---|---|---|
| I1, I2 | As usinas estão cadastradas na conta da empresa, com acesso compartilhado ao cliente | fato | resposta do irmão, 2026-11-19 |
| I2 | A API da Huawei limita login (5 a cada 10 min) e chamadas de dados (~1 por minuto por endpoint) | hipótese | guias de terceiros; conferir na referência oficial da Huawei |
| I2 | Sungrow e Solis exigem aprovação do fabricante para acesso por aplicativo de terceiro | hipótese | guias de terceiros; conferir nos portais de desenvolvedor |
| I2 | Os dados chegam a cada 5 minutos nas três marcas | hipótese | guias de terceiros |
| I6 | Os créditos de energia valem 60 meses | hipótese | dito pelo irmão; conferir na regra vigente da distribuidora |
| I7 | Créditos de energia não podem ser vendidos a terceiros por dinheiro; só redistribuídos entre unidades do mesmo titular ou em modelos regulados | hipótese | conhecimento geral, **não verificado**; precisa de parecer de quem conheça a regulação |
| I9 | As APIs não têm endpoint de criação de usina | hipótese | nenhuma menção encontrada na pesquisa, que cobriu só leitura de dados |
| I10 | Depende de a empresa ter acesso às faturas dos clientes | hipótese | pergunta aberta ao irmão |

## Perguntas abertas

- O irmão tem acesso às faturas dos clientes hoje, ou só ao que o cliente envia? Destrava I6 e I10.
- Quais marcas e quantas usinas existem hoje? Define o tamanho real de I2.
- Quem confere a regra de créditos de energia? Destrava a decisão sobre I7.

## Descartadas

Nenhuma.

## Promovidas

Nenhuma. A primeira feature, `01-iam`, é de base e não vem desta lista.
