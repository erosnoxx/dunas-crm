package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.SerialNumber;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Edita número de série e data de instalação. Administrador e Comercial (ADR-7). Local, cliente e
 * modelo não são lidos: o equipamento de um local desativado continua editável (ADR-30).
 */
@UseCase
@RequiredArgsConstructor
public class UpdateInstalledEquipment {

    private final InstalledEquipmentRepository repository;
    private final Clock clock;

    /**
     * @param actor        quem edita
     * @param equipmentId  equipamento editado
     * @param serialNumber novo número de série
     * @param installedOn  nova data de instalação, opcional e não posterior a hoje
     */
    public record Params(
            Actor actor,
            UUID equipmentId,
            String serialNumber,
            LocalDate installedOn
    ) {
    }

    public void execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var serialNumber = SerialNumber.of(params.serialNumber());
        var installedOn = params.installedOn() == null ? null : InstallationDate.of(params.installedOn(), clock);

        var equipment = repository.findById(params.equipmentId())
                .orElseThrow(() -> new NotFoundException("installed_equipment_not_found", "equipamento não encontrado"));

        equipment.edit(serialNumber, installedOn);

        if (repository.existsByModelAndSerialNumber(equipment.getModelId(), serialNumber, equipment.getId()))
            throw new ConflictException("serial_number_taken", "este modelo já tem um equipamento com esse número de série");

        repository.save(equipment);
    }
}
