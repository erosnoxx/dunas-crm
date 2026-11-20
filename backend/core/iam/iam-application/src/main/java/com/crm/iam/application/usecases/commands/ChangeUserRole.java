package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Troca o papel de um usuário. Só o Administrador troca (ADR-5); as regras de quem pode ter o papel
 * trocado ({@code cannot_change_own_role}, {@code user_not_active}) são do {@code User}.
 */
@UseCase
@RequiredArgsConstructor
public class ChangeUserRole {

    private final UserRepository userRepository;
    private final EventPublisher publisher;
    private final Clock clock;

    /**
     * @param actor  quem troca
     * @param userId usuário que muda de papel
     * @param role   nome do novo papel
     */
    public record Params(
            Actor actor,
            UUID userId,
            String role
    ) {
    }

    public void execute(Params params) {
        params.actor().requireAdministrator();

        var role = RoleParser.parse(params.role());

        var user = userRepository.findById(params.userId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        user.changeRole(params.actor().userId(), role, Instant.now(clock));

        userRepository.save(user);

        publisher.publishAll(user.pullEvents());
    }
}
