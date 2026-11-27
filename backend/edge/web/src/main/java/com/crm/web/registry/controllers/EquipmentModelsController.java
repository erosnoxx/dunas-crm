package com.crm.web.registry.controllers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.EquipmentModelFilter;
import com.crm.registry.application.catalog.schemas.EquipmentModelSummary;
import com.crm.registry.application.catalog.schemas.ValueInput;
import com.crm.registry.application.catalog.usecases.commands.DeactivateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.ReactivateEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.RegisterEquipmentModel;
import com.crm.registry.application.catalog.usecases.commands.UpdateEquipmentModel;
import com.crm.registry.application.catalog.usecases.queries.GetEquipmentModel;
import com.crm.registry.application.catalog.usecases.queries.ListEquipmentModels;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.registry.requests.RegisterEquipmentModelRequest;
import com.crm.web.registry.requests.UpdateEquipmentModelRequest;
import com.crm.web.registry.requests.ValueRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Modelos de equipamento do catálogo. A checagem de papel é do use case, nunca daqui.
 *
 * <p>O filtro da lista é ligado direto de {@code text}, {@code typeId} e {@code status}: o Spring
 * converte o texto nos tipos, então o controller não conhece o domínio e valor inválido vira 400.
 */
@RestController
@RequestMapping("/catalog/equipment-models")
@RequiredArgsConstructor
public class EquipmentModelsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final RegisterEquipmentModel registerEquipmentModel;
    private final UpdateEquipmentModel updateEquipmentModel;
    private final DeactivateEquipmentModel deactivateEquipmentModel;
    private final ReactivateEquipmentModel reactivateEquipmentModel;
    private final ListEquipmentModels listEquipmentModels;
    private final GetEquipmentModel getEquipmentModel;

    @PostMapping
    public ResponseEntity<UUIDResponse> register(
            Actor actor,
            @Valid @RequestBody RegisterEquipmentModelRequest body
    ) {
        var created = registerEquipmentModel.execute(new RegisterEquipmentModel.Params(
                actor,
                body.manufacturerId(),
                body.typeId(),
                body.name(),
                valuesOf(body.values())
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<EquipmentModelSummary> list(
            Actor actor,
            @ModelAttribute EquipmentModelFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = Pageable.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return listEquipmentModels.execute(new ListEquipmentModels.Params(
                actor,
                filter,
                pageable
        ));
    }

    @GetMapping("/{id}")
    public EquipmentModelSummary detail(
            Actor actor,
            @PathVariable UUID id
    ) {
        return getEquipmentModel.execute(new GetEquipmentModel.Params(actor, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEquipmentModelRequest body
    ) {
        updateEquipmentModel.execute(new UpdateEquipmentModel.Params(
                actor,
                id,
                body.name(),
                valuesOf(body.values())
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateEquipmentModel.execute(new DeactivateEquipmentModel.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateEquipmentModel.execute(new ReactivateEquipmentModel.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    private static List<ValueInput> valuesOf(List<ValueRequest> values) {
        return values.stream()
                .map(value -> new ValueInput(value.definitionId(), value.amount()))
                .toList();
    }
}
