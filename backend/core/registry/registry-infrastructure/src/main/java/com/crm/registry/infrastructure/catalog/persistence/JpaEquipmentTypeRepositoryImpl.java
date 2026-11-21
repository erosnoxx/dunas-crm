package com.crm.registry.infrastructure.catalog.persistence;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeCriteria;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentTypeRepository;
import com.crm.registry.domain.catalog.entities.EquipmentType;
import com.crm.registry.domain.catalog.vos.TypeName;
import jakarta.persistence.EntityManager;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Repository de {@link EquipmentType} sobre JPA, com a definição da característica. Quem garante o
 * nome único, sem diferenciar caixa, é o índice do banco: a violação sai como
 * {@code type_name_taken}, o mesmo código da checagem do use case.
 *
 * <p>A definição tem versão na tabela, mas não no domínio. Antes de gravar, ela recebe a versão
 * já carregada: nula, o Hibernate a tomaria por linha nova e tentaria inseri-la de novo. Dentro do
 * use case é a versão lida junto com o tipo, então a edição concorrente da mesma definição ainda
 * cai no lock otimista.
 */
public class JpaEquipmentTypeRepositoryImpl
        extends RepositoryImpl<EquipmentType, EquipmentTypeCriteria, EquipmentTypeEntity, EquipmentTypeJpaRepository>
        implements EquipmentTypeRepository {

    private static final String NAME_CONSTRAINT = "uk_equipment_types_name";

    public JpaEquipmentTypeRepositoryImpl(
            EquipmentTypeJpaRepository jpaRepository,
            EntityManager em,
            EquipmentTypeMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                EquipmentTypeEntity.class
        );
    }

    @Override
    public EquipmentType save(EquipmentType type) {
        var entity = mapper.toPersistence(type);

        if (entity.getVersion() != null)
            entity.getCharacteristics().forEach(this::keepStoredVersion);

        var saved = translatingConflicts(() -> jpaRepository.saveAndFlush(entity));

        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByName(TypeName name, UUID exceptId) {
        return jpaRepository.existsByName(name.getValue(), exceptId);
    }

    @Override
    protected <T> T translatingConflicts(Supplier<T> operation) {
        try {
            return super.translatingConflicts(operation);
        }
        catch (ConflictException e) {
            if (UniqueConstraint.isViolated(e, NAME_CONSTRAINT))
                throw new ConflictException("type_name_taken", "já existe um tipo com este nome", e.getCause());

            throw e;
        }
    }

    private void keepStoredVersion(CharacteristicDefinitionEntity definition) {
        var stored = em.find(CharacteristicDefinitionEntity.class, definition.getId());

        if (stored != null)
            definition.setVersion(stored.getVersion());
    }
}
