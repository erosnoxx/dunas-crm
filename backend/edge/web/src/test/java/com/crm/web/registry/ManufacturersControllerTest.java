package com.crm.web.registry;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.application.catalog.usecases.commands.DeactivateManufacturer;
import com.crm.registry.application.catalog.usecases.commands.ReactivateManufacturer;
import com.crm.registry.application.catalog.usecases.commands.RegisterManufacturer;
import com.crm.registry.application.catalog.usecases.commands.RenameManufacturer;
import com.crm.registry.application.catalog.usecases.queries.ListManufacturers;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.shared.application.schemas.response.UUIDResponse;
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

@DisplayName("ManufacturersController")
class ManufacturersControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final RegisterManufacturer registerManufacturer = mock(RegisterManufacturer.class);
    private final RenameManufacturer renameManufacturer = mock(RenameManufacturer.class);
    private final DeactivateManufacturer deactivateManufacturer = mock(DeactivateManufacturer.class);
    private final ReactivateManufacturer reactivateManufacturer = mock(ReactivateManufacturer.class);
    private final ListManufacturers listManufacturers = mock(ListManufacturers.class);

    private final MockMvc mvc = IamMvc.standalone(new ManufacturersController(
            registerManufacturer,
            renameManufacturer,
            deactivateManufacturer,
            reactivateManufacturer,
            listManufacturers
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
    @DisplayName("cadastrar responde 201 com o id do fabricante")
    void registerRespondsCreatedWithTheId() throws Exception {
        when(registerManufacturer.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/catalog/manufacturers"), """
                        {"name": "Huawei"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(registerManufacturer).execute(new RegisterManufacturer.Params(ADMINISTRATOR, "Huawei"));
    }

    @Test
    @DisplayName("renomear responde 204")
    void updateRespondsNoContent() throws Exception {
        mvc.perform(json(put("/catalog/manufacturers/{id}", ID), """
                        {"name": "Solis"}
                        """))
                .andExpect(status().isNoContent());

        verify(renameManufacturer).execute(new RenameManufacturer.Params(ADMINISTRATOR, ID, "Solis"));
    }

    @Test
    @DisplayName("desativar e reativar respondem 204")
    void deactivateAndReactivateRespondNoContent() throws Exception {
        mvc.perform(withActor(post("/catalog/manufacturers/{id}/deactivation", ID)))
                .andExpect(status().isNoContent());
        mvc.perform(withActor(post("/catalog/manufacturers/{id}/reactivation", ID)))
                .andExpect(status().isNoContent());

        verify(deactivateManufacturer).execute(new DeactivateManufacturer.Params(ADMINISTRATOR, ID));
        verify(reactivateManufacturer).execute(new ReactivateManufacturer.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("lista passa página, tamanho e filtro ao use case")
    void listPassesPageSizeAndFilter() throws Exception {
        when(listManufacturers.execute(any())).thenReturn(new Page<>(
                List.of(new ManufacturerSummary(ID, "Huawei", CatalogStatus.ACTIVE, 3)),
                1, 10, 11, 2
        ));

        mvc.perform(withActor(get("/catalog/manufacturers"))
                        .param("page", "1")
                        .param("size", "10")
                        .param("text", "hua")
                        .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Huawei"))
                .andExpect(jsonPath("$.content[0].modelCount").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(listManufacturers).execute(new ListManufacturers.Params(
                ADMINISTRATOR,
                new ManufacturerFilter("hua", StatusFilter.ALL),
                Pageable.of(1, 10)
        ));
    }

    @Test
    @DisplayName("lista sem parâmetros usa 20 itens e o filtro ativo; size 500 vira 100")
    void listDefaultsToActiveAndCapsTheSize() throws Exception {
        when(listManufacturers.execute(any())).thenReturn(new Page<>(List.of(), 0, 20, 0, 0));

        mvc.perform(withActor(get("/catalog/manufacturers")).param("size", "500"))
                .andExpect(status().isOk());
        mvc.perform(withActor(get("/catalog/manufacturers")))
                .andExpect(status().isOk());

        verify(listManufacturers).execute(new ListManufacturers.Params(
                ADMINISTRATOR,
                new ManufacturerFilter(null, StatusFilter.ACTIVE),
                Pageable.of(0, 100)
        ));
        verify(listManufacturers).execute(new ListManufacturers.Params(
                ADMINISTRATOR,
                new ManufacturerFilter(null, StatusFilter.ACTIVE),
                Pageable.of(0, 20)
        ));
    }

    @Test
    @DisplayName("nome vazio responde 400 com invalidParams")
    void emptyNameIsA400WithInvalidParams() throws Exception {
        mvc.perform(json(post("/catalog/manufacturers"), """
                        {"name": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists());

        verifyNoInteractions(registerManufacturer);
    }

    @Test
    @DisplayName("erro de domínio mantém o código")
    void domainErrorsKeepTheirCode() throws Exception {
        when(registerManufacturer.execute(any()))
                .thenThrow(new ConflictException("manufacturer_name_taken", "já existe um fabricante com este nome"));

        mvc.perform(json(post("/catalog/manufacturers"), """
                        {"name": "Huawei"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("manufacturer_name_taken"));
    }

    @Test
    @DisplayName("sem ator responde 401")
    void withoutActorIsA401() throws Exception {
        mvc.perform(get("/catalog/manufacturers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }
}
