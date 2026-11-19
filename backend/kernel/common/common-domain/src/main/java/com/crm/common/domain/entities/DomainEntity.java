package com.crm.common.domain.entities;

import com.crm.common.domain.exceptions.DomainException;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio, identificada por {@link UUID}: duas instâncias são iguais quando são da
 * mesma classe e têm o mesmo id, independente dos demais atributos.
 */
@Getter
public abstract class DomainEntity {
    private final UUID id;

    protected DomainEntity(UUID id) {
        if (id == null)
            throw new DomainException("id da entidade não pode ser nulo");

        this.id = id;
    }

    protected DomainEntity() {
        this.id = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (other == null || getClass() != other.getClass())
            return false;

        return id.equals(((DomainEntity) other).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
