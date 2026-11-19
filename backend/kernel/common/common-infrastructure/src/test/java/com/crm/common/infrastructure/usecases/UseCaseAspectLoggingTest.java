package com.crm.common.infrastructure.usecases;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.crm.common.application.annotations.UseCase;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("UseCaseAspect, log de sucesso")
class UseCaseAspectLoggingTest {

    @UseCase
    private static final class CommandUseCase {
    }

    @UseCase(transactional = false)
    private static final class QueryUseCase {
    }

    private static final UseCase COMMAND = CommandUseCase.class.getAnnotation(UseCase.class);
    private static final UseCase QUERY = QueryUseCase.class.getAnnotation(UseCase.class);

    private final Logger logger = (Logger) LoggerFactory.getLogger(UseCaseAspect.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;
    private UseCaseAspect aspect;

    @BeforeEach
    void setUp() {
        var transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, TransactionCallback.class)
                        .doInTransaction(null));

        aspect = new UseCaseAspect(
                transactionTemplate,
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults()
        );

        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
    }

    private ProceedingJoinPoint joinPoint(Object target) throws Throwable {
        var joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.proceed()).thenReturn("ok");

        return joinPoint;
    }

    private Level successLevel() {
        return appender.list.stream()
                .filter(event -> event.getMessage().startsWith("use case {} concluído"))
                .findFirst()
                .orElseThrow()
                .getLevel();
    }

    @Test
    @DisplayName("command que termina bem vai para INFO")
    void commandSuccessIsInfo() throws Throwable {
        aspect.around(joinPoint(new CommandUseCase()), COMMAND);

        assertThat(successLevel()).isEqualTo(Level.INFO);
    }

    @Test
    @DisplayName("query que termina bem vai para DEBUG: o estado de sessão é lido a cada requisição autenticada")
    void querySuccessIsDebug() throws Throwable {
        aspect.around(joinPoint(new QueryUseCase()), QUERY);

        assertThat(successLevel()).isEqualTo(Level.DEBUG);
    }
}
