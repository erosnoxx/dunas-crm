package com.crm.common.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("JpaIdempotencyGuardImpl")
class JpaIdempotencyGuardImplTest {

    private static final Instant NOW = Instant.parse("2026-10-01T15:00:00Z");

    private record TestRequest(String value) {
    }

    private record TestResult(String value) {
    }

    private IdempotentExecutionJpaRepository repository;
    private ObjectMapper objectMapper;
    private TransactionTemplate transactionTemplate;
    private JpaIdempotencyGuardImpl guard;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(IdempotentExecutionJpaRepository.class);
        objectMapper = new ObjectMapper();
        transactionTemplate = mock(TransactionTemplate.class);

        // mesma técnica de UseCaseAspectTest: o mock só existe pra evitar um gerenciador de
        // transação real — o que importa aqui é COMO o guard usa o TransactionTemplate, não o
        // comportamento do Spring em si.
        when(transactionTemplate.execute(any()))
            .thenAnswer(invocation -> invocation.getArgument(0, TransactionCallback.class).doInTransaction(null));

        doAnswer(invocation -> {
            invocation.getArgument(0, Consumer.class).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        guard = new JpaIdempotencyGuardImpl(
                repository,
                objectMapper,
                transactionTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private Supplier<TestResult> countingOperation(List<TestResult> calls, TestResult toReturn) {
        return () -> {
            calls.add(toReturn);
            return toReturn;
        };
    }

    @Test
    @DisplayName("recusa chave nula ou vazia sem chamar a operação")
    void rejectsNullOrBlankKey() {
        var calls = new ArrayList<TestResult>();
        var operation = countingOperation(calls, new TestResult("x"));

        assertThatThrownBy(() -> guard.runOnce(null, new TestRequest("a"), TestResult.class, operation))
            .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> guard.runOnce("  ", new TestRequest("a"), TestResult.class, operation))
            .isInstanceOf(BadRequestException.class);

        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("chave nova: roda a operação e devolve o resultado")
    void runsOperationOnFreshKey() {
        var calls = new ArrayList<TestResult>();
        var expected = new TestResult("ok");

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(1);
        when(repository.findById("key-1")).thenReturn(Optional.of(new IdempotentExecutionEntity("key-1", "irrelevante", NOW)));

        var result = guard.runOnce("key-1", new TestRequest("a"), TestResult.class, countingOperation(calls, expected));

        assertThat(result).isEqualTo(expected);
        assertThat(calls).containsExactly(expected);
    }

    @Test
    @DisplayName("chave repetida, mesmo payload, COMPLETED: devolve o resultado gravado sem rodar a operação de novo")
    void replaysCompletedResult() throws Exception {
        var calls = new ArrayList<TestResult>();
        var request = new TestRequest("a");
        var fingerprint = guard.fingerprintOf(request);

        var existing = new IdempotentExecutionEntity("key-1", fingerprint, NOW);
        existing.markCompleted(objectMapper.writeValueAsString(new TestResult("cacheado")), NOW);

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(0);
        when(repository.findById("key-1")).thenReturn(Optional.of(existing));

        var result = guard.runOnce("key-1", request, TestResult.class, countingOperation(calls, new TestResult("novo")));

        assertThat(result).isEqualTo(new TestResult("cacheado"));
        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("mesma chave, payload diferente: ConflictException, sem rodar a operação")
    void rejectsDifferentFingerprintSameKey() {
        var calls = new ArrayList<TestResult>();
        var existing = new IdempotentExecutionEntity("key-1", "hash-de-outro-payload", NOW);

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(0);
        when(repository.findById("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> guard.runOnce("key-1", new TestRequest("a"), TestResult.class, countingOperation(calls, new TestResult("x"))))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("dados diferentes");

        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("mesma chave, mesmo payload, IN_PROGRESS: ConflictException — duas chamadas concorrentes não duplicam efeito")
    void rejectsConcurrentInProgress() {
        var calls = new ArrayList<TestResult>();
        var request = new TestRequest("a");
        var fingerprint = guard.fingerprintOf(request);
        var existing = new IdempotentExecutionEntity("key-1", fingerprint, NOW);

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(0);
        when(repository.findById("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> guard.runOnce("key-1", request, TestResult.class, countingOperation(calls, new TestResult("x"))))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("em andamento");

        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("mesma chave, mesmo payload, FAILED: reabre para nova tentativa e roda a operação")
    void retriesAfterFailure() {
        var calls = new ArrayList<TestResult>();
        var request = new TestRequest("a");
        var fingerprint = guard.fingerprintOf(request);
        var existing = new IdempotentExecutionEntity("key-1", fingerprint, NOW);
        existing.markFailed("timeout anterior", NOW);

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(0);
        when(repository.findById("key-1")).thenReturn(Optional.of(existing));
        when(repository.beginRetry("key-1")).thenReturn(1);

        var result = guard.runOnce("key-1", request, TestResult.class, countingOperation(calls, new TestResult("segunda tentativa")));

        assertThat(result).isEqualTo(new TestResult("segunda tentativa"));
        assertThat(calls).hasSize(1);
    }

    /**
     * Regressão do cenário de corrida: duas chamadas concorrentes veem o mesmo FAILED, só uma
     * pode reabrir. {@code beginRetry} devolvendo 0 é como o banco sinaliza "a outra chegou
     * primeiro".
     */
    @Test
    @DisplayName("mesma chave FAILED, mas beginRetry perde a corrida: ConflictException, sem rodar a operação")
    void rejectsConcurrentRetryRace() {
        var calls = new ArrayList<TestResult>();
        var request = new TestRequest("a");
        var fingerprint = guard.fingerprintOf(request);
        var existing = new IdempotentExecutionEntity("key-1", fingerprint, NOW);
        existing.markFailed("timeout anterior", NOW);

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(0);
        when(repository.findById("key-1")).thenReturn(Optional.of(existing));
        when(repository.beginRetry("key-1")).thenReturn(0);

        assertThatThrownBy(() -> guard.runOnce("key-1", request, TestResult.class, countingOperation(calls, new TestResult("x"))))
            .isInstanceOf(ConflictException.class);

        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("operação lança exception: propaga a exception original, sem embrulhar")
    void rethrowsOriginalExceptionFromOperation() {
        var original = new ConflictException("sem disponibilidade");

        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(1);
        when(repository.findById("key-1")).thenReturn(Optional.of(new IdempotentExecutionEntity("key-1", "irrelevante", NOW)));

        Supplier<TestResult> failingOperation = () -> {
            throw original;
        };

        assertThatThrownBy(() -> guard.runOnce("key-1", new TestRequest("a"), TestResult.class, failingOperation))
            .isSameAs(original);
    }

    /**
     * O que este teste consegue provar, e o que não consegue — vale deixar explícito: o
     * {@code TransactionTemplate} aqui é um mock que só executa a lambda na hora, sem modelar
     * transação nenhuma de verdade. Ele não prova isolamento transacional real (isso exigiria
     * banco de verdade observando commit/rollback). O que ele prova, e que já é valioso: o
     * código faz **duas chamadas distintas** a {@code executeWithoutResult} — uma pra
     * reivindicar a chave, outra pra registrar o resultado — em vez de uma chamada só
     * envolvendo tudo. Sob a semântica real do Spring ({@code PROPAGATION_REQUIRED}, sem
     * transação ambiente antes de cada chamada), duas chamadas discretas ao boundary são duas
     * transações; uma chamada só, envolvendo {@code operation}, seria uma.
     */
    @Test
    @DisplayName("reivindicar a chave e registrar o resultado são duas chamadas distintas ao TransactionTemplate")
    void claimAndRecordAreSeparateTransactionCalls() {
        when(repository.insertIfAbsent(eq("key-1"), any(), any())).thenReturn(1);
        when(repository.findById("key-1")).thenReturn(Optional.of(new IdempotentExecutionEntity("key-1", "irrelevante", NOW)));

        guard.runOnce("key-1", new TestRequest("a"), TestResult.class, () -> new TestResult("ok"));

        // uma para tryInsert (reivindicar), outra para recordSuccess (registrar) — nunca uma só
        verify(transactionTemplate, times(1)).execute(any());
        verify(transactionTemplate, times(1)).executeWithoutResult(any());
    }
}
