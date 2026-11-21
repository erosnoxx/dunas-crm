package com.crm.registry.application.catalog;

import com.crm.shared.application.Actor;
import com.crm.shared.domain.enums.Role;

import java.util.Objects;

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

    /** A consulta é aberta aos três papéis, mas o ator é exigido: nulo é erro de programação, não de negócio. */
    public static void requireReader(Actor actor) {
        Objects.requireNonNull(actor, "o ator é obrigatório");
    }
}
