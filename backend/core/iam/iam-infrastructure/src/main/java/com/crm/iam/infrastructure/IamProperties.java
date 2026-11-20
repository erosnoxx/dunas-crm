package com.crm.iam.infrastructure;

import com.crm.common.application.ratelimit.RateLimit;
import com.crm.iam.application.IamSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuração do IAM ({@code crm.iam.*}). Todo valor que a spec fixa tem default, então subir o
 * app não exige configurar nada (architecture.md §9).
 *
 * <pre>
 * crm.iam.session-validity=30d
 * crm.iam.session-renew-interval=5m
 * crm.iam.session-cache-ttl=60s
 * crm.iam.invitation-validity=48h
 * crm.iam.password-reset-validity=1h
 * crm.iam.login-limit-per-email.max-attempts=5
 * crm.iam.login-limit-per-email.window=15m
 * crm.iam.app-url=http://localhost:5173
 * crm.iam.argon2.memory-kib=19456
 * </pre>
 *
 * <p>A senha do Administrador inicial não é propriedade nem tem default: vem das variáveis
 * {@code CRM_ADMIN_*} resolvidas no JSON do seeder, e a falta de uma derruba o boot.
 *
 * @param sessionValidity             validade da sessão a partir do último uso
 * @param sessionRenewInterval        intervalo mínimo entre renovações da sessão
 * @param sessionCacheTtl             tempo de vida da sessão no cache
 * @param invitationValidity          validade do link de convite
 * @param passwordResetValidity       validade do link de redefinição de senha
 * @param loginLimitPerEmail          limite de erros de login por e-mail
 * @param loginLimitPerOrigin         limite de erros de login por origem
 * @param passwordResetLimitPerEmail  limite de pedidos de redefinição por e-mail
 * @param appUrl                      endereço do frontend, base dos links enviados por e-mail
 * @param argon2                      custo do hash de senha
 */
@ConfigurationProperties(prefix = "crm.iam")
public record IamProperties(
        @DefaultValue("30d") Duration sessionValidity,
        @DefaultValue("5m") Duration sessionRenewInterval,
        @DefaultValue("60s") Duration sessionCacheTtl,
        @DefaultValue("48h") Duration invitationValidity,
        @DefaultValue("1h") Duration passwordResetValidity,
        Limit loginLimitPerEmail,
        Limit loginLimitPerOrigin,
        Limit passwordResetLimitPerEmail,
        @DefaultValue("http://localhost:5173") String appUrl,
        @DefaultValue Argon2 argon2
) {

    static final String LIMITED = "muitas tentativas; tente de novo em alguns minutos";

    /**
     * Os três limites têm defaults diferentes, e {@code @DefaultValue} só dá o mesmo valor a todo
     * campo de um tipo aninhado: por isso o default de cada limite é aplicado aqui.
     */
    public IamProperties {
        loginLimitPerEmail = Limit.orDefault(loginLimitPerEmail, 5, Duration.ofMinutes(15));
        loginLimitPerOrigin = Limit.orDefault(loginLimitPerOrigin, 20, Duration.ofMinutes(15));
        passwordResetLimitPerEmail = Limit.orDefault(passwordResetLimitPerEmail, 3, Duration.ofHours(1));
    }

    /**
     * Monta o que os use cases consomem. Os limites recusam a tentativa quando o contador está fora
     * do ar: liberar login sem freio é pior do que recusá-lo.
     */
    public IamSettings toSettings() {
        return new IamSettings(
                sessionValidity,
                sessionRenewInterval,
                sessionCacheTtl,
                invitationValidity,
                passwordResetValidity,
                loginLimitPerEmail.toRateLimit("iam.login.email"),
                loginLimitPerOrigin.toRateLimit("iam.login.origin"),
                passwordResetLimitPerEmail.toRateLimit("iam.password-reset.email")
        );
    }

    /**
     * Tentativas permitidas numa janela. Campo ausente cai no default do limite, definido em
     * {@link IamProperties}.
     *
     * @param maxAttempts tentativas por janela
     * @param window      duração da janela
     */
    public record Limit(Integer maxAttempts, Duration window) {

        static Limit orDefault(
                Limit configured,
                int defaultAttempts,
                Duration defaultWindow
        ) {
            if (configured == null)
                return new Limit(defaultAttempts, defaultWindow);

            return new Limit(
                    configured.maxAttempts == null ? defaultAttempts : configured.maxAttempts,
                    configured.window == null ? defaultWindow : configured.window
            );
        }

        RateLimit toRateLimit(String scope) {
            return RateLimit.failClosed(scope, maxAttempts, window, "rate_limited", LIMITED);
        }
    }

    /**
     * Custo do Argon2id. Os defaults seguem o mínimo recomendado pela OWASP (19 MiB, 2 passadas,
     * 1 thread); produção ajusta pelo tempo de hash medido no servidor.
     *
     * @param memoryKib   memória por hash, em KiB
     * @param iterations  passadas sobre a memória
     * @param parallelism threads por hash
     * @param saltLength  tamanho do sal, em bytes
     * @param hashLength  tamanho do hash, em bytes
     */
    public record Argon2(
            @DefaultValue("19456") int memoryKib,
            @DefaultValue("2") int iterations,
            @DefaultValue("1") int parallelism,
            @DefaultValue("16") int saltLength,
            @DefaultValue("32") int hashLength
    ) {
    }
}
