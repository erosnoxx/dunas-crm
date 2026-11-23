package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/** Desativa um equipamento instalado. Administrador e Comercial (ADR-7). Local, cliente e modelo não são lidos (ADR-30). */
@UseCase
@RequiredArgsConstructor
public class DeactivateInstalledEquipment {

    private final InstalledEquipmentRepository repository;

    /**
     * @param actor       quem executa
     * @param equipmentId equipamento alvo
     */
    public record Params(
            Actor actor,
            UUID equipmentId
    ) {
    }

    public void execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        var equipment = repository.findById(params.equipmentId())
                .orElseThrow(() -> new NotFoundException("installed_equipment_not_found", "equipamento não encontrado"));

        equipment.deactivate();

        repository.save(equipment);
    }
}
