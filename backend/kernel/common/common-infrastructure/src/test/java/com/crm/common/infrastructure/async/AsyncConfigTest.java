package com.crm.common.infrastructure.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AsyncConfig")
class AsyncConfigTest {

    @Test
    @DisplayName("fila cheia descarta a tarefa sem lançar nada em quem enviou")
    void saturatedQueueDropsWithoutThrowing() {
        var executor = new AsyncConfig().sideEffectExecutor(new AsyncProperties(1, 1, 1, Duration.ofSeconds(1)));
        executor.initialize();

        var release = new CountDownLatch(1);

        Runnable blocking = () -> {
            try {
                release.await();
            }

            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        executor.execute(blocking);
        executor.execute(blocking);

        assertThatCode(() -> executor.execute(() -> { })).doesNotThrowAnyException();

        release.countDown();
        executor.shutdown();
    }

    @Test
    @DisplayName("threads com nome reconhecível no log")
    void namesThreads() {
        var executor = new AsyncConfig().sideEffectExecutor(new AsyncProperties(2, 8, 500, Duration.ofSeconds(30)));

        assertThat(executor.getThreadNamePrefix()).isEqualTo("crm-side-effect-");
    }
}
