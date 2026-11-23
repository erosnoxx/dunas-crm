package com.crm.iam.infrastructure.mappers;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.infrastructure.persistence.entities.UserEntity;
import com.crm.shared.domain.vos.contact.Email;

/**
 * Converte {@link User} em {@link UserEntity} e de volta. {@code createdAt} e {@code updatedAt}
 * são só da persistência e nunca chegam ao domínio.
 */
public class UserMapper implements EntityMapper<User, UserEntity> {

    @Override
    public User toDomain(UserEntity entity) {
        var passwordHash = entity.getPasswordHash() == null ? null : PasswordHash.of(entity.getPasswordHash());

        return User.reconstitute(
                entity.getId(),
                entity.getVersion(),
                UserName.of(entity.getName()),
                Email.of(entity.getEmail()),
                entity.getRole(),
                entity.getStatus(),
                passwordHash,
                entity.isMustChangePassword()
        );
    }

    @Override
    public UserEntity toPersistence(User user) {
        var entity = new UserEntity();

        entity.setId(user.getId());
        entity.setVersion(user.getVersion().orElse(null));
        entity.setName(user.getName().getValue());
        entity.setEmail(user.getEmail().getValue());
        entity.setRole(user.getRole());
        entity.setStatus(user.getStatus());
        entity.setPasswordHash(user.getPasswordHash().map(PasswordHash::getValue).orElse(null));
        entity.setMustChangePassword(user.isMustChangePassword());

        return entity;
    }
}
