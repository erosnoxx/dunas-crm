package com.crm.registry.application.customers.usecases.queries;

import com.crm.registry.application.customers.contracts.services.ZipCodeLookupService;
import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.shared.domain.vos.address.ZipCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("FindAddressByZipCode")
class FindAddressByZipCodeTest {

    private final ZipCodeAddress address =
            new ZipCodeAddress("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP");

    private ZipCodeLookupService lookup;
    private FindAddressByZipCode findAddress;

    @BeforeEach
    void setUp() {
        lookup = mock(ZipCodeLookupService.class);
        findAddress = new FindAddressByZipCode(lookup);
    }

    @Test
    @DisplayName("Administrador e Comercial recebem o endereço, e a máscara do CEP é aceita")
    void editorsGetTheAddress() {
        when(lookup.find(ZipCode.of("01310100"))).thenReturn(Optional.of(address));

        assertThat(findAddress.execute(new FindAddressByZipCode.Params(ADMINISTRATOR, "01310-100"))).isSameAs(address);
        assertThat(findAddress.execute(new FindAddressByZipCode.Params(COMMERCIAL, "01310100"))).isSameAs(address);
    }

    @Test
    @DisplayName("CEP que não existe é zip_code_not_found")
    void unknownZipCode() {
        when(lookup.find(ZipCode.of("99999999"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> findAddress.execute(new FindAddressByZipCode.Params(ADMINISTRATOR, "99999-999")))
                .satisfies(applicationCode("zip_code_not_found"));
    }

    @Test
    @DisplayName("Técnico é recusado antes de consultar o terceiro")
    void technicianIsRefused() {
        assertThatThrownBy(() -> findAddress.execute(new FindAddressByZipCode.Params(TECHNICIAN, "01310100")))
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(lookup);
    }

    @Test
    @DisplayName("CEP com formato inválido é 400, sem consultar o terceiro")
    void malformedZipCode() {
        assertThatThrownBy(() -> findAddress.execute(new FindAddressByZipCode.Params(ADMINISTRATOR, "123")))
                .isInstanceOf(com.crm.common.domain.exceptions.ValidationException.class);

        verifyNoInteractions(lookup);
    }
}
