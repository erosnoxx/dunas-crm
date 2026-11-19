package com.crm.common.infrastructure.persistence;

import com.crm.common.application.idempotency.IdempotencyGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Mora neste pacote (não no pacote pai {@code idempotency}) para que a entity e a repository do
 * Spring Data continuem package-private — nada fora daqui precisa alcançá-las, só o contrato
 * {@code IdempotencyGuard}. Mesmo padrão de {@code outbox}/{@code outbox.persistence}.
 */
@Configuration
public class IdempotencyPersistenceConfig {

    @Bean
    public IdempotencyGuard idempotencyGuard(
            IdempotentExecutionJpaRepository repository,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        return new JpaIdempotencyGuardImpl(
                repository,
                objectMapper,
                transactionTemplate,
                clock
        );
    }
}
