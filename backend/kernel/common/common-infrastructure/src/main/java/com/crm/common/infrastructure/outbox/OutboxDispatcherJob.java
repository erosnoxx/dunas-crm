package com.crm.common.infrastructure.outbox;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.common.application.messaging.IntegrationEventSender;
import com.crm.common.application.outbox.OutboxEvent;
import com.crm.common.application.outbox.OutboxReader;
import com.crm.common.infrastructure.observability.CorrelationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Worker que tira os eventos do outbox e os entrega ao broker.
 *
 * <p>É a segunda metade do padrão: o produtor grava o evento na mesma transação do aggregate
 * (ver {@code OutboxWriter}), e este job publica depois. A entrega é <b>at-least-once</b> — o
 * mesmo evento pode chegar mais de uma vez, e todo consumidor é idempotente.
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxDispatcherJob {

    private final OutboxReader outboxReader;

    /**
     * Opcional de propósito. Enquanto não existir um {@code IntegrationEventSender} (o broker
     * só entra a partir da primeira feature ordering → payment), o outbox continua
     * <b>gravando</b> normalmente — o que é a parte que garante correção — e o despacho fica
     * parado. Os eventos acumulam como PENDING e saem quando o sender existir.
     *
     * <p>A alternativa seria um sender no-op, que descartaria evento em silêncio. Preferimos
     * acumular.
     */
    private final Optional<IntegrationEventSender> sender;

    private final OutboxProperties properties;

    /**
     * O {@code @Transactional} não é opcional: o lock pessimista com SKIP LOCKED de
     * {@code findPending} só existe dentro de uma transação. Sem ele nenhum lock é adquirido,
     * duas instâncias selecionam as mesmas linhas e o mesmo evento é publicado em duplicidade.
     *
     * <p>Uma transação para o lote inteiro, de propósito: o lock precisa ser mantido enquanto
     * os eventos são publicados. Se um evento no meio do lote fizer a transação rolar para
     * trás, os anteriores são republicados no ciclo seguinte — que é exatamente o
     * at-least-once que o consumidor já absorve.
     */
    @Transactional
    public void dispatch() {
        // um ciclo é uma unidade de trabalho: o log dele se lê junto. Cada evento, abaixo, reabre
        // o contexto com a correlação de quem o originou.
        try (var ignored = CorrelationContext.openNew()) {
            dispatchCycle();
        }
    }

    private void dispatchCycle() {
        if (sender.isEmpty()) {
            log.warn("nenhum IntegrationEventSender registrado — eventos de integração ficam acumulados como PENDING");
            return;
        }

        var pending = outboxReader.findPending(properties.batchSize());

        if (pending.isEmpty())
            return;

        log.debug("despachando {} evento(s) do outbox", pending.size());

        dispatchAll(pending);
    }

    /**
     * Falha de transporte aborta o ciclo inteiro **sem** penalizar nenhum evento: se o broker
     * está fora, não há motivo para tentar os outros 99, e contar a tentativa levaria a fila
     * toda a DEAD depois de poucos minutos de indisponibilidade.
     *
     * <p>Os eventos já marcados como processados antes do aborto continuam marcados — eles foram
     * de fato entregues.
     */
    private void dispatchAll(List<OutboxEvent> pending) {
        try {
            for (var event : pending) {
                dispatchOne(event);
            }
        }

        catch (ExternalServiceException e) {
            log.warn("transporte indisponível — ciclo abortado sem penalizar eventos: {}", e.getMessage());
        }
    }

    /**
     * A falha <b>desta</b> mensagem não derruba o lote: ela é marcada individualmente, para que
     * um payload quebrado não impeça os demais de serem entregues. Ver o contrato de falha em
     * {@link IntegrationEventSender}.
     */
    private void dispatchOne(OutboxEvent event) {
        var correlationId = event.correlationId() != null ? event.correlationId() : CorrelationContext.newId();

        try (var ignored = CorrelationContext.open(event.id().toString(), correlationId)) {
            sendAndMark(event);
        }
    }

    private void sendAndMark(OutboxEvent event) {
        try {
            sender.orElseThrow().send(event);

            outboxReader.markAsProcessed(event.id());
        }

        catch (ExternalServiceException e) {
            throw e;
        }

        catch (RuntimeException e) {
            log.warn(
                    "falha ao despachar evento de outbox {} ({}), tentativa {} — {}",
                    event.id(), event.eventType(), event.attemptCount() + 1, e.getMessage()
            );

            outboxReader.markAsFailed(event.id(), e.getMessage());
        }
    }
}
