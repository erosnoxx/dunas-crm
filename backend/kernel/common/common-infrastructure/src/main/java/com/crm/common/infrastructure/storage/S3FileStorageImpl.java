package com.crm.common.infrastructure.storage;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.application.storage.FileStorage;
import com.crm.common.application.storage.StoredObject;
import com.crm.common.application.storage.UploadTicket;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * {@link FileStorage} sobre qualquer armazenamento compatível com S3 (SeaweedFS hoje), pelo
 * AWS SDK v2. Trocar de serviço (outro autohospedado, ou o S3 do provedor) é só configuração.
 *
 * <p>Falha de transporte vira {@link ExternalServiceException}, o mesmo contrato dos outros
 * adaptadores de infraestrutura nossa que podem ficar fora do ar.
 */
@RequiredArgsConstructor
class S3FileStorageImpl implements FileStorage {

    // montagem nossa, nunca entrada de usuário: valor ruim aqui é bug (ver FileStorage).
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_.\\-]+(/[A-Za-z0-9_.\\-]+)*");
    private static final int MAX_KEY_LENGTH = 200;

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;
    private final Clock clock;

    @Override
    public UploadTicket prepareUpload(String key, String contentType, Duration validFor) {
        requireValidKey(key);

        if (contentType == null || contentType.isBlank())
            throw new InternalServerErrorException("contentType é obrigatório");

        var put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        var presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(validFor)
                        .putObjectRequest(put)
                        .build()
        );

        return new UploadTicket(
                presigned.url().toString(),
                contentType,
                clock.instant().plus(validFor)
        );
    }

    @Override
    public Optional<StoredObject> find(String key) {
        requireValidKey(key);

        try {
            var head = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());

            return Optional.of(new StoredObject(head.contentLength(), head.contentType()));
        }

        catch (NoSuchKeyException e) {
            return Optional.empty();
        }

        catch (SdkException e) {
            throw unavailable(e);
        }
    }

    @Override
    public void delete(String key) {
        requireValidKey(key);

        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        }

        catch (SdkException e) {
            throw unavailable(e);
        }
    }

    @Override
    public String publicUrl(String key) {
        requireValidKey(key);

        return publicBaseUrl + "/" + key;
    }

    private static void requireValidKey(String key) {
        if (key == null || key.length() > MAX_KEY_LENGTH || !VALID_KEY.matcher(key).matches() || key.contains(".."))
            throw new InternalServerErrorException("chave de arquivo inválida");
    }

    private static ExternalServiceException unavailable(SdkException cause) {
        return new ExternalServiceException("armazenamento de arquivos indisponível", cause);
    }
}
