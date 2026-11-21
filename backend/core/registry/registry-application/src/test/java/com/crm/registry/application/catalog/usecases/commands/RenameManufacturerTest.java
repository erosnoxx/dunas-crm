package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.catalog.CatalogFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RenameManufacturer")
class RenameManufacturerTest {

    private ManufacturerRepository repository;
    private RenameManufacturer renameManufacturer;

    private final Manufacturer manufacturer = Manufacturer.register(ManufacturerName.of("Huawei"));

    @BeforeEach
    void setUp() {
        repository = mock(ManufacturerRepository.class);

        when(repository.findById(manufacturer.getId())).thenReturn(Optional.of(manufacturer));

        renameManufacturer = new RenameManufacturer(repository);
    }

    @Test
    @DisplayName("renomeia e salva")
    void renamesAndSaves() {
        renameManufacturer.execute(new RenameManufacturer.Params(ADMINISTRATOR, manufacturer.getId(), "Huawei Solar"));

        assertThat(manufacturer.getName()).isEqualTo(ManufacturerName.of("Huawei Solar"));
        verify(repository).save(manufacturer);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> renameManufacturer.execute(
                new RenameManufacturer.Params(TECHNICIAN, manufacturer.getId(), "Outro")))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Administrador e Comercial podem renomear")
    void administratorAndCommercialCanWrite() {
        renameManufacturer.execute(new RenameManufacturer.Params(ADMINISTRATOR, manufacturer.getId(), "Um"));
        renameManufacturer.execute(new RenameManufacturer.Params(COMMERCIAL, manufacturer.getId(), "Dois"));

        verify(repository, times(2)).save(manufacturer);
    }

    @Test
    @DisplayName("nome já usado por outro fabricante é manufacturer_name_taken e nada é salvo")
    void refusesARepeatedNameInAnotherCase() {
        when(repository.existsByName(ManufacturerName.of("SOLIS"), manufacturer.getId())).thenReturn(true);

        assertThatThrownBy(() -> renameManufacturer.execute(
                new RenameManufacturer.Params(COMMERCIAL, manufacturer.getId(), "SOLIS")))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("manufacturer_name_taken"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("renomear para o mesmo nome não conflita consigo: o repository recebe o id do próprio fabricante")
    void renameToTheSameNameDoesNotConflictWithItself() {
        renameManufacturer.execute(new RenameManufacturer.Params(ADMINISTRATOR, manufacturer.getId(), "HUAWEI"));

        verify(repository).existsByName(ManufacturerName.of("HUAWEI"), manufacturer.getId());
        verify(repository).save(manufacturer);
    }

    @Test
    @DisplayName("fabricante que não existe é manufacturer_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renameManufacturer.execute(new RenameManufacturer.Params(ADMINISTRATOR, unknown, "Novo")))
                .satisfies(applicationCode("manufacturer_not_found"));

        verify(repository, never()).save(any());
    }
}
