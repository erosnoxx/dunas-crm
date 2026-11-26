package com.crm.registry.infrastructure.integrations.brasilapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ajustes da consulta de CEP.
 *
 * <pre>
 * crm.brasilapi.base-url=https://brasilapi.com.br
 * crm.brasilapi.timeout=3s
 * crm.brasilapi.max-attempts=2
 * </pre>
 *
 * @param baseUrl     raiz da BrasilAPI
 * @param timeout     limite de conexão e de leitura de cada chamada
 * @param maxAttempts tentativas no total, contando a primeira; só falha transitória repete
 */
@ConfigurationProperties(prefix = "crm.brasilapi")
public record BrasilApiProperties(
        @DefaultValue("https://brasilapi.com.br") String baseUrl,
        @DefaultValue("3s") Duration timeout,
        @DefaultValue("2") int maxAttempts
) {
}
