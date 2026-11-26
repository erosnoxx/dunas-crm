package com.crm.common.infrastructure.observability;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Leva o MDC da thread que agenda a tarefa para a thread do executor.
 *
 * <p>Sem isto, o efeito colateral assíncrono (e-mail, chamada a terceiro depois do commit) roda
 * numa thread de pool com o MDC vazio, e o log dele perde o {@code correlationId} da requisição
 * que o originou, que é justamente o log que se quer seguir.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        var captured = MDC.getCopyOfContextMap();

        return () -> {
            var previous = MDC.getCopyOfContextMap();

            try {
                restore(captured);
                task.run();
            }

            finally {
                restore(previous);
            }
        };
    }

    private static void restore(java.util.Map<String, String> contextMap) {
        if (contextMap == null) {
            MDC.clear();
            return;
        }

        MDC.setContextMap(contextMap);
    }
}
