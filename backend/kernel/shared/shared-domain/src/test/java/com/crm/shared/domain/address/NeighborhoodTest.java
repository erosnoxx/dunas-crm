package com.crm.shared.domain.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.vos.address.Neighborhood;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Neighborhood")
class NeighborhoodTest {

    @Test
    @DisplayName("aceita valor dentro do intervalo")
    void acceptsValid() {
        assertEquals("Centro", Neighborhood.of("Centro").getValue());
    }

    @Test
    @DisplayName("recusa muito curto ou muito longo")
    void rejectsOutOfRange() {
        assertThrows(ValidationException.class, () -> Neighborhood.of("A"));
        assertThrows(ValidationException.class, () -> Neighborhood.of("A".repeat(101)));
    }
}
