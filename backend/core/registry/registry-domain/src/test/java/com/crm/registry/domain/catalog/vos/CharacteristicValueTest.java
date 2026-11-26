package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CharacteristicValue")
class CharacteristicValueTest {

    @Test
    @DisplayName("guarda a definição e o valor")
    void keepsFields() {
        var id = UUID.randomUUID();
        var amount = CharacteristicAmount.of("5");

        var value = new CharacteristicValue(id, amount);

        assertThat(value.definitionId()).isEqualTo(id);
        assertThat(value.amount()).isEqualTo(amount);
    }

    @Test
    @DisplayName("recusa definição nula")
    void rejectsNullDefinition() {
        var amount = CharacteristicAmount.of("5");

        assertThatThrownBy(() -> new CharacteristicValue(null, amount))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa valor nulo")
    void rejectsNullAmount() {
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> new CharacteristicValue(id, null))
                .isInstanceOf(ValidationException.class);
    }
}
