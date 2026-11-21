package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Cadastra um modelo de um fabricante e de um tipo. Administrador e Comercial (ADR-6).
 *
 * <p>Ordem dos erros: papel, fabricante e tipo existem, regras do domínio (fabricante e tipo ativos,
 * valores conferem com o tipo) e, por último, nome livre para o fabricante. A checagem de nome é só a
 * resposta amigável; quem garante é o UNIQUE do banco.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterEquipmentModel {

    private final EquipmentModelRepository repository;
    private final ManufacturerRepository manufacturerRepository;
    private final EquipmentTypeRepository typeRepository;

    /**
     * @param actor          quem cadastra
     * @param manufacturerId fabricante do modelo
     * @param typeId         tipo do modelo
     * @param name           nome do modelo
     * @param values         valores da característica; nulo vale lista vazia
     */
    public record Params(
            Actor actor,
            UUID manufacturerId,
            UUID typeId,
            String name,
            List<ValueInput> values
    ) {
    }

    public UUIDResponse execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var manufacturer = manufacturerRepository.findById(params.manufacturerId())
                .orElseThrow(() -> new NotFoundException("manufacturer_not_found", "fabricante não encontrado"));

        var type = typeRepository.findById(params.typeId())
                .orElseThrow(() -> new NotFoundException("type_not_found", "tipo não encontrado"));

        var name = ModelName.of(params.name());

        var model = EquipmentModel.register(manufacturer, type, name, ValueInputParser.parse(params.values()));

        if (repository.existsByManufacturerAndName(manufacturer.getId(), name, null))
            throw new ConflictException("model_name_taken", "este fabricante já tem um modelo com esse nome");

        repository.save(model);

        return new UUIDResponse(model.getId());
    }
}
