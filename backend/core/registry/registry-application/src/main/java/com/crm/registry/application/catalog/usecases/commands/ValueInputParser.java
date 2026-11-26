package com.crm.registry.application.catalog.usecases.commands;

import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.domain.catalog.vos.CharacteristicAmount;
import com.crm.registry.domain.catalog.vos.CharacteristicValue;

import java.util.List;

/**
 * Converte os valores que chegam como texto da requisição em {@link CharacteristicValue}.
 *
 * <p>Lista nula vale vazia: o tipo com característica opcional aceita modelo sem valor, e o
 * obrigatório é recusado pelo domínio ({@code characteristic_required}). Número fora do formato sai
 * como {@code invalid_amount} (400), do próprio VO.
 */
final class ValueInputParser {

    private ValueInputParser() {
    }

    static List<CharacteristicValue> parse(List<ValueInput> inputs) {
        if (inputs == null)
            return List.of();

        return inputs.stream()
                .map(input -> new CharacteristicValue(input.definitionId(), CharacteristicAmount.of(input.amount())))
                .toList();
    }
}
