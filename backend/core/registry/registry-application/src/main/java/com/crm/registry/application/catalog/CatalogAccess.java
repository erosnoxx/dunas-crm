package com.crm.registry.application.catalog;

import com.crm.shared.application.Actor;
import com.crm.shared.domain.enums.Role;

/**
 * Regra de acesso de escrita do catálogo (ADR-6): Administrador e Comercial editam; o Técnico só lê.
 */
public final class CatalogAccess {

    private CatalogAccess() {
    }

    /** Exige que o ator possa editar o catálogo; chame antes de tocar qualquer repository. */
    public static void requireEditor(Actor actor) {
        actor.requireAnyRole(Role.ADMINISTRATOR, Role.COMMERCIAL);
    }
}
