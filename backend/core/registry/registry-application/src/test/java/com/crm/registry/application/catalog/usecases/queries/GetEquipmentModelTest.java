package com.crm.registry.application.catalog.usecases.queries;

import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("GetEquipmentModel")
class GetEquipmentModelTest {

    private EquipmentModelReader reader;
    private GetEquipmentModel getEquipmentModel;

    private final EquipmentModelSummary summary = new EquipmentModelSummary(
            UUID.randomUUID(),
            "SUN2000",
            CatalogStatus.ACTIVE,
            new EquipmentModelSummary.Ref(UUID.randomUUID(), "Huawei"),
            new EquipmentModelSummary.Ref(UUID.randomUUID(), "Inversor"),
            List.of()
    );

    @BeforeEach
    void setUp() {
        reader = mock(EquipmentModelReader.class);
        getEquipmentModel = new GetEquipmentModel(reader);
    }

    @Test
    @DisplayName("Técnico não escreve, mas consulta: os três papéis recebem o resumo do leitor")
    void technicianIsForbiddenToWriteButCanQuery() {
        when(reader.findSummary(summary.id())).thenReturn(Optional.of(summary));

        for (var actor : List.of(ADMINISTRATOR, COMMERCIAL, TECHNICIAN))
            assertThat(getEquipmentModel.execute(new GetEquipmentModel.Params(actor, summary.id()))).isSameAs(summary);
    }

    @Test
    @DisplayName("modelo que não existe é model_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(reader.findSummary(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getEquipmentModel.execute(new GetEquipmentModel.Params(TECHNICIAN, unknown)))
                .satisfies(applicationCode("model_not_found"));
    }

    @Test
    @DisplayName("ator nulo é erro de programação e o leitor não é consultado")
    void aNullActorIsAProgrammingError() {
        assertThatThrownBy(() -> getEquipmentModel.execute(new GetEquipmentModel.Params(null, summary.id())))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(reader);
    }
}
