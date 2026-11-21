package com.crm.registry.application.catalog.schemas;

/** Filtro de status das listas do catálogo; fica na application para a web ligar o parâmetro sem ver o domínio. */
public enum StatusFilter {
    ACTIVE,
    DEACTIVATED,
    ALL;

    /** Nulo vale {@link #ACTIVE}: a lista do catálogo mostra só o que está em uso. */
    public static StatusFilter orActive(StatusFilter status) {
        return status == null ? ACTIVE : status;
    }
}
