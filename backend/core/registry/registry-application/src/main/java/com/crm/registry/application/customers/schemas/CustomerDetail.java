package com.crm.registry.application.customers.schemas;

import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.identity.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detalhe do cliente com localizações e equipamentos instalados.
 *
 * @param locations todas as localizações, ativas ou não
 */
public record CustomerDetail(
        UUID id,
        String document,
        Document.DocumentType documentType,
        String name,
        String tradeName,
        String email,
        String phone,
        CustomerStatus status,
        List<LocationView> locations
) {

    /** Localização do cliente com seus equipamentos. */
    public record LocationView(
            UUID id,
            String name,
            CustomerStatus status,
            AddressView address,
            List<EquipmentView> equipment
    ) {
    }

    /** Endereço de uma localização. */
    public record AddressView(
            String zipCode,
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            UF state
    ) {
    }

    /**
     * Equipamento instalado.
     *
     * @param characteristic valor da característica do modelo; nulo quando o modelo não tem valor
     */
    public record EquipmentView(
            UUID id,
            String serialNumber,
            LocalDate installedOn,
            CustomerStatus status,
            ModelRef model,
            Ref manufacturer,
            Ref type,
            CharacteristicView characteristic
    ) {
    }

    /** Modelo do equipamento, com o status que ele tem no catálogo. */
    public record ModelRef(
            UUID id,
            String name,
            CatalogStatus status
    ) {
    }

    /** Referência por id e nome (fabricante, tipo). */
    public record Ref(
            UUID id,
            String name
    ) {
    }

    /** Característica do tipo com o valor do modelo. */
    public record CharacteristicView(
            String name,
            MeasurementUnit unit,
            BigDecimal amount
    ) {
    }
}
