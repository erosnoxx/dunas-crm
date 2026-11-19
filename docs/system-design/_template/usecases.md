# Use cases — <Nome da feature>

> Template — apague estas linhas de instrução ao preencher.
>
> Não fechar este documento antes de existir um artefato visual aprovado em `design/` (regra 6 de [`docs/CLAUDE.md`](../../CLAUDE.md)), nas features com tela.
>
> Forma dos use cases (sem interface, sem sufixo, `@UseCase`, separação `commands`/`queries`): [`architecture.md`](../../rules/architecture.md) §7. **Não reexplicar aqui.**

## Commands

Mudam estado. Sempre passam pelo aggregate.

### `<NomeDoCommand>`

- **Pacote:** `com.crm.<bc>.application.usecases.commands`
- **Anotação:** `@UseCase`, e os atributos que saem do default, com o motivo (`retriable = true` só quando chama serviço externo).
- **Entrada:** campos, e de onde vêm. Endpoint "self" nunca aceita id do próprio usuário no body: vem do principal autenticado.
- **Pré-condições:** o que precisa ser verdade antes. Qual exception é lançada quando não é (tipo + mensagem pt-BR minúscula, e o `code` se o frontend precisa distinguir).
- **Fluxo:** passos numerados. Carrega aggregate → chama método de domínio → salva → publica os eventos registrados.
- **Pós-condições:** o que ficou verdade depois, e quais eventos foram publicados.
- **Endpoint:** método + rota + nível de auth. Ou "sem endpoint", dizendo quem consome.

## Queries

Só leem. Retornam DTO/projeção. **Não carregam aggregate à toa.**

### `<NomeDaQuery>`

- **Pacote:** `com.crm.<bc>.application.usecases.queries`
- **Anotação:** `@UseCase(transactional = false)`
- **Entrada / filtro:** e o `Criteria` correspondente, se for listagem.
- **Retorno:** a projeção, campo a campo.
- **Cache:** se usa Redis, qual a chave, qual o TTL de segurança, e **qual evento invalida** (invalidação por evento é o mecanismo principal: [`architecture.md`](../../rules/architecture.md) §6.2).
- **Endpoint:** método + rota + nível de auth.

## Erros

Os códigos estáveis que o frontend usa (contrato público: [`architecture.md`](../../rules/architecture.md) §7.3).

| `code` | Status | Mensagem (pt-BR) | Quando |
|---|---|---|---|
| `<snake_case>` | 4xx | `<mensagem>` | `<situação>` |

## Consumo de integration event

Para cada evento de outro BC que esta feature consome:

- **Evento:** nome e BC de origem.
- **Listener:** classe em `<bc>-infrastructure`. É adapter de entrada: só traduz a mensagem e chama um use case desta `<bc>-application`. **Nenhuma regra de negócio no listener.**
- **Idempotência:** como uma reentrega é absorvida sem efeito duplicado (entrega é at-least-once).

## Fora de escopo

Use cases que alguém poderia esperar aqui e que **não** existem nesta rodada, com uma linha dizendo por quê ou para onde foram.
