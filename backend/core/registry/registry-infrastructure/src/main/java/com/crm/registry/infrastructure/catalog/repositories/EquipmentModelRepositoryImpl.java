package com.crm.registry.infrastructure.catalog.repositories;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.registry.application.catalog.criteria.EquipmentModelCriteria;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.vos.ModelName;
import com.crm.registry.infrastructure.catalog.mappers.EquipmentModelMapper;
import com.crm.registry.infrastructure.catalog.persistence.jpa.EquipmentModelJpaRepository;
import com.crm.registry.infrastructure.catalog.persistence.entities.EquipmentModelEntity;
import jakarta.persistence.EntityManager;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Repository de {@link EquipmentModel} sobre JPA, com os valores. Quem garante o nome único por
 * fabricante, sem diferenciar caixa, é o índice do banco: a violação sai como
 * {@code model_name_taken}, o mesmo código da checagem do use case.
 */
public class EquipmentModelRepositoryImpl
        extends RepositoryImpl<EquipmentModel, EquipmentModelCriteria, EquipmentModelEntity, EquipmentModelJpaRepository>
        implements EquipmentModelRepository {

    private static final String NAME_CONSTRAINT = "uk_equipment_models_manufacturer_name";

    public EquipmentModelRepositoryImpl(
            EquipmentModelJpaRepository jpaRepository,
            EntityManager em,
            EquipmentModelMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                EquipmentModelEntity.class
        );
    }

    @Override
    public boolean existsByManufacturerAndName(UUID manufacturerId, ModelName name, UUID exceptId) {
        return jpaRepository.existsByManufacturerAndName(
                manufacturerId,
                name.getValue(),
                exceptId
        );
    }

    @Override
    protected <T> T translatingConflicts(Supplier<T> operation) {
        try {
            return super.translatingConflicts(operation);
        }
        catch (ConflictException e) {
            if (UniqueConstraint.isViolated(e, NAME_CONSTRAINT))
                throw new ConflictException("model_name_taken", "este fabricante já tem um modelo com esse nome", e.getCause());

            throw e;
        }
    }
}
