package com.crm.registry.domain.customers.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.entities.EquipmentModel;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.InstallationDate;
import com.crm.registry.domain.customers.vos.SerialNumber;

import java.util.Optional;
import java.util.UUID;

/**
 * Equipamento instalado em um local, de um modelo do catálogo.
 *
 * <p>Aggregate próprio: guarda só {@code locationId} e {@code modelId} (imutáveis). Um equipamento
 * novo exige local ativo, cliente ativo (sem propagação, ADR-6) e modelo ativo; o fabricante do
 * modelo não é conferido. Editar, desativar e reativar não recebem local, cliente nem modelo
 * (ADR-30). O ciclo de vida é um enum com guardas; nada se apaga e nenhuma mudança gera domain event.
 */
public class InstalledEquipment extends AggregateRoot {

    private final UUID locationId;

    private final UUID modelId;

    private SerialNumber serialNumber;

    private InstallationDate installedOn;

    private CustomerStatus status;

    private InstalledEquipment(
            UUID id,
            Long version,
            UUID locationId,
            UUID modelId,
            SerialNumber serialNumber,
            InstallationDate installedOn,
            CustomerStatus status
    ) {
        super(id, version);

        this.locationId = locationId;
        this.modelId = modelId;
        this.serialNumber = serialNumber;
        this.installedOn = installedOn;
        this.status = status;
    }

    /** Instala o equipamento, que nasce ativo; {@code installedOn} pode ser {@code null}. */
    public static InstalledEquipment install(
            Location location,
            Customer customer,
            EquipmentModel model,
            SerialNumber serialNumber,
            InstallationDate installedOn
    ) {
        if (!location.getCustomerId().equals(customer.getId()))
            throw new DomainException("o cliente informado não é o dono do local");
        if (!location.isActive())
            throw new DomainException("location_deactivated", "este local está desativado");
        if (!customer.isActive())
            throw new DomainException("customer_deactivated", "este cliente está desativado");
        if (!model.isActive())
            throw new DomainException("model_deactivated", "este modelo está desativado");

        return new InstalledEquipment(
                UUID.randomUUID(),
                null,
                location.getId(),
                model.getId(),
                serialNumber,
                installedOn,
                CustomerStatus.ACTIVE
        );
    }

    /** Reconstrução do banco: não valida de novo, não registra evento. */
    public static InstalledEquipment reconstitute(
            UUID id,
            Long version,
            UUID locationId,
            UUID modelId,
            SerialNumber serialNumber,
            InstallationDate installedOn,
            CustomerStatus status
    ) {
        return new InstalledEquipment(
                id,
                version,
                locationId,
                modelId,
                serialNumber,
                installedOn,
                status
        );
    }

    public UUID getLocationId() {
        return locationId;
    }

    public UUID getModelId() {
        return modelId;
    }

    public SerialNumber getSerialNumber() {
        return serialNumber;
    }

    public Optional<InstallationDate> getInstalledOn() {
        return Optional.ofNullable(installedOn);
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    /** Substitui número de série e data de instalação (esta pode ser {@code null}). */
    public void edit(SerialNumber serialNumber, InstallationDate installedOn) {
        requireActive();

        this.serialNumber = serialNumber;
        this.installedOn = installedOn;
    }

    public void deactivate() {
        requireActive();

        this.status = CustomerStatus.DEACTIVATED;
    }

    public void reactivate() {
        if (status != CustomerStatus.DEACTIVATED)
            throw new DomainException("installed_equipment_not_deactivated", "este equipamento não está desativado");

        this.status = CustomerStatus.ACTIVE;
    }

    private void requireActive() {
        if (status != CustomerStatus.ACTIVE)
            throw new DomainException("installed_equipment_deactivated", "este equipamento está desativado");
    }
}
