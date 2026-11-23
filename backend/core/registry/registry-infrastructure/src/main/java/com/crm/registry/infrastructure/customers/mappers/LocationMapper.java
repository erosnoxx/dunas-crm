package com.crm.registry.infrastructure.customers.mappers;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.registry.domain.customers.entities.Location;
import com.crm.registry.domain.customers.vos.LocationName;
import com.crm.registry.infrastructure.customers.persistence.entities.LocationEntity;
import com.crm.shared.domain.vos.address.Address;
import com.crm.shared.domain.vos.address.AddressComplement;
import com.crm.shared.domain.vos.address.AddressNumber;
import com.crm.shared.domain.vos.address.CityName;
import com.crm.shared.domain.vos.address.Neighborhood;
import com.crm.shared.domain.vos.address.Street;
import com.crm.shared.domain.vos.address.ZipCode;

/**
 * Converte {@link Location} em {@link LocationEntity} e de volta; o endereço vira colunas da
 * linha. Complemento ausente vira coluna nula; {@code createdAt} e {@code updatedAt} nunca chegam
 * ao domínio.
 */
public class LocationMapper implements EntityMapper<Location, LocationEntity> {

    @Override
    public Location toDomain(LocationEntity entity) {
        return Location.reconstitute(
                entity.getId(),
                entity.getVersion(),
                entity.getCustomerId(),
                LocationName.of(entity.getName()),
                new Address(
                        ZipCode.of(entity.getZipCode()),
                        Street.of(entity.getStreet()),
                        AddressNumber.of(entity.getNumber()),
                        entity.getComplement() == null ? null : AddressComplement.of(entity.getComplement()),
                        Neighborhood.of(entity.getNeighborhood()),
                        CityName.of(entity.getCity()),
                        entity.getState()
                ),
                entity.getStatus()
        );
    }

    @Override
    public LocationEntity toPersistence(Location location) {
        var address = location.getAddress();
        var entity = new LocationEntity();

        entity.setId(location.getId());
        entity.setVersion(location.getVersion().orElse(null));
        entity.setCustomerId(location.getCustomerId());
        entity.setName(location.getName().getValue());
        entity.setZipCode(address.zipCode().getValue());
        entity.setStreet(address.street().getValue());
        entity.setNumber(address.number().getValue());
        entity.setComplement(address.complement() == null ? null : address.complement().getValue());
        entity.setNeighborhood(address.neighborhood().getValue());
        entity.setCity(address.city().getValue());
        entity.setState(address.state());
        entity.setStatus(location.getStatus());

        return entity;
    }
}
