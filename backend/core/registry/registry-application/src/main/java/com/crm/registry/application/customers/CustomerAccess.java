package com.crm.registry.application.customers;

import com.crm.shared.application.Actor;
import com.crm.shared.domain.enums.Role;

/**
 * Regra de acesso de clientes, localizações e equipamentos instalados (ADR-7): Administrador e
 * Comercial editam; o Técnico só lê.
 */
public final class CustomerAccess {

    private CustomerAccess() {
    }

    /** Exige que o ator possa editar; deve ser a primeira linha do use case, antes de qualquer VO ou repository. */
    public static void requireEditor(Actor actor) {
        actor.requireAnyRole(Role.ADMINISTRATOR, Role.COMMERCIAL);
    }

    /** Os três papéis consultam. Ator nulo é erro de programação e falha ao ser dereferenciado aqui. */
    public static void requireReader(Actor actor) {
        actor.requireAnyRole(Role.ADMINISTRATOR, Role.COMMERCIAL, Role.TECHNICIAN);
    }
}
