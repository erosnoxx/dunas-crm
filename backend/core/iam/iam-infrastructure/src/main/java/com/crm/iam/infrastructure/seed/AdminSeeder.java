package com.crm.iam.infrastructure.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.infrastructure.seed.Seeder;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.PasswordHash;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.UserName;
import com.crm.shared.domain.vos.contact.Email;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Cria o primeiro Administrador no boot (ADR-2), porque convite exige alguém que convide.
 *
 * <p>Só semeia quando não existe nenhum usuário, então reiniciar o app não recria nem sobrescreve
 * a conta. A conta nasce ativa e com troca de senha obrigatória no primeiro acesso. A senha passa
 * pela mesma regra de qualquer outra (ADR-3) e só o hash é guardado.
 *
 * <p>{@link #isAlreadySeeded()} e {@link #seed(List)} não são atômicos entre si: se duas instâncias
 * subirem ao mesmo tempo, as duas podem tentar criar a conta. A rede de segurança é o
 * {@code UNIQUE(email)} do banco, que faz a segunda falhar sem duplicar.
 */
@RequiredArgsConstructor
public class AdminSeeder implements Seeder<AdminSeed> {

    private final UserRepository userRepository;
    private final PasswordHasher hasher;
    private final Clock clock;

    @Override
    public boolean isAlreadySeeded() {
        return userRepository.count() > 0;
    }

    @Override
    public void seed(List<AdminSeed> data) {
        for (var admin : data)
            userRepository.save(createAdministrator(admin));
    }

    @Override
    public String resourcePath() {
        return "seed/iam-admin.json";
    }

    @Override
    public Class<AdminSeed> recordType() {
        return AdminSeed.class;
    }

    private User createAdministrator(AdminSeed admin) {
        var user = invite(admin);

        user.acceptInvitation(hashOf(admin));
        user.markPasswordChangeRequired();

        return user;
    }

    private User invite(AdminSeed admin) {
        try {
            return User.invite(
                    UserName.of(admin.name()),
                    Email.of(admin.email()),
                    Role.ADMINISTRATOR,
                    Instant.now(clock)
            );
        }
        catch (ValidationException e) {
            throw invalid(e);
        }
    }

    private PasswordHash hashOf(AdminSeed admin) {
        try {
            return hasher.hash(Password.of(admin.password()));
        }
        catch (ValidationException e) {
            throw invalid(e);
        }
    }

    /** A mensagem da regra diz qual critério faltou, nunca a senha (ADR-3). */
    private static InternalServerErrorException invalid(ValidationException e) {
        return new InternalServerErrorException(
                "dados do administrador inicial inválidos (CRM_ADMIN_NAME, CRM_ADMIN_EMAIL, CRM_ADMIN_PASSWORD): "
                        + e.getMessage(),
                e
        );
    }
}
