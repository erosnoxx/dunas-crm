package com.crm.web.registry.controllers;

import com.crm.registry.application.customers.schemas.ZipCodeAddress;
import com.crm.registry.application.customers.usecases.queries.FindAddressByZipCode;
import com.crm.web.iam.attributes.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static com.crm.web.iam.IamMvc.ADMINISTRATOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ZipCodesController")
class ZipCodesControllerTest {

    private final FindAddressByZipCode findAddress = mock(FindAddressByZipCode.class);
    private final MockMvc mvc = IamMvc.standalone(new ZipCodesController(findAddress));

    @Test
    @DisplayName("responde 200 com o endereço e passa o CEP como veio")
    void answersTheAddress() throws Exception {
        when(findAddress.execute(any())).thenReturn(
                new ZipCodeAddress("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP"));

        mvc.perform(get("/zip-codes/01310-100").requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zipCode").value("01310100"))
                .andExpect(jsonPath("$.street").value("Avenida Paulista"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.state").value("SP"));
    }
}
