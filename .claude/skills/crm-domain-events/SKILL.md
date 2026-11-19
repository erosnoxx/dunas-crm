---
name: crm-domain-events
description: Use ao criar um domain event ou integration event no crm, ao publicá-lo, ou ao consumir o evento de outro BC (listener, fila, DLQ, projeção local, idempotência do consumidor). Invoque antes de escrever qualquer `record ... implements DomainEvent|IntegrationEvent` e antes de qualquer listener de mensageria.
---

# Eventos

Regras e motivo: [`architecture.md`](../../../docs/rules/architecture.md) §5 (comunicação entre BCs, outbox) e §5.1 (domain ≠ integration). Referências vivas: `SpringEventPublisherImpl` (roteamento), `JpaOutboxWriterImpl` e `OutboxDispatcherJob` (outbox).

## Qual dos dois

| Quem precisa saber? | Tipo |
|---|---|
| Outra parte do **mesmo** BC (um listener que manda e-mail, atualiza projeção interna) | `DomainEvent` |
| **Outro BC** | `IntegrationEvent` |

Evento é para **consistência eventual**. Consistência imediata dentro do BC é escrita no use case, não por evento (§6.1). Comando que precisa do resultado de outro BC no mesmo request **não** é evento: é `orchestration` (§5.4).

## Escrever o evento

`record` imutável, no **passado** (`VisitCanceled`, `UserInvited`), em `<bc>-domain`:

```java
public record VisitCanceled(UUID aggregateId, Instant occurredOn) implements DomainEvent {
}
```

- `aggregateId()` e `occurredOn()` são obrigatórios. O `occurredOn` é o `Instant now` que o domínio recebeu: nunca `Instant.now()`.
- **Integration event** acrescenta `eventType()`, que é a **routing key** (ex. `"iam.user-invited"`): estável, minúscula, prefixada pelo BC (convenção sugerida: ainda não há consumidor que a fixe). É **contrato público**: renomear ou tirar campo quebra consumidor, então só se **acrescenta**.
- **Só tipos simples** nos campos de um integration event (`UUID`, `String`, números, `Instant`): ele é serializado por Jackson no outbox, e carregar VO ou aggregate vazaria o modelo do BC e quebraria o consumidor. Quem consome nunca importa classe do produtor: lê o payload com um tipo **dele**.
- Sem dado sensível no payload (senha, token, documento completo).

## Registrar e publicar

1. **Domínio:** o aggregate chama `registerEvent(...)` dentro do método de negócio. Nunca publica.
2. **Application:** depois do `save`, `publisher.publishAll(aggregate.pullEvents())`, na instância que você alterou ([`crm-add-use-case`](../crm-add-use-case/SKILL.md)). O `EventPublisher` roteia sozinho:
   - `IntegrationEvent` → **outbox**, na mesma transação do aggregate (estado e evento persistem juntos, ou nenhum);
   - `DomainEvent` → in-process, **só depois do commit** (sem transação ativa, na hora).

## Consumir

- **Domain event (mesmo BC):** `@EventListener` num adapter da `<bc>-infrastructure` que chama um use case da própria `<bc>-application`. O listener roda **depois do commit**: se ele falhar, o aggregate já foi salvo.
- **Integration event (outro BC):** o listener é um **adapter de entrada na `<bc>-infrastructure` do consumidor**, como um controller: traduz a mensagem e chama um use case do próprio BC. Queue, binding e DLQ do consumidor moram na infraestrutura dele; exchange e fábrica de listener (falha vai para a DLQ, sem requeue infinito) são do `common`.
- **Idempotente, sempre.** A entrega é at-least-once: a mesma mensagem pode chegar duas vezes. Use chave natural (`aggregateId` + `eventType`/`occurredOn`) ou o `IdempotencyGuard`.
- **Atraso é normal** (segundos, dominado por `crm.outbox.poll-delay`): a tela trata como carregamento.
- Para **ler fato alheio**, o consumidor mantém uma **projeção local** (tabela dele, só leitura, atualizada só pelo listener), nunca consulta o outro BC (§5.3). Decisão que precisa estar certa agora não é fato a copiar: é comando via `orchestration`.

> Ainda não há listener nem integration event de referência no repositório. O primeiro caso real fixa o desenho; combine com o usuário e registre-o aqui.

## Falha

Mensagem que estoura as tentativas vira `DEAD` no outbox, loga `ERROR` e **não é reprocessada sozinha**: alguém precisa olhar. Falha de negócio esperada no consumidor **não** é `ERROR`.

## Teste

- **Aggregate:** o evento certo sai de `pullEvents()`, com o `occurredOn` recebido, e a lista esvazia.
- **Use case:** o `publishAll` é chamado depois do `save` (`InOrder`).
- **Integration event:** teste de serialização do payload (campos e `eventType`), porque ele é contrato.
- **Consumidor:** o listener chama o use case com os tipos do BC, e processar a mesma mensagem duas vezes não duplica efeito.

## Checklist

- [ ] Domain ou integration, conforme quem precisa saber
- [ ] Nome no passado; `aggregateId` e `occurredOn` (vindo do `now` recebido)
- [ ] Integration event: `eventType` estável, só tipos simples, sem dado sensível
- [ ] Aggregate só registra; use case publica depois do `save`
- [ ] Consumidor idempotente, adapter na `<bc>-infrastructure` dele
- [ ] Nenhum BC importa classe de evento de outro BC; `mvn test` verde
