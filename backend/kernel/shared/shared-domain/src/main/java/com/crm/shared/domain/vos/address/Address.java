package com.crm.shared.domain.vos.address;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.shared.domain.enums.UF;

/**
 * Endereço completo. Todos os sub-campos são obrigatórios juntos, exceto {@code complement} —
 * não existe endereço parcialmente preenchido (defeito do projeto anterior, 00-overview.md).
 */
public record Address(
        ZipCode zipCode,
        Street street,
        AddressNumber number,
        AddressComplement complement,
        Neighborhood neighborhood,
        CityName city,
        UF state
) {

    public Address {
        if (zipCode == null || street == null || number == null || neighborhood == null || city == null || state == null)
            throw new ValidationException("endereço precisa de cep, rua, número, bairro, cidade e estado");
    }
}
