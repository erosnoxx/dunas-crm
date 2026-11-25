package com.crm.registry.infrastructure.integrations.brasilapi;

import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.registry.infrastructure.integrations.brasilapi.dtos.BrasilApiCepResponse;

/** A única classe que conhece o modelo da BrasilAPI e o nosso. */
class BrasilApiTranslator {

    ZipCodeAddress toAddress(BrasilApiCepResponse response) {
        return new ZipCodeAddress(
                response.cep() == null ? null : response.cep().replaceAll("\\D", ""),
                blankToNull(response.street()),
                blankToNull(response.neighborhood()),
                blankToNull(response.city()),
                response.state() == null ? null : blankToNull(response.state().toUpperCase())
        );
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
