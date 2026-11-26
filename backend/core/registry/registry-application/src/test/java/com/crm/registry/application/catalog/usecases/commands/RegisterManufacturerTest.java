package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.ManufacturerRepository;
import com.crm.registry.domain.catalog.entities.Manufacturer;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

@DisplayName("RegisterManufacturer")
class RegisterManufacturerTest {

    private ManufacturerRepository repository;
    private RegisterManufacturer registerManufacturer;

    @BeforeEach
    void setUp() {
        repository = mock(ManufacturerRepository.class);

        registerManufacturer = new RegisterManufacturer(repository);
    }

    @Test
    @DisplayName("cadastra um fabricante ativo e devolve o id")
    void registersAndReturnsTheId() {
        var response = registerManufacturer.execute(new RegisterManufacturer.Params(ADMINISTRATOR, "Huawei"));

        var saved = ArgumentCaptor.forClass(Manufacturer.class);
        verify(repository).existsByName(ManufacturerName.of("Huawei"), null);
        verify(repository).save(saved.capture());

        assertThat(response.id()).isEqualTo(saved.getValue().getId());
        assertThat(saved.getValue().getName()).isEqualTo(ManufacturerName.of("Huawei"));
        assertThat(saved.getValue().getStatus()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> registerManufacturer.execute(new RegisterManufacturer.Params(TECHNICIAN, "Huawei")))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Administrador e Comercial podem cadastrar")
    void administratorAndCommercialCanWrite() {
        registerManufacturer.execute(new RegisterManufacturer.Params(ADMINISTRATOR, "Huawei"));
        registerManufacturer.execute(new RegisterManufacturer.Params(COMMERCIAL, "Solis"));

        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("nome repetido (em outra caixa) é manufacturer_name_taken e nada é salvo")
    void refusesARepeatedNameInAnotherCase() {
        when(repository.existsByName(ManufacturerName.of("HUAWEI"), null)).thenReturn(true);

        assertThatThrownBy(() -> registerManufacturer.execute(new RegisterManufacturer.Params(COMMERCIAL, "HUAWEI")))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("manufacturer_name_taken"));

        verify(repository, never()).save(any());
    }
}
