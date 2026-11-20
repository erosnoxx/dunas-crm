package com.crm.iam.infrastructure.listeners;

import com.crm.common.application.cache.Cache;
import com.crm.iam.application.contracts.repositories.SessionRepository;
import com.crm.iam.application.usecases.commands.AuthenticateSession;
import com.crm.iam.domain.events.UserRoleChanged;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.Instant;

/**
 * Invalida no cache as sessões do usuário que mudou de papel, porque o papel vai junto da sessão em
 * cache e o novo papel precisa valer na próxima requisição.
 *
 * <p>Não há use case para isto: é só ler as sessões ativas e remover as chaves, sem regra nem
 * escrita, então o listener fala direto com o repository e com o cache.
 *
 * <p>A mudança de papel já foi gravada: uma falha aqui não pode virar erro para quem a pediu, então
 * é logada. O que sobra é a janela do TTL do cache de sessão.
 */
@Slf4j
@RequiredArgsConstructor
public class UserRoleChangedListener {

    private final SessionRepository sessionRepository;
    private final Cache cache;
    private final Clock clock;

    @EventListener
    public void on(UserRoleChanged event) {
        try {
            var sessions = sessionRepository.findActiveByUserId(event.aggregateId(), Instant.now(clock));

            for (var session : sessions)
                cache.evict(AuthenticateSession.CACHE_KEY_PREFIX + session.getTokenHash().getValue());
        }
        catch (RuntimeException e) {
            log.error("falha ao invalidar as sessões do usuário que mudou de papel {}", event.aggregateId(), e);
        }
    }
}
