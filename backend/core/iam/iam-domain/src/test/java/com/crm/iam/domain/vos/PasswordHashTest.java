package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordHash")
class PasswordHashTest {

    private static final String PHC = "$argon2id$v=19$m=19456,t=2,p=1$c29tZXNhbHQ$aGFzaGhhc2hoYXNo";

    @Test
    @DisplayName("aceita hash no formato Argon2id")
    void acceptsArgon2id() {
        assertThat(PasswordHash.of(PHC).getValue()).isEqualTo(PHC);
    }

    @Test
    @DisplayName("recusa nulo e vazio")
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> PasswordHash.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> PasswordHash.of(" "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("hash da senha não pode ser vazio");
    }

    @Test
    @DisplayName("recusa texto que não comece com $argon2id$")
    void rejectsOtherFormats() {
        assertThatThrownBy(() -> PasswordHash.of("Dunas#2026"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("formato de hash da senha inválido");
        assertThatThrownBy(() -> PasswordHash.of("$2a$10$abcdefghijklmnopqrstuv"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("formato de hash da senha inválido");
    }
}
