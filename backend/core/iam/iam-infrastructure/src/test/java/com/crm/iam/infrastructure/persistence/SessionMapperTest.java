package com.crm.iam.infrastructure.persistence;

import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.TokenHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionMapper")
class SessionMapperTest {

    private static final TokenHash HASH = TokenHash.of("b".repeat(64));

    private final SessionMapper mapper = new SessionMapper();

    private Session session(Long version, Instant revokedAt, UUID revokedBy) {
        return Session.reconstitute(
                UUID.randomUUID(),
                version,
                UUID.randomUUID(),
                HASH,
                DeviceLabel.of("Galaxy A54, app DUNAS"),
                Instant.parse("2026-11-19T10:00:00Z"),
                Instant.parse("2026-11-19T10:10:00Z"),
                Instant.parse("2026-12-19T10:10:00Z"),
                revokedAt,
                revokedBy
        );
    }

    @Test
    @DisplayName("ida e volta preserva todos os campos e a versão")
    void roundTripKeepsEveryField() {
        var session = session(5L, Instant.parse("2026-11-19T10:20:00Z"), UUID.randomUUID());

        var back = mapper.toDomain(mapper.toPersistence(session));

        assertThat(back.getId()).isEqualTo(session.getId());
        assertThat(back.getVersion()).contains(5L);
        assertThat(back.getUserId()).isEqualTo(session.getUserId());
        assertThat(back.getTokenHash()).isEqualTo(HASH);
        assertThat(back.getDevice()).isEqualTo(session.getDevice());
        assertThat(back.getStartedAt()).isEqualTo(session.getStartedAt());
        assertThat(back.getLastUsedAt()).isEqualTo(session.getLastUsedAt());
        assertThat(back.getExpiresAt()).isEqualTo(session.getExpiresAt());
        assertThat(back.getRevokedAt()).isEqualTo(session.getRevokedAt());
        assertThat(back.getRevokedBy()).isEqualTo(session.getRevokedBy());
    }

    @Test
    @DisplayName("versão nula é linha nova e a revogação ausente fica nula")
    void newRowKeepsRevocationEmpty() {
        var entity = mapper.toPersistence(session(null, null, null));

        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getRevokedAt()).isNull();
        assertThat(entity.getRevokedBy()).isNull();

        var back = mapper.toDomain(entity);

        assertThat(back.getVersion()).isEmpty();
        assertThat(back.getRevokedAt()).isEmpty();
        assertThat(back.getRevokedBy()).isEmpty();
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(session(null, null, null));

        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        var back = mapper.toDomain(entity);

        assertThat(mapper.toPersistence(back).getCreatedAt()).isNull();
        assertThat(mapper.toPersistence(back).getUpdatedAt()).isNull();
    }
}
