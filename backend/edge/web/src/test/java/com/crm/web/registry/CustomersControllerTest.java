package com.crm.web.registry;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerStatusFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;
import com.crm.registry.application.customers.usecases.commands.DeactivateCustomer;
import com.crm.registry.application.customers.usecases.commands.ReactivateCustomer;
import com.crm.registry.application.customers.usecases.commands.RegisterCustomer;
import com.crm.registry.application.customers.usecases.commands.UpdateCustomer;
import com.crm.registry.application.customers.usecases.queries.GetCustomer;
import com.crm.registry.application.customers.usecases.queries.ListCustomers;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.shared.domain.vos.identity.Document;
import com.crm.web.iam.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static com.crm.web.iam.IamMvc.ADMINISTRATOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CustomersController")
class CustomersControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private final RegisterCustomer registerCustomer = mock(RegisterCustomer.class);
    private final UpdateCustomer updateCustomer = mock(UpdateCustomer.class);
    private final DeactivateCustomer deactivateCustomer = mock(DeactivateCustomer.class);
    private final ReactivateCustomer reactivateCustomer = mock(ReactivateCustomer.class);
    private final ListCustomers listCustomers = mock(ListCustomers.class);
    private final GetCustomer getCustomer = mock(GetCustomer.class);

    private final MockMvc mvc = IamMvc.standalone(new CustomersController(
            registerCustomer,
            updateCustomer,
            deactivateCustomer,
            reactivateCustomer,
            listCustomers,
            getCustomer
    ));

    private static MockHttpServletRequestBuilder withActor(MockHttpServletRequestBuilder builder) {
        return builder.requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR);
    }

    private static MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder builder,
            String body
    ) {
        return withActor(builder).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("cadastrar responde 201 com o id e passa os cinco campos, opcionais nulos")
    void registerRespondsCreatedWithTheId() throws Exception {
        when(registerCustomer.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/customers"), """
                        {"document": "529.982.247-25", "name": "Maria Silva"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(registerCustomer).execute(new RegisterCustomer.Params(
                ADMINISTRATOR,
                "529.982.247-25",
                "Maria Silva",
                null,
                null,
                null
        ));
    }

    @Test
    @DisplayName("cadastrar leva nome fantasia, e-mail e telefone ao use case")
    void registerPassesTheOptionalFields() throws Exception {
        when(registerCustomer.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/customers"), """
                        {"document": "11222333000181", "name": "Solar Ltda", "tradeName": "Solar",
                         "email": "a@b.com", "phone": "11999998888"}
                        """))
                .andExpect(status().isCreated());

        verify(registerCustomer).execute(new RegisterCustomer.Params(
                ADMINISTRATOR,
                "11222333000181",
                "Solar Ltda",
                "Solar",
                "a@b.com",
                "11999998888"
        ));
    }

    @Test
    @DisplayName("alterar responde 204 e ignora um document no corpo")
    void updateRespondsNoContentAndIgnoresADocumentInTheBody() throws Exception {
        mvc.perform(json(put("/customers/{id}", ID), """
                        {"document": "00000000000", "name": "Maria Souza", "email": "m@x.com"}
                        """))
                .andExpect(status().isNoContent());

        verify(updateCustomer).execute(new UpdateCustomer.Params(
                ADMINISTRATOR,
                ID,
                "Maria Souza",
                null,
                "m@x.com",
                null
        ));
    }

    @Test
    @DisplayName("desativar e reativar respondem 204")
    void deactivateAndReactivateRespondNoContent() throws Exception {
        mvc.perform(withActor(post("/customers/{id}/deactivation", ID)))
                .andExpect(status().isNoContent());
        mvc.perform(withActor(post("/customers/{id}/reactivation", ID)))
                .andExpect(status().isNoContent());

        verify(deactivateCustomer).execute(new DeactivateCustomer.Params(ADMINISTRATOR, ID));
        verify(reactivateCustomer).execute(new ReactivateCustomer.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("lista passa texto, status, página e tamanho; size 500 vira 100 e size 0 vira 1")
    void listPassesTextStatusPageAndSize() throws Exception {
        when(listCustomers.execute(any())).thenReturn(new Page<CustomerSummary>(List.of(), 0, 20, 0, 0));

        mvc.perform(withActor(get("/customers"))
                        .param("text", "maria")
                        .param("status", "ALL")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());
        mvc.perform(withActor(get("/customers")).param("size", "500")).andExpect(status().isOk());
        mvc.perform(withActor(get("/customers")).param("size", "0")).andExpect(status().isOk());
        mvc.perform(withActor(get("/customers"))).andExpect(status().isOk());

        verify(listCustomers).execute(new ListCustomers.Params(
                ADMINISTRATOR,
                new CustomerFilter("maria", CustomerStatusFilter.ALL),
                Pageable.of(2, 10)
        ));
        var active = new CustomerFilter(null, CustomerStatusFilter.ACTIVE);
        verify(listCustomers).execute(new ListCustomers.Params(ADMINISTRATOR, active, Pageable.of(0, 100)));
        verify(listCustomers).execute(new ListCustomers.Params(ADMINISTRATOR, active, Pageable.of(0, 1)));
        verify(listCustomers).execute(new ListCustomers.Params(ADMINISTRATOR, active, Pageable.of(0, 20)));
    }

    @Test
    @DisplayName("status desconhecido responde 400")
    void unknownStatusIsA400() throws Exception {
        mvc.perform(withActor(get("/customers")).param("status", "FOO"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listCustomers);
    }

    @Test
    @DisplayName("detalhe devolve o cliente")
    void detailRespondsTheDetail() throws Exception {
        when(getCustomer.execute(any())).thenReturn(new CustomerDetail(
                ID,
                "52998224725",
                Document.DocumentType.CPF,
                "Maria Silva",
                null,
                null,
                null,
                CustomerStatus.ACTIVE,
                List.of()
        ));

        mvc.perform(withActor(get("/customers/{id}", ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.documentType").value("CPF"));

        verify(getCustomer).execute(new GetCustomer.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("nome vazio ou documento ausente respondem 400 com invalidParams")
    void emptyNameIsA400WithInvalidParams() throws Exception {
        mvc.perform(json(post("/customers"), """
                        {"name": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'document')]").exists());

        mvc.perform(json(put("/customers/{id}", ID), """
                        {"name": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists());

        verifyNoInteractions(registerCustomer, updateCustomer);
    }

    @Test
    @DisplayName("nome com 151 caracteres responde 400")
    void nameWith151CharactersIsA400() throws Exception {
        mvc.perform(json(post("/customers"), """
                        {"document": "52998224725", "name": "%s"}
                        """.formatted("a".repeat(151))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists());

        verifyNoInteractions(registerCustomer);
    }

    @Test
    @DisplayName("erro de domínio mantém o código")
    void domainErrorsKeepTheirCode() throws Exception {
        when(registerCustomer.execute(any()))
                .thenThrow(new ConflictException("customer_document_taken", "já existe um cliente com este documento"));

        mvc.perform(json(post("/customers"), """
                        {"document": "52998224725", "name": "Maria Silva"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("customer_document_taken"));
    }

    @Test
    @DisplayName("sem ator responde 401")
    void withoutActorIsA401() throws Exception {
        mvc.perform(get("/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }
}
