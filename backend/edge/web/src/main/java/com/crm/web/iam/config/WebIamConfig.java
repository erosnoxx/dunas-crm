package com.crm.web.iam.config;

import com.crm.web.iam.resolvers.ActorArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Registra o resolvedor de {@link com.crm.shared.application.Actor} em todos os controllers (IAM e catálogo). */
@Configuration
public class WebIamConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ActorArgumentResolver());
    }
}
