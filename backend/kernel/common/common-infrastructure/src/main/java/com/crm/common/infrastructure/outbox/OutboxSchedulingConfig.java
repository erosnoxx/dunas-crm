package com.crm.common.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Agenda o despacho do outbox de forma programática.
 *
 * <p>Existe para que o intervalo venha de {@link OutboxProperties}, junto dos outros ajustes do
 * outbox. Com {@code @Scheduled(fixedDelayString = "${crm.outbox.poll-delay:5000}")} o valor e
 * o default ficavam inline numa string de anotação, fora do record — e {@code OutboxProperties}
 * dava a falsa impressão de listar todos os botões do outbox.
 *
 * <p>Fica numa classe separada do {@code OutboxConfig} de propósito: injetar aqui o
 * {@code OutboxDispatcherJob} que o próprio {@code OutboxConfig} declara criaria uma
 * autorreferência.
 *
 * <p>O job é <b>injetado</b>, nunca construído aqui: é o proxy do Spring que carrega o
 * {@code @Transactional} de {@code dispatch()}, e sem ele o lock com SKIP LOCKED não teria
 * efeito nenhum.
 */
@Configuration
@RequiredArgsConstructor
class OutboxSchedulingConfig implements SchedulingConfigurer {

    private final OutboxDispatcherJob dispatcherJob;
    private final OutboxProperties properties;

    /**
     * Fixed delay (não fixed rate): o intervalo conta a partir do <b>fim</b> de um ciclo. Um
     * ciclo lento não acumula execuções sobrepostas competindo pelas mesmas linhas.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(dispatcherJob::dispatch, properties.pollDelay());
    }
}
