package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.enums.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.enums.UserStatus;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Reenvia o convite: invalida os links pendentes do usuário e emite um novo.
 *
 * <p>Só o Administrador reenvia (ADR-5), e só a quem ainda está {@code INVITED}. O link antigo
 * deixa de valer antes de o novo existir.
 */
@UseCase
@RequiredArgsConstructor
public class ResendInvitation {

    private final UserRepository userRepository;
    private final OneTimeTokenRepository tokenRepository;
    private final EventPublisher publisher;
    private final IamSettings settings;
    private final Clock clock;

    /**
     * @param actor  quem reenvia
     * @param userId usuário convidado
     */
    public record Params(
            Actor actor,
            UUID userId
    ) {
    }

    public void execute(Params params) {
        params.actor().requireAdministrator();

        var now = Instant.now(clock);

        var user = userRepository.findById(params.userId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));

        if (user.getStatus() != UserStatus.INVITED)
            throw new UnprocessableEntityException("user_not_invited", "este usuário não está com convite pendente");

        for (var pending : tokenRepository.findPending(user.getId(), TokenPurpose.INVITATION)) {
            pending.invalidate(now);

            tokenRepository.save(pending);
        }

        var token = OneTimeToken.issue(
                user.getId(),
                TokenPurpose.INVITATION,
                TokenSecret.generate(),
                settings.invitationValidity(),
                now
        );

        tokenRepository.save(token);

        publisher.publishAll(token.pullEvents());
    }
}
