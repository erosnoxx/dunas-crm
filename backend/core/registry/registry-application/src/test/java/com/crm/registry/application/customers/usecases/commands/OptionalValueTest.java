package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.registry.domain.customers.vos.CustomerName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OptionalValue")
class OptionalValueTest {

    @Test
    @DisplayName("nulo é ausente")
    void nullIsAbsent() {
        assertThat(OptionalValue.of(null, CustomerName::of)).isNull();
    }

    @Test
    @DisplayName("texto passa pelo VO")
    void textGoesThroughTheValueObject() {
        assertThat(OptionalValue.of("Solar Sul", CustomerName::of)).isEqualTo(CustomerName.of("Solar Sul"));
    }

    @Test
    @DisplayName("texto em branco vira erro de validação do VO")
    void blankIsRejectedByTheValueObject() {
        assertThatThrownBy(() -> OptionalValue.of("  ", CustomerName::of)).isInstanceOf(ValidationException.class);
    }
}
