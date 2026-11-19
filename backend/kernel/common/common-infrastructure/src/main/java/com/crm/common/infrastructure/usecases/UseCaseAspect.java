package com.crm.common.infrastructure.usecases;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.*;
import com.crm.common.domain.exceptions.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Interpreta a anotação {@link UseCase}. É o que mantém a camada de application em Java puro:
 * nenhum use case declara {@code @Transactional}, {@code @Retryable} ou qualquer coisa de Spring.
 *
 * <p>Vale para todos os bounded contexts sem registro extra por BC.
 *
 * <p><b>Ordem de composição</b> (de fora para dentro):
 *
 * <pre>
 *   log  →  retry  →  circuit breaker  →  transação  →  tradução de exception  →  use case
 * </pre>
 *
 * <ul>
 *   <li><b>transação dentro do retry</b>: cada tentativa recebe uma transação nova. Repetir
 *       dentro de uma transação já marcada como rollback-only não serviria para nada.
 *   <li><b>tradução mais interna</b>: tudo acima dela só vê {@code ApplicationException} ou
 *       falha técnica crua, o que torna a decisão de "isto é retentável?" trivial.
 *   <li><b>retry fora do circuit breaker</b>: ordem recomendada pelo próprio Resilience4j.
 * </ul>
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class UseCaseAspect {

    private final TransactionTemplate transactionTemplate;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Around("@within(useCase)")
    public Object around(ProceedingJoinPoint joinPoint, UseCase useCase) {
        var name = joinPoint.getTarget().getClass().getSimpleName();

        var decorated = decorate(joinPoint, useCase, name);

        return logging(decorated, name, useCase);
    }

    private Supplier<Object> decorate(
            ProceedingJoinPoint joinPoint,
            UseCase useCase,
            String name
    ) {
        Supplier<Object> supplier = withTransaction(() -> translating(joinPoint), useCase);

        if (useCase.circuitBreaker())
            supplier = CircuitBreaker.decorateSupplier(circuitBreakerRegistry.circuitBreaker(name), supplier);

        if (useCase.retriable())
            supplier = Retry.decorateSupplier(retry(name, useCase.maxRetries()), supplier);

        return supplier;
    }

    private Supplier<Object> withTransaction(Supplier<Object> supplier, UseCase useCase) {
        if (!useCase.transactional())
            return supplier;

        return () -> transactionTemplate.execute(status -> supplier.get());
    }

    /**
     * Fronteira de saída da application: a partir daqui ninguém mais vê exception de domínio.
     * É isto que permite a web capturar só tipos de application, sem nomear
     * {@code DomainException} nem {@code ValidationException}.
     *
     * <p>A ordem dos {@code catch} importa: {@code ValidationException} estende
     * {@code DomainException} e precisa vir antes, senão todo VO inválido viraria 422 em vez
     * de 400.
     */
    private Object translating(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        }

        catch (ApplicationException e) {
            throw e;
        }

        catch (ValidationException e) {
            throw new BadRequestException(e.getCode().orElse(null), e.getMessage(), e);
        }

        catch (DomainException e) {
            throw new UnprocessableEntityException(e.getCode().orElse(null), e.getMessage(), e);
        }

        catch (RuntimeException | Error e) {
            throw e;
        }

        catch (Throwable t) {
            throw new InternalServerErrorException("falha inesperada ao executar o caso de uso", t);
        }
    }

    private Retry retry(String name, int maxRetries) {
        // maxRetries conta repetições, não tentativas: 3 retries = 1 chamada + 3 repetições.
        var config = RetryConfig.custom()
                .maxAttempts(maxRetries + 1)
                .retryOnException(this::isRetryable)
                .build();

        return retryRegistry.retry(name, config);
    }

    /**
     * Repetir só faz sentido para falha transitória. Erro de negócio (404, 409, 422, ...) não
     * melhora na segunda tentativa, e repeti-lo sobre uma operação não idempotente duplica
     * trabalho.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ExternalServiceException)
            return true;

        return !(throwable instanceof ApplicationException);
    }

    private Object logging(
            Supplier<Object> supplier,
            String name,
            UseCase useCase
    ) {
        var startedAt = System.nanoTime();

        log.debug("use case {} iniciado", name);

        try {
            var result = supplier.get();

            logSuccess(useCase, name, elapsedMs(startedAt));

            return result;
        }

        catch (RuntimeException e) {
            log.warn(
                    "use case {} falhou em {}ms — {}: {}",
                    name, elapsedMs(startedAt), e.getClass().getSimpleName(), e.getMessage()
            );

            throw e;
        }
    }

    /**
     * Query é leitura frequente (o estado de sessão é lido a cada requisição autenticada): o
     * sucesso dela vai para DEBUG, senão vira uma linha de log por requisição. Command muda
     * estado, e o sucesso dele continua em INFO.
     */
    private static void logSuccess(
            UseCase useCase,
            String name,
            long elapsedMs
    ) {
        if (useCase.transactional()) {
            log.info("use case {} concluído em {}ms", name, elapsedMs);
            return;
        }

        log.debug("use case {} concluído em {}ms", name, elapsedMs);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
