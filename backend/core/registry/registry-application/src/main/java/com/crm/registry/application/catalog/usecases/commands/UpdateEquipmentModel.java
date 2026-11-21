package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.catalog.CatalogAccess;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Edita nome e valores de um modelo; fabricante e tipo não mudam (ADR-4). Administrador e Comercial (ADR-6).
 *
 * <p>O domínio recusa o modelo desativado e confere os valores antes da checagem de nome: a API do
 * domínio reúne essas checagens, então a ordem é imposta por ela (o 400 do parse e as regras do domínio
 * vêm antes de {@code model_name_taken}); nada foi salvo até lá. O tipo pode estar desativado.
 */
@UseCase
@RequiredArgsConstructor
public class UpdateEquipmentModel {

    private final EquipmentModelRepository repository;
    private final EquipmentTypeRepository typeRepository;

    /**
     * @param actor   quem edita
     * @param modelId modelo editado
     * @param name    novo nome
     * @param values  novos valores; nulo vale lista vazia
     */
    public record Params(
            Actor actor,
            UUID modelId,
            String name,
            List<ValueInput> values
    ) {
    }

    public void execute(Params params) {
        CatalogAccess.requireEditor(params.actor());

        var model = repository.findById(params.modelId())
                .orElseThrow(() -> new NotFoundException("model_not_found", "modelo não encontrado"));

        var type = typeRepository.findById(model.getTypeId())
                .orElseThrow(() -> new NotFoundException("type_not_found", "tipo não encontrado"));

        var name = ModelName.of(params.name());

        model.edit(type, name, ValueInputParser.parse(params.values()));

        // o próprio modelo fica fora da checagem: manter o nome (ou trocar a caixa) não conflita
        if (repository.existsByManufacturerAndName(model.getManufacturerId(), name, model.getId()))
            throw new ConflictException("model_name_taken", "este fabricante já tem um modelo com esse nome");

        repository.save(model);
    }
}
