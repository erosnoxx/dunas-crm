package com.crm.common.application.messaging;


import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.common.application.outbox.OutboxEvent;

/**
 * Entrega ao broker um evento já tirado do outbox.
 *
 * <p>Usado só pelo worker de despacho. Nenhum use case de bounded context conhece este contrato
 * — a application do produtor só enxerga o {@code OutboxWriter}.
 *
 * <h2>Contrato de falha — a distinção importa</h2>
 *
 * <p>A implementação <b>precisa</b> separar dois tipos de falha, porque o worker trata cada uma
 * de forma oposta:
 *
 * <ul>
 *   <li>{@link ExternalServiceException} — <b>o transporte falhou, não é culpa desta mensagem</b>
 *       (broker inalcançável, publicação não confirmada, timeout). O worker aborta o ciclo e
 *       <b>não</b> conta a tentativa: o evento continua PENDING e é tentado de novo depois.
 *   <li>qualquer outra {@code RuntimeException} — <b>o problema é desta mensagem</b> (payload
 *       irrecuperável, rejeição definitiva). O worker conta a tentativa e, ao estourar o limite,
 *       marca o evento como DEAD.
 * </ul>
 *
 * <p>Confundir as duas tem consequência concreta: se uma queda de broker fosse contada como
 * tentativa, poucos minutos de indisponibilidade levariam a fila inteira a DEAD.
 *
 * <p>A entrega é <b>at-least-once</b>. A implementação deve propagar o id do evento de outbox
 * como identificador da mensagem, para que o consumidor consiga descartar reentrega.
 */
public interface IntegrationEventSender {
    void send(OutboxEvent event);
}
