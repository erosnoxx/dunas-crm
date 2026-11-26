package com.crm.registry.application.customers.schemas;

/** Filtro de status da lista de clientes; próprio de {@code customers}, não reaproveita o do catálogo. */
public enum CustomerStatusFilter {
    ACTIVE,
    DEACTIVATED,
    ALL;

    /** Nulo vale {@link #ACTIVE}: a lista mostra só o que está em uso. */
    public static CustomerStatusFilter orActive(CustomerStatusFilter status) {
        return status == null ? ACTIVE : status;
    }
}
