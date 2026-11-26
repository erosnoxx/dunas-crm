package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Manufacturer")
class ManufacturerTest {

    private static final ManufacturerName HUAWEI = ManufacturerName.of("Huawei");

    private static void assertFailsWith(Runnable action, String code, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    assertThat(((DomainException) e).getCode()).contains(code);
                    assertThat(e).hasMessage(message);
                });
    }

    @Test
    @DisplayName("nasce ativo, com id e sem versão")
    void registersActive() {
        var manufacturer = Manufacturer.register(HUAWEI);

        assertThat(manufacturer.getId()).isNotNull();
        assertThat(manufacturer.getName()).isEqualTo(HUAWEI);
        assertThat(manufacturer.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(manufacturer.isActive()).isTrue();
        assertThat(manufacturer.getVersion()).isEmpty();
    }

    @Test
    @DisplayName("renomeia um fabricante ativo")
    void renameChangesTheName() {
        var manufacturer = Manufacturer.register(HUAWEI);

        manufacturer.rename(ManufacturerName.of("Solis"));

        assertThat(manufacturer.getName()).isEqualTo(ManufacturerName.of("Solis"));
    }

    @Test
    @DisplayName("recusa renomear um fabricante desativado")
    void renameOfADeactivatedManufacturerFails() {
        var manufacturer = Manufacturer.register(HUAWEI);
        manufacturer.deactivate();

        assertFailsWith(
                () -> manufacturer.rename(ManufacturerName.of("Solis")),
                "manufacturer_deactivated",
                "este fabricante está desativado"
        );
        assertThat(manufacturer.getName()).isEqualTo(HUAWEI);
    }

    @Test
    @DisplayName("recusa desativar duas vezes")
    void deactivateTwiceFails() {
        var manufacturer = Manufacturer.register(HUAWEI);
        manufacturer.deactivate();

        assertThat(manufacturer.isActive()).isFalse();
        assertFailsWith(
                manufacturer::deactivate,
                "manufacturer_deactivated",
                "este fabricante está desativado"
        );
    }

    @Test
    @DisplayName("recusa reativar um fabricante ativo")
    void reactivateAnActiveOneFails() {
        var manufacturer = Manufacturer.register(HUAWEI);

        assertFailsWith(
                manufacturer::reactivate,
                "manufacturer_not_deactivated",
                "este fabricante não está desativado"
        );
    }

    @Test
    @DisplayName("reativa um fabricante desativado")
    void reactivateBringsItBackToActive() {
        var manufacturer = Manufacturer.register(HUAWEI);
        manufacturer.deactivate();

        manufacturer.reactivate();

        assertThat(manufacturer.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(manufacturer.isActive()).isTrue();
    }

    @Test
    @DisplayName("reconstitui mantendo id, versão, nome e situação")
    void reconstituteKeepsIdAndVersion() {
        var id = UUID.randomUUID();

        var manufacturer = Manufacturer.reconstitute(id, 3L, HUAWEI, CatalogStatus.DEACTIVATED);

        assertThat(manufacturer.getId()).isEqualTo(id);
        assertThat(manufacturer.getVersion()).contains(3L);
        assertThat(manufacturer.getName()).isEqualTo(HUAWEI);
        assertThat(manufacturer.getStatus()).isEqualTo(CatalogStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("nenhuma operação registra domain event")
    void registersNoDomainEvent() {
        var manufacturer = Manufacturer.register(HUAWEI);

        manufacturer.rename(ManufacturerName.of("Solis"));
        manufacturer.deactivate();
        manufacturer.reactivate();

        assertThat(manufacturer.pullEvents()).isEmpty();
    }
}
