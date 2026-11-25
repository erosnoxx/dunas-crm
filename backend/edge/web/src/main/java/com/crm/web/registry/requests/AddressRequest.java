package com.crm.web.registry.requests;

import com.crm.registry.application.customers.schemas.AddressInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Endereço no corpo de local; o controller o converte em {@link AddressInput}. */
public record AddressRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 9, message = "deve ter no máximo 9 caracteres") String zipCode,
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 150, message = "deve ter de 2 a 150 caracteres") String street,
        @NotBlank(message = "é obrigatório") @Size(max = 15, message = "deve ter no máximo 15 caracteres") String number,
        @Size(min = 1, max = 50, message = "deve ter de 1 a 50 caracteres") String complement,
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 100, message = "deve ter de 2 a 100 caracteres") String neighborhood,
        @NotBlank(message = "é obrigatório") @Size(max = 100, message = "deve ter no máximo 100 caracteres") String city,
        @NotBlank(message = "é obrigatório") @Size(min = 2, max = 2, message = "deve ter 2 caracteres") String state
) {

    /** Converte para a entrada do use case. */
    public AddressInput toInput() {
        return new AddressInput(
                zipCode,
                street,
                number,
                complement,
                neighborhood,
                city,
                state
        );
    }
}
