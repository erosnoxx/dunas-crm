package com.crm.web.registry;

import com.crm.registry.application.customers.usecases.commands.DeactivateLocation;
import com.crm.registry.application.customers.usecases.commands.ReactivateLocation;
import com.crm.registry.application.customers.usecases.commands.RegisterLocation;
import com.crm.registry.application.customers.usecases.commands.UpdateLocation;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.registry.requests.LocationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Locais do cliente. A checagem de papel é do use case, nunca daqui. */
@RestController
@RequiredArgsConstructor
public class LocationsController {

    private final RegisterLocation registerLocation;
    private final UpdateLocation updateLocation;
    private final DeactivateLocation deactivateLocation;
    private final ReactivateLocation reactivateLocation;

    @PostMapping("/customers/{customerId}/locations")
    public ResponseEntity<UUIDResponse> register(
            Actor actor,
            @PathVariable UUID customerId,
            @Valid @RequestBody LocationRequest body
    ) {
        var created = registerLocation.execute(new RegisterLocation.Params(
                actor,
                customerId,
                body.name(),
                body.address().toInput()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<Void> update(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody LocationRequest body
    ) {
        updateLocation.execute(new UpdateLocation.Params(
                actor,
                id,
                body.name(),
                body.address().toInput()
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/locations/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateLocation.execute(new DeactivateLocation.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/locations/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateLocation.execute(new ReactivateLocation.Params(actor, id));

        return ResponseEntity.noContent().build();
    }
}
