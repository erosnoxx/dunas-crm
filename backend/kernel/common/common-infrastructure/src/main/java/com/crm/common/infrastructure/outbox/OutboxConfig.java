package com.crm.common.infrastructure.outbox;

import com.crm.common.application.messaging.IntegrationEventSender;
import com.crm.common.application.outbox.OutboxReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Optional;

/**
 * Liga o outbox. Basta o módulo estar no classpath — nenhum bounded context e nem o {@code boot}
 * precisam registrar nada.
 *
 * <p>O {@code @EnableScheduling} fica aqui, e não em {@code boot}, deliberadamente. A leitura
 * estrita da arquitetura diria que habilitar scheduling é configuração do container inteiro e
 * portanto pertence a {@code boot}; o problema é o modo de falha: se {@code boot} esquecer,
 * o worker simplesmente nunca roda, nenhum evento de integração sai, e <b>não há erro nenhum</b>
 * apontando o motivo. Autocontido troca uma impureza de camada por uma falha impossível.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {

    /**
     * O {@code Optional} no sender é o que permite o app subir antes de o broker existir: o
     * outbox grava normalmente e o despacho fica parado, em vez de o contexto do Spring falhar
     * por não achar um {@code IntegrationEventSender}.
     */
    @Bean
    public OutboxDispatcherJob outboxDispatcherJob(
            OutboxReader outboxReader,
            Optional<IntegrationEventSender> sender,
            OutboxProperties properties
    ) {
        return new OutboxDispatcherJob(outboxReader, sender, properties);
    }
}
