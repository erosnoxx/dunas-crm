package com.crm.registry.application.customers.usecases.queries;

import com.crm.common.application.annotations.UseCase;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.registry.application.customers.CustomerAccess;
import com.crm.registry.application.customers.contracts.services.ZipCodeLookupService;
import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.shared.application.Actor;
import com.crm.shared.domain.vos.address.ZipCode;
import lombok.RequiredArgsConstructor;

/** Preenche o endereço a partir do CEP (ADR-34). Só quem edita cadastra local, então só ele consulta. */
@UseCase(transactional = false)
@RequiredArgsConstructor
public class FindAddressByZipCode {

    private final ZipCodeLookupService lookup;

    /**
     * @param actor   quem consulta
     * @param zipCode o CEP, com ou sem máscara
     */
    public record Params(
            Actor actor,
            String zipCode
    ) {
    }

    public ZipCodeAddress execute(Params params) {
        CustomerAccess.requireEditor(params.actor());

        return lookup.find(ZipCode.of(params.zipCode()))
                .orElseThrow(() -> new NotFoundException("zip_code_not_found", "CEP não encontrado"));
    }
}
