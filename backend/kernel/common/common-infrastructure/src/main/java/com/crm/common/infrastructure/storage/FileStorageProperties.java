package com.crm.common.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;

/**
 * Configuração do armazenamento de objetos compatível com S3 (architecture.md §12).
 *
 * <p>Só existe quando {@code crm.storage.endpoint} está definido: sem ele nenhum
 * {@code FileStorage} é criado. Desde a feature 02, {@code party} depende dele (logo e capa da
 * marca): sem o endpoint, a aplicação não sobe. Em Docker, a variável de ambiente é
 * {@code CRM_STORAGE_ENDPOINT}.
 *
 * @param endpoint       onde o <b>servidor</b> fala com o armazenamento (em Docker, o nome do
 *                       serviço, como {@code http://seaweedfs:8333})
 * @param publicEndpoint onde o <b>navegador</b> fala com ele: é o host que entra na URL
 *                       pré-assinada e na URL pública. Em dev difere de {@code endpoint}, porque
 *                       o navegador não resolve o nome do serviço do Docker. Vazio: usa
 *                       {@code endpoint}
 * @param publicBucket   bucket de leitura pública (capa, logo, galeria)
 * @param createBucketOnStartup cria o bucket ao subir, se não existir. Para dev e teste
 */
@ConfigurationProperties("crm.storage")
public record FileStorageProperties(
        URI endpoint,
        URI publicEndpoint,
        @DefaultValue("us-east-1") String region,
        @DefaultValue("crm-public") String publicBucket,
        String accessKey,
        String secretKey,
        @DefaultValue("false") boolean createBucketOnStartup
) {

    public URI effectivePublicEndpoint() {
        return publicEndpoint != null ? publicEndpoint : endpoint;
    }
}
