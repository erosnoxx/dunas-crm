package com.crm.iam.application.usecases.commands;

import com.crm.common.application.exceptions.BadRequestException;
import com.crm.common.application.exceptions.InvalidParam;
import com.crm.shared.domain.enums.Role;

import java.util.Arrays;
import java.util.List;

/**
 * Converte o papel que chega como texto da requisição em {@link Role}.
 *
 * <p>Papel desconhecido é entrada inválida (400), e não erro de domínio: o texto ainda não é um
 * papel. O nome do enum é exato, sem normalizar a caixa.
 */
final class RoleParser {

    private RoleParser() {
    }

    static Role parse(String raw) {
        return Arrays.stream(Role.values())
                .filter(role -> role.name().equals(raw))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "papel inválido",
                        List.of(new InvalidParam("role", "papel desconhecido"))
                ));
    }
}
