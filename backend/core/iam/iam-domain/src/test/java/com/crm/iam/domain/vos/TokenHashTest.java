package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenHash")
class TokenHashTest {

    @Test
    @DisplayName("aceita 64 caracteres hexadecimais minúsculos")
    void acceptsHex64() {
        assertThat(TokenHash.of("0123456789abcdef".repeat(4)).getValue()).hasSize(64);
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> TokenHash.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TokenHash.of(""))
                .isInstanceOf(ValidationException.class)
                .hasMessage("hash do token não pode ser vazio");
    }

    @Test
    @DisplayName("recusa tamanho diferente de 64, maiúsculas e caractere fora de hexadecimal")
    void rejectsOtherFormats() {
        for (var invalid : new String[]{"abc", "a".repeat(63), "a".repeat(65), "A".repeat(64), "g".repeat(64)})
            assertThatThrownBy(() -> TokenHash.of(invalid))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("formato de hash do token inválido");
    }
}
