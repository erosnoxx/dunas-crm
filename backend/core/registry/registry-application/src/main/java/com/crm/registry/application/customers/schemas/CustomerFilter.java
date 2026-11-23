package com.crm.registry.application.customers.schemas;

/**
 * Filtros da lista de clientes; texto nulo ou vazio não filtra e status nulo vale {@code ACTIVE}.
 *
 * @param text   trecho do nome, do nome fantasia, do documento, da cidade de uma localização ativa ou do número de série de um equipamento ativo
 * @param status status do cliente
 */
public record CustomerFilter(
        String text,
        CustomerStatusFilter status
) {

    public CustomerFilter {
        status = CustomerStatusFilter.orActive(status);
    }
}
