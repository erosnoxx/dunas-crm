package com.crm.iam.application.schemas;

import java.util.List;

/** Usuário com as sessões ativas dele, para a tela de detalhe do Administrador. */
public record UserDetail(
        UserSummary user,
        List<SessionSummary> sessions
) {
}
