package com.crm.common.infrastructure.outbox.persistence;


import com.crm.common.application.exceptions.InternalServerErrorException;

/**
 * O worker acabou de ler este evento e não o encontrou para marcar o resultado — é quebra de
 * invariante interna, não um recurso pedido por um cliente.
 *
 * <p>Por isso estende {@code InternalServerErrorException} e não {@code NotFoundException}:
 * este caminho nunca atende uma request HTTP, e devolver 404 seria enganoso se vazasse.
 */
class OutboxEventNotFoundException extends InternalServerErrorException {

    OutboxEventNotFoundException(String message) {
        super(message);
    }
}
