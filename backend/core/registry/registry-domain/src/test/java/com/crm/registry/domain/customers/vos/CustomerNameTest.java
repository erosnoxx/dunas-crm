package com.crm.registry.domain.customers.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomerName")
class CustomerNameTest {

    private static final String RANGE = "nome do cliente deve ter entre 2 e 150 caracteres";

    @Test
    @DisplayName("aceita o limite inferior de 2 caractere(s)")
    void acceptsLowerLimit() {
        assertThat(CustomerName.of("a".repeat(2)).getValue()).hasSize(2);
    }

    @Test
    @DisplayName("aceita o limite superior de 150 caracteres")
    void acceptsUpperLimit() {
        assertThat(CustomerName.of("a".repeat(150)).getValue()).hasSize(150);
    }

    @Test
    @DisplayName("apara os espaços das pontas")
    void trimsEnds() {
        assertThat(CustomerName.of("  Abc  ").getValue()).isEqualTo("Abc");
    }

    @Test
    @DisplayName("recusa um caractere a mais que o limite superior")
    void rejectsAboveUpperLimit() {
        assertThatThrownBy(() -> CustomerName.of("a".repeat(150 + 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> CustomerName.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa só espaços")
    void rejectsBlank() {
        assertThatThrownBy(() -> CustomerName.of("   ")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("recusa um caractere a menos que o limite inferior")
    void rejectsBelowLowerLimit() {
        assertThatThrownBy(() -> CustomerName.of("a"))
                .isInstanceOf(ValidationException.class)
                .hasMessage(RANGE);
    }
}
