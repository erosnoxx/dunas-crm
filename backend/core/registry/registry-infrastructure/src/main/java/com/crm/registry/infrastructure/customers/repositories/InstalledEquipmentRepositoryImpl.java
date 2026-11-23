package com.crm.registry.infrastructure.customers.repositories;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.registry.application.customers.contracts.repositories.InstalledEquipmentRepository;
import com.crm.registry.application.customers.criteria.InstalledEquipmentCriteria;
import com.crm.registry.domain.customers.entities.InstalledEquipment;
import com.crm.registry.domain.customers.vos.SerialNumber;
import com.crm.registry.infrastructure.catalog.repositories.UniqueConstraint;
import com.crm.registry.infrastructure.customers.mappers.InstalledEquipmentMapper;
import com.crm.registry.infrastructure.customers.persistence.entities.InstalledEquipmentEntity;
import com.crm.registry.infrastructure.customers.persistence.jpa.InstalledEquipmentJpaRepository;
import jakarta.persistence.EntityManager;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Repository de {@link InstalledEquipment} sobre JPA. Quem garante a série única por modelo, sem
 * diferenciar caixa, é o índice do banco: duas instalações simultâneas da mesma série terminam com
 * uma gravada e a outra em {@code serial_number_taken}, o mesmo código da checagem do use case.
 */
public class InstalledEquipmentRepositoryImpl
        extends RepositoryImpl<InstalledEquipment, InstalledEquipmentCriteria, InstalledEquipmentEntity, InstalledEquipmentJpaRepository>
        implements InstalledEquipmentRepository {

    private static final String SERIAL_CONSTRAINT = "uk_installed_equipment_model_serial";

    public InstalledEquipmentRepositoryImpl(
            InstalledEquipmentJpaRepository jpaRepository,
            EntityManager em,
            InstalledEquipmentMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                InstalledEquipmentEntity.class
        );
    }

    @Override
    public boolean existsByModelAndSerialNumber(UUID modelId, SerialNumber serialNumber, UUID exceptId) {
        return jpaRepository.existsByModelAndSerialNumber(modelId, serialNumber.getValue(), exceptId);
    }

    @Override
    protected <T> T translatingConflicts(Supplier<T> operation) {
        try {
            return super.translatingConflicts(operation);
        }
        catch (ConflictException e) {
            if (UniqueConstraint.isViolated(e, SERIAL_CONSTRAINT))
                throw new ConflictException("serial_number_taken", "este modelo já tem um equipamento instalado com esse número de série", e.getCause());

            throw e;
        }
    }
}
