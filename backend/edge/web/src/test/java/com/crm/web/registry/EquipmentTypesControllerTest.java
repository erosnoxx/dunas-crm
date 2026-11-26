package com.crm.web.registry;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.application.catalog.usecases.commands.DeactivateEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.ReactivateEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.RegisterEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.UpdateEquipmentType;
import com.crm.registry.application.catalog.usecases.queries.ListEquipmentTypes;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
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

@DisplayName("EquipmentTypesController")
class EquipmentTypesControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final String BODY = """
            {"name": "Inversor", "characteristic": {"name": "Potência", "unit": "KILOWATT", "required": true}}
            """;

    private final RegisterEquipmentType registerEquipmentType = mock(RegisterEquipmentType.class);
    private final UpdateEquipmentType updateEquipmentType = mock(UpdateEquipmentType.class);
    private final DeactivateEquipmentType deactivateEquipmentType = mock(DeactivateEquipmentType.class);
    private final ReactivateEquipmentType reactivateEquipmentType = mock(ReactivateEquipmentType.class);
    private final ListEquipmentTypes listEquipmentTypes = mock(ListEquipmentTypes.class);

    private final MockMvc mvc = IamMvc.standalone(new EquipmentTypesController(
            registerEquipmentType,
            updateEquipmentType,
            deactivateEquipmentType,
            reactivateEquipmentType,
            listEquipmentTypes
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
    @DisplayName("cadastrar responde 201 com o id do tipo")
    void registerRespondsCreatedWithTheId() throws Exception {
        when(registerEquipmentType.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/catalog/equipment-types"), BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(registerEquipmentType).execute(new RegisterEquipmentType.Params(
                ADMINISTRATOR,
                "Inversor",
                new CharacteristicInput("Potência", "KILOWATT", true)
        ));
    }

    @Test
    @DisplayName("alterar responde 204")
    void updateRespondsNoContent() throws Exception {
        mvc.perform(json(put("/catalog/equipment-types/{id}", ID), BODY))
                .andExpect(status().isNoContent());

        verify(updateEquipmentType).execute(new UpdateEquipmentType.Params(
                ADMINISTRATOR,
                ID,
                "Inversor",
                new CharacteristicInput("Potência", "KILOWATT", true)
        ));
    }

    @Test
    @DisplayName("desativar e reativar respondem 204")
    void deactivateAndReactivateRespondNoContent() throws Exception {
        mvc.perform(withActor(post("/catalog/equipment-types/{id}/deactivation", ID)))
                .andExpect(status().isNoContent());
        mvc.perform(withActor(post("/catalog/equipment-types/{id}/reactivation", ID)))
                .andExpect(status().isNoContent());

        verify(deactivateEquipmentType).execute(new DeactivateEquipmentType.Params(ADMINISTRATOR, ID));
        verify(reactivateEquipmentType).execute(new ReactivateEquipmentType.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("lista passa página, tamanho e status ao use case")
    void listPassesPageSizeAndFilter() throws Exception {
        var characteristic = new EquipmentTypeSummary.CharacteristicView(ID, "Potência", MeasurementUnit.KILOWATT, true);

        when(listEquipmentTypes.execute(any())).thenReturn(new Page<>(
                List.of(new EquipmentTypeSummary(ID, "Inversor", CatalogStatus.ACTIVE, 2, List.of(characteristic))),
                1, 10, 11, 2
        ));

        mvc.perform(withActor(get("/catalog/equipment-types"))
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "DEACTIVATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].characteristics[0].unit").value("KILOWATT"))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(listEquipmentTypes).execute(new ListEquipmentTypes.Params(
                ADMINISTRATOR,
                StatusFilter.DEACTIVATED,
                Pageable.of(1, 10)
        ));
    }

    @Test
    @DisplayName("lista sem parâmetros usa 20 itens e deixa o status para o use case; size 500 vira 100")
    void listDefaultsAndCapsTheSize() throws Exception {
        when(listEquipmentTypes.execute(any())).thenReturn(new Page<>(List.of(), 0, 20, 0, 0));

        mvc.perform(withActor(get("/catalog/equipment-types")).param("size", "500"))
                .andExpect(status().isOk());
        mvc.perform(withActor(get("/catalog/equipment-types")))
                .andExpect(status().isOk());

        verify(listEquipmentTypes).execute(new ListEquipmentTypes.Params(ADMINISTRATOR, null, Pageable.of(0, 100)));
        verify(listEquipmentTypes).execute(new ListEquipmentTypes.Params(ADMINISTRATOR, null, Pageable.of(0, 20)));
    }

    @Test
    @DisplayName("nome vazio e característica ausente respondem 400 com invalidParams")
    void emptyNameIsA400WithInvalidParams() throws Exception {
        mvc.perform(json(post("/catalog/equipment-types"), """
                        {"name": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'characteristic')]").exists());

        mvc.perform(json(post("/catalog/equipment-types"), """
                        {"name": "Inversor", "characteristic": {"name": "", "unit": "KILOWATT", "required": true}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'characteristic.name')]").exists());

        verifyNoInteractions(registerEquipmentType);
    }

    @Test
    @DisplayName("característica sem required responde 400 com invalidParams")
    void missingRequiredIsA400WithInvalidParams() throws Exception {
        mvc.perform(json(put("/catalog/equipment-types/{id}", ID), """
                        {"name": "Inversor", "characteristic": {"name": "Potência", "unit": "KILOWATT"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'characteristic.required')]").exists());

        mvc.perform(json(put("/catalog/equipment-types/{id}", ID), """
                        {"name": "Inversor", "characteristic": {"name": "Potência", "unit": "KILOWATT", "required": null}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'characteristic.required')]").exists());

        verifyNoInteractions(updateEquipmentType);
    }

    @Test
    @DisplayName("erro de domínio mantém o código")
    void domainErrorsKeepTheirCode() throws Exception {
        when(registerEquipmentType.execute(any()))
                .thenThrow(new ConflictException("type_name_taken", "já existe um tipo com este nome"));

        mvc.perform(json(post("/catalog/equipment-types"), BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("type_name_taken"));
    }

    @Test
    @DisplayName("sem ator responde 401")
    void withoutActorIsA401() throws Exception {
        mvc.perform(get("/catalog/equipment-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }
}
