package com.crm.common.infrastructure.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Dá um {@code requestId}/{@code correlationId} novo a cada execução de um método
 * {@code @Scheduled}, para que o log de um job (limpeza de token, renovação de conta, limpeza
 * de reserva) possa ser lido como uma execução só.
 *
 * <p>Vale para todo job de todo BC sem registro por BC, pelo mesmo motivo do aspect de
 * {@code @UseCase}: é técnico e genérico. O despacho do outbox é agendado programaticamente
 * (não por {@code @Scheduled}) e abre o próprio contexto.
 */
@Aspect
public class ScheduledCorrelationAspect {

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object aroundScheduled(ProceedingJoinPoint joinPoint) throws Throwable {
        try (var ignored = CorrelationContext.openNew()) {
            return joinPoint.proceed();
        }
    }
}
