package com.crm.common.infrastructure.persistence.entities;

import com.crm.common.infrastructure.common.TimeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PersistenceTimestampsListener")
class PersistenceTimestampsListenerTest {

    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");

    private static final class SampleEntity extends JpaEntity<UUID> {
    }

    private final PersistenceTimestampsListener listener =
            new PersistenceTimestampsListener(Clock.fixed(NOW, TimeConfig.ZONE));

    @Test
    @DisplayName("prePersist preenche createdAt com a hora do Clock, no fuso de Brasília")
    void prePersistStampsCreatedAt() {
        var entity = new SampleEntity();

        listener.prePersist(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, TimeConfig.ZONE));
        assertThat(entity.getCreatedAt().getOffset().getId()).isEqualTo("-03:00");
    }

    @Test
    @DisplayName("prePersist não sobrescreve createdAt já preenchido")
    void prePersistKeepsExistingCreatedAt() {
        var entity = new SampleEntity();
        var original = OffsetDateTime.ofInstant(NOW.minusSeconds(3600), TimeConfig.ZONE);
        entity.setCreatedAt(original);

        listener.prePersist(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(original);
    }

    @Test
    @DisplayName("preUpdate preenche updatedAt com a hora do Clock")
    void preUpdateStampsUpdatedAt() {
        var entity = new SampleEntity();

        listener.preUpdate(entity);

        assertThat(entity.getUpdatedAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, TimeConfig.ZONE));
    }
}
