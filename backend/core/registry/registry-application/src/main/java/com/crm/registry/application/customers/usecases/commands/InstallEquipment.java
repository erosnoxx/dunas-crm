package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.application.customers.contracts.repositories.LocationRepository;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.SerialNumber;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Instala um equipamento em um local. Administrador e Comercial (ADR-7).
 *
 * <p>Ordem: papel, formato (série e data, antes de ler qualquer repository), local, modelo e cliente
 * existentes, regras de ativo do aggregate e, por fim, unicidade da série dentro do modelo.
 */
@UseCase
@RequiredArgsConstructor
public class InstallEquipment {

    private final InstalledEquipmentRepository repository;
    private final LocationRepository locations;
    private final CustomerRepository customers;
    private final EquipmentModelRepository models;
    private final Clock clock;

    /**
     * @param actor        quem instala
     * @param locationId   local do equipamento
     * @param modelId      modelo do catálogo
     * @param serialNumber número de série
     * @param installedOn  data de instalação, opcional e não posterior a hoje
     */
    public record Params(
            Actor actor,
            UUID locationId,
            UUID modelId,
            String serialNumber,
            LocalDate installedOn
    ) {
    }

    public UUIDResponse execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var serialNumber = SerialNumber.of(params.serialNumber());
        var installedOn = params.installedOn() == null ? null : InstallationDate.of(params.installedOn(), clock);

        var location = locations.findById(params.locationId())
                .orElseThrow(() -> new NotFoundException("location_not_found", "local não encontrado"));
        var model = models.findById(params.modelId())
                .orElseThrow(() -> new NotFoundException("model_not_found", "modelo não encontrado"));
        var customer = customers.findById(location.getCustomerId())
                .orElseThrow(() -> new NotFoundException("customer_not_found", "cliente não encontrado"));

        var equipment = InstalledEquipment.install(
                location,
                customer,
                model,
                serialNumber,
                installedOn
        );

        if (repository.existsByModelAndSerialNumber(model.getId(), serialNumber, null))
            throw new ConflictException("serial_number_taken", "este modelo já tem um equipamento com esse número de série");

        repository.save(equipment);

        return new UUIDResponse(equipment.getId());
    }
}
