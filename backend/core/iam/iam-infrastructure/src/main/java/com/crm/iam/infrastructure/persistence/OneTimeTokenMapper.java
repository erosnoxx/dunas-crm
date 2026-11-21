package com.crm.iam.infrastructure.persistence;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.iam.domain.entities.OneTimeToken;
import com.crm.iam.domain.vos.TokenHash;

/**
 * Converte {@link OneTimeToken} em {@link OneTimeTokenEntity} e de volta. {@code createdAt} e
 * {@code updatedAt} são só da persistência e nunca chegam ao domínio.
 */
public class OneTimeTokenMapper implements EntityMapper<OneTimeToken, OneTimeTokenEntity> {

    @Override
    public OneTimeToken toDomain(OneTimeTokenEntity entity) {
        return OneTimeToken.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getUserId(),
                entity.getPurpose(),
                TokenHash.of(entity.getTokenHash()),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getInvalidatedAt()
        );
    }

    @Override
    public OneTimeTokenEntity toPersistence(OneTimeToken token) {
        var entity = new OneTimeTokenEntity();

        entity.setId(token.getId());
        entity.setVersion(token.getVersion().orElse(null));
        entity.setUserId(token.getUserId());
        entity.setPurpose(token.getPurpose());
        entity.setTokenHash(token.getTokenHash().getValue());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt().orElse(null));
        entity.setInvalidatedAt(token.getInvalidatedAt().orElse(null));

        return entity;
    }
}
