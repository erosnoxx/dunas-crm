package com.crm.common.domain.states;

import com.crm.common.domain.events.DomainEvent;
import lombok.Getter;

/**
 * Base de uma máquina de estados de domínio: guarda o estado atual e o evento que a última
 * transição produziu.
 *
 * @param <V> enum com os estados possíveis
 */
@Getter
public abstract class StateMachine<V extends Enum<V>> {
    protected V value;
    protected DomainEvent event;
}
