package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.TypeName;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import lombok.RequiredArgsConstructor;

/**
 * Cadastra um tipo de equipamento com a sua característica. Administrador e Comercial (ADR-6).
 *
 * <p>A checagem de nome repetido é só a resposta amigável; quem garante é o UNIQUE do banco.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterEquipmentType {

    private final EquipmentTypeRepository repository;

    /**
     * @param actor          quem cadastra
     * @param name           nome do tipo
     * @param characteristic característica que os modelos do tipo trazem
     */
    public record Params(
            Actor actor,
            String name,
            CharacteristicInput characteristic
    ) {
    }

    public UUIDResponse execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var name = TypeName.of(params.name());

        if (repository.existsByName(name, null))
            throw new ConflictException("type_name_taken", "já existe um tipo com este nome");

        var characteristic = params.characteristic();

        var type = EquipmentType.register(
                name,
                CharacteristicName.of(characteristic.name()),
                MeasurementUnitParser.parse(characteristic.unit()),
                characteristic.required()
        );

        repository.save(type);

        return new UUIDResponse(type.getId());
    }
}
