package com.crm.common.infrastructure.messaging;

import com.crm.common.infrastructure.observability.CorrelationContext;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("CorrelationIdListenerAdvice")
class CorrelationIdListenerAdviceTest {

    private final CorrelationIdListenerAdvice advice = new CorrelationIdListenerAdvice();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private String[] seenWhileProcessing(Message message) throws Throwable {
        var seen = new String[2];
        var invocation = mock(MethodInvocation.class);

        when(invocation.getArguments()).thenReturn(new Object[]{message, null});
        when(invocation.proceed()).thenAnswer(call -> {
            seen[0] = MDC.get(CorrelationContext.REQUEST_ID);
            seen[1] = MDC.get(CorrelationContext.CORRELATION_ID);
            return null;
        });

        advice.invoke(invocation);

        return seen;
    }

    @Test
    @DisplayName("devolve ao MDC o correlationId da mensagem e usa o messageId como requestId")
    void restoresTheProducerCorrelation() throws Throwable {
        var message = MessageBuilder.withBody(new byte[0])
                .setCorrelationId("corr-from-producer")
                .setMessageId("outbox-event-id")
                .build();

        var seen = seenWhileProcessing(message);

        assertThat(seen[0]).isEqualTo("outbox-event-id");
        assertThat(seen[1]).isEqualTo("corr-from-producer");
    }

    @Test
    @DisplayName("mensagem sem correlação ganha uma nova: o log continua legível")
    void messageWithoutCorrelationGetsANewOne() throws Throwable {
        var seen = seenWhileProcessing(MessageBuilder.withBody(new byte[0]).build());

        assertThat(seen[0]).isNotBlank();
        assertThat(seen[1]).isNotBlank();
    }

    @Test
    @DisplayName("descarta correlationId inválido vindo do broker")
    void discardsAnInvalidCorrelationId() throws Throwable {
        var message = MessageBuilder.withBody(new byte[0])
                .setCorrelationId("a b\nc")
                .build();

        var seen = seenWhileProcessing(message);

        assertThat(seen[1]).isNotEqualTo("a b\nc").isNotBlank();
    }

    @Test
    @DisplayName("limpa o MDC quando o listener termina, mesmo com falha")
    void clearsTheMdcEvenOnFailure() throws Throwable {
        var invocation = mock(MethodInvocation.class);
        var failure = new AtomicReference<Throwable>();

        when(invocation.getArguments()).thenReturn(new Object[]{MessageBuilder.withBody(new byte[0]).build()});
        when(invocation.proceed()).thenThrow(new IllegalStateException("boom"));

        try {
            advice.invoke(invocation);
        }
        catch (IllegalStateException e) {
            failure.set(e);
        }

        assertThat(failure.get()).isNotNull();
        assertThat(MDC.get(CorrelationContext.CORRELATION_ID)).isNull();
    }
}
