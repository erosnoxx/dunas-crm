package com.crm;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/** Os dublês dos testes ponta a ponta: relógio que o teste adianta e e-mail capturado. */
@TestConfiguration
class EndToEndTestBeans {

    static final Instant START = Instant.parse("2026-11-20T12:00:00Z");

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(START);
    }

    @Bean
    @Primary
    CapturingEmailSender capturingEmailSender() {
        return new CapturingEmailSender();
    }
}
