# Open questions: 03-customers

Pendências desta feature. As fechadas pelo usuário viraram ADR-12 a ADR-16, e as que ele delegou viraram ADR-17 a ADR-26, em [`decisions.md`](decisions.md). Fica aberto só o que não se decide sem o usuário.

**Decisão de arquitetura pendente:** nenhuma. O uso do catálogo pelo `customers` cabe na [`architecture.md`](../../rules/architecture.md) §4.4 como está (ADR-8), e nenhuma decisão desta spec depende de regra nova.

## Mockups

As 19 telas de [`design/`](design/README.md) estão desenhadas e **pendentes de aprovação**. `domain-model.md` e `usecases.md` só fecham depois delas (regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md)), e as colunas da lista, o conteúdo da ficha e a forma dos formulários podem mudar o retorno das queries.

O desenho levantou pontos para decidir junto da aprovação:

- **Busca pela série sem o motivo:** o cliente achado pela série aparece na lista sem dizer qual equipamento casou, porque o `CustomerSummary` não traz série. Mostrar a série casada pede um campo novo no retorno do `ListCustomers`; sem ele, o técnico abre a ficha para achar a unidade.
- **"Nenhum cliente cadastrado" × todos desativados:** com a situação padrão (ativos), uma lista vazia não diz se não há cliente nenhum ou se todos estão desativados. O mockup mantém busca e filtro visíveis; distinguir os dois pede uma segunda contagem com `status=ALL` ou um texto que sirva aos dois.
- **E-mail e telefone no `CustomerSummary`:** a lista desenhada não os mostra; ficam no retorno só se outra tela precisar.
- **Editar local e equipamento de cliente desativado:** o domínio permite (ADR-6), e a tela 12 mantém "Editar local" e "Editar"; confirmar que é o desejado.
- **Mensagens sem texto na spec:** CEP incompleto, campo obrigatório do endereço e CPF ou CNPJ inválido na tela; o mockup propõe os textos.

**Fecha com:** o usuário aprovar as telas e decidir os pontos acima.
