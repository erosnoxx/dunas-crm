package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.contracts.readers.EquipmentTypeReader;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ListEquipmentTypes")
class ListEquipmentTypesTest {

    private EquipmentTypeReader reader;
    private ListEquipmentTypes listEquipmentTypes;

    private final Pageable pageable = Pageable.of(0, 10, "name", "ASC");
    private final Page<EquipmentTypeSummary> page = new Page<>(List.of(), 0, 10, 0, 0);

    @BeforeEach
    void setUp() {
        reader = mock(EquipmentTypeReader.class);
        listEquipmentTypes = new ListEquipmentTypes(reader);
    }

    @Test
    @DisplayName("Técnico não escreve, mas consulta: os três papéis recebem a página do leitor")
    void technicianIsForbiddenToWriteButCanQuery() {
        when(reader.search(StatusFilter.DEACTIVATED, pageable)).thenReturn(page);

        for (var actor : List.of(ADMINISTRATOR, COMMERCIAL, TECHNICIAN))
            assertThat(listEquipmentTypes.execute(
                    new ListEquipmentTypes.Params(actor, StatusFilter.DEACTIVATED, pageable))).isSameAs(page);
    }

    @Test
    @DisplayName("status nulo chega ao leitor como ACTIVE")
    void defaultStatusFilterIsActive() {
        listEquipmentTypes.execute(new ListEquipmentTypes.Params(ADMINISTRATOR, null, pageable));

        verify(reader).search(StatusFilter.ACTIVE, pageable);
    }

    @Test
    @DisplayName("ator nulo é erro de programação e o leitor não é consultado")
    void aNullActorIsAProgrammingError() {
        assertThatThrownBy(() -> listEquipmentTypes.execute(
                new ListEquipmentTypes.Params(null, StatusFilter.ALL, pageable)))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(reader);
    }
}
