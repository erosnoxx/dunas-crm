package com.crm.common.infrastructure.async;

import com.crm.common.infrastructure.observability.MdcTaskDecorator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Executor dos efeitos colaterais lentos disparados por domain event depois do commit (e-mail,
 * chamada a terceiro), fora da thread do request (architecture.md §5.1).
 *
 * <p>O listener declara {@code @Async(AsyncConfig.SIDE_EFFECT_EXECUTOR)} junto de
 * {@code @EventListener}. Fila cheia descarta a tarefa com log {@code ERROR}: a requisição que
 * disparou o efeito já commitou, e devolver erro a ela agora seria mentir sobre o que aconteceu.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig {

    public static final String SIDE_EFFECT_EXECUTOR = "sideEffectExecutor";

    @Bean(name = SIDE_EFFECT_EXECUTOR)
    public ThreadPoolTaskExecutor sideEffectExecutor(AsyncProperties properties) {
        var executor = new ThreadPoolTaskExecutor();

        executor.setThreadNamePrefix("crm-side-effect-");
        // sem isto o log do efeito colateral perde o correlationId da requisição que o originou
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.maxPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds((int) properties.shutdownTimeout().toSeconds());
        executor.setRejectedExecutionHandler(AsyncConfig::discard);

        return executor;
    }

    private static void discard(Runnable task, ThreadPoolExecutor executor) {
        log.error("fila de efeitos colaterais cheia ({} tarefas); tarefa descartada", executor.getQueue().size());
    }
}
