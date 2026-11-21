package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.entities.DomainEntity;
import com.crm.registry.domain.catalog.vos.CharacteristicName;
import com.crm.registry.domain.catalog.vos.MeasurementUnit;
import lombok.Getter;

import java.util.UUID;

/**
 * Definição de uma característica que os modelos de um {@link EquipmentType} trazem (nome,
 * unidade e se é obrigatória).
 *
 * <p>Só existe dentro do tipo, que a carrega, a salva e é o único a alterá-la. O id é o que o valor
 * do modelo referencia e não muda.
 */
@Getter
public class CharacteristicDefinition extends DomainEntity {

    private CharacteristicName name;

    private MeasurementUnit unit;

    private boolean required;

    private CharacteristicDefinition(
            UUID id,
            CharacteristicName name,
            MeasurementUnit unit,
            boolean required
    ) {
        super(id);

        this.name = name;
        this.unit = unit;
        this.required = required;
    }

    public static CharacteristicDefinition create(
            CharacteristicName name,
            MeasurementUnit unit,
            boolean required
    ) {
        return new CharacteristicDefinition(
                UUID.randomUUID(),
                name,
                unit,
                required
        );
    }

    public static CharacteristicDefinition reconstitute(
            UUID id,
            CharacteristicName name,
            MeasurementUnit unit,
            boolean required
    ) {
        return new CharacteristicDefinition(
                id,
                name,
                unit,
                required
        );
    }

    /** Só o {@link EquipmentType} altera a definição, depois de conferir a trava. */
    void change(
            CharacteristicName name,
            MeasurementUnit unit,
            boolean required
    ) {
        this.name = name;
        this.unit = unit;
        this.required = required;
    }
}
