# Open questions: 01-iam

## Números propostos que o usuário ainda não confirmou

Estão marcados como *proposta* em [`usecases.md`](usecases.md): convite válido por 48 horas, redefinição por 1 hora, intervalo de renovação da sessão de 5 minutos, limite de login por origem (20 em 15 minutos) e de pedidos de redefinição por e-mail (3 por hora).

**Fecha com:** o usuário confirmar ou trocar os números.

## Como o rótulo do aparelho é obtido

O mockup mostra "Galaxy A54, app DUNAS" e "Chrome, Windows". O backend usa o cabeçalho `X-Device-Name` quando ele vem (o app, ou o frontend se mandar um nome legível) e, sem ele, o `User-Agent` cru; os dois são cortados em 80 caracteres, e sem nenhum o rótulo é "Aparelho desconhecido". Nenhuma biblioteca lê o `User-Agent`, então a tela de sessões mostra o texto cru do navegador, e não "Chrome, Windows". O rótulo é só informativo.

**Fecha com:** decidir se o frontend monta o nome legível e o manda em `X-Device-Name`, ou se o backend passa a interpretar o `User-Agent`.

## Custo do Argon2id medido na VPS

A biblioteca (Spring Security Crypto com Bouncy Castle) e os defaults (19 MiB, 2 passadas, 1 thread, o mínimo da OWASP) estão no código, em `crm.iam.argon2.*`. Falta medir na VPS-alvo e ajustar para cada hash levar de 200 a 500 ms.

**Fecha com:** a medida na VPS e os valores de `crm.iam.argon2.*` no ambiente de produção.

## Achados da implementação do backend

Itens que a execução do plano encontrou e que pedem decisão do usuário.

- **Tempo do pedido de redefinição.** A resposta de `POST /auth/password/forgot` é igual para qualquer e-mail, mas só a conta `ACTIVE` grava no banco (invalida os links pendentes e grava o novo). A diferença de tempo pode revelar quem tem conta ativa. Alternativas: igualar o trabalho, ou responder antes de fazê-lo.
- **`revoked_by` nulo na desativação.** O `UserDeactivated` não carrega quem desativou, então as sessões revogadas pela desativação ficam com `revoked_by` nulo ("revogada pela desativação"). Alternativa: acrescentar o `actorId` ao `UserDeactivated`, o que muda o modelo de domínio.
- **Texto dos e-mails.** Assuntos e corpos do convite e da redefinição (`IamEmailTemplates`) foram escritos na implementação e ainda não foram aprovados.
- **Código `csrf_invalid`.** A cadeia de segurança responde 403 `csrf_invalid` à escrita por cookie sem o cabeçalho de CSRF. O código e a mensagem estão na tabela de erros de [`usecases.md`](usecases.md) como *proposta*.
- **Falha ao revogar na desativação.** Se o listener do `UserDeactivated` falhar, a desativação continua gravada (a falha só é logada) e as sessões seguem vivas no banco. A autenticação as recusa porque a conta não está ativa, salvo a sessão que estiver no cache, que vale até o TTL (60 s). E um `ReactivateUser` depois disso devolveria a vida a essas sessões. Alternativas: revogar as sessões na mesma transação da desativação, ou fazer o `ReactivateUser` revogar o que sobrou.
- **`crm.iam.app-url` em produção.** O default é o endereço do frontend em desenvolvimento (`http://localhost:5173`). Um deploy que esqueça a propriedade manda convites e links de redefinição apontando para localhost, sem erro nenhum. Alternativa: sem default no perfil `prod`, derrubando o boot.
- **Senha inicial com `${`.** A senha do Administrador inicial passa pela resolução de placeholder do seeder, então uma senha que contenha `${` é lida como outra variável e o boot falha. Hoje é só uma nota em `application.properties`.
