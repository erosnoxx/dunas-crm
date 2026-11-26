package com.crm.registry.infrastructure.integrations.brasilapi;

import com.crm.registry.infrastructure.integrations.brasilapi.dtos.BrasilApiCepResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BrasilApiTranslator")
class BrasilApiTranslatorTest {

    /** Resposta real de {@code GET /api/cep/v2/89010025} (documentação da BrasilAPI). */
    private static final String PAYLOAD = """
            {"cep":"89010025","state":"SC","city":"Blumenau","neighborhood":"Centro",
             "street":"Rua Doutor Luiz de Freitas Melo","service":"viacep",
             "location":{"type":"Point","coordinates":{"longitude":"-49.0629788","latitude":"-26.9244749"}}}
            """;

    private final BrasilApiTranslator translator = new BrasilApiTranslator();

    @Test
    @DisplayName("traduz o payload real, ignorando o que não usamos")
    void translatesTheRealPayload() throws Exception {
        var response = new ObjectMapper().readValue(PAYLOAD, BrasilApiCepResponse.class);

        var address = translator.toAddress(response);

        assertThat(address.zipCode()).isEqualTo("89010025");
        assertThat(address.street()).isEqualTo("Rua Doutor Luiz de Freitas Melo");
        assertThat(address.neighborhood()).isEqualTo("Centro");
        assertThat(address.city()).isEqualTo("Blumenau");
        assertThat(address.state()).isEqualTo("SC");
    }

    @Test
    @DisplayName("CEP geral da cidade vem sem logradouro nem bairro: ficam nulos")
    void blankStreetAndNeighborhoodBecomeNull() {
        var address = translator.toAddress(new BrasilApiCepResponse("69900-970", "ac", "Rio Branco", " ", ""));

        assertThat(address.zipCode()).isEqualTo("69900970");
        assertThat(address.street()).isNull();
        assertThat(address.neighborhood()).isNull();
        assertThat(address.state()).isEqualTo("AC");
    }
}
