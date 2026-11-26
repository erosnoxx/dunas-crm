package com.crm.iam.application.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.shared.application.Actor;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.CurrentUser;
import lombok.RequiredArgsConstructor;

/** Quem está logado, para o cliente montar a interface; é o que a conta com troca de senha pendente ainda pode chamar. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class GetCurrentUser {

    private final UserReader userReader;

    public CurrentUser execute(Actor actor) {
        return userReader.findCurrent(actor.userId())
                .orElseThrow(() -> new NotFoundException("user_not_found", "usuário não encontrado"));
    }
}
