package com.crm.web.registry;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.catalog.schemas.ManufacturerFilter;
import com.crm.registry.application.catalog.schemas.ManufacturerSummary;
import com.crm.registry.application.catalog.usecases.commands.DeactivateManufacturer;
import com.crm.registry.application.catalog.usecases.commands.ReactivateManufacturer;
import com.crm.registry.application.catalog.usecases.commands.RegisterManufacturer;
import com.crm.registry.application.catalog.usecases.commands.RenameManufacturer;
import com.crm.registry.application.catalog.usecases.queries.ListManufacturers;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.registry.requests.ManufacturerRequest;
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

import java.util.UUID;

/**
 * Fabricantes do catálogo. A checagem de papel é do use case, nunca daqui.
 *
 * <p>O filtro da lista é ligado direto de {@code text} e {@code status}: o Spring converte o texto
 * no enum, então o controller não conhece o domínio e valor desconhecido vira 400.
 */
@RestController
@RequestMapping("/catalog/manufacturers")
@RequiredArgsConstructor
public class ManufacturersController {

    private static final int MAX_PAGE_SIZE = 100;

    private final RegisterManufacturer registerManufacturer;
    private final RenameManufacturer renameManufacturer;
    private final DeactivateManufacturer deactivateManufacturer;
    private final ReactivateManufacturer reactivateManufacturer;
    private final ListManufacturers listManufacturers;

    @PostMapping
    public ResponseEntity<UUIDResponse> register(
            Actor actor,
            @Valid @RequestBody ManufacturerRequest body
    ) {
        var created = registerManufacturer.execute(new RegisterManufacturer.Params(actor, body.name()));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Page<ManufacturerSummary> list(
            Actor actor,
            @ModelAttribute ManufacturerFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = Pageable.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return listManufacturers.execute(new ListManufacturers.Params(
                actor,
                filter,
                pageable
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> rename(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody ManufacturerRequest body
    ) {
        renameManufacturer.execute(new RenameManufacturer.Params(
                actor,
                id,
                body.name()
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateManufacturer.execute(new DeactivateManufacturer.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateManufacturer.execute(new ReactivateManufacturer.Params(actor, id));

        return ResponseEntity.noContent().build();
    }
}
