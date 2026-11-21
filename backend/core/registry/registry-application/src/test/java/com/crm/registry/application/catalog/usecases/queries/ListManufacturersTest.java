package com.crm.registry.application.catalog.usecases.queries;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.contracts.readers.ManufacturerReader;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;
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

@DisplayName("ListManufacturers")
class ListManufacturersTest {

    private ManufacturerReader reader;
    private ListManufacturers listManufacturers;

    private final Pageable pageable = Pageable.of(0, 10, "name", "ASC");
    private final Page<ManufacturerSummary> page = new Page<>(List.of(), 0, 10, 0, 0);

    @BeforeEach
    void setUp() {
        reader = mock(ManufacturerReader.class);
        listManufacturers = new ListManufacturers(reader);
    }

    @Test
    @DisplayName("Técnico não escreve, mas consulta: os três papéis recebem a página do leitor")
    void technicianIsForbiddenToWriteButCanQuery() {
        var filter = new ManufacturerFilter("hua", StatusFilter.ALL);
        when(reader.search(filter, pageable)).thenReturn(page);

        for (var actor : List.of(ADMINISTRATOR, COMMERCIAL, TECHNICIAN))
            assertThat(listManufacturers.execute(new ListManufacturers.Params(actor, filter, pageable))).isSameAs(page);
    }

    @Test
    @DisplayName("filtro sem status chega ao leitor como ACTIVE")
    void defaultStatusFilterIsActive() {
        listManufacturers.execute(new ListManufacturers.Params(
                ADMINISTRATOR, new ManufacturerFilter("hua", null), pageable));

        verify(reader).search(new ManufacturerFilter("hua", StatusFilter.ACTIVE), pageable);
    }

    @Test
    @DisplayName("ator nulo é erro de programação e o leitor não é consultado")
    void aNullActorIsAProgrammingError() {
        assertThatThrownBy(() -> listManufacturers.execute(
                new ListManufacturers.Params(null, new ManufacturerFilter(null, null), pageable)))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(reader);
    }
}
