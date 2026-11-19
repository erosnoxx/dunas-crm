# Telas complementares do IAM

Geradas no mesmo canvas ([`README.md`](README.md)) para cobrir o que a spec exige e os 12 mockups aprovados não desenhavam ([`docs/plans/2026-11-19-iam-frontend.md`](../../../plans/2026-11-19-iam-frontend.md), Tarefa 1). **Status: aprovadas** pelo usuário em 2026-11-19, com as cópias e as telas derivadas listadas abaixo.

## Artefatos

Canvas: <https://claude.ai/artifact/CvVeA26TCvv9dBdXWwZ4eU> (as linhas "Senha esquecida e troca de senha", "Conta e navegação" e "Administrador, o que faltava").

| # | Tela | O que valida | Status |
|---|---|---|---|
| 13 | Esqueci minha senha, celular | Um campo só (e-mail) e a promessa "se ele existir, enviamos": a tela nunca diz se o e-mail tem conta | aprovado |
| 14 | Esqueci minha senha, link enviado | O aviso é o mesmo exista o e-mail ou não; validade de 1 hora dita; saída para o login ou pedir de novo | aprovado |
| 15 | Redefinir senha, celular | Reaproveita a tela de definir senha (campo, checklist e contador) com título e botão próprios | aprovado |
| 16 | Trocar senha, celular | "Senha atual" mais nova senha com checklist; dentro da casca autenticada, com voltar | aprovado |
| 17 | Trocar a senha inicial, celular | A obrigatória do primeiro Administrador (ADR-2): sem voltar, sem navegação, só "Sair" | aprovado |
| 18 | Menu da conta, celular | O avatar abre uma folha com Minhas sessões, Trocar senha e Sair | aprovado |
| 19 | Navegação de Comercial e Técnico, web | Sem item de trabalho ainda: só a conta, as sessões e a troca de senha; página inicial provisória | aprovado |
| 20 | Minhas sessões, web | A mesma lista do celular, na casca da web | aprovado |
| 21 | Convidar usuário, celular | O formulário do diálogo da web, em tela cheia, com o botão fixo no rodapé | aprovado |
| 22 | Usuário com convite pendente, celular | Ações do convite pendente: Reenviar convite (principal) e Desativar; papel só leitura (a troca exige conta ativa) | aprovado |
| 23 | O próprio usuário, celular | Sem trocar papel nem desativar (ADR-8); a sessão do aparelho marcada e sem "Encerrar" | aprovado |
| 24 | Lista com paginação, celular | "Mostrando N de M" e "Carregar mais" | aprovado |
| 25 | Usuário com convite pendente, web | O mesmo painel, na lista da web | aprovado |
| 26 | O próprio usuário e paginação, web | O painel do próprio usuário e o rodapé "Mostrando 1 a 5 de 54" com Anterior e Próxima | aprovado |

## Derivadas, sem artboard próprio

Entram na comparação final para aprovação:
- esqueci minha senha, redefinir senha e trocar senha **na web** usam o painel centralizado de 440 px do `Entrar · web`;
- a troca voluntária na web fica na casca, com a barra lateral;
- a barra lateral do Administrador passa a ter também "Trocar senha" no bloco da conta;
- os estados de erro e de bloqueio do login na web, e o celular sem aviso, seguem os do celular com o `Alert` no mesmo lugar.

## Cópias novas (aprovadas)

| Onde | Texto |
|---|---|
| Esqueci minha senha | "Esqueceu a senha?" · "Informe o e-mail da sua conta. Se ele existir, enviamos um link para criar uma nova senha." · botão "Enviar link" · "Voltar para o login" |
| Link enviado | "Confira seu e-mail" · "Se o e-mail estiver cadastrado, enviamos um link para criar uma nova senha. O link vale por 1 hora." · "Voltar para o login" · "Pedir o link de novo" |
| Redefinir senha | "Crie uma nova senha" · "Defina uma senha para voltar a entrar." · botão "Salvar nova senha" |
| Link de redefinição expirado ou usado (reaproveita o artboard do convite expirado) | "Este link expirou" ou "Este link já foi usado ou foi substituído" · "Por segurança, o link deixa de valer depois de um tempo. Peça um novo na tela de login." · botão "Pedir novo link" |
| Trocar a senha inicial | "Troque a senha inicial" · "Sua conta foi criada com uma senha provisória. Defina a sua para continuar." · botão "Salvar e continuar" · "Sair" |
| Convite pendente | "Convite não aceito" · "Tiago ainda não definiu a senha. Reenviar o convite invalida o link anterior." · "Reenviar convite" |
| O próprio usuário | "Você não pode mudar o seu papel nem desativar a sua conta." |
| Página inicial provisória | "Início" · "Em construção" · "As telas de trabalho chegam com as próximas funcionalidades. Por enquanto, aqui você acessa a sua conta e as suas sessões." |
| Paginação | "Mostrando 1 a 5 de 54 usuários" · "Anterior" · "Próxima" · "Carregar mais" |
| Avisos de sucesso (sem artboard) | "Convite enviado" · "Convite reenviado" · "Senha alterada" |
