package com.crm.registry.infrastructure.integrations.brasilapi;

import com.crm.registry.application.customers.contracts.services.ZipCodeLookupService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Mora dentro da integração porque conhece o {@link RestClient}: fora dela, o ArchUnit recusa. */
@Configuration
@EnableConfigurationProperties(BrasilApiProperties.class)
public class BrasilApiConfig {

    @Bean
    public ZipCodeLookupService zipCodeLookupService(BrasilApiProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());

        var http = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();

        return new BrasilApiZipCodeLookupService(new BrasilApiClient(http), new BrasilApiTranslator(), properties);
    }
}
