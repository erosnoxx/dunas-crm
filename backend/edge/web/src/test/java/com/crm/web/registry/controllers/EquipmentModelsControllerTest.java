package com.crm.web.registry.controllers;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.NotFoundException;
import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.application.catalog.usecases.commands.DeactivateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.ReactivateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.RegisterEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.UpdateEquipmentModel;
import com.crm.registry.application.catalog.usecases.queries.GetEquipmentModel;
import com.crm.registry.application.catalog.usecases.queries.ListEquipmentModels;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.iam.attributes.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
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

@DisplayName("EquipmentModelsController")
class EquipmentModelsControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final UUID MANUFACTURER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID TYPE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID DEFINITION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");

    private final RegisterEquipmentModel registerEquipmentModel = mock(RegisterEquipmentModel.class);
    private final UpdateEquipmentModel updateEquipmentModel = mock(UpdateEquipmentModel.class);
    private final DeactivateEquipmentModel deactivateEquipmentModel = mock(DeactivateEquipmentModel.class);
    private final ReactivateEquipmentModel reactivateEquipmentModel = mock(ReactivateEquipmentModel.class);
    private final ListEquipmentModels listEquipmentModels = mock(ListEquipmentModels.class);
    private final GetEquipmentModel getEquipmentModel = mock(GetEquipmentModel.class);

    private final MockMvc mvc = IamMvc.standalone(new EquipmentModelsController(
            registerEquipmentModel,
            updateEquipmentModel,
            deactivateEquipmentModel,
            reactivateEquipmentModel,
            listEquipmentModels,
            getEquipmentModel
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

    private static EquipmentModelSummary summary() {
        return new EquipmentModelSummary(
                ID,
                "SUN2000-5KTL",
                CatalogStatus.ACTIVE,
                new EquipmentModelSummary.Ref(MANUFACTURER_ID, "Huawei"),
                new EquipmentModelSummary.Ref(TYPE_ID, "Inversor"),
                List.of(new EquipmentModelSummary.ValueView(DEFINITION_ID, "Potência", MeasurementUnit.KILOWATT, new BigDecimal("5.000")))
        );
    }

    @Test
    @DisplayName("cadastrar responde 201 com o id do modelo")
    void registerRespondsCreatedWithTheId() throws Exception {
        when(registerEquipmentModel.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/catalog/equipment-models"), """
                        {"manufacturerId": "%s", "typeId": "%s", "name": "SUN2000-5KTL",
                         "values": [{"definitionId": "%s", "amount": "5.000"}]}
                        """.formatted(MANUFACTURER_ID, TYPE_ID, DEFINITION_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(registerEquipmentModel).execute(new RegisterEquipmentModel.Params(
                ADMINISTRATOR,
                MANUFACTURER_ID,
                TYPE_ID,
                "SUN2000-5KTL",
                List.of(new ValueInput(DEFINITION_ID, "5.000"))
        ));
    }

    @Test
    @DisplayName("alterar responde 204 e o corpo não leva fabricante nem tipo")
    void updateRespondsNoContent() throws Exception {
        mvc.perform(json(put("/catalog/equipment-models/{id}", ID), """
                        {"name": "SUN2000-6KTL", "values": [{"definitionId": "%s", "amount": "6"}]}
                        """.formatted(DEFINITION_ID)))
                .andExpect(status().isNoContent());

        verify(updateEquipmentModel).execute(new UpdateEquipmentModel.Params(
                ADMINISTRATOR,
                ID,
                "SUN2000-6KTL",
                List.of(new ValueInput(DEFINITION_ID, "6"))
        ));
    }

    @Test
    @DisplayName("desativar e reativar respondem 204")
    void deactivateAndReactivateRespondNoContent() throws Exception {
        mvc.perform(withActor(post("/catalog/equipment-models/{id}/deactivation", ID)))
                .andExpect(status().isNoContent());
        mvc.perform(withActor(post("/catalog/equipment-models/{id}/reactivation", ID)))
                .andExpect(status().isNoContent());

        verify(deactivateEquipmentModel).execute(new DeactivateEquipmentModel.Params(ADMINISTRATOR, ID));
        verify(reactivateEquipmentModel).execute(new ReactivateEquipmentModel.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("lista passa página, tamanho e filtro ao use case")
    void listPassesPageSizeAndFilter() throws Exception {
        when(listEquipmentModels.execute(any())).thenReturn(new Page<>(List.of(summary()), 1, 10, 11, 2));

        mvc.perform(withActor(get("/catalog/equipment-models"))
                        .param("page", "1")
                        .param("size", "10")
                        .param("text", "sun")
                        .param("typeId", TYPE_ID.toString())
                        .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].manufacturer.name").value("Huawei"))
                .andExpect(jsonPath("$.content[0].values[0].amount").value(5.0))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(listEquipmentModels).execute(new ListEquipmentModels.Params(
                ADMINISTRATOR,
                new EquipmentModelFilter("sun", TYPE_ID, StatusFilter.ALL),
                Pageable.of(1, 10)
        ));
    }

    @Test
    @DisplayName("lista sem parâmetros usa 20 itens e o filtro ativo; size 500 vira 100")
    void listDefaultsToActiveAndCapsTheSize() throws Exception {
        when(listEquipmentModels.execute(any())).thenReturn(new Page<>(List.of(), 0, 20, 0, 0));

        mvc.perform(withActor(get("/catalog/equipment-models")).param("size", "500"))
                .andExpect(status().isOk());
        mvc.perform(withActor(get("/catalog/equipment-models")))
                .andExpect(status().isOk());

        var filter = new EquipmentModelFilter(null, null, StatusFilter.ACTIVE);

        verify(listEquipmentModels).execute(new ListEquipmentModels.Params(ADMINISTRATOR, filter, Pageable.of(0, 100)));
        verify(listEquipmentModels).execute(new ListEquipmentModels.Params(ADMINISTRATOR, filter, Pageable.of(0, 20)));
    }

    @Test
    @DisplayName("detalhe devolve o modelo")
    void detail() throws Exception {
        when(getEquipmentModel.execute(any())).thenReturn(summary());

        mvc.perform(withActor(get("/catalog/equipment-models/{id}", ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SUN2000-5KTL"))
                .andExpect(jsonPath("$.type.name").value("Inversor"))
                .andExpect(jsonPath("$.values[0].unit").value("KILOWATT"));

        verify(getEquipmentModel).execute(new GetEquipmentModel.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("detalhe de modelo inexistente mantém o código")
    void detailOfUnknownModelKeepsTheCode() throws Exception {
        when(getEquipmentModel.execute(any())).thenThrow(new NotFoundException("model_not_found", "modelo não encontrado"));

        mvc.perform(withActor(get("/catalog/equipment-models/{id}", ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("model_not_found"));
    }

    @Test
    @DisplayName("nome vazio e valores ausentes respondem 400 com invalidParams")
    void emptyNameIsA400WithInvalidParams() throws Exception {
        mvc.perform(json(post("/catalog/equipment-models"), """
                        {"manufacturerId": "%s", "typeId": "%s", "name": ""}
                        """.formatted(MANUFACTURER_ID, TYPE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'values')]").exists());

        mvc.perform(json(put("/catalog/equipment-models/{id}", ID), """
                        {"name": "", "values": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'name')]").exists());

        verifyNoInteractions(registerEquipmentModel, updateEquipmentModel);
    }

    @Test
    @DisplayName("valor nulo na lista e amount longo demais respondem 400 com invalidParams")
    void nullValueAndOversizedAmountAreA400() throws Exception {
        mvc.perform(json(post("/catalog/equipment-models"), """
                        {"manufacturerId": "%s", "typeId": "%s", "name": "X", "values": [null]}
                        """.formatted(MANUFACTURER_ID, TYPE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name =~ /values.*/)]").exists());

        mvc.perform(json(put("/catalog/equipment-models/{id}", ID), """
                        {"name": "X", "values": [null]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name =~ /values.*/)]").exists());

        mvc.perform(json(put("/catalog/equipment-models/{id}", ID), """
                        {"name": "X", "values": [{"definitionId": "%s", "amount": "%s"}]}
                        """.formatted(DEFINITION_ID, "1".repeat(33))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name =~ /values.*amount/)]").exists());

        verifyNoInteractions(registerEquipmentModel, updateEquipmentModel);
    }

    @Test
    @DisplayName("erro de domínio mantém o código")
    void domainErrorsKeepTheirCode() throws Exception {
        when(registerEquipmentModel.execute(any()))
                .thenThrow(new ConflictException("model_name_taken", "este fabricante já tem um modelo com esse nome"));

        mvc.perform(json(post("/catalog/equipment-models"), """
                        {"manufacturerId": "%s", "typeId": "%s", "name": "SUN2000-5KTL", "values": []}
                        """.formatted(MANUFACTURER_ID, TYPE_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("model_name_taken"));
    }

    @Test
    @DisplayName("sem ator responde 401")
    void withoutActorIsA401() throws Exception {
        mvc.perform(get("/catalog/equipment-models"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }
}
