package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;

import java.util.UUID;

/**
 * Valor de um modelo para uma definição de característica do tipo dele. Um valor por definição.
 *
 * @param definitionId id da definição, dentro do tipo
 * @param amount       o valor
 */
public record CharacteristicValue(UUID definitionId, CharacteristicAmount amount) {

    public CharacteristicValue {
        if (definitionId == null)
            throw new ValidationException("a definição da característica não pode ser nula");
        if (amount == null)
            throw new ValidationException("o valor da característica não pode ser nulo");
    }
}
