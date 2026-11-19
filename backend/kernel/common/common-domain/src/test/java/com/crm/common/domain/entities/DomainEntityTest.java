package com.crm.common.domain.entities;

import com.crm.common.domain.exceptions.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DomainEntity")
class DomainEntityTest {

    private static final class Item extends DomainEntity {
        Item(UUID id) {
            super(id);
        }
    }

    private static final class OtherItem extends DomainEntity {
        OtherItem(UUID id) {
            super(id);
        }
    }

    @Test
    @DisplayName("recusa id nulo")
    void rejectsNullId() {
        var exception = assertThrows(DomainException.class, () -> new Item(null));

        assertEquals("id da entidade não pode ser nulo", exception.getMessage());
    }

    @Test
    @DisplayName("é igual a outra instância da mesma classe com o mesmo id")
    void equalsSameClassSameId() {
        var id = UUID.randomUUID();

        assertEquals(new Item(id), new Item(id));
        assertEquals(new Item(id).hashCode(), new Item(id).hashCode());
    }

    @Test
    @DisplayName("é diferente quando o id muda")
    void differsByIdentity() {
        assertNotEquals(new Item(UUID.randomUUID()), new Item(UUID.randomUUID()));
    }

    @Test
    @DisplayName("é diferente de outra classe, mesmo com o mesmo id")
    void differsAcrossClasses() {
        var id = UUID.randomUUID();

        assertNotEquals(new Item(id), new OtherItem(id));
    }
}
