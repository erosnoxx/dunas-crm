package com.crm.registry.infrastructure.integrations.brasilapi;

import com.crm.common.application.exceptions.ExternalServiceException;
import com.crm.shared.domain.vos.address.ZipCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("BrasilApiZipCodeLookupService")
class BrasilApiZipCodeLookupServiceTest {

    private static final String URL = "https://brasilapi.test/api/cep/v2/89010025";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://brasilapi.test");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final BrasilApiZipCodeLookupService service = new BrasilApiZipCodeLookupService(
            new BrasilApiClient(builder.build()),
            new BrasilApiTranslator(),
            new BrasilApiProperties("https://brasilapi.test", Duration.ofSeconds(1), 2));

    @Test
    @DisplayName("devolve o endereço traduzido")
    void returnsTheAddress() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"cep\":\"89010025\",\"state\":\"SC\",\"city\":\"Blumenau\",\"neighborhood\":\"Centro\",\"street\":\"Rua X\"}",
                MediaType.APPLICATION_JSON));

        var address = service.find(ZipCode.of("89010-025")).orElseThrow();

        assertThat(address.city()).isEqualTo("Blumenau");
        assertThat(address.state()).isEqualTo("SC");
    }

    @Test
    @DisplayName("404 é CEP desconhecido, sem repetir")
    void notFoundIsEmpty() {
        server.expect(times(1), requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(service.find(ZipCode.of("89010025"))).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("resposta sem cidade é CEP desconhecido")
    void withoutCityIsEmpty() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"cep\":\"89010025\",\"state\":\"SC\"}", MediaType.APPLICATION_JSON));

        assertThat(service.find(ZipCode.of("89010025"))).isEmpty();
    }

    @Test
    @DisplayName("5xx repete uma vez e depois vira ExternalServiceException")
    void serverErrorIsRetriedThenExternalFailure() {
        server.expect(times(2), requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.find(ZipCode.of("89010025"))).isInstanceOf(ExternalServiceException.class);
        server.verify();
    }

    @Test
    @DisplayName("5xx seguido de sucesso devolve o endereço")
    void recoversOnTheSecondAttempt() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"cep\":\"89010025\",\"state\":\"SC\",\"city\":\"Blumenau\"}", MediaType.APPLICATION_JSON));

        assertThat(service.find(ZipCode.of("89010025"))).isPresent();
    }
}
