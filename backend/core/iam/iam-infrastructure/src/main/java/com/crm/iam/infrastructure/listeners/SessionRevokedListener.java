package com.crm.iam.infrastructure.listeners;

import com.crm.common.application.cache.Cache;
import com.crm.iam.application.usecases.commands.AuthenticateSession;
import com.crm.iam.domain.events.SessionRevoked;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

/**
 * Tira a sessão revogada do cache na hora, o que torna o encerramento remoto imediato (ADR-1).
 *
 * <p>É a invalidação por evento que o cache de sessão tem como mecanismo principal; o TTL é só a
 * rede de segurança (architecture.md §6.2). Remove só a chave daquela sessão, pelo hash que o
 * evento traz.
 */
@RequiredArgsConstructor
public class SessionRevokedListener {

    private final Cache cache;

    @EventListener
    public void on(SessionRevoked event) {
        cache.evict(AuthenticateSession.CACHE_KEY_PREFIX + event.tokenHash().getValue());
    }
}
