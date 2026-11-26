package com.crm.iam.infrastructure.persistence;

import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.vos.TokenHash;
import com.crm.iam.domain.enums.TokenPurpose;
import com.crm.iam.infrastructure.mappers.OneTimeTokenMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OneTimeTokenMapper")
class OneTimeTokenMapperTest {

    private static final TokenHash HASH = TokenHash.of("a".repeat(64));

    private final OneTimeTokenMapper mapper = new OneTimeTokenMapper();

    private OneTimeToken token(Long version, Instant usedAt, Instant invalidatedAt) {
        return OneTimeToken.reconstitute(
                UUID.randomUUID(),
                version,
                UUID.randomUUID(),
                TokenPurpose.PASSWORD_RESET,
                HASH,
                Instant.parse("2026-11-19T11:00:00Z"),
                usedAt,
                invalidatedAt
        );
    }

    @Test
    @DisplayName("ida e volta preserva todos os campos e a versão")
    void roundTripKeepsEveryField() {
        var token = token(3L, Instant.parse("2026-11-19T10:30:00Z"), Instant.parse("2026-11-19T10:45:00Z"));

        var back = mapper.toDomain(mapper.toPersistence(token));

        assertThat(back.getId()).isEqualTo(token.getId());
        assertThat(back.getVersion()).contains(3L);
        assertThat(back.getUserId()).isEqualTo(token.getUserId());
        assertThat(back.getPurpose()).isEqualTo(TokenPurpose.PASSWORD_RESET);
        assertThat(back.getTokenHash()).isEqualTo(HASH);
        assertThat(back.getExpiresAt()).isEqualTo(token.getExpiresAt());
        assertThat(back.getUsedAt()).isEqualTo(token.getUsedAt());
        assertThat(back.getInvalidatedAt()).isEqualTo(token.getInvalidatedAt());
    }

    @Test
    @DisplayName("versão nula é linha nova e as datas opcionais ficam ausentes")
    void newRowKeepsOptionalDatesEmpty() {
        var entity = mapper.toPersistence(token(null, null, null));

        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getUsedAt()).isNull();
        assertThat(entity.getInvalidatedAt()).isNull();

        var back = mapper.toDomain(entity);

        assertThat(back.getVersion()).isEmpty();
        assertThat(back.getUsedAt()).isEmpty();
        assertThat(back.getInvalidatedAt()).isEmpty();
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(token(null, null, null));

        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        var back = mapper.toDomain(entity);

        assertThat(mapper.toPersistence(back).getCreatedAt()).isNull();
        assertThat(mapper.toPersistence(back).getUpdatedAt()).isNull();
    }
}
