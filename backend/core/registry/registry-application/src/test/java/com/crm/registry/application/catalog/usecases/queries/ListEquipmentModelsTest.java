package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.contracts.readers.EquipmentModelReader;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ListEquipmentModels")
class ListEquipmentModelsTest {

    private EquipmentModelReader reader;
    private ListEquipmentModels listEquipmentModels;

    private final Pageable pageable = Pageable.of(0, 10, "name", "ASC");
    private final Page<EquipmentModelSummary> page = new Page<>(List.of(), 0, 10, 0, 0);
    private final UUID typeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reader = mock(EquipmentModelReader.class);
        listEquipmentModels = new ListEquipmentModels(reader);
    }

    @Test
    @DisplayName("Técnico não escreve, mas consulta: os três papéis recebem a página do leitor")
    void technicianIsForbiddenToWriteButCanQuery() {
        var filter = new EquipmentModelFilter("sun", typeId, StatusFilter.ALL);
        when(reader.search(filter, pageable)).thenReturn(page);

        for (var actor : List.of(ADMINISTRATOR, COMMERCIAL, TECHNICIAN))
            assertThat(listEquipmentModels.execute(new ListEquipmentModels.Params(actor, filter, pageable)))
                    .isSameAs(page);
    }

    @Test
    @DisplayName("filtro sem status chega ao leitor como ACTIVE")
    void defaultStatusFilterIsActive() {
        listEquipmentModels.execute(new ListEquipmentModels.Params(
                ADMINISTRATOR, new EquipmentModelFilter("sun", typeId, null), pageable));

        verify(reader).search(new EquipmentModelFilter("sun", typeId, StatusFilter.ACTIVE), pageable);
    }

    @Test
    @DisplayName("ator nulo é erro de programação e o leitor não é consultado")
    void aNullActorIsAProgrammingError() {
        assertThatThrownBy(() -> listEquipmentModels.execute(
                new ListEquipmentModels.Params(null, new EquipmentModelFilter(null, null, null), pageable)))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(reader);
    }
}
