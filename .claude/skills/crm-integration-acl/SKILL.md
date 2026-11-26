---
name: crm-integration-acl
description: Use ao integrar o crm com qualquer sistema de terceiro (nuvem de fabricante de inversor, gateway, API externa, provedor de e-mail) ou ao escrever um webhook de entrada — contrato, Client, Translator, DTOs externos, configuração e teste. Invoque antes de escrever qualquer classe que fale com a rede de fora.
---

# Integração com terceiro (Anti-Corruption Layer)

A regra e o motivo estão em [`docs/rules/architecture.md`](../../../docs/rules/architecture.md) §4.3, travados por `AntiCorruptionLayerRulesTest`. Esta skill é como se escreve uma integração. A referência viva, completa e testada, é `common.infrastructure.integrations.smtp`: abra-a antes de começar.

## Ordem

1. **Contrato primeiro, a partir do domínio.** `<Nome>Service` em `<bc>.application.contracts.services` (ou `common-application`, se mais de um BC usa). Tipos de entrada e retorno são **nossos**, nomeados como o negócio fala. Não copie a assinatura nem o vocabulário da API externa: trocar de fornecedor não pode mexer em `domain` nem `application`.
2. **Pacote da integração:** `<bc>.infrastructure.integrations.<nome>`, com:

   | Classe | Papel |
   |---|---|
   | `<Fornecedor>Client` | fala com o terceiro (HTTP/SDK); só conhece o modelo dele |
   | `<Fornecedor>Translator` | **única** classe que conhece o modelo externo e o nosso |
   | `<Fornecedor><Nome>ServiceImpl` | implementa o contrato: pede o formato ao Translator, chama o Client, pede a volta |
   | `dtos/` | o modelo externo; nunca sai do pacote |
   | `<Fornecedor>Config` | `@Configuration` da integração |

   Client, Translator e ServiceImpl ficam **package-private**; só a Config é pública. A Config mora **dentro** da integração porque conhece o tipo de terceiro (fora do pacote, o ArchUnit recusa).
3. **Propriedades:** `record` com `@ConfigurationProperties("crm.<nome>")` e `@DefaultValue` (subir o app não pode exigir configuração). Segredo (chave, token) vem de variável de ambiente e **nunca** vai para log, mensagem de exceção nem repositório.
4. **Falhas:** comunicação com o terceiro vira `ExternalServiceException`; falha ao montar/ler (mecanismo interno) vira `InternalServerErrorException`. Nenhuma exception do terceiro sobe crua ([`code-style.md`](../../../docs/rules/code-style.md) §6).
5. **Repetição:** retry só de falha **transitória** (rede, 5xx, limite de taxa), no próprio `ServiceImpl` com Resilience4j, como o `SmtpEmailSenderServiceImpl`. Erro de autenticação ou de montagem não melhora na segunda tentativa. `@UseCase(retriable = true, circuitBreaker = true)` só em use case que chama o serviço externo diretamente **e** tem efeito idempotente.
6. **SDK próprio do terceiro?** Registre o pacote dele em `THIRD_PARTY_ACCESS` do `AntiCorruptionLayerRulesTest`: com SDK, o modelo externo são as classes dele, e sem o registro nada impede que vazem.
7. **Teste do Translator** com **payload real** do terceiro (copiado da documentação ou de uma resposta capturada), sem rede. É o teste que avisa quando o terceiro muda o formato. Veja `SmtpTranslatorTest`.

## Vários fornecedores para o mesmo conceito

Um contrato, uma pasta `integrations.<fornecedor>` por fornecedor (ex. um `GenerationReader` do domínio, e `integrations.huawei`, `integrations.solis`, `integrations.sungrow`, cada um com seu Client, Translator e DTOs). O domínio nunca sabe qual fornecedor está atrás.

- **Limite de chamadas do fornecedor é preocupação do Client** dele (espaçamento entre chamadas, renovação de token, janela de login), nunca do use case.
- Guarde a resposta **bruta** quando o formato do terceiro mudar com frequência: dá para reprocessar sem perder histórico.
- Credencial de cada conta/cliente do fornecedor é segredo de integração, guardado cifrado: não é responsabilidade do `iam`.

## Webhook de entrada

O controller mora em `web`: recebe o DTO do terceiro, entrega ao Translator (permitido para `web` em `..infrastructure.integrations..`) e chama o use case com tipos nossos. Entrada que pode chegar duplicada usa o `IdempotencyGuard` ([`architecture.md`](../../../docs/rules/architecture.md) §5.4). Confira assinatura/segredo do webhook antes de qualquer outra coisa.

## Checklist

- [ ] Contrato escrito a partir do domínio, sem vocabulário do terceiro
- [ ] Translator é a única classe que lê o modelo externo
- [ ] Config dentro do pacote da integração; propriedades com `@DefaultValue`
- [ ] Nenhuma exception de terceiro vaza; nenhum segredo em log
- [ ] Pacote do SDK registrado no ArchUnit (se houver SDK)
- [ ] Teste de Translator com payload real; `mvn test` verde
