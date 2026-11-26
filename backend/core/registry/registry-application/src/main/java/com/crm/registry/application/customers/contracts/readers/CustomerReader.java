package com.crm.registry.application.customers.contracts.readers;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;

import java.util.Optional;
import java.util.UUID;

/** Leitura de clientes, direto em projeção, sem montar o aggregate. */
public interface CustomerReader {

    Page<CustomerSummary> search(
            CustomerFilter filter,
            Pageable pageable
    );

    Optional<CustomerDetail> findDetail(UUID customerId);
}
