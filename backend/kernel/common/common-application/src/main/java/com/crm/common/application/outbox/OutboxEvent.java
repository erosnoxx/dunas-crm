package com.crm.common.application.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Um evento de integração já serializado, como o worker o vê.
 *
 * <p>É o contrato entre o {@link OutboxReader} e o despachante: payload em String, sem nenhum
 * tipo de domínio nem de JPA. O worker não conhece o evento original e não precisa conhecer.
 *
 * @param attemptCount tentativas já feitas — usado só para log e diagnóstico. A decisão de
 *                     reagendar ou desistir é do lado de persistência, não do worker.
 * @param correlationId id que liga o evento à requisição, job ou mensagem que o originou (D39).
 *                      Nulo nas linhas gravadas antes de existir a correlação; quem despacha
 *                      gera um novo nesse caso.
 */
public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant occurredOn,
        int attemptCount,
        String correlationId
) {
}
