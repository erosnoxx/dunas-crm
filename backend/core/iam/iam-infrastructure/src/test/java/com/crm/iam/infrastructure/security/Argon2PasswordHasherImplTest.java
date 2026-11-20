package com.crm.iam.infrastructure.security;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Argon2PasswordHasherImpl")
class Argon2PasswordHasherImplTest {

    private static final String RAW = "Senha@Forte1";

    /** Parâmetros leves: o teste confere o comportamento, não o custo. */
    private final Argon2PasswordHasherImpl hasher = new Argon2PasswordHasherImpl(
            1024,
            1,
            1,
            16,
            32
    );

    @Test
    @DisplayName("hash devolve um PasswordHash válido no formato PHC argon2id")
    void hashReturnsArgon2idPhcHash() {
        var hash = hasher.hash(Password.of(RAW));

        assertThat(hash.getValue()).startsWith("$argon2id$");
        assertThat(PasswordHash.of(hash.getValue())).isEqualTo(hash);
    }

    @Test
    @DisplayName("duas chamadas com a mesma senha dão hashes diferentes")
    void samePasswordGivesDifferentHashes() {
        var first = hasher.hash(Password.of(RAW));
        var second = hasher.hash(Password.of(RAW));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("matches é verdadeiro para a senha certa")
    void matchesIsTrueForTheRightPassword() {
        var hash = hasher.hash(Password.of(RAW));

        assertThat(hasher.matches(Password.of(RAW), hash)).isTrue();
    }

    @Test
    @DisplayName("matches é falso para a senha errada")
    void matchesIsFalseForTheWrongPassword() {
        var hash = hasher.hash(Password.of(RAW));

        assertThat(hasher.matches(Password.of("Outra@Senha2"), hash)).isFalse();
    }

    @Test
    @DisplayName("matches não apara as pontas: senha com espaço a mais não confere")
    void matchesDoesNotTrim() {
        var hash = hasher.hash(Password.of(RAW));

        assertThat(hasher.matches(Password.of(RAW + " "), hash)).isFalse();
    }

    @Test
    @DisplayName("matches é falso, sem lançar, para um hash malformado")
    void matchesIsFalseForMalformedHash() {
        var malformed = PasswordHash.of("$argon2id$lixo");

        assertThat(hasher.matches(Password.of(RAW), malformed)).isFalse();
    }

    @Test
    @DisplayName("o hash não contém a senha")
    void hashDoesNotContainThePassword() {
        var hash = hasher.hash(Password.of(RAW));

        assertThat(hash.getValue()).doesNotContain(RAW);
    }

    @Test
    @DisplayName("hash com parâmetros leves roda em menos de 1 s")
    void hashWithLightParametersIsFast() {
        var password = Password.of(RAW);

        assertThat(Duration.ofNanos(elapsedNanos(() -> hasher.hash(password))))
                .isLessThan(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("falha da biblioteca vira InternalServerErrorException sem expor a senha")
    void libraryFailureBecomesInternalServerError() {
        var broken = new Argon2PasswordHasherImpl(
                0,
                0,
                0,
                0,
                0
        );

        assertThatThrownBy(() -> broken.hash(Password.of(RAW)))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageNotContaining(RAW);
    }

    @Test
    @DisplayName("cada parâmetro do construtor chega ao lugar certo no hash PHC")
    void constructorParametersReachTheRightPlaceInThePhcHash() {
        var distinct = new Argon2PasswordHasherImpl(
                1024,
                2,
                3,
                16,
                32
        );

        var phc = distinct.hash(Password.of(RAW)).getValue().split("\\$");

        // ["", "argon2id", "v=19", "m=...,t=...,p=...", sal, hash], os dois últimos em base64 sem preenchimento
        assertThat(phc[3]).isEqualTo("m=1024,t=2,p=3");
        assertThat(phc[4]).hasSize(22);
        assertThat(phc[5]).hasSize(43);
    }

    private static long elapsedNanos(Runnable action) {
        var start = System.nanoTime();

        action.run();

        return System.nanoTime() - start;
    }
}
