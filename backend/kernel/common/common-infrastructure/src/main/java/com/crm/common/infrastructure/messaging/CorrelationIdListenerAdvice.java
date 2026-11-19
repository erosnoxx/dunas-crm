package com.crm.common.infrastructure.messaging;

import com.crm.common.infrastructure.observability.CorrelationContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.core.Message;

/**
 * Devolve ao MDC o {@code correlationId} que o produtor pôs na mensagem, enquanto o listener
 * trabalha (D39). É o elo que fecha a trilha: requisição, outbox, broker, consumidor.
 *
 * <p>O {@code messageId} é o id do evento de outbox (ver {@link RabbitIntegrationEventSenderImpl}),
 * e vira o {@code requestId} do processamento. Mensagem sem correlação (produzida antes desta
 * convenção, ou por outro sistema) ganha uma nova: o log continua legível, só começa a trilha ali.
 */
public class CorrelationIdListenerAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        var properties = messageIn(invocation.getArguments());

        var correlationId = properties != null && CorrelationContext.isValid(properties.getCorrelationId())
                ? properties.getCorrelationId()
                : CorrelationContext.newId();

        var requestId = properties != null && CorrelationContext.isValid(properties.getMessageId())
                ? properties.getMessageId()
                : CorrelationContext.newId();

        try (var ignored = CorrelationContext.open(requestId, correlationId)) {
            return invocation.proceed();
        }
    }

    private static org.springframework.amqp.core.MessageProperties messageIn(Object[] arguments) {
        for (var argument : arguments) {
            if (argument instanceof Message message)
                return message.getMessageProperties();
        }

        return null;
    }
}
