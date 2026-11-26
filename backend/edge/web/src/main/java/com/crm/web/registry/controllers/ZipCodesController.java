package com.crm.web.registry.controllers;

import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.registry.application.customers.usecases.queries.FindAddressByZipCode;
import com.crm.shared.application.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de CEP para preencher o endereço de um local. A checagem de papel é do use case. */
@RestController
@RequestMapping("/zip-codes")
@RequiredArgsConstructor
public class ZipCodesController {

    private final FindAddressByZipCode findAddressByZipCode;

    @GetMapping("/{zipCode}")
    public ZipCodeAddress find(Actor actor, @PathVariable String zipCode) {
        return findAddressByZipCode.execute(new FindAddressByZipCode.Params(actor, zipCode));
    }
}
