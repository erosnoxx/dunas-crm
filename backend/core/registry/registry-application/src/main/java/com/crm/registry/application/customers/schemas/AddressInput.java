package com.crm.registry.application.customers.schemas;

/**
 * Endereço como chega da requisição: tudo ainda é texto, o use case o converte em VOs.
 *
 * @param zipCode      cep, com ou sem traço
 * @param street       rua
 * @param number       número
 * @param complement   complemento; nulo = ausente
 * @param neighborhood bairro
 * @param city         cidade
 * @param state        nome exato da UF (SP, RJ...)
 */
public record AddressInput(
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state
) {
}
