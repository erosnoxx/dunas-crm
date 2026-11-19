# Open questions: 01-iam

## Números propostos que o usuário ainda não confirmou

Estão marcados como *proposta* em [`usecases.md`](usecases.md): convite válido por 48 horas, redefinição por 1 hora, intervalo de renovação da sessão de 5 minutos, limite de login por origem (20 em 15 minutos) e de pedidos de redefinição por e-mail (3 por hora).

**Fecha com:** o usuário confirmar ou trocar os números.

## Tela de reativar usuário

O ADR-10 cria a reativação, e os mockups aprovados não têm o botão nem o estado de usuário desativado. Dois artboards novos (web e celular) foram acrescentados ao canvas e aguardam aprovação.

**Fecha com:** o usuário aprovar os dois artboards.

## Redesenho do seeder

O ADR-2 depende de um seeder idempotente, transacional e seguro para produção, e o atual não é ([`architecture.md`](../../rules/architecture.md) §13). O `Seeder<T>` lê JSON do classpath e recebe uma lista, e "criar só se não existir nenhum usuário" pede uma checagem que o contrato atual não tem. A conta nasce com `mustChangePassword`.

**Fecha com:** proposta de desenho aprovada pelo usuário, antes do plano da 01-iam.

## Como o rótulo do aparelho é obtido

O mockup mostra "Galaxy A54, app DUNAS" e "Chrome, Windows". No navegador sai do `User-Agent`; no app, o nome do aparelho vem de um cabeçalho enviado pelo wrapper. O `User-Agent` não é confiável e o rótulo é só informativo.

**Fecha com:** o plano, com a decisão de qual biblioteca (ou regra própria) lê o `User-Agent`.

## Hash de senha: biblioteca e parâmetros do Argon2id

O contrato `PasswordHasher` está fixado; a biblioteca e os parâmetros (memória, iterações, paralelismo) não.

**Fecha com:** o plano, com medida de tempo na VPS-alvo.
