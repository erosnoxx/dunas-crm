package com.crm.iam.domain.vos;

import com.crm.common.domain.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenSecret")
class TokenSecretTest {

    @Test
    @DisplayName("generate devolve valores diferentes a cada chamada")
    void generatesDistinctValues() {
        assertThat(TokenSecret.generate().getValue()).isNotEqualTo(TokenSecret.generate().getValue());
    }

    @Test
    @DisplayName("generate devolve 43 caracteres base64 url-safe")
    void generatesUrlSafe43Chars() {
        assertThat(TokenSecret.generate().getValue()).hasSize(43).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("hash é determinístico, tem 64 caracteres hexadecimais e sobrevive ao of")
    void hashIsDeterministic() {
        var secret = TokenSecret.generate();

        var hash = secret.hash();

        assertThat(hash).isEqualTo(secret.hash());
        assertThat(hash.getValue()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(TokenSecret.of(secret.getValue()).hash()).isEqualTo(hash);
    }

    @Test
    @DisplayName("hash é o SHA-256 do texto do segredo")
    void hashIsSha256() {
        var secret = TokenSecret.of("a".repeat(43));

        assertThat(secret.hash().getValue())
                .isEqualTo("66d34fba71f8f450f7e45598853e53bfc23bbd129027cbb131a2f4ffd7878cd0");
    }

    @Test
    @DisplayName("toString não contém o valor")
    void toStringIsMasked() {
        var secret = TokenSecret.generate();

        assertThat(secret.toString()).doesNotContain(secret.getValue());
    }

    @Test
    @DisplayName("of recusa nulo, vazio e formato diferente")
    void ofRejectsInvalid() {
        assertThatThrownBy(() -> TokenSecret.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TokenSecret.of("  ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TokenSecret.of("curto"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("formato de segredo do link inválido");
        assertThatThrownBy(() -> TokenSecret.of("+".repeat(43)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("formato de segredo do link inválido");
    }
}
