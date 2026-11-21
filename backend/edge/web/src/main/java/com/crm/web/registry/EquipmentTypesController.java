package com.crm.web.registry;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.registry.application.catalog.schemas.EquipmentTypeSummary;
import com.crm.registry.application.catalog.schemas.StatusFilter;
import com.crm.registry.application.catalog.usecases.commands.DeactivateEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.ReactivateEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.RegisterEquipmentType;
import com.crm.registry.application.catalog.usecases.commands.UpdateEquipmentType;
import com.crm.registry.application.catalog.usecases.queries.ListEquipmentTypes;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.registry.requests.CharacteristicRequest;
import com.crm.web.registry.requests.EquipmentTypeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tipos de equipamento do catálogo. A checagem de papel é do use case, nunca daqui.
 *
 * <p>O {@code status} da lista chega como o enum do use case (valor desconhecido vira 400) e, sem
 * ele, segue nulo: quem assume {@code ACTIVE} é o use case.
 */
@RestController
@RequestMapping("/catalog/equipment-types")
@RequiredArgsConstructor
public class EquipmentTypesController {

    private static final int MAX_PAGE_SIZE = 100;

    private final RegisterEquipmentType registerEquipmentType;
    private final UpdateEquipmentType updateEquipmentType;
    private final DeactivateEquipmentType deactivateEquipmentType;
    private final ReactivateEquipmentType reactivateEquipmentType;
    private final ListEquipmentTypes listEquipmentTypes;

    @PostMapping
    public ResponseEntity<UUIDResponse> register(
            Actor actor,
            @Valid @RequestBody EquipmentTypeRequest body
    ) {
        var created = registerEquipmentType.execute(new RegisterEquipmentType.Params(
                actor,
                body.name(),
                characteristicOf(body.characteristic())
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<EquipmentTypeSummary> list(
            Actor actor,
            @RequestParam(required = false) StatusFilter status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = Pageable.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return listEquipmentTypes.execute(new ListEquipmentTypes.Params(
                actor,
                status,
                pageable
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody EquipmentTypeRequest body
    ) {
        updateEquipmentType.execute(new UpdateEquipmentType.Params(
                actor,
                id,
                body.name(),
                characteristicOf(body.characteristic())
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateEquipmentType.execute(new DeactivateEquipmentType.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateEquipmentType.execute(new ReactivateEquipmentType.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    private static CharacteristicInput characteristicOf(CharacteristicRequest request) {
        return new CharacteristicInput(
                request.name(),
                request.unit(),
                request.required()
        );
    }
}
