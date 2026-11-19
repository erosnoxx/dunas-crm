package com.crm.iam.infrastructure.persistence;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.iam.domain.entities.Session;
import com.crm.iam.domain.vos.DeviceLabel;
import com.crm.iam.domain.vos.TokenHash;

/**
 * Converte {@link Session} em {@link SessionEntity} e de volta. {@code createdAt} e
 * {@code updatedAt} são só da persistência e nunca chegam ao domínio.
 */
public class SessionMapper implements EntityMapper<Session, SessionEntity> {

    @Override
    public Session toDomain(SessionEntity entity) {
        return Session.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getUserId(),
                TokenHash.of(entity.getTokenHash()),
                DeviceLabel.of(entity.getDevice()),
                entity.getStartedAt(),
                entity.getLastUsedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getRevokedBy()
        );
    }

    @Override
    public SessionEntity toPersistence(Session session) {
        var entity = new SessionEntity();

        entity.setId(session.getId());
        entity.setVersion(session.getVersion().orElse(null));
        entity.setUserId(session.getUserId());
        entity.setTokenHash(session.getTokenHash().getValue());
        entity.setDevice(session.getDevice().getValue());
        entity.setStartedAt(session.getStartedAt());
        entity.setLastUsedAt(session.getLastUsedAt());
        entity.setExpiresAt(session.getExpiresAt());
        entity.setRevokedAt(session.getRevokedAt().orElse(null));
        entity.setRevokedBy(session.getRevokedBy().orElse(null));

        return entity;
    }
}
