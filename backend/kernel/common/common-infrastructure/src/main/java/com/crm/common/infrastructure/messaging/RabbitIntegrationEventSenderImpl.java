package com.crm.common.infrastructure.messaging;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.common.application.messaging.IntegrationEventSender;
import com.crm.common.application.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Publica no RabbitMQ o evento que o worker tirou do outbox.
 *
 * <p>O payload já vem serializado como JSON pelo writer — este sender não conhece o tipo do
 * evento e não desserializa nada.
 *
 * <p><b>Publisher confirm é obrigatório aqui.</b> Sem esperar a confirmação do broker, um
 * {@code send} pode retornar com sucesso e a mensagem nunca ser persistida no broker; o worker
 * marcaria o evento como PROCESSED e ele estaria perdido em silêncio — destruindo exatamente a
 * garantia que o outbox existe para dar. Exige
 * {@code spring.rabbitmq.publisher-confirm-type=simple}.
 */
@RequiredArgsConstructor
class RabbitIntegrationEventSenderImpl implements IntegrationEventSender {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    @Override
    public void send(OutboxEvent event) {
        var confirmed = publish(event);

        if (!Boolean.TRUE.equals(confirmed))
            throw new ExternalServiceException(
                    "broker não confirmou a publicação do evento de integração: " + event.id()
            );
    }

    /**
     * Qualquer {@link AmqpException} é traduzida para {@link ExternalServiceException}, e isso é
     * semântico, não cosmético: o contrato de {@link IntegrationEventSender} diz que
     * {@code ExternalServiceException} significa "o transporte falhou, não é culpa desta
     * mensagem" — e o worker usa isso para abortar o ciclo <b>sem</b> penalizar os eventos.
     */
    private Boolean publish(OutboxEvent event) {
        try {
            return rabbitTemplate.invoke(operations -> {
                operations.send(properties.exchange(), event.eventType(), toMessage(event));

                return operations.waitForConfirms(properties.confirmTimeout().toMillis());
            });
        }

        catch (AmqpException e) {
            throw new ExternalServiceException("falha ao publicar evento de integração no broker", e);
        }
    }

    /**
     * {@code messageId} recebe o id do evento de outbox de propósito: a entrega é at-least-once,
     * e é por esse id que o consumidor descarta reentrega. O {@code correlationId} leva a trilha
     * da requisição que originou o evento até o log do consumidor (D39).
     */
    private Message toMessage(OutboxEvent event) {
        return MessageBuilder
                .withBody(event.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(event.id().toString())
                .setCorrelationId(event.correlationId())
                .setTimestamp(Date.from(event.occurredOn()))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("eventType", event.eventType())
                .setHeader("aggregateId", event.aggregateId().toString())
                .build();
    }
}
