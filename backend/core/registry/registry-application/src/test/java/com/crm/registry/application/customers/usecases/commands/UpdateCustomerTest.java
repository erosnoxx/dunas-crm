package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.vos.CustomerName;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("UpdateCustomer")
class UpdateCustomerTest {

    private CustomerRepository repository;
    private UpdateCustomer updateCustomer;

    private final Customer customer = individual();

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));
        updateCustomer = new UpdateCustomer(repository);
    }

    private UpdateCustomer.Params params(UUID id, String name, String trade, String email, String phone) {
        return new UpdateCustomer.Params(ADMINISTRATOR, id, name, trade, email, phone);
    }

    @Test
    @DisplayName("edita e salva; o Params não recebe documento")
    void updateDoesNotReceiveTheDocument() {
        var document = customer.getDocument();

        updateCustomer.execute(params(customer.getId(), "Maria Lima", null, null, "+5511999999999"));

        assertThat(customer.getName()).isEqualTo(CustomerName.of("Maria Lima"));
        assertThat(customer.getDocument()).isEqualTo(document);
        assertThat(UpdateCustomer.Params.class.getRecordComponents())
                .extracting(c -> c.getName()).doesNotContain("document");
        verify(repository).save(customer);
    }

    @Test
    @DisplayName("Técnico com campo inválido recebe forbidden e o repository não é tocado")
    void technicianWithAnInvalidFieldIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> updateCustomer.execute(
                new UpdateCustomer.Params(TECHNICIAN, customer.getId(), " ", null, null, null)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Administrador e Comercial podem editar")
    void administratorAndCommercialCanWrite() {
        updateCustomer.execute(params(customer.getId(), "Um", null, "a@x.com", null));
        updateCustomer.execute(new UpdateCustomer.Params(COMMERCIAL, customer.getId(), "Dois", null, "a@x.com", null));

        verify(repository, times(2)).save(customer);
    }

    @Test
    @DisplayName("nome inválido de cliente inexistente é erro de formato e findById nunca é chamado")
    void updateWithAnInvalidNameOfAnUnknownCustomerIsABadRequest() {
        assertThatThrownBy(() -> updateCustomer.execute(params(UUID.randomUUID(), " ", null, "a@x.com", null)))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).findById(any());
    }

    @Test
    @DisplayName("cliente que não existe é customer_not_found")
    void updateNotFound() {
        var unknown = UUID.randomUUID();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateCustomer.execute(params(unknown, "Maria", null, "a@x.com", null)))
                .satisfies(applicationCode("customer_not_found"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("cliente desativado sem contato é customer_deactivated")
    void updateOfADeactivatedCustomerWithoutContactIsDeactivated() {
        customer.deactivate();

        assertThatThrownBy(() -> updateCustomer.execute(params(customer.getId(), "Maria", null, null, null)))
                .satisfies(domainCode("customer_deactivated"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("CPF com nome fantasia é trade_name_not_allowed")
    void updateCpfWithTradeNameFails() {
        assertThatThrownBy(() -> updateCustomer.execute(params(customer.getId(), "Maria", "Solar", "a@x.com", null)))
                .satisfies(domainCode("trade_name_not_allowed"));

        verify(repository, never()).save(any());
    }
}
