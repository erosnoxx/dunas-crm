package com.crm.registry.domain.catalog.vos;

import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.enums.MeasurementUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enums do catálogo")
class EnumsTest {

    @Test
    @DisplayName("MeasurementUnit tem as quatro unidades da spec")
    void measurementUnits() {
        assertThat(MeasurementUnit.values())
                .containsExactly(
                        MeasurementUnit.KILOWATT,
                        MeasurementUnit.WATT,
                        MeasurementUnit.WATT_PEAK,
                        MeasurementUnit.BTU_PER_HOUR
                );
    }

    @Test
    @DisplayName("CatalogStatus tem ativo e desativado")
    void catalogStatuses() {
        assertThat(CatalogStatus.values()).containsExactly(CatalogStatus.ACTIVE, CatalogStatus.DEACTIVATED);
    }
}
