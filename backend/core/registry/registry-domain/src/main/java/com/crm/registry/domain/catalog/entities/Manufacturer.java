package com.crm.registry.domain.catalog.entities;

import com.crm.common.domain.entities.AggregateRoot;
import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.catalog.enums.CatalogStatus;
import com.crm.registry.domain.catalog.vos.ManufacturerName;
import lombok.Getter;

import java.util.UUID;

/**
 * Fabricante de equipamento do catálogo.
 *
 * <p>O ciclo de vida (ativo, desativado) é um enum com guardas nos métodos, sem máquina de
 * estados: são dois estados e duas transições. Só um fabricante ativo pode ser renomeado. A
 * unicidade do nome é do banco, não do aggregate. Nada se apaga e nenhuma mudança gera domain event.
 */
@Getter
public class Manufacturer extends AggregateRoot {

    private ManufacturerName name;

    private CatalogStatus status;

    private Manufacturer(
            UUID id,
            Long version,
            ManufacturerName name,
            CatalogStatus status
    ) {
        super(id, version);

        this.name = name;
        this.status = status;
    }

    /** Cadastra o fabricante, que nasce ativo. */
    public static Manufacturer register(ManufacturerName name) {
        return new Manufacturer(
                UUID.randomUUID(),
                null,
                name,
                CatalogStatus.ACTIVE
        );
    }

    public static Manufacturer reconstitute(
            UUID id,
            Long version,
            ManufacturerName name,
            CatalogStatus status
    ) {
        return new Manufacturer(
                id,
                version,
                name,
                status
        );
    }

    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }

    public void rename(ManufacturerName newName) {
        requireActive();

        this.name = newName;
    }

    public void deactivate() {
        requireActive();

        this.status = CatalogStatus.DEACTIVATED;
    }

    public void reactivate() {
        if (status != CatalogStatus.DEACTIVATED)
            throw new DomainException("manufacturer_not_deactivated", "este fabricante não está desativado");

        this.status = CatalogStatus.ACTIVE;
    }

    private void requireActive() {
        if (status != CatalogStatus.ACTIVE)
            throw new DomainException("manufacturer_deactivated", "este fabricante está desativado");
    }
}
