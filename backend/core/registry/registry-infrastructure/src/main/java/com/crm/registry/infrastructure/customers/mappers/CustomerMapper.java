package com.crm.registry.infrastructure.customers.mappers;

import com.crm.common.infrastructure.persistence.repositories.EntityMapper;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.registry.infrastructure.customers.persistence.entities.CustomerEntity;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;

/**
 * Converte {@link Customer} em {@link CustomerEntity} e de volta. Opcional ausente vira coluna
 * nula; {@code createdAt} e {@code updatedAt} são só da persistência e nunca chegam ao domínio.
 */
public class CustomerMapper implements EntityMapper<Customer, CustomerEntity> {

    @Override
    public Customer toDomain(CustomerEntity entity) {
        return Customer.reconstitute(
                entity.getId(),
                entity.getVersion(),
                Document.of(entity.getDocument()),
                CustomerName.of(entity.getName()),
                entity.getTradeName() == null ? null : CustomerName.of(entity.getTradeName()),
                entity.getEmail() == null ? null : Email.of(entity.getEmail()),
                entity.getPhone() == null ? null : Phone.of(entity.getPhone()),
                entity.getStatus()
        );
    }

    @Override
    public CustomerEntity toPersistence(Customer customer) {
        var entity = new CustomerEntity();

        entity.setId(customer.getId());
        entity.setVersion(customer.getVersion().orElse(null));
        entity.setDocument(customer.getDocument().getValue());
        entity.setName(customer.getName().getValue());
        entity.setTradeName(customer.getTradeName().map(CustomerName::getValue).orElse(null));
        entity.setEmail(customer.getEmail().map(Email::getValue).orElse(null));
        entity.setPhone(customer.getPhone().map(Phone::getValue).orElse(null));
        entity.setStatus(customer.getStatus());

        return entity;
    }
}
