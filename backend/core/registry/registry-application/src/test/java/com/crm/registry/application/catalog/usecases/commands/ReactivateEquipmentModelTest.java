package com.crm.registry.application.catalog.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.catalog.contracts.repositories.EquipmentModelRepository;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.catalog.vos.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ModelName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
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

@DisplayName("ReactivateEquipmentModel")
class ReactivateEquipmentModelTest {

    private EquipmentModelRepository repository;
    private ReactivateEquipmentModel reactivateEquipmentModel;

    private final EquipmentModel model = EquipmentModel.reconstitute(
            UUID.randomUUID(), 1L, UUID.randomUUID(), UUID.randomUUID(),
            ModelName.of("SUN2000"), List.of(), CatalogStatus.DEACTIVATED
    );

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentModelRepository.class);

        when(repository.findById(model.getId())).thenReturn(Optional.of(model));

        reactivateEquipmentModel = new ReactivateEquipmentModel(repository);
    }

    @Test
    @DisplayName("salva o modelo reativado")
    void savesTheNewStatus() {
        reactivateEquipmentModel.execute(new ReactivateEquipmentModel.Params(COMMERCIAL, model.getId()));

        assertThat(model.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        verify(repository).save(model);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbidden() {
        assertThatThrownBy(() -> reactivateEquipmentModel.execute(
                new ReactivateEquipmentModel.Params(TECHNICIAN, model.getId())))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("modelo ativo é model_not_deactivated")
    void notDeactivated() {
        model.reactivate();

        assertThatThrownBy(() -> reactivateEquipmentModel.execute(
                new ReactivateEquipmentModel.Params(ADMINISTRATOR, model.getId())))
                .satisfies(domainCode("model_not_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("modelo que não existe é model_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactivateEquipmentModel.execute(
                new ReactivateEquipmentModel.Params(ADMINISTRATOR, unknown)))
                .satisfies(applicationCode("model_not_found"));

        verify(repository, never()).save(any());
    }
}
