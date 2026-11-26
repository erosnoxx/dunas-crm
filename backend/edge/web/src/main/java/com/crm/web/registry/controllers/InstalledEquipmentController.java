package com.crm.web.registry.controllers;

import com.crm.registry.application.customers.usecases.commands.DeactivateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.InstallEquipment;
import com.crm.registry.application.customers.usecases.commands.ReactivateInstalledEquipment;
import com.crm.registry.application.customers.usecases.commands.UpdateInstalledEquipment;
import com.crm.shared.application.Actor;
import com.crm.shared.application.schemas.response.UUIDResponse;
import com.crm.web.registry.requests.InstallEquipmentRequest;
import com.crm.web.registry.requests.UpdateInstalledEquipmentRequest;
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

/** Equipamentos instalados nos locais. A checagem de papel é do use case, nunca daqui. */
@RestController
@RequiredArgsConstructor
public class InstalledEquipmentController {

    private final InstallEquipment installEquipment;
    private final UpdateInstalledEquipment updateInstalledEquipment;
    private final DeactivateInstalledEquipment deactivateInstalledEquipment;
    private final ReactivateInstalledEquipment reactivateInstalledEquipment;

    @PostMapping("/locations/{locationId}/equipment")
    public ResponseEntity<UUIDResponse> install(
            Actor actor,
            @PathVariable UUID locationId,
            @Valid @RequestBody InstallEquipmentRequest body
    ) {
        var created = installEquipment.execute(new InstallEquipment.Params(
                actor,
                locationId,
                body.modelId(),
                body.serialNumber(),
                body.installedOn()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/installed-equipment/{id}")
    public ResponseEntity<Void> update(
            Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInstalledEquipmentRequest body
    ) {
        updateInstalledEquipment.execute(new UpdateInstalledEquipment.Params(
                actor,
                id,
                body.serialNumber(),
                body.installedOn()
        ));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/installed-equipment/{id}/deactivation")
    public ResponseEntity<Void> deactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        deactivateInstalledEquipment.execute(new DeactivateInstalledEquipment.Params(actor, id));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/installed-equipment/{id}/reactivation")
    public ResponseEntity<Void> reactivate(
            Actor actor,
            @PathVariable UUID id
    ) {
        reactivateInstalledEquipment.execute(new ReactivateInstalledEquipment.Params(actor, id));

        return ResponseEntity.noContent().build();
    }
}
