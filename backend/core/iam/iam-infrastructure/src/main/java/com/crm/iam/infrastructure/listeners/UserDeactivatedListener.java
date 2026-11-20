package com.crm.iam.infrastructure.listeners;

import com.crm.iam.application.usecases.commands.RevokeUserSessions;
import com.crm.iam.domain.events.UserDeactivated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Revoga as sessões ativas do usuário desativado (efeito imediato). O {@link RevokeUserSessions}
 * publica um {@code SessionRevoked} por sessão, e é o listener dele que tira cada uma do cache.
 *
 * <p>O evento chega depois do commit, mas ainda dentro da fase de commit da transação que o
 * publicou: um use case que abrisse transação comum entraria nessa já encerrada e as revogações
 * nunca seriam gravadas. Por isso a chamada roda numa transação nova ({@code REQUIRES_NEW}).
 *
 * <p>O evento não traz quem desativou, então a revogação fica sem autor: {@code revoked_by} nulo
 * significa "revogada pela desativação da conta". A desativação já foi gravada, então uma falha
 * aqui não pode desfazê-la nem virar erro para quem a pediu: é logada. O que sobra é a janela do TTL
 * do cache, e a autenticação recusa conta que não está ativa.
 */
@Slf4j
@RequiredArgsConstructor
public class UserDeactivatedListener {

    private final RevokeUserSessions revokeUserSessions;
    private final TransactionTemplate requiresNew;

    @EventListener
    public void on(UserDeactivated event) {
        try {
            requiresNew.executeWithoutResult(status ->
                    revokeUserSessions.execute(new RevokeUserSessions.Params(event.aggregateId(), null)));
        }
        catch (RuntimeException e) {
            log.error("falha ao revogar as sessões do usuário desativado {}", event.aggregateId(), e);
        }
    }
}
