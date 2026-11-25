package com.crm.registry.application.customers.schemas;

/**
 * O endereço de um CEP. {@code street} e {@code neighborhood} ficam {@code null} quando o CEP é
 * geral da cidade (sem logradouro nem bairro).
 *
 * @param zipCode      os 8 dígitos
 * @param street       logradouro, ou {@code null}
 * @param neighborhood bairro, ou {@code null}
 * @param city         cidade
 * @param state        UF, em maiúsculas
 */
public record ZipCodeAddress(
        String zipCode,
        String street,
        String neighborhood,
        String city,
        String state
) {
}
