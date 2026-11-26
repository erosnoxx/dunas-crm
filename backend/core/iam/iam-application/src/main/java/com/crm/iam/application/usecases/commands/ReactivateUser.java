package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Reativa um usuário desativado. Só o Administrador reativa (ADR-5).
 *
 * <p>Não publica evento e nenhuma sessão volta: o usuário entra de novo pelo login.
 */
@UseCase
@RequiredArgsConstructor
public class ReactivateUser {

    private final UserRepository userRepository;

    /**
     * @param actor  quem reativa
     * @param userId usuário reativado
     */
    public record Params(
            Actor actor,
            UUID userId
    ) {
    }

    public void execute(Params params) {
        params.actor().requireAdministrator();

        var user = userRepository.findById(params.userId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        user.reactivate();

        userRepository.save(user);
    }
}
