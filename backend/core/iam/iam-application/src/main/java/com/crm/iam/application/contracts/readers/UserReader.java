package com.crm.iam.application.contracts.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.iam.application.schemas.CurrentUser;
import com.crm.iam.application.schemas.UserFilter;
import com.crm.iam.application.schemas.UserSummary;

import java.util.Optional;
import java.util.UUID;

/** Leitura de usuários para as telas, direto em projeção, sem montar o aggregate. */
public interface UserReader {

    Page<UserSummary> search(
            UserFilter filter,
            Pageable pageable
    );

    Optional<UserSummary> findSummary(UUID id);

    /** Quem está logado, com a flag de troca de senha que o resumo não traz. */
    Optional<CurrentUser> findCurrent(UUID id);
}
