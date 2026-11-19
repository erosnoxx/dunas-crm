package com.crm.common.infrastructure.messaging;

import com.crm.common.application.messaging.IntegrationEventSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Transporte dos integration events. Declara só o que é comum a todos os bounded contexts: a
 * exchange, o sender, o conversor JSON e a fábrica de listener (com a política de retry/DLQ,
 * comum a qualquer BC consumidor).
 *
 * <p><b>Queue, binding e DLQ de cada BC consumidor moram na {@code <bc>-infrastructure} dele</b>,
 * não aqui — é config de escopo de um BC. Esta classe não sabe quem consome o quê.
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingConfig {

    /**
     * Topic exchange, durável: a routing key é o {@code eventType}, então um consumidor pode
     * assinar um evento específico ({@code ordering.order.paid}) ou uma família
     * ({@code ordering.#}) sem que o produtor saiba que ele existe.
     */
    @Bean
    public TopicExchange crmEventsExchange(MessagingProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    public IntegrationEventSender integrationEventSender(
            RabbitTemplate rabbitTemplate,
            MessagingProperties properties
    ) {
        return new RabbitIntegrationEventSenderImpl(rabbitTemplate, properties);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * {@code defaultRequeueRejected(false)}: sem isto, uma mensagem que falha no listener volta
     * pra fila e é reentregue para sempre (o default do Spring AMQP é requeue) — uma mensagem
     * "envenenada" trava o consumidor em loop infinito. Com isto, a falha vai direto pra DLQ
     * (declarada por cada fila do BC consumidor), e o nack/retry manual é decisão de quem olhar
     * a DLQ depois — nunca automático e silencioso.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter
    ) {
        var factory = new SimpleRabbitListenerContainerFactory();

        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(withCorrelation(factory.getAdviceChain()));

        return factory;
    }

    /**
     * Acrescenta a correlação à cadeia que o Spring Boot já montou (retry, por exemplo), em vez
     * de trocá-la: {@code setAdviceChain} substitui o que havia.
     */
    private static Advice[] withCorrelation(Advice[] existing) {
        var chain = new ArrayList<Advice>(existing == null ? List.<Advice>of() : List.of(existing));

        chain.add(0, new CorrelationIdListenerAdvice());

        return chain.toArray(Advice[]::new);
    }
}
