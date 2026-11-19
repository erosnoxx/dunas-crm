package com.crm.common.infrastructure.outbox.persistence;


import com.crm.common.application.exceptions.InternalServerErrorException;

/**
 * Falha ao serializar um evento de integração. Estende {@code InternalServerErrorException}
 * porque nenhuma exception nativa pode vazar crua da infraestrutura: serialização é mecanismo
 * interno, não regra de negócio nem integração externa.
 */
class EventSerializationException extends InternalServerErrorException {

    EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
