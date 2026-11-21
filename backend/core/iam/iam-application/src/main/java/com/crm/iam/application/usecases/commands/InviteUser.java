package com.crm.iam.application.usecases.commands;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.events.EventPublisher;
import com.crm.common.application.exceptions.ConflictException;
import com.crm.iam.application.Actor;
import com.crm.iam.application.IamSettings;
import com.crm.iam.application.contracts.repositories.OneTimeTokenRepository;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserName;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.shared.domain.vos.contact.Email;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

/**
 * Convida um usuário: cria a conta {@code INVITED} e emite o link de convite.
 *
 * <p>Só o Administrador convida (ADR-5). O e-mail com o link sai de um listener, depois do commit,
 * a partir do {@code OneTimeTokenIssued}: e-mail que falha não desfaz o convite. A checagem de
 * e-mail repetido é só a resposta amigável; quem garante é o UNIQUE do banco.
 */
@UseCase
@RequiredArgsConstructor
public class InviteUser {

    private final UserRepository userRepository;
    private final OneTimeTokenRepository tokenRepository;
    private final EventPublisher publisher;
    private final IamSettings settings;
    private final Clock clock;

    /**
     * @param actor quem convida
     * @param name  nome do convidado
     * @param email e-mail do convidado, em qualquer caixa
     * @param role  nome do papel ({@code ADMINISTRATOR}, {@code COMMERCIAL} ou {@code TECHNICIAN})
     */
    public record Params(
            Actor actor,
            String name,
            String email,
            String role
    ) {
    }

    public UUIDResponse execute(Params params) {
        params.actor().requireAdministrator();

        var now = Instant.now(clock);
        var role = RoleParser.parse(params.role());
        var email = Email.of(params.email());

        if (userRepository.findByEmail(email).isPresent())
            throw new ConflictException("email_already_registered", "já existe um usuário com este e-mail");

        var user = User.invite(
                UserName.of(params.name()),
                email,
                role,
                now
        );

        userRepository.save(user);

        var token = OneTimeToken.issue(
                user.getId(),
                TokenPurpose.INVITATION,
                TokenSecret.generate(),
                settings.invitationValidity(),
                now
        );

        tokenRepository.save(token);

        publisher.publishAll(user.pullEvents());
        publisher.publishAll(token.pullEvents());

        return new UUIDResponse(user.getId());
    }
}
