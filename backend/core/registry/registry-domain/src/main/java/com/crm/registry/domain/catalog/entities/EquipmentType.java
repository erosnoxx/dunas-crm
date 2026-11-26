package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.registry.domain.catalog.vos.TypeName;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Tipo de equipamento do catálogo (Inversor, Ar-condicionado) e a definição da característica que
 * os modelos dele trazem.
 *
 * <p>Tem exatamente uma {@link CharacteristicDefinition} nesta versão (ADR-3). O ciclo de vida é
 * um enum com guardas, sem máquina de estados; só um tipo ativo é alterado. O tipo não sabe se há
 * modelos: o use case passa {@code hasModels}, e então a unidade não muda e a característica não
 * passa a obrigatória (ADR-4). {@link #checkValues} é a regra "o modelo traz o que o tipo pede".
 */
public class EquipmentType extends AggregateRoot {

    @Getter
    private TypeName name;

    @Getter
    private CatalogStatus status;

    private final CharacteristicDefinition characteristic;

    private EquipmentType(
            UUID id,
            Long version,
            TypeName name,
            CatalogStatus status,
            CharacteristicDefinition characteristic
    ) {
        super(id, version);

        this.name = name;
        this.status = status;
        this.characteristic = characteristic;
    }

    /** Cadastra o tipo, que nasce ativo, com a definição da sua característica. */
    public static EquipmentType register(
            TypeName name,
            CharacteristicName characteristicName,
            MeasurementUnit unit,
            boolean required
    ) {
        return new EquipmentType(
                UUID.randomUUID(),
                null,
                name,
                CatalogStatus.ACTIVE,
                CharacteristicDefinition.create(characteristicName, unit, required)
        );
    }

    public static EquipmentType reconstitute(
            UUID id,
            Long version,
            TypeName name,
            CatalogStatus status,
            List<CharacteristicDefinition> characteristics
    ) {
        if (characteristics == null || characteristics.size() != 1)
            throw new DomainException("o tipo deve ter exatamente uma característica");

        return new EquipmentType(
                id,
                version,
                name,
                status,
                characteristics.getFirst()
        );
    }

    /** A única definição do tipo. */
    public CharacteristicDefinition getCharacteristic() {
        return characteristic;
    }

    public List<CharacteristicDefinition> getCharacteristics() {
        return List.of(characteristic);
    }

    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }

    public void rename(TypeName newName) {
        requireActive();

        this.name = newName;
    }

    /**
     * Altera a definição, mantendo o id. Com modelos, a unidade não muda e a característica não
     * passa de opcional a obrigatória; nome novo e obrigatória a opcional passam. Se recusa, nada muda.
     */
    public void changeCharacteristic(
            CharacteristicName newName,
            MeasurementUnit newUnit,
            boolean newRequired,
            boolean hasModels
    ) {
        requireActive();

        var unitChanges = newUnit != characteristic.getUnit();
        var turnsRequiredOn = newRequired && !characteristic.isRequired();

        if (hasModels && (unitChanges || turnsRequiredOn))
            throw new DomainException(
                    "characteristic_locked",
                    "o tipo já tem modelos: a unidade e a obrigatoriedade não podem mudar"
            );

        characteristic.change(newName, newUnit, newRequired);
    }

    public void deactivate() {
        requireActive();

        this.status = CatalogStatus.DEACTIVATED;
    }

    public void reactivate() {
        if (status != CatalogStatus.DEACTIVATED)
            throw new DomainException("type_not_deactivated", "este tipo não está desativado");

        this.status = CatalogStatus.ACTIVE;
    }

    /**
     * Confere os valores de um modelo contra a definição do tipo, sem mudar estado: valor de outra
     * definição ou repetido é desconhecido; definição obrigatória sem valor é recusada.
     */
    public void checkValues(List<CharacteristicValue> values) {
        var seen = new HashSet<UUID>();

        for (var value : values)
            if (!value.definitionId().equals(characteristic.getId()) || !seen.add(value.definitionId()))
                throw new DomainException(
                        "characteristic_unknown",
                        "a característica informada não existe neste tipo"
                );

        if (characteristic.isRequired() && seen.isEmpty())
            throw new DomainException(
                    "characteristic_required",
                    "informe a característica " + characteristic.getName().getValue()
            );
    }

    private void requireActive() {
        if (status != CatalogStatus.ACTIVE)
            throw new DomainException("type_deactivated", "este tipo está desativado");
    }
}
