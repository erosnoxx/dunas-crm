package com.crm.common.infrastructure.persistence;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.application.idempotency.IdempotencyGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Duas transações curtas e próprias — reivindicar a chave, e registrar o resultado — com
 * {@code operation} rodando **fora** das duas. Não é um detalhe de implementação qualquer: se
 * este guard abrisse uma única transação envolvendo {@code operation} inteira, cada use case
 * chamado dentro dela (via {@code @UseCase}, `TransactionTemplate` com `PROPAGATION_REQUIRED`)
 * entraria nessa mesma transação — juntando BCs diferentes numa transação de fato, sem
 * ninguém decidir isso de propósito. Isso destruiria a razão de a orquestração cross-BC exigir
 * compensação escrita à mão (nada ficaria "órfão" pra compensar, porque tudo desfaria sozinho
 * num rollback) e prenderia locks de linha (ex. o contador de capacidade de `ticketing`) pelo
 * tempo do fluxo inteiro, não só do próprio passo.
 *
 * <p>A tentativa de inserção acontece na própria transação porque um {@code INSERT} que viola
 * constraint única **aborta a transação no Postgres** — comandos seguintes na mesma transação
 * falhariam com "current transaction is aborted", mesmo capturando a exception em Java. Por
 * isso resolver a linha existente (ler, decidir, talvez reabrir) sempre acontece numa transação
 * nova, nunca na mesma que falhou.
 */
@Slf4j
@RequiredArgsConstructor
class JpaIdempotencyGuardImpl implements IdempotencyGuard {

    private final IdempotentExecutionJpaRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public <T> T runOnce(String key, Object requestFingerprint, Class<T> resultType, Supplier<T> operation) {
        requireKey(key);

        var fingerprint = fingerprintOf(requestFingerprint);
        var replay = claim(key, fingerprint, resultType);

        if (replay.isPresent())
            return replay.get();

        return execute(key, operation);
    }

    /**
     * Vazio = esta chamada deve rodar {@code operation} agora (chave nova, ou {@code FAILED}
     * reaberta). Presente = repetição legítima de uma execução já concluída — o valor é o
     * resultado gravado, {@code operation} não roda de novo.
     */
    private <T> Optional<T> claim(String key, String fingerprint, Class<T> resultType) {
        if (tryInsert(key, fingerprint))
            return Optional.empty();

        return resolveExisting(key, fingerprint, resultType);
    }

    private boolean tryInsert(String key, String fingerprint) {
        return transactionTemplate.execute(status ->
            repository.insertIfAbsent(key, fingerprint, Instant.now(clock))) == 1;
    }

    private <T> Optional<T> resolveExisting(String key, String fingerprint, Class<T> resultType) {
        return transactionTemplate.execute(status -> {
            var entity = repository.findById(key)
                .orElseThrow(() -> new InternalServerErrorException(
                    "execução idempotente sumiu entre a tentativa de inserção e a leitura: " + key
                ));

            if (!entity.matchesFingerprint(fingerprint))
                throw new ConflictException("chave de idempotência já foi usada com dados diferentes");

            if (entity.isCompleted())
                return Optional.of(deserialize(entity.getResultPayload(), resultType));

            if (entity.isInProgress())
                throw new ConflictException("já existe uma execução em andamento para esta chave");

            // FAILED: nada foi de fato concluído, então uma nova tentativa é legítima — mas só
            // uma, se houver corrida entre duas chamadas concorrentes vendo o mesmo FAILED.
            if (repository.beginRetry(key) == 0)
                throw new ConflictException("outra tentativa concorrente já está reprocessando esta chave");

            return Optional.<T>empty();
        });
    }

    private <T> T execute(String key, Supplier<T> operation) {
        try {
            var result = operation.get();

            recordSuccess(key, result);

            return result;
        }
        catch (RuntimeException e) {
            recordFailure(key, e.getMessage());

            throw e;
        }
    }

    /**
     * Melhor esforço, de propósito: {@code operation} já teve o efeito real no mundo (o use
     * case chamado já commitou a própria transação). Se só o registro deste guard falhar, o
     * chamador ainda recebe o resultado verdadeiro — não faz sentido transformar um sucesso
     * real em erro porque a contabilidade de deduplicação tropeçou. O custo fica documentado:
     * uma repetição futura com a mesma chave pode não encontrar o registro certo. Logado em
     * ERROR porque isso é operacional, não deveria acontecer em silêncio.
     */
    private <T> void recordSuccess(String key, T result) {
        try {
            var payload = serialize(result);

            transactionTemplate.executeWithoutResult(status ->
                repository.findById(key).ifPresent(entity -> {
                    entity.markCompleted(payload, Instant.now(clock));
                    repository.save(entity);
                }));
        }
        catch (RuntimeException e) {
            log.error("falha ao registrar sucesso da execução idempotente {} — o resultado já é real, só a deduplicação futura fica comprometida: {}", key, e.getMessage(), e);
        }
    }

    /** Mesmo raciocínio de {@link #recordSuccess}: nunca deixa a contabilidade mascarar o erro real. */
    private void recordFailure(String key, String reason) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                repository.findById(key).ifPresent(entity -> {
                    entity.markFailed(reason, Instant.now(clock));
                    repository.save(entity);
                }));
        }
        catch (RuntimeException e) {
            log.error("falha ao registrar falha da execução idempotente {}: {}", key, e.getMessage(), e);
        }
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank())
            throw new BadRequestException("chave de idempotência não pode ser nula ou vazia");
    }

    // package-private (não private): o teste calcula o hash esperado com o mesmo método, em
    // vez de duplicar a lógica de hashing só para montar o cenário de mock.
    String fingerprintOf(Object requestFingerprint) {
        try {
            var json = objectMapper.writeValueAsString(requestFingerprint);
            var digest = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        }
        catch (JsonProcessingException e) {
            throw new InternalServerErrorException("falha ao serializar o payload para calcular a chave de idempotência", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new InternalServerErrorException("SHA-256 indisponível nesta JVM", e);
        }
    }

    private String serialize(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        }
        catch (JsonProcessingException e) {
            throw new InternalServerErrorException("falha ao serializar o resultado da execução idempotente", e);
        }
    }

    private <T> T deserialize(String payload, Class<T> resultType) {
        try {
            return objectMapper.readValue(payload, resultType);
        }
        catch (JsonProcessingException e) {
            throw new InternalServerErrorException("falha ao desserializar o resultado gravado da execução idempotente", e);
        }
    }
}
