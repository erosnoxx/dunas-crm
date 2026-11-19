package com.crm.common.infrastructure.storage;

import com.crm.common.application.storage.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;

/**
 * Liga o {@link FileStorage}. Só quando {@code crm.storage.endpoint} está definido; desde a
 * feature 02, {@code party} depende dele (as imagens da marca), então sem o endpoint a aplicação
 * não sobe.
 *
 * <p>O cliente usa o cliente HTTP do JDK ({@code url-connection-client}) e não o Apache nem o
 * Netty que o SDK traz por padrão: uma dependência a menos para uma API que roda numa VPS de
 * 8 GB dividida com o banco e o broker.
 *
 * <p>Path-style é obrigatório: o armazenamento autohospedado não resolve
 * {@code bucket.host} por DNS.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
@ConditionalOnProperty(prefix = "crm.storage", name = "endpoint")
public class StorageConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(FileStorageProperties properties) {
        var client = S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials(properties))
                .forcePathStyle(true)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();

        if (properties.createBucketOnStartup())
            createBucketIfAbsent(client, properties.publicBucket());

        return client;
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(FileStorageProperties properties) {
        // o host que entra na URL assinada é o que o NAVEGADOR alcança, não o do Docker.
        return S3Presigner.builder()
                .endpointOverride(properties.effectivePublicEndpoint())
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    public FileStorage fileStorage(
            S3Client client,
            S3Presigner presigner,
            FileStorageProperties properties,
            Clock clock
    ) {
        var publicBaseUrl = trimTrailingSlash(properties.effectivePublicEndpoint().toString())
                + "/" + properties.publicBucket();

        return new S3FileStorageImpl(client, presigner, properties.publicBucket(), publicBaseUrl, clock);
    }

    private static StaticCredentialsProvider credentials(FileStorageProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
        );
    }

    private static void createBucketIfAbsent(S3Client client, String bucket) {
        try {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());

            log.info("bucket de arquivos {} criado", bucket);
        }

        catch (BucketAlreadyOwnedByYouException e) {
            log.debug("bucket de arquivos {} já existe", bucket);
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
