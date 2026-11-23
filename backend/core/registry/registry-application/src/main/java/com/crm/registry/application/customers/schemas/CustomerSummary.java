package com.crm.registry.application.customers.schemas;

import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.identity.Document;

import java.util.List;
import java.util.UUID;

/**
 * Linha da lista de clientes. Sem e-mail e telefone: só o detalhe os traz (ADR-29).
 *
 * @param locationCount  localizações ativas do cliente
 * @param equipmentCount equipamentos ativos nas localizações ativas do cliente
 * @param cities         cidades das localizações ativas
 * @param matchedSerials seriais que casaram com o texto da busca
 */
public record CustomerSummary(
        UUID id,
        String name,
        String tradeName,
        String document,
        Document.DocumentType documentType,
        CustomerStatus status,
        long locationCount,
        long equipmentCount,
        List<CityView> cities,
        List<MatchedSerial> matchedSerials
) {

    /** Cidade de uma localização do cliente. */
    public record CityView(
            String city,
            UF state
    ) {
    }

    /** Equipamento cujo serial casou com a busca. */
    public record MatchedSerial(
            String serialNumber,
            String modelName
    ) {
    }
}
