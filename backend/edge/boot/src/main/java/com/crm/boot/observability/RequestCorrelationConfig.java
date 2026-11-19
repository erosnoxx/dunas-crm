package com.crm.boot.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registra o filtro de correlação antes de qualquer outro, inclusive o do Spring Security
 * (ordem -100): o log de uma requisição recusada na autenticação também precisa do id.
 */
@Configuration
public class RequestCorrelationConfig {

    @Bean
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter() {
        var registration = new FilterRegistrationBean<>(new RequestCorrelationFilter());

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }
}
