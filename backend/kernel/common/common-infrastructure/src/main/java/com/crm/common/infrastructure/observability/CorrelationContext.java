package com.crm.common.infrastructure.observability;

import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Os dois ids que acompanham tudo que o sistema faz (D39, architecture.md §12.3), guardados no
 * MDC do SLF4J para aparecerem em todo log.
 *
 * <ul>
 *   <li>{@code requestId} identifica <b>esta unidade de trabalho</b>: uma requisição HTTP, uma
 *       execução de job ou o processamento de uma mensagem.
 *   <li>{@code correlationId} atravessa as fronteiras assíncronas: nasce na requisição (ou no
 *       webhook, ou no job) que origina o fato, viaja no outbox e na mensagem do broker, e volta
 *       ao MDC no consumidor. É o que permite seguir uma compra por API, fila e webhook.
 * </ul>
 *
 * <p>Os dois são texto opaco. O valor que vem de fora (cabeçalho, mensagem) só é aceito se casar
 * com {@link #isValid}, para que ninguém consiga injetar quebra de linha ou texto longo no log.
 */
public final class CorrelationContext {

    public static final String REQUEST_ID = "requestId";
    public static final String CORRELATION_ID = "correlationId";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private CorrelationContext() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** Aceita só o que é seguro pôr num log: letras, dígitos, ponto, sublinhado e hífen, até 64. */
    public static boolean isValid(String value) {
        return value != null && VALID.matcher(value).matches();
    }

    public static Optional<String> correlationId() {
        return Optional.ofNullable(MDC.get(CORRELATION_ID));
    }

    /**
     * Põe os dois ids no MDC até o {@link Scope} ser fechado, e então restaura o que havia. Use
     * sempre em try-with-resources: uma thread de pool que herda o id da tarefa anterior polui
     * os logs da seguinte.
     */
    public static Scope open(String requestId, String correlationId) {
        var previousRequestId = MDC.get(REQUEST_ID);
        var previousCorrelationId = MDC.get(CORRELATION_ID);

        put(REQUEST_ID, requestId);
        put(CORRELATION_ID, correlationId);

        return () -> {
            put(REQUEST_ID, previousRequestId);
            put(CORRELATION_ID, previousCorrelationId);
        };
    }

    /** Uma unidade de trabalho nova, que é ela mesma a raiz da correlação (jobs agendados). */
    public static Scope openNew() {
        var id = newId();

        return open(id, id);
    }

    private static void put(String key, String value) {
        if (value == null)
            MDC.remove(key);
        else
            MDC.put(key, value);
    }

    /** Fechar nunca lança: o {@code close} roda no {@code finally} de quem trabalha. */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        @Override
        void close();
    }
}
