package com.crm.iam.infrastructure.security;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.iam.application.contracts.services.PasswordHasher;
import com.crm.iam.domain.vos.Password;
import com.crm.iam.domain.vos.PasswordHash;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * {@link PasswordHasher} com Argon2id (formato PHC, com sal próprio em cada hash).
 *
 * <p>Os parâmetros de custo vêm de fora, para que o tempo de hash seja ajustado por ambiente sem
 * mexer no código. Falha da biblioteca é falha de mecanismo (500): a mensagem nunca carrega a
 * senha nem o hash, e a exception original, que também não os carrega, vai só como causa.
 */
public class Argon2PasswordHasherImpl implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;

    /**
     * @param memoryKib   memória usada por hash, em KiB
     * @param iterations  passadas sobre a memória
     * @param parallelism threads por hash
     * @param saltLength  tamanho do sal, em bytes
     * @param hashLength  tamanho do hash, em bytes
     */
    public Argon2PasswordHasherImpl(
            int memoryKib,
            int iterations,
            int parallelism,
            int saltLength,
            int hashLength
    ) {
        this.encoder = new Argon2PasswordEncoder(
                saltLength,
                hashLength,
                parallelism,
                memoryKib,
                iterations
        );
    }

    @Override
    public PasswordHash hash(Password password) {
        try {
            return PasswordHash.of(encoder.encode(password.getValue()));
        }
        catch (RuntimeException e) {
            throw new InternalServerErrorException("não foi possível calcular o hash da senha", e);
        }
    }

    @Override
    public boolean matches(
            Password password,
            PasswordHash hash
    ) {
        try {
            return encoder.matches(password.getValue(), hash.getValue());
        }
        catch (RuntimeException e) {
            throw new InternalServerErrorException("não foi possível conferir a senha", e);
        }
    }
}
