package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.TypeName;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Edita nome e característica do tipo numa operação só. Administrador e Comercial (ADR-6).
 *
 * <p>Com modelos, o domínio recusa trocar a unidade ou ligar a obrigatoriedade
 * ({@code characteristic_locked}); como o aggregate só é salvo no fim, nada é gravado nesse caso.
 */
@UseCase
@RequiredArgsConstructor
public class UpdateEquipmentType {

    private final EquipmentTypeRepository repository;
    private final EquipmentModelReader modelReader;

    /**
     * @param actor          quem edita
     * @param typeId         tipo editado
     * @param name           novo nome
     * @param characteristic nova definição da característica
     */
    public record Params(
            Actor actor,
            UUID typeId,
            String name,
            CharacteristicInput characteristic
    ) {
    }

    public void execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var type = repository.findById(params.typeId())
                .orElseThrow(() -> new NotFoundException("type_not_found", "tipo não encontrado"));

        var name = TypeName.of(params.name());
        var characteristic = params.characteristic();
        var characteristicName = CharacteristicName.of(characteristic.name());
        var unit = MeasurementUnitParser.parse(characteristic.unit());

        // o domínio recusa o desativado antes do conflito de nome (ordem do usecases.md); nada foi salvo ainda
        type.rename(name);

        // o próprio tipo fica fora da checagem: renomear para o mesmo nome (ou trocar a caixa) não conflita
        if (repository.existsByName(name, type.getId()))
            throw new ConflictException("type_name_taken", "já existe um tipo com este nome");

        type.changeCharacteristic(
                characteristicName,
                unit,
                characteristic.required(),
                modelReader.hasModelsOfType(type.getId())
        );

        repository.save(type);
    }
}
