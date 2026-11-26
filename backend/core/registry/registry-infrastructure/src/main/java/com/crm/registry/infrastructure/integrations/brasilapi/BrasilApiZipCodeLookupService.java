package com.crm.registry.infrastructure.integrations.brasilapi;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.registry.application.customers.contracts.services.ZipCodeLookupService;
import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.shared.domain.vos.address.ZipCode;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.Optional;

/**
 * Consulta de CEP pela BrasilAPI. Repete só falha transitória (rede, 5xx, 429); resposta sem
 * cidade ou UF é tratada como CEP desconhecido, porque o formulário trava esses dois campos.
 */
class BrasilApiZipCodeLookupService implements ZipCodeLookupService {

    private final BrasilApiClient client;
    private final BrasilApiTranslator translator;
    private final Retry retry;

    BrasilApiZipCodeLookupService(
            BrasilApiClient client,
            BrasilApiTranslator translator,
            BrasilApiProperties properties
    ) {
        this.client = client;
        this.translator = translator;
        this.retry = Retry.of("brasilapi-cep", RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(200), 2))
                .retryOnException(e -> e instanceof ResourceAccessException
                        || e instanceof BrasilApiClient.TransientFailure)
                .build());
    }

    @Override
    public Optional<ZipCodeAddress> find(ZipCode zipCode) {
        try {
            return Retry.decorateSupplier(retry, () -> client.find(zipCode.getValue())).get()
                    .map(translator::toAddress)
                    .filter(address -> address.city() != null && address.state() != null);
        }

        catch (ResourceAccessException | BrasilApiClient.TransientFailure e) {
            throw new ExternalServiceException("falha ao consultar o CEP", e);
        }
    }
}
