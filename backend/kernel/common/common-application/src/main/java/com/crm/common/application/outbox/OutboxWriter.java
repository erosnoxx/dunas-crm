package com.crm.common.application.outbox;


import com.crm.common.domain.events.IntegrationEvent;

/**
 * Lado de escrita do outbox: grava o integration event na mesma transação do aggregate, para que o
 * evento exista se, e somente se, a mudança de estado for confirmada.
 */
public interface OutboxWriter {
    void save(IntegrationEvent event);
}
