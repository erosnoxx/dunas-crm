package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Password")
class PasswordTest {

    private static void assertRejected(String raw, String expectedMessage) {
        assertThatThrownBy(() -> Password.of(raw))
                .isInstanceOf(ValidationException.class)
                .hasMessage(expectedMessage)
                .satisfies(e -> assertThat(((ValidationException) e).getCode()).contains("invalid_password"));
    }

    @Test
    @DisplayName("aceita uma senha que cumpre todos os critérios")
    void acceptsValidPassword() {
        assertThat(Password.of("Dunas#2026").getValue()).isEqualTo("Dunas#2026");
    }

    @Test
    @DisplayName("aceita senha nos limites de 8 e 30 caracteres")
    void acceptsLimits() {
        assertThat(Password.of("Aa1#aaaa").getValue()).hasSize(8);
        assertThat(Password.of("Aa1#" + "a".repeat(26)).getValue()).hasSize(30);
    }

    @Test
    @DisplayName("recusa senha com 7 caracteres")
    void rejectsTooShort() {
        assertRejected("Aa1#aaa", "a senha precisa de no mínimo 8 caracteres");
    }

    @Test
    @DisplayName("recusa senha com 31 caracteres")
    void rejectsTooLong() {
        assertRejected("Aa1#" + "a".repeat(27), "a senha precisa de no máximo 30 caracteres");
    }

    @Test
    @DisplayName("recusa senha sem maiúscula")
    void rejectsWithoutUppercase() {
        assertRejected("dunas#2026", "a senha precisa de ao menos uma letra maiúscula");
    }

    @Test
    @DisplayName("recusa senha sem minúscula")
    void rejectsWithoutLowercase() {
        assertRejected("DUNAS#2026", "a senha precisa de ao menos uma letra minúscula");
    }

    @Test
    @DisplayName("recusa senha sem número")
    void rejectsWithoutDigit() {
        assertRejected("Dunas#Solar", "a senha precisa de ao menos um número");
    }

    @Test
    @DisplayName("recusa senha sem caractere especial")
    void rejectsWithoutSpecial() {
        assertRejected("Dunas2026", "a senha precisa de ao menos um caractere especial");
    }

    @Test
    @DisplayName("recusa nulo")
    void rejectsNull() {
        assertThatThrownBy(() -> Password.of(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("não apara os espaços das pontas: a senha guardada os mantém")
    void doesNotTrim() {
        assertThat(Password.of(" Dunas#2026 ").getValue()).isEqualTo(" Dunas#2026 ");
    }

    @Test
    @DisplayName("espaço no lugar do especial não conta como caractere especial")
    void whitespaceIsNotSpecial() {
        assertRejected("Aa1     ", "a senha precisa de ao menos um caractere especial");
    }

    @Test
    @DisplayName("aceita letra acentuada como minúscula e símbolo não ASCII como especial")
    void acceptsNonAscii() {
        assertThat(Password.of("Açúcar1€").getValue()).isEqualTo("Açúcar1€");
    }

    @Test
    @DisplayName("isValid é verdadeiro para o que of aceita, incluindo os limites, acentuada e especial não ASCII")
    void isValidAcceptsWhatOfAccepts() {
        assertThat(Password.isValid("Dunas#2026")).isTrue();
        assertThat(Password.isValid("Aa1#aaaa")).isTrue();
        assertThat(Password.isValid("Aa1#" + "a".repeat(26))).isTrue();
        assertThat(Password.isValid(" Dunas#2026 ")).isTrue();
        assertThat(Password.isValid("Açúcar1€")).isTrue();
    }

    @Test
    @DisplayName("isValid é falso, sem lançar, para nulo e para cada critério que falta")
    void isValidRejectsWhatOfRejects() {
        assertThat(Password.isValid(null)).isFalse();
        assertThat(Password.isValid("")).isFalse();
        assertThat(Password.isValid("Aa1#aaa")).isFalse();
        assertThat(Password.isValid("Aa1#" + "a".repeat(27))).isFalse();
        assertThat(Password.isValid("dunas#2026")).isFalse();
        assertThat(Password.isValid("DUNAS#2026")).isFalse();
        assertThat(Password.isValid("Dunas#Solar")).isFalse();
        assertThat(Password.isValid("Dunas2026")).isFalse();
        assertThat(Password.isValid("Aa1     ")).isFalse();
    }

    @Test
    @DisplayName("toString não contém o valor")
    void toStringIsMasked() {
        var password = Password.of("Dunas#2026");

        assertThat(password.toString()).isEqualTo("***").doesNotContain("Dunas");
    }

    @Test
    @DisplayName("equals e hashCode não dependem do valor")
    void equalsDoesNotExposeValue() {
        var first = Password.of("Dunas#2026");
        var second = Password.of("Dunas#2026");

        assertThat(first).isEqualTo(first).isNotEqualTo(second);
        assertThat(first.hashCode()).isNotEqualTo("Dunas#2026".hashCode());
    }
}
