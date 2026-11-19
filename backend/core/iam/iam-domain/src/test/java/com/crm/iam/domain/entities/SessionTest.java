package com.crm.iam.domain.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.iam.domain.events.SessionRevoked;
import com.crm.iam.domain.vos.DeviceLabel;
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

@DisplayName("Session")
class SessionTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private static final Duration VALIDITY = Duration.ofDays(30);

    private static final Duration MIN_INTERVAL = Duration.ofMinutes(5);

    private static final Instant EXPIRES_AT = NOW.plus(VALIDITY);

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000cc");

    private static final TokenSecret SECRET = TokenSecret.generate();

    private static final DeviceLabel DEVICE = DeviceLabel.of("Galaxy A54, app DUNAS");

    private static Session started() {
        return Session.start(USER_ID, SECRET, DEVICE, VALIDITY, NOW);
    }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getCode()).contains(code));
    }

    @Test
    @DisplayName("iniciar guarda o hash do segredo, lastUsedAt = now e expiresAt = now + 30 dias")
    void startStoresHashAndExpiry() {
        var session = started();

        assertThat(session.getId()).isNotNull();
        assertThat(session.getUserId()).isEqualTo(USER_ID);
        assertThat(session.getTokenHash()).isEqualTo(SECRET.hash());
        assertThat(session.getDevice()).isEqualTo(DEVICE);
        assertThat(session.getStartedAt()).isEqualTo(NOW);
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        assertThat(session.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(session.getRevokedAt()).isEmpty();
        assertThat(session.getRevokedBy()).isEmpty();
        assertThat(session.getVersion()).isEmpty();
        assertThat(session.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("o aggregate não expõe o segredo: nenhum getter o devolve e o toString não o contém")
    void aggregateDoesNotExposeSecret() {
        var session = started();

        var exposing = Arrays.stream(Session.class.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType() == TokenSecret.class)
                .toList();

        assertThat(exposing).isEmpty();
        assertThat(session.toString()).doesNotContain(SECRET.getValue());
    }

    @Test
    @DisplayName("está ativa antes de expiresAt")
    void activeBeforeExpiry() {
        assertThat(started().isActiveAt(EXPIRES_AT.minusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("em expiresAt e depois não está ativa")
    void inactiveAtOrAfterExpiry() {
        assertThat(started().isActiveAt(EXPIRES_AT)).isFalse();
        assertThat(started().isActiveAt(EXPIRES_AT.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("revogada não está ativa, mesmo antes de expiresAt")
    void revokedIsInactive() {
        var session = started();

        session.revoke(ADMIN_ID, NOW.plusSeconds(10));

        assertThat(session.isActiveAt(NOW.plusSeconds(11))).isFalse();
    }

    @Test
    @DisplayName("renovar com 4 min 59 s desde o último uso devolve false e não muda nada")
    void renewBeforeIntervalDoesNothing() {
        var session = started();

        var renewed = session.renewIfDue(NOW.plusSeconds(299), VALIDITY, MIN_INTERVAL);

        assertThat(renewed).isFalse();
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        assertThat(session.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("renovar com exatamente 5 min devolve true e move lastUsedAt e expiresAt")
    void renewAtIntervalRenews() {
        var session = started();
        var at = NOW.plus(MIN_INTERVAL);

        var renewed = session.renewIfDue(at, VALIDITY, MIN_INTERVAL);

        assertThat(renewed).isTrue();
        assertThat(session.getLastUsedAt()).isEqualTo(at);
        assertThat(session.getExpiresAt()).isEqualTo(at.plus(VALIDITY));
        assertThat(session.getStartedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("renovar uma sessão expirada falha com session_invalid")
    void renewExpiredFails() {
        var session = started();

        assertCode(() -> session.renewIfDue(EXPIRES_AT, VALIDITY, MIN_INTERVAL), "session_invalid");
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("renovar uma sessão revogada falha com session_invalid e nada muda")
    void renewRevokedFails() {
        var session = started();

        session.revoke(USER_ID, NOW.plusSeconds(10));

        assertCode(
                () -> session.renewIfDue(NOW.plus(MIN_INTERVAL), VALIDITY, MIN_INTERVAL),
                "session_invalid"
        );
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        assertThat(session.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("revogar preenche revokedAt e revokedBy e registra SessionRevoked com o hash")
    void revokeMarksAndRegistersEvent() {
        var session = started();
        var at = NOW.plusSeconds(10);

        session.revoke(ADMIN_ID, at);

        assertThat(session.getRevokedAt()).contains(at);
        assertThat(session.getRevokedBy()).contains(ADMIN_ID);
        assertThat(session.pullEvents()).singleElement().isInstanceOfSatisfying(
                SessionRevoked.class,
                event -> {
                    assertThat(event.aggregateId()).isEqualTo(session.getId());
                    assertThat(event.occurredOn()).isEqualTo(at);
                    assertThat(event.tokenHash()).isEqualTo(SECRET.hash());
                }
        );
    }

    @Test
    @DisplayName("a segunda revogação não muda nada e não registra evento")
    void secondRevokeIsNoop() {
        var session = started();

        session.revoke(USER_ID, NOW.plusSeconds(10));
        session.pullEvents();
        session.revoke(ADMIN_ID, NOW.plusSeconds(20));

        assertThat(session.getRevokedAt()).contains(NOW.plusSeconds(10));
        assertThat(session.getRevokedBy()).contains(USER_ID);
        assertThat(session.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("reconstituir devolve o estado lido do banco, com a versão e sem eventos")
    void reconstituteRestoresState() {
        var id = UUID.randomUUID();
        var revoked = NOW.plusSeconds(5);

        var session = Session.reconstitute(
                id,
                4L,
                USER_ID,
                SECRET.hash(),
                DEVICE,
                NOW,
                NOW.plusSeconds(2),
                EXPIRES_AT,
                revoked,
                ADMIN_ID
        );

        assertThat(session.getId()).isEqualTo(id);
        assertThat(session.getVersion()).contains(4L);
        assertThat(session.getUserId()).isEqualTo(USER_ID);
        assertThat(session.getTokenHash()).isEqualTo(SECRET.hash());
        assertThat(session.getDevice()).isEqualTo(DEVICE);
        assertThat(session.getStartedAt()).isEqualTo(NOW);
        assertThat(session.getLastUsedAt()).isEqualTo(NOW.plusSeconds(2));
        assertThat(session.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(session.getRevokedAt()).contains(revoked);
        assertThat(session.getRevokedBy()).contains(ADMIN_ID);
        assertThat(session.isActiveAt(NOW.plusSeconds(6))).isFalse();
        assertThat(session.pullEvents()).isEmpty();
    }
}
