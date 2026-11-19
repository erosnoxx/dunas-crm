package com.crm.iam.domain.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OneTimeToken")
class OneTimeTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private static final Duration VALIDITY = Duration.ofHours(48);

    private static final Instant EXPIRES_AT = NOW.plus(VALIDITY);

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    private static final TokenSecret SECRET = TokenSecret.generate();

    private static OneTimeToken issued() {
        var token = OneTimeToken.issue(USER_ID, TokenPurpose.INVITATION, SECRET, VALIDITY, NOW);

        token.pullEvents();

        return token;
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).contains(code));
    }

    @Test
    @DisplayName("emitir guarda o hash do segredo, a validade e deixa o token pendente")
    void issueStoresHashAndExpiry() {
        var token = OneTimeToken.issue(USER_ID, TokenPurpose.PASSWORD_RESET, SECRET, VALIDITY, NOW);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getPurpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);
        assertThat(token.getTokenHash()).isEqualTo(SECRET.hash());
        assertThat(token.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(token.getUsedAt()).isEmpty();
        assertThat(token.getInvalidatedAt()).isEmpty();
        assertThat(token.getVersion()).isEmpty();
    }

    @Test
    @DisplayName("emitir registra OneTimeTokenIssued com o segredo e a finalidade")
    void issueRegistersEventWithSecretAndPurpose() {
        var token = OneTimeToken.issue(USER_ID, TokenPurpose.INVITATION, SECRET, VALIDITY, NOW);

        assertThat(token.pullEvents()).singleElement().isInstanceOfSatisfying(
                OneTimeTokenIssued.class,
                event -> {
                    assertThat(event.aggregateId()).isEqualTo(token.getId());
                    assertThat(event.occurredOn()).isEqualTo(NOW);
                    assertThat(event.userId()).isEqualTo(USER_ID);
                    assertThat(event.purpose()).isEqualTo(TokenPurpose.INVITATION);
                    assertThat(event.secret()).isEqualTo(SECRET);
                }
        );
    }

    @Test
    @DisplayName("o evento não vaza o segredo no toString")
    void eventToStringDoesNotLeakSecret() {
        var token = OneTimeToken.issue(USER_ID, TokenPurpose.INVITATION, SECRET, VALIDITY, NOW);

        assertThat(token.pullEvents().getFirst().toString()).doesNotContain(SECRET.getValue());
    }

    @Test
    @DisplayName("o aggregate não expõe o segredo: nenhum getter o devolve e o toString não o contém")
    void aggregateDoesNotExposeSecret() {
        var token = OneTimeToken.issue(USER_ID, TokenPurpose.INVITATION, SECRET, VALIDITY, NOW);

        var exposing = Arrays.stream(OneTimeToken.class.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == TokenSecret.class)
                .toList();

        assertThat(exposing).isEmpty();
        assertThat(token.toString()).doesNotContain(SECRET.getValue());
        assertThat(token.getTokenHash().getValue()).doesNotContain(SECRET.getValue());
    }

    @Test
    @DisplayName("consumir antes de expiresAt marca usedAt")
    void consumeBeforeExpiryMarksUsed() {
        var token = issued();
        var at = EXPIRES_AT.minusSeconds(1);

        token.consume(at);

        assertThat(token.getUsedAt()).contains(at);
    }

    @Test
    @DisplayName("consumir em expiresAt ou depois falha com link_expired")
    void consumeAtOrAfterExpiryFails() {
        assertCode(() -> issued().consume(EXPIRES_AT), "link_expired");
        assertCode(() -> issued().consume(EXPIRES_AT.plusSeconds(1)), "link_expired");
    }

    @Test
    @DisplayName("consumir de novo falha com link_used e mantém o primeiro usedAt")
    void consumeTwiceFails() {
        var token = issued();

        token.consume(NOW.plusSeconds(10));

        assertCode(() -> token.consume(NOW.plusSeconds(20)), "link_used");
        assertThat(token.getUsedAt()).contains(NOW.plusSeconds(10));
    }

    @Test
    @DisplayName("consumir um link invalidado falha com link_used")
    void consumeInvalidatedFails() {
        var token = issued();

        token.invalidate(NOW.plusSeconds(10));

        assertCode(() -> token.consume(NOW.plusSeconds(20)), "link_used");
        assertThat(token.getUsedAt()).isEmpty();
    }

    @Test
    @DisplayName("invalidar marca invalidatedAt")
    void invalidateMarksInvalidated() {
        var token = issued();

        token.invalidate(NOW.plusSeconds(10));

        assertThat(token.getInvalidatedAt()).contains(NOW.plusSeconds(10));
    }

    @Test
    @DisplayName("invalidar é idempotente: repetir não muda o primeiro invalidatedAt")
    void invalidateIsIdempotent() {
        var token = issued();

        token.invalidate(NOW.plusSeconds(10));
        token.invalidate(NOW.plusSeconds(20));

        assertThat(token.getInvalidatedAt()).contains(NOW.plusSeconds(10));
    }

    @Test
    @DisplayName("invalidar não altera quem já foi usado")
    void invalidateKeepsUsedToken() {
        var token = issued();

        token.consume(NOW.plusSeconds(10));
        token.invalidate(NOW.plusSeconds(20));

        assertThat(token.getInvalidatedAt()).isEmpty();
        assertThat(token.getUsedAt()).contains(NOW.plusSeconds(10));
    }

    @Test
    @DisplayName("reconstituir devolve o estado lido do banco, com a versão")
    void reconstituteRestoresState() {
        var id = UUID.randomUUID();
        var used = NOW.plusSeconds(5);

        var token = OneTimeToken.reconstitute(
                id,
                3L,
                USER_ID,
                TokenPurpose.PASSWORD_RESET,
                SECRET.hash(),
                EXPIRES_AT,
                used,
                null
        );

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getVersion()).contains(3L);
        assertThat(token.getUserId()).isEqualTo(USER_ID);
        assertThat(token.getPurpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);
        assertThat(token.getTokenHash()).isEqualTo(SECRET.hash());
        assertThat(token.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(token.getUsedAt()).contains(used);
        assertThat(token.getInvalidatedAt()).isEmpty();
        assertThat(token.pullEvents()).isEmpty();
    }
}
