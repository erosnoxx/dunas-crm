package com.crm.common.infrastructure.usecases;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registra o aspect de {@code @UseCase}. Fica em {@code common-infrastructure} porque é
 * técnico e genérico: vale para todos os bounded contexts sem nenhum registro por BC.
 *
 * <p>{@code TransactionTemplate} é declarado aqui (transação programática) exatamente para
 * que nenhum use case precise de {@code @Transactional}.
 *
 * <p>Os registros do Resilience4j também nascem aqui: o projeto usa só o núcleo da biblioteca, sem
 * o starter do Spring Boot, então ninguém mais os cria e o contexto não subiria sem eles.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    public TransactionTemplate useCaseTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public UseCaseAspect useCaseAspect(
            TransactionTemplate useCaseTransactionTemplate,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        return new UseCaseAspect(useCaseTransactionTemplate, retryRegistry, circuitBreakerRegistry);
    }
}
