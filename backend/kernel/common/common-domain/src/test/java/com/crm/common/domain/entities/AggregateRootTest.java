package com.crm.common.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AggregateRoot")
class AggregateRootTest {

    private static final class Order extends AggregateRoot {
        Order(UUID id) {
            super(id);
        }

        Order(UUID id, Long version) {
            super(id, version);
        }
    }

    @Test
    @DisplayName("aggregate novo, ainda não gravado, não tem versão")
    void newAggregateHasNoVersion() {
        assertEquals(Optional.empty(), new Order(UUID.randomUUID()).getVersion());
    }

    @Test
    @DisplayName("aggregate lido do banco carrega a versão lida")
    void storedAggregateCarriesVersion() {
        assertEquals(Optional.of(3L), new Order(UUID.randomUUID(), 3L).getVersion());
    }
}
