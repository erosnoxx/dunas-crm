package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserName")
class UserNameTest {

    @Test
    @DisplayName("aceita de 2 a 80 caracteres e apara as pontas")
    void acceptsLimits() {
        assertThat(UserName.of("Jo").getValue()).isEqualTo("Jo");
        assertThat(UserName.of("a".repeat(80)).getValue()).hasSize(80);
        assertThat(UserName.of("  Maria  ").getValue()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> UserName.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> UserName.of("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do usuário não pode ser vazio");
    }

    @Test
    @DisplayName("recusa menos de 2 e mais de 80 caracteres")
    void rejectsOutOfRange() {
        assertThatThrownBy(() -> UserName.of("J"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do usuário deve ter entre 2 e 80 caracteres");
        assertThatThrownBy(() -> UserName.of("a".repeat(81)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("nome do usuário deve ter entre 2 e 80 caracteres");
    }
}
