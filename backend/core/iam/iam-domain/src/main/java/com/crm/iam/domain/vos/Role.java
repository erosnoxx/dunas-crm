package com.crm.iam.domain.vos;

/**
 * Papel de um usuário da empresa. Cada usuário tem exatamente um, e é ele que decide o que a
 * sessão pode fazer.
 */
public enum Role {
    ADMINISTRATOR,
    COMMERCIAL,
    TECHNICIAN
}
