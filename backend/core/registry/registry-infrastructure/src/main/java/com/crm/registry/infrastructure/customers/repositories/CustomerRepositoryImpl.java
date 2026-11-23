package com.crm.registry.infrastructure.customers.repositories;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.infrastructure.persistence.repositories.RepositoryImpl;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.application.customers.criteria.CustomerCriteria;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.infrastructure.catalog.repositories.UniqueConstraint;
import com.crm.registry.infrastructure.customers.mappers.CustomerMapper;
import com.crm.registry.infrastructure.customers.persistence.entities.CustomerEntity;
import com.crm.registry.infrastructure.customers.persistence.jpa.CustomerJpaRepository;
import com.crm.shared.domain.vos.identity.Document;
import jakarta.persistence.EntityManager;

import java.util.function.Supplier;

/**
 * Repository de {@link Customer} sobre JPA. Quem garante o documento único é o índice do banco:
 * dois cadastros simultâneos do mesmo documento terminam com um gravado e o outro em
 * {@code customer_document_taken}, o mesmo código da checagem do use case.
 */
public class CustomerRepositoryImpl
        extends RepositoryImpl<Customer, CustomerCriteria, CustomerEntity, CustomerJpaRepository>
        implements CustomerRepository {

    private static final String DOCUMENT_CONSTRAINT = "uk_customers_document";

    public CustomerRepositoryImpl(
            CustomerJpaRepository jpaRepository,
            EntityManager em,
            CustomerMapper mapper
    ) {
        super(
                jpaRepository,
                em,
                mapper,
                CustomerEntity.class
        );
    }

    @Override
    public boolean existsByDocument(Document document) {
        return jpaRepository.existsByDocument(document.getValue());
    }

    @Override
    protected <T> T translatingConflicts(Supplier<T> operation) {
        try {
            return super.translatingConflicts(operation);
        }
        catch (ConflictException e) {
            if (UniqueConstraint.isViolated(e, DOCUMENT_CONSTRAINT))
                throw new ConflictException(
                    "customer_document_taken",
                    "já existe um cliente com este documento",
                    e.getCause()
            );

            throw e;
        }
    }
}
