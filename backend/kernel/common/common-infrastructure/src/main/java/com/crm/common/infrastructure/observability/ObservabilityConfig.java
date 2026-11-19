package com.crm.common.infrastructure.observability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liga a correlação de log nos jobs agendados. A do HTTP mora em {@code boot}, junto dos
 * filtros de segurança; a das mensagens, em {@code MessagingConfig}; a das tarefas assíncronas,
 * em {@code AsyncConfig}.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public ScheduledCorrelationAspect scheduledCorrelationAspect() {
        return new ScheduledCorrelationAspect();
    }
}
