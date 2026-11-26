package com.crm.common.infrastructure.usecases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("UseCaseConfig")
class UseCaseConfigTest {

    @Test
    @DisplayName("monta o aspect só com o gerenciador de transação: os registros do Resilience4j vêm da própria config")
    void aspectNeedsOnlyTheTransactionManager() {
        new ApplicationContextRunner()
                .withUserConfiguration(UseCaseConfig.class)
                .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(UseCaseAspect.class));
    }
}
