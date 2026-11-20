package com.crm.iam.application.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.iam.application.Actor;
import com.crm.iam.application.contracts.readers.UserReader;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.application.schemas.UserSummary;
import lombok.RequiredArgsConstructor;

/** Lista os usuários com filtro e paginação. Só o Administrador lista (ADR-5). */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class ListUsers {

    private final UserReader userReader;

    /**
     * @param actor    quem consulta
     * @param filter   papel, status e texto em nome ou e-mail; campo nulo ou vazio não filtra
     * @param pageable página pedida
     */
    public record Params(
            Actor actor,
            UserFilter filter,
            Pageable pageable
    ) {
    }

    public Page<UserSummary> execute(Params params) {
        params.actor().requireAdministrator();

        return userReader.search(params.filter(), params.pageable());
    }
}
