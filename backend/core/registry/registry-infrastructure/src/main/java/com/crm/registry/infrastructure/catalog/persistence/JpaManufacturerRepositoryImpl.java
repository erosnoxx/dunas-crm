package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerCriteria;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import jakarta.persistence.EntityManager;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Repository de {@link Manufacturer} sobre JPA. Quem garante o nome único, sem diferenciar caixa,
 * é o índice do banco: dois cadastros simultâneos do mesmo nome terminam com um gravado e o outro
 * em {@code manufacturer_name_taken}, o mesmo código da checagem do use case.
 */
public class JpaManufacturerRepositoryImpl
        extends RepositoryImpl<Manufacturer, ManufacturerCriteria, ManufacturerEntity, ManufacturerJpaRepository>
        implements ManufacturerRepository {

    private static final String NAME_CONSTRAINT = "uk_manufacturers_name";

    public JpaManufacturerRepositoryImpl(
            ManufacturerJpaRepository jpaRepository,
            EntityManager em,
            ManufacturerMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                ManufacturerEntity.class
        );
    }

    @Override
    public boolean existsByName(ManufacturerName name, UUID exceptId) {
        return jpaRepository.existsByName(name.getValue(), exceptId);
    }

    @Override
    protected <T> T translatingConflicts(Supplier<T> operation) {
        try {
            return super.translatingConflicts(operation);
        }
        catch (ConflictException e) {
            if (UniqueConstraint.isViolated(e, NAME_CONSTRAINT))
                throw new ConflictException("manufacturer_name_taken", "já existe um fabricante com este nome", e.getCause());

            throw e;
        }
    }
}
