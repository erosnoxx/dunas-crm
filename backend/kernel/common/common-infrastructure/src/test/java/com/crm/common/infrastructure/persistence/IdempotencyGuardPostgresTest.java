package com.crm.common.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.idempotency.IdempotencyGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O guard contra o Postgres real (Testcontainers; exige Docker): a chave repetida bate na chave primária. */
@SpringBootTest(
        classes = IdempotencyGuardPostgresTest.TestApplication.class,
        properties = {
                "spring.flyway.locations=classpath:db/migration/common",
                "spring.flyway.table=flyway_schema_history_common",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@DisplayName("IdempotencyGuard contra o Postgres")
class IdempotencyGuardPostgresTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = IdempotentExecutionEntity.class)
    @EnableJpaRepositories(basePackageClasses = IdempotentExecutionJpaRepository.class)
    @Import(IdempotencyPersistenceConfig.class)
    static class TestApplication {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC);
        }

        /** Sem spring-web neste módulo, o Spring Boot não monta o ObjectMapper. */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    record Request(String value) {
    }

    record Result(String value) {
    }

    @Autowired
    private IdempotencyGuard guard;

    @Test
    @DisplayName("a mesma chave duas vezes roda a operação uma vez só e devolve o mesmo resultado")
    void sameKeyRunsOnce() {
        var key = UUID.randomUUID().toString();
        var runs = new AtomicInteger();

        var first = guard.runOnce(key, new Request("a"), Result.class, () -> new Result("rodada " + runs.incrementAndGet()));
        var second = guard.runOnce(key, new Request("a"), Result.class, () -> new Result("rodada " + runs.incrementAndGet()));

        assertThat(runs).hasValue(1);
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("outro pedido na mesma chave: recusa, sem rodar a operação de novo")
    void sameKeyOtherRequestIsRejected() {
        var key = UUID.randomUUID().toString();
        var runs = new AtomicInteger();

        guard.runOnce(key, new Request("a"), Result.class, () -> new Result("rodada " + runs.incrementAndGet()));

        assertThatThrownBy(() -> guard.runOnce(key, new Request("b"), Result.class, () -> new Result("rodada " + runs.incrementAndGet())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("dados diferentes");
        assertThat(runs).hasValue(1);
    }
}
