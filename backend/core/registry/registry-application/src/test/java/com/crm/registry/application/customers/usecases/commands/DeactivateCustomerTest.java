package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DeactivateCustomer")
class DeactivateCustomerTest {

    private CustomerRepository repository;
    private DeactivateCustomer deactivateCustomer;

    private final Customer customer = individual();

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        deactivateCustomer = new DeactivateCustomer(repository);
    }

    @Test
    @DisplayName("salva o cliente desativado")
    void savesTheNewStatus() {
        deactivateCustomer.execute(new DeactivateCustomer.Params(ADMINISTRATOR, customer.getId()));

        verify(repository).save(customer);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("Técnico recebe forbidden e o repository não é tocado")
    void technicianIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> deactivateCustomer.execute(new DeactivateCustomer.Params(TECHNICIAN, customer.getId())))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("cliente que não existe é customer_not_found")
    void notFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateCustomer.execute(new DeactivateCustomer.Params(COMMERCIAL, unknown)))
                .satisfies(applicationCode("customer_not_found"));

        verify(repository, never()).save(any());
    }
}
