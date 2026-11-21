package com.crm.iam.infrastructure.persistence;

import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.shared.domain.enums.Role;
import com.crm.iam.domain.vos.UserName;
import com.crm.iam.domain.vos.UserStatus;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper")
class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    @DisplayName("ida e volta preserva todos os campos e a versão")
    void roundTripKeepsEveryField() {
        var user = User.reconstitute(
                UUID.randomUUID(),
                7L,
                UserName.of("Maria Souza"),
                Email.of("Maria@Empresa.com"),
                Role.COMMERCIAL,
                UserStatus.ACTIVE,
                PasswordHash.of("$argon2id$v=19$m=19456,t=2,p=1$abc$def"),
                true
        );

        var back = mapper.toDomain(mapper.toPersistence(user));

        assertThat(back.getId()).isEqualTo(user.getId());
        assertThat(back.getVersion()).contains(7L);
        assertThat(back.getName()).isEqualTo(user.getName());
        assertThat(back.getEmail()).isEqualTo(Email.of("maria@empresa.com"));
        assertThat(back.getRole()).isEqualTo(Role.COMMERCIAL);
        assertThat(back.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(back.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(back.isMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("conta convidada sem senha: versão nula é linha nova e o hash fica ausente")
    void invitedUserIsNewRowWithoutHash() {
        var user = User.invite(
                UserName.of("Joao Lima"),
                Email.of("joao@empresa.com"),
                Role.TECHNICIAN,
                Instant.parse("2026-11-19T10:00:00Z")
        );

        var entity = mapper.toPersistence(user);

        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getPasswordHash()).isNull();

        var back = mapper.toDomain(entity);

        assertThat(back.getVersion()).isEmpty();
        assertThat(back.getPasswordHash()).isEmpty();
        assertThat(back.getStatus()).isEqualTo(UserStatus.INVITED);
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(User.invite(
                UserName.of("Ana Paula"),
                Email.of("ana@empresa.com"),
                Role.ADMINISTRATOR,
                Instant.parse("2026-11-19T10:00:00Z")
        ));

        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        var back = mapper.toDomain(entity);

        assertThat(mapper.toPersistence(back).getCreatedAt()).isNull();
        assertThat(mapper.toPersistence(back).getUpdatedAt()).isNull();
    }
}
