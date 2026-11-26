package com.crm.registry.application.customers.contracts.repositories;

import com.crm.common.application.repository.Repository;
import com.crm.registry.application.customers.criteria.CustomerCriteria;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.shared.domain.vos.identity.Document;

/** Persistência do aggregate {@link Customer}. */
public interface CustomerRepository extends Repository<Customer, CustomerCriteria> {

    /** Diz se já existe cliente com este documento; o documento é imutável, então não há id a ignorar. */
    boolean existsByDocument(Document document);
}
