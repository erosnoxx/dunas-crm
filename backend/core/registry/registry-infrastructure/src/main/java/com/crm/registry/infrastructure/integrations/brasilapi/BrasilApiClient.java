package com.crm.registry.infrastructure.integrations.brasilapi;

import com.crm.registry.infrastructure.integrations.brasilapi.dtos.BrasilApiCepResponse;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/** Fala com a BrasilAPI; só conhece o modelo dela. 404 e 400 são "CEP não existe"; o resto de erro sobe. */
class BrasilApiClient {

    private final RestClient http;

    BrasilApiClient(RestClient http) {
        this.http = http;
    }

    Optional<BrasilApiCepResponse> find(String digits) {
        return http.get()
                .uri("/api/cep/v2/{cep}", digits)
                .exchange((request, response) -> {
                    var status = response.getStatusCode().value();

                    if (status == 404 || status == 400)
                        return Optional.<BrasilApiCepResponse>empty();

                    if (response.getStatusCode().isError())
                        throw new TransientFailure("BrasilAPI respondeu " + status);

                    return Optional.ofNullable(response.bodyTo(BrasilApiCepResponse.class));
                });
    }

    /** Falha que vale repetir: 5xx e 429. */
    static class TransientFailure extends RuntimeException {
        TransientFailure(String message) {
            super(message);
        }
    }
}
