package com.crm.iam.infrastructure.seed;

/**
 * Um item de {@code seed/iam-admin.json}: os dados do primeiro Administrador, que vêm das variáveis
 * de ambiente {@code CRM_ADMIN_*}.
 *
 * <p>A senha é um segredo, então o {@code toString} não a mostra.
 *
 * @param name     nome do Administrador
 * @param email    e-mail do Administrador, que é o login
 * @param password senha inicial, em texto, que a conta é obrigada a trocar no primeiro acesso
 */
public record AdminSeed(
        String name,
        String email,
        String password
) {

    @Override
    public String toString() {
        return "AdminSeed[name=" + name + ", email=" + email + ", password=***]";
    }
}
