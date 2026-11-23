package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.vos.ModelName;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Modelo de equipamento de um fabricante e de um tipo, com os valores da característica do tipo.
 *
 * <p>Fabricante e tipo são referenciados por id e não mudam depois do cadastro. Só um modelo
 * <b>novo</b> é bloqueado por fabricante ou tipo desativado; editar um modelo existente aceita o tipo
 * desativado. A regra "o modelo traz o que o tipo pede" é do {@link EquipmentType#checkValues}. O
 * ciclo de vida é um enum com guardas, sem máquina de estados, e a unicidade (fabricante, nome) é do
 * banco. Toda operação valida antes de mudar: se recusa, o estado fica como estava.
 */
public class EquipmentModel extends AggregateRoot {

    @Getter
    private final UUID manufacturerId;

    @Getter
    private final UUID typeId;

    @Getter
    private ModelName name;

    @Getter
    private CatalogStatus status;

    private List<CharacteristicValue> values;

    private EquipmentModel(
            UUID id,
            Long version,
            UUID manufacturerId,
            UUID typeId,
            ModelName name,
            List<CharacteristicValue> values,
            CatalogStatus status
    ) {
        super(id, version);

        this.manufacturerId = manufacturerId;
        this.typeId = typeId;
        this.name = name;
        this.values = List.copyOf(values);
        this.status = status;
    }

    /** Cadastra o modelo, que nasce ativo, para um fabricante e um tipo ativos. */
    public static EquipmentModel register(
            Manufacturer manufacturer,
            EquipmentType type,
            ModelName name,
            List<CharacteristicValue> values
    ) {
        if (!manufacturer.isActive())
            throw new DomainException("manufacturer_deactivated", "este fabricante está desativado");
        if (!type.isActive())
            throw new DomainException("type_deactivated", "este tipo está desativado");

        type.checkValues(values);

        return new EquipmentModel(
                UUID.randomUUID(),
                null,
                manufacturer.getId(),
                type.getId(),
                name,
                values,
                CatalogStatus.ACTIVE
        );
    }

    public static EquipmentModel reconstitute(
            UUID id,
            Long version,
            UUID manufacturerId,
            UUID typeId,
            ModelName name,
            List<CharacteristicValue> values,
            CatalogStatus status
    ) {
        return new EquipmentModel(
                id,
                version,
                manufacturerId,
                typeId,
                name,
                values,
                status
        );
    }

    /** Os valores do modelo, numa cópia que não pode ser alterada. */
    public List<CharacteristicValue> getValues() {
        return values;
    }

    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }

    /**
     * Troca nome e valores. O {@code type} é o tipo do modelo, que pode estar desativado: serve só
     * para conferir os valores.
     */
    public void edit(EquipmentType type, ModelName newName, List<CharacteristicValue> newValues) {
        requireActive();

        if (!type.getId().equals(typeId))
            throw new DomainException("type_mismatch", "o tipo informado não é o deste modelo");

        type.checkValues(newValues);

        this.name = newName;
        this.values = List.copyOf(newValues);
    }

    public void deactivate() {
        requireActive();

        this.status = CatalogStatus.DEACTIVATED;
    }

    public void reactivate() {
        if (status != CatalogStatus.DEACTIVATED)
            throw new DomainException("model_not_deactivated", "este modelo não está desativado");

        this.status = CatalogStatus.ACTIVE;
    }

    private void requireActive() {
        if (status != CatalogStatus.ACTIVE)
            throw new DomainException("model_deactivated", "este modelo está desativado");
    }
}
