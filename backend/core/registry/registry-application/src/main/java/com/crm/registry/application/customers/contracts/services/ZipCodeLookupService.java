package com.crm.registry.application.customers.contracts.services;

import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.shared.domain.vos.address.ZipCode;

import java.util.Optional;

/** Consulta o endereço de um CEP num sistema de terceiro (ADR-34). */
public interface ZipCodeLookupService {

    /**
     * @return o endereço, ou vazio se o CEP não existe
     * @throws com.crm.common.application.exceptions.ExternalServiceException se o terceiro não responde
     */
    Optional<ZipCodeAddress> find(ZipCode zipCode);
}
