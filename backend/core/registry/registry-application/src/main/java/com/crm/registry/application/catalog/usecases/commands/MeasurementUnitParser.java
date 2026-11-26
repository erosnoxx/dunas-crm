package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.InvalidParam;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;

import java.util.Arrays;
import java.util.List;

/**
 * Converte a unidade que chega como texto da requisição em {@link MeasurementUnit}.
 *
 * <p>Unidade desconhecida é entrada inválida (400), e não erro de domínio: o texto ainda não é uma
 * unidade. O nome do enum é exato, sem normalizar a caixa.
 */
final class MeasurementUnitParser {

    private MeasurementUnitParser() {
    }

    static MeasurementUnit parse(String raw) {
        return Arrays.stream(MeasurementUnit.values())
                .filter(unit -> unit.name().equals(raw))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "unidade inválida",
                        List.of(new InvalidParam("unit", "unidade desconhecida"))
                ));
    }
}
