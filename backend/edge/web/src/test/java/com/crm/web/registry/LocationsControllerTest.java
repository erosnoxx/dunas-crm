package com.crm.web.registry;

import com.crm.common.application.exceptions.ConcurrentUpdateException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.registry.application.customers.schemas.AddressInput;
import com.crm.registry.application.customers.usecases.commands.DeactivateLocation;
import com.crm.registry.application.customers.usecases.commands.ReactivateLocation;
import com.crm.registry.application.customers.usecases.commands.RegisterLocation;
import com.crm.registry.application.customers.usecases.commands.UpdateLocation;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.iam.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static com.crm.web.iam.IamMvc.ADMINISTRATOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LocationsController")
class LocationsControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private static final String ADDRESS = """
            {"zipCode": "01310-100", "street": "Av. Paulista", "number": "1000",
             "complement": "sala 2", "neighborhood": "Bela Vista", "city": "São Paulo", "state": "SP"}
            """;

    private static final AddressInput ADDRESS_INPUT = new AddressInput(
            "01310-100",
            "Av. Paulista",
            "1000",
            "sala 2",
            "Bela Vista",
            "São Paulo",
            "SP"
    );

    private final RegisterLocation registerLocation = mock(RegisterLocation.class);
    private final UpdateLocation updateLocation = mock(UpdateLocation.class);
    private final DeactivateLocation deactivateLocation = mock(DeactivateLocation.class);
    private final ReactivateLocation reactivateLocation = mock(ReactivateLocation.class);

    private final MockMvc mvc = IamMvc.standalone(new LocationsController(
            registerLocation,
            updateLocation,
            deactivateLocation,
            reactivateLocation
    ));

    private static MockHttpServletRequestBuilder withActor(MockHttpServletRequestBuilder builder) {
        return builder.requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return withActor(builder).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String body(String address) {
        return "{\"name\": \"Matriz\", \"address\": " + address + "}";
    }

    @Test
    @DisplayName("cadastrar responde 201 com o id e converte o endereço")
    void registerRespondsCreatedWithTheId() throws Exception {
        when(registerLocation.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/customers/{customerId}/locations", CUSTOMER_ID), body(ADDRESS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(registerLocation).execute(new RegisterLocation.Params(
                ADMINISTRATOR,
                CUSTOMER_ID,
                "Matriz",
                ADDRESS_INPUT
        ));
    }

    @Test
    @DisplayName("complemento ausente chega nulo ao use case")
    void absentComplementReachesTheUseCaseAsNull() throws Exception {
        when(registerLocation.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/customers/{customerId}/locations", CUSTOMER_ID), body("""
                        {"zipCode": "01310100", "street": "Rua A", "number": "S/N",
                         "neighborhood": "Centro", "city": "Recife", "state": "PE"}
                        """)))
                .andExpect(status().isCreated());

        verify(registerLocation).execute(new RegisterLocation.Params(
                ADMINISTRATOR,
                CUSTOMER_ID,
                "Matriz",
                new AddressInput("01310100", "Rua A", "S/N", null, "Centro", "Recife", "PE")
        ));
    }

    @Test
    @DisplayName("alterar responde 204 e passa nome e endereço")
    void updateRespondsNoContent() throws Exception {
        mvc.perform(json(put("/locations/{id}", ID), body(ADDRESS)))
                .andExpect(status().isNoContent());

        verify(updateLocation).execute(new UpdateLocation.Params(ADMINISTRATOR, ID, "Matriz", ADDRESS_INPUT));
    }

    @Test
    @DisplayName("desativar e reativar respondem 204")
    void deactivateAndReactivateRespondNoContent() throws Exception {
        mvc.perform(withActor(post("/locations/{id}/deactivation", ID))).andExpect(status().isNoContent());
        mvc.perform(withActor(post("/locations/{id}/reactivation", ID))).andExpect(status().isNoContent());

        verify(deactivateLocation).execute(new DeactivateLocation.Params(ADMINISTRATOR, ID));
        verify(reactivateLocation).execute(new ReactivateLocation.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("erro de domínio mantém o código")
    void domainErrorsKeepTheirCode() throws Exception {
        when(registerLocation.execute(any()))
                .thenThrow(new UnprocessableEntityException("customer_deactivated", "cliente desativado"));
        mvc.perform(json(post("/customers/{customerId}/locations", CUSTOMER_ID), body(ADDRESS)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("customer_deactivated"));

        doThrow(new ConcurrentUpdateException())
                .when(updateLocation).execute(any());
        mvc.perform(json(put("/locations/{id}", ID), body(ADDRESS)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("concurrent_update"));
    }

    @Test
    @DisplayName("endereço ausente responde 400 com invalidParams")
    void addressMissingIsA400WithInvalidParams() throws Exception {
        mvc.perform(json(post("/customers/{customerId}/locations", CUSTOMER_ID), """
                        {"name": "Matriz"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'address')]").exists());

        verifyNoInteractions(registerLocation);
    }

    @Test
    @DisplayName("UF com 3 letras responde 400")
    void stateWith3LettersIsA400() throws Exception {
        mvc.perform(json(post("/customers/{customerId}/locations", CUSTOMER_ID),
                        body(ADDRESS.replace("\"SP\"", "\"SPP\""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'address.state')]").exists());

        verifyNoInteractions(registerLocation);
    }

    @Test
    @DisplayName("CEP com 10 caracteres responde 400")
    void zipCodeWith10CharactersIsA400() throws Exception {
        mvc.perform(json(post("/customers/{customerId}/locations", CUSTOMER_ID),
                        body(ADDRESS.replace("01310-100", "01310-1000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'address.zipCode')]").exists());

        verifyNoInteractions(registerLocation);
    }

    @Test
    @DisplayName("sem ator responde 401")
    void withoutActorIsA401() throws Exception {
        mvc.perform(post("/locations/{id}/deactivation", ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }
}
