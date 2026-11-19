package com.crm.common.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ajustes do cache.
 *
 * <pre>
 * crm.cache.key-prefix=crm:
 * crm.cache.default-ttl=5m
 * crm.cache.scan-batch-size=500
 * </pre>
 *
 * @param keyPrefix      prefixo aplicado a toda chave, para isolar o namespace da aplicação
 *                       dentro de uma instância de Redis compartilhada
 * @param defaultTtl     TTL usado quando o chamador passa um TTL nulo ou não positivo. Nunca
 *                       existe entrada sem expiração: TTL é a rede de segurança para quando
 *                       alguém esquecer de invalidar por evento
 * @param scanBatchSize  tamanho do lote do SCAN usado na invalidação por prefixo
 */
@ConfigurationProperties(prefix = "crm.cache")
public record CacheProperties(
        @DefaultValue("crm:") String keyPrefix,
        @DefaultValue("5m") Duration defaultTtl,
        @DefaultValue("500") int scanBatchSize
) {
}
