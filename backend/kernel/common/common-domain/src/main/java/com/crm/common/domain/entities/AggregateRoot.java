package com.crm.common.domain.entities;

import com.crm.common.domain.events.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz de aggregate: a única entrada para alterar o aggregate e a fonte dos {@link DomainEvent}
 * que a mudança produziu.
 *
 * <p>Os eventos ficam retidos até {@link #pullEvents()}, chamado pela application depois de gravar.
 */
public abstract class AggregateRoot extends DomainEntity {
    private final List<DomainEvent> events = new ArrayList<>();

    private final Long version;

    /** Aggregate novo, ainda não gravado: sem versão. */
    protected AggregateRoot(UUID id) {
        this(id, null);
    }

    /**
     * Aggregate lido do banco, com a versão lida. O save confere essa versão e recusa a escrita
     * se outra transação gravou o aggregate no meio (docs/architecture.md §8).
     */
    protected AggregateRoot(UUID id, Long version) {
        super(id);
        this.version = version;
    }

    protected AggregateRoot(Long version) {
        super();
        this.version = version;
    }

    /** Vazio enquanto o aggregate não foi gravado. */
    public Optional<Long> getVersion() {
        return Optional.ofNullable(version);
    }

    protected void registerEvent(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(events);
        events.clear();
        return copy;
    }
}

