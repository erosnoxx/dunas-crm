package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.InvalidParam;
import com.crm.registry.application.customers.schemas.AddressInput;
import com.crm.shared.domain.enums.UF;
import com.crm.shared.domain.vos.address.Address;
import com.crm.shared.domain.vos.address.AddressComplement;
import com.crm.shared.domain.vos.address.AddressNumber;
import com.crm.shared.domain.vos.address.CityName;
import com.crm.shared.domain.vos.address.Neighborhood;
import com.crm.shared.domain.vos.address.Street;
import com.crm.shared.domain.vos.address.ZipCode;

import java.util.Arrays;
import java.util.List;

/**
 * Converte o endereço que chega como texto da requisição em {@link Address}.
 *
 * <p>UF desconhecida é entrada inválida (400), e não erro de domínio: o texto ainda não é uma UF.
 * O nome do enum é exato, sem normalizar a caixa. O objeto de entrada é exigido pela {@code web}.
 */
final class AddressParser {

    private AddressParser() {
    }

    static Address parse(AddressInput input) {
        var zipCode = ZipCode.of(input.zipCode());
        var street = Street.of(input.street());
        var number = AddressNumber.of(input.number());
        var complement = OptionalValue.of(input.complement(), AddressComplement::of);
        var neighborhood = Neighborhood.of(input.neighborhood());
        var city = CityName.of(input.city());
        var state = Arrays.stream(UF.values())
                .filter(uf -> uf.name().equals(input.state()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "uf inválida",
                        List.of(new InvalidParam("address.state", "uf desconhecida"))
                ));

        return new Address(
                zipCode,
                street,
                number,
                complement,
                neighborhood,
                city,
                state
        );
    }
}
