package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ReactivateManufacturer")
class ReactivateManufacturerTest {

    private ManufacturerRepository repository;
    private ReactivateManufacturer reactivateManufacturer;

    private final Manufacturer manufacturer = Manufacturer.register(ManufacturerName.of("Huawei"));

    @BeforeEach
    void setUp() {
        repository = mock(ManufacturerRepository.class);

        manufacturer.deactivate();
        when(repository.findById(manufacturer.getId())).thenReturn(Optional.of(manufacturer));

        reactivateManufacturer = new ReactivateManufacturer(repository);
    }

    @Test
    @DisplayName("salva o fabricante reativado")
    void savesTheNewStatus() {
        reactivateManufacturer.execute(new ReactivateManufacturer.Params(ADMINISTRATOR, manufacturer.getId()));

        var saved = ArgumentCaptor.forClass(Manufacturer.class);
        verify(repository).save(saved.capture());

        assertThat(saved.getValue().getId()).isEqualTo(manufacturer.getId());
        assertThat(saved.getValue().getStatus()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> reactivateManufacturer.execute(
                new ReactivateManufacturer.Params(TECHNICIAN, manufacturer.getId())))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Comercial também pode")
    void commercialCanWrite() {
        reactivateManufacturer.execute(new ReactivateManufacturer.Params(COMMERCIAL, manufacturer.getId()));

        verify(repository).save(manufacturer);
    }

    @Test
    @DisplayName("fabricante que não existe é manufacturer_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactivateManufacturer.execute(new ReactivateManufacturer.Params(ADMINISTRATOR, unknown)))
                .satisfies(applicationCode("manufacturer_not_found"));

        verify(repository, never()).save(any());
    }
}
