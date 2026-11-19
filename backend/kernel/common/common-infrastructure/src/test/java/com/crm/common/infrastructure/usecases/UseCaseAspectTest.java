package com.crm.common.infrastructure.usecases;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.*;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.common.domain.exceptions.ValidationException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("UseCaseAspect")
class UseCaseAspectTest {

    @UseCase
    private static final class DefaultUseCase {
    }

    @UseCase(transactional = false)
    private static final class QueryUseCase {
    }

    @UseCase(retriable = true, maxRetries = 2)
    private static final class RetriableUseCase {
    }

    private static final UseCase DEFAULT = DefaultUseCase.class.getAnnotation(UseCase.class);
    private static final UseCase QUERY = QueryUseCase.class.getAnnotation(UseCase.class);
    private static final UseCase RETRIABLE = RetriableUseCase.class.getAnnotation(UseCase.class);

    private TransactionTemplate transactionTemplate;
    private UseCaseAspect aspect;

    @BeforeEach
    void setUp() {
        transactionTemplate = mock(TransactionTemplate.class);

        // executa o callback direto: o que interessa aqui é se a transação foi usada, não o
        // comportamento do gerenciador de transação do Spring
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, TransactionCallback.class)
                        .doInTransaction(null));

        aspect = new UseCaseAspect(
                transactionTemplate,
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults()
        );
    }

    private ProceedingJoinPoint joinPointReturning(Object value) throws Throwable {
        var joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.getTarget()).thenReturn(new DefaultUseCase());
        when(joinPoint.proceed()).thenReturn(value);

        return joinPoint;
    }

    private ProceedingJoinPoint joinPointThrowing(Throwable throwable) throws Throwable {
        var joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.getTarget()).thenReturn(new DefaultUseCase());
        when(joinPoint.proceed()).thenThrow(throwable);

        return joinPoint;
    }

    @Test
    @DisplayName("devolve o resultado do use case")
    void returnsResult() throws Throwable {
        var result = aspect.around(joinPointReturning("ok"), DEFAULT);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("traduz ValidationException para 400")
    void translatesValidationToBadRequest() throws Throwable {
        var joinPoint = joinPointThrowing(new ValidationException("e-mail inválido"));

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("e-mail inválido")
                .hasCauseInstanceOf(ValidationException.class);
    }

    /**
     * Regressão de ordem de catch: {@code ValidationException} estende {@code DomainException},
     * então capturar a genérica antes faria todo VO inválido virar 422 em vez de 400.
     */
    @Test
    @DisplayName("traduz DomainException para 422")
    void translatesDomainToUnprocessable() throws Throwable {
        var joinPoint = joinPointThrowing(new DomainException("evento já está cancelado"));

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("evento já está cancelado")
                .hasCauseInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("preserva o código ao traduzir DomainException")
    void preservesDomainCode() throws Throwable {
        var joinPoint = joinPointThrowing(new DomainException("link_used", "este link já foi usado"));

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isInstanceOfSatisfying(
                        UnprocessableEntityException.class,
                        exception -> assertThat(exception.getCode()).contains("link_used")
                );
    }

    @Test
    @DisplayName("preserva o código ao traduzir ValidationException")
    void preservesValidationCode() throws Throwable {
        var joinPoint = joinPointThrowing(new ValidationException("code_invalid", "código inválido ou expirado"));

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isInstanceOfSatisfying(
                        BadRequestException.class,
                        exception -> assertThat(exception.getCode()).contains("code_invalid")
                );
    }

    @Test
    @DisplayName("sem código no domínio, a exception de application também fica sem")
    void keepsEmptyCode() throws Throwable {
        var joinPoint = joinPointThrowing(new DomainException("evento já está cancelado"));

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isInstanceOfSatisfying(
                        UnprocessableEntityException.class,
                        exception -> assertThat(exception.getCode()).isEmpty()
                );
    }

    @Test
    @DisplayName("deixa passar exception de application sem tocar")
    void passesApplicationExceptionThrough() throws Throwable {
        var original = new NotFoundException("evento não encontrado");

        var joinPoint = joinPointThrowing(original);

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isSameAs(original);
    }

    @Test
    @DisplayName("converte exception checada inesperada em 500")
    void wrapsCheckedException() throws Throwable {
        var joinPoint = joinPointThrowing(new Exception("falha crua"));

        assertThatThrownBy(() -> aspect.around(joinPoint, DEFAULT))
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    @DisplayName("usa transação por padrão")
    void usesTransactionByDefault() throws Throwable {
        aspect.around(joinPointReturning("ok"), DEFAULT);

        verify(transactionTemplate).execute(any());
    }

    @Test
    @DisplayName("query com transactional=false não abre transação")
    void skipsTransactionOnQuery() throws Throwable {
        aspect.around(joinPointReturning("ok"), QUERY);

        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    @DisplayName("não repete erro de negócio, mesmo com retriable ligado")
    void doesNotRetryBusinessErrors() throws Throwable {
        var attempts = new AtomicInteger();

        var joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(new RetriableUseCase());
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            attempts.incrementAndGet();
            throw new ConflictException("já existe");
        });

        assertThatThrownBy(() -> aspect.around(joinPoint, RETRIABLE))
                .isInstanceOf(ConflictException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("repete falha de serviço externo até maxRetries")
    void retriesExternalServiceFailures() throws Throwable {
        var attempts = new AtomicInteger();

        var joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(new RetriableUseCase());
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            attempts.incrementAndGet();
            throw new ExternalServiceException("gateway fora");
        });

        assertThatThrownBy(() -> aspect.around(joinPoint, RETRIABLE))
                .isInstanceOf(ExternalServiceException.class);

        // maxRetries conta repetições: 1 chamada + 2 repetições
        assertThat(attempts.get()).isEqualTo(3);
    }
}
