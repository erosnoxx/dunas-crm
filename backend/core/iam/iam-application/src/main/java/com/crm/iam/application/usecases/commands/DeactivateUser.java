package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Desativa um usuário. Só o Administrador desativa (ADR-5).
 *
 * <p>As sessões do usuário são revogadas por um listener do {@code UserDeactivated}, que chama
 * {@link RevokeUserSessions}, e não aqui.
 */
@UseCase
@RequiredArgsConstructor
public class DeactivateUser {

    private final UserRepository userRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    /**
     * @param actor  quem desativa
     * @param userId usuário desativado
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

        user.deactivate(params.actor().userId(), Instant.now(clock));

        userRepository.save(user);

        publisher.publishAll(user.pullEvents());
    }
}
