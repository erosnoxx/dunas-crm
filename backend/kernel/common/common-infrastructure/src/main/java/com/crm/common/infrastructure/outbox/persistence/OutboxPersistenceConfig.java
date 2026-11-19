package com.crm.common.infrastructure.outbox.persistence;

import com.crm.common.application.outbox.OutboxReader;
import com.crm.common.application.outbox.OutboxWriter;
import com.crm.common.infrastructure.outbox.OutboxProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Registra os beans de persistência do outbox.
 *
 * <p>Mora neste pacote (e não no {@code OutboxConfig} do pacote pai) para que a entity, a
 * repository do Spring Data e as duas implementações continuem package-private: nada fora do
 * outbox precisa alcançá-las, só os contratos {@code OutboxWriter}/{@code OutboxReader}.
 */
@Configuration
public class OutboxPersistenceConfig {

    @Bean
    public OutboxWriter outboxWriter(
            OutboxEventJpaRepository repository,
            ObjectMapper objectMapper
    ) {
        return new JpaOutboxWriterImpl(repository, objectMapper);
    }

    @Bean
    public OutboxReader outboxReader(
            OutboxEventJpaRepository repository,
            OutboxProperties properties,
            Clock clock
    ) {
        return new JpaOutboxReaderImpl(
                repository,
                properties.maxAttempts(),
                properties.retryBackoff(),
                clock
        );
    }
}
