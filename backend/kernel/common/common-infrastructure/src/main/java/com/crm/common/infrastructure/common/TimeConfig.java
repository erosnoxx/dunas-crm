package com.crm.common.infrastructure.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Fonte única de hora do sistema. Todo código que precisa de "agora" recebe este {@link Clock}
 * injetado (ou um {@code Instant now} já derivado dele) — nunca chama {@code Instant.now()}
 * direto. O {@code ClockRulesTest} em {@code boot} quebra o build se alguém chamar.
 *
 * <p>{@code America/Sao_Paulo}, não um deslocamento fixo {@code -03:00}: o nome do fuso
 * acompanha qualquer mudança de horário de verão sem ninguém precisar lembrar de trocar o valor.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
