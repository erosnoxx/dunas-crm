package com.crm.common.infrastructure.storage;

import com.crm.common.application.exceptions.InternalServerErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A assinatura da URL é local (não faz chamada de rede), então dá para conferi-la sem
 * armazenamento no ar. O que fala com o servidor (find, delete) depende de um armazenamento de
 * verdade e fica para o teste de integração.
 */
@DisplayName("S3FileStorageImpl")
class S3FileStorageImplTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final URI PUBLIC = URI.create("http://localhost:8333");

    private final S3FileStorageImpl storage = newStorage();

    private static S3FileStorageImpl newStorage() {
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("dev-key", "dev-secret"));

        var presigner = S3Presigner.builder()
                .endpointOverride(PUBLIC)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        // o cliente nunca é chamado nestes testes
        var client = S3Client.builder()
                .endpointOverride(PUBLIC)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .forcePathStyle(true)
                .build();

        return new S3FileStorageImpl(
                client, presigner, "crm-public", "http://localhost:8333/crm-public",
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("prepareUpload devolve uma URL assinada, em path-style, no host que o navegador alcança")
    void presignsAPathStyleUrlOnThePublicHost() {
        var ticket = storage.prepareUpload("events/abc/cover.png", "image/png", Duration.ofMinutes(10));

        assertThat(ticket.url()).startsWith("http://localhost:8333/crm-public/events/abc/cover.png?");
        assertThat(ticket.url()).contains("X-Amz-Signature=").contains("X-Amz-Expires=600");
        assertThat(ticket.contentType()).isEqualTo("image/png");
        assertThat(ticket.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("publicUrl monta o endereço a partir da base pública e da chave")
    void buildsThePublicUrl() {
        assertThat(storage.publicUrl("brands/x/logo.svg"))
                .isEqualTo("http://localhost:8333/crm-public/brands/x/logo.svg");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/abs", "a//b", "../etc/passwd", "a/../b", "a b", "a?x=1", "a\nb"})
    @DisplayName("recusa chave com traversal, barra solta, espaço ou quebra de linha")
    void rejectsUnsafeKeys(String key) {
        assertThatThrownBy(() -> storage.publicUrl(key)).isInstanceOf(InternalServerErrorException.class);
        assertThatThrownBy(() -> storage.prepareUpload(key, "image/png", Duration.ofMinutes(1)))
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    @DisplayName("recusa chave nula, chave longa demais e contentType vazio")
    void rejectsNullLongAndBlank() {
        assertThatThrownBy(() -> storage.publicUrl(null)).isInstanceOf(InternalServerErrorException.class);
        assertThatThrownBy(() -> storage.publicUrl("a".repeat(201))).isInstanceOf(InternalServerErrorException.class);
        assertThatThrownBy(() -> storage.prepareUpload("a/b.png", " ", Duration.ofMinutes(1)))
                .isInstanceOf(InternalServerErrorException.class);
    }
}
