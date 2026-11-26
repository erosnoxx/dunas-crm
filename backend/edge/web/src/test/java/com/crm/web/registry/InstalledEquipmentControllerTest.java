package com.crm.web.registry;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.UnprocessableEntityException;
import com.crm.registry.application.customers.usecases.commands.DeactivateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.InstallEquipment;
import com.crm.registry.application.customers.usecases.commands.ReactivateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.UpdateInstalledEquipment;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.iam.ActorRequestAttribute;
import com.crm.web.iam.IamMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
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

@DisplayName("InstalledEquipmentController")
class InstalledEquipmentControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final UUID LOCATION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID MODEL_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    private final InstallEquipment installEquipment = mock(InstallEquipment.class);
    private final UpdateInstalledEquipment updateInstalledEquipment = mock(UpdateInstalledEquipment.class);
    private final DeactivateInstalledEquipment deactivateInstalledEquipment = mock(DeactivateInstalledEquipment.class);
    private final ReactivateInstalledEquipment reactivateInstalledEquipment = mock(ReactivateInstalledEquipment.class);

    private final MockMvc mvc = IamMvc.standalone(new InstalledEquipmentController(
            installEquipment,
            updateInstalledEquipment,
            deactivateInstalledEquipment,
            reactivateInstalledEquipment
    ));

    private static MockHttpServletRequestBuilder withActor(MockHttpServletRequestBuilder builder) {
        return builder.requestAttr(ActorRequestAttribute.NAME, ADMINISTRATOR);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return withActor(builder).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String install(String date) {
        return "{\"modelId\": \"" + MODEL_ID + "\", \"serialNumber\": \"SN-1\", \"installedOn\": " + date + "}";
    }

    @Test
    @DisplayName("instalar responde 201 com o id e passa a data")
    void registerRespondsCreatedWithTheId() throws Exception {
        when(installEquipment.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/locations/{locationId}/equipment", LOCATION_ID), install("\"2026-03-15\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(installEquipment).execute(new InstallEquipment.Params(
                ADMINISTRATOR,
                LOCATION_ID,
                MODEL_ID,
                "SN-1",
                LocalDate.of(2026, 3, 15)
        ));
    }

    @Test
    @DisplayName("data nula ou ausente chega nula ao use case")
    void nullDateReachesTheUseCaseAsNull() throws Exception {
        when(installEquipment.execute(any())).thenReturn(new UUIDResponse(ID));

        mvc.perform(json(post("/locations/{locationId}/equipment", LOCATION_ID), install("null")))
                .andExpect(status().isCreated());
        mvc.perform(json(put("/installed-equipment/{id}", ID), "{\"serialNumber\": \"SN-2\"}"))
                .andExpect(status().isNoContent());

        verify(installEquipment).execute(new InstallEquipment.Params(
                ADMINISTRATOR,
                LOCATION_ID,
                MODEL_ID,
                "SN-1",
                null
        ));
        verify(updateInstalledEquipment).execute(new UpdateInstalledEquipment.Params(ADMINISTRATOR, ID, "SN-2", null));
    }

    @Test
    @DisplayName("alterar responde 204 e passa série e data")
    void updateRespondsNoContent() throws Exception {
        mvc.perform(json(put("/installed-equipment/{id}", ID), """
                        {"serialNumber": "SN-9", "installedOn": "2026-01-02"}
                        """))
                .andExpect(status().isNoContent());

        verify(updateInstalledEquipment).execute(new UpdateInstalledEquipment.Params(
                ADMINISTRATOR,
                ID,
                "SN-9",
                LocalDate.of(2026, 1, 2)
        ));
    }

    @Test
    @DisplayName("desativar e reativar respondem 204")
    void deactivateAndReactivateRespondNoContent() throws Exception {
        mvc.perform(withActor(post("/installed-equipment/{id}/deactivation", ID))).andExpect(status().isNoContent());
        mvc.perform(withActor(post("/installed-equipment/{id}/reactivation", ID))).andExpect(status().isNoContent());

        verify(deactivateInstalledEquipment).execute(new DeactivateInstalledEquipment.Params(ADMINISTRATOR, ID));
        verify(reactivateInstalledEquipment).execute(new ReactivateInstalledEquipment.Params(ADMINISTRATOR, ID));
    }

    @Test
    @DisplayName("erro de domínio mantém o código")
    void domainErrorsKeepTheirCode() throws Exception {
        doThrow(new ConflictException("serial_number_taken", "número de série em uso"))
                .when(updateInstalledEquipment).execute(any());
        mvc.perform(json(put("/installed-equipment/{id}", ID), "{\"serialNumber\": \"SN-2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("serial_number_taken"));

        when(installEquipment.execute(any()))
                .thenThrow(new UnprocessableEntityException("location_deactivated", "local desativado"));
        mvc.perform(json(post("/locations/{locationId}/equipment", LOCATION_ID), install("null")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("location_deactivated"));
    }

    @Test
    @DisplayName("modelo ausente ou série em branco respondem 400 com invalidParams")
    void missingModelOrBlankSerialIsA400() throws Exception {
        mvc.perform(json(post("/locations/{locationId}/equipment", LOCATION_ID), """
                        {"serialNumber": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'modelId')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.name == 'serialNumber')]").exists());

        verifyNoInteractions(installEquipment);
    }

    @Test
    @DisplayName("data inválida responde 400 corpo da requisição inválido")
    void invalidDateIsA400() throws Exception {
        mvc.perform(json(post("/locations/{locationId}/equipment", LOCATION_ID), install("\"2026-13-40\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("corpo da requisição inválido"));
        mvc.perform(json(put("/installed-equipment/{id}", ID), """
                        {"serialNumber": "S", "installedOn": "15/03/2026"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(installEquipment, updateInstalledEquipment);
    }

    @Test
    @DisplayName("sem ator responde 401")
    void withoutActorIsA401() throws Exception {
        mvc.perform(post("/installed-equipment/{id}/deactivation", ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("session_invalid"));
    }
}
