package com.crm.registry.application.customers.usecases.commands;

import com.crm.common.application.exceptions.ConflictException;
import com.crm.common.application.exceptions.ForbiddenException;
import com.crm.common.domain.exceptions.ValidationException;
import com.crm.registry.application.customers.contracts.repositories.CustomerRepository;
import com.crm.registry.domain.customers.entities.Customer;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

@DisplayName("RegisterCustomer")
class RegisterCustomerTest {

    private CustomerRepository repository;
    private RegisterCustomer registerCustomer;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        registerCustomer = new RegisterCustomer(repository);
    }

    private RegisterCustomer.Params params(String document, String name, String trade, String email, String phone) {
        return new RegisterCustomer.Params(ADMINISTRATOR, document, name, trade, email, phone);
    }

    @Test
    @DisplayName("cadastra, salva e devolve o id")
    void registersAndReturnsTheId() {
        var response = registerCustomer.execute(params(CPF, "Maria Souza", null, "maria@x.com", null));

        var saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(saved.capture());

        assertThat(response.id()).isEqualTo(saved.getValue().getId());
        assertThat(saved.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("Técnico com campo inválido recebe forbidden e o repository não é tocado")
    void technicianWithAnInvalidFieldIsForbiddenAndTheRepositoryIsUntouched() {
        assertThatThrownBy(() -> registerCustomer.execute(
                new RegisterCustomer.Params(TECHNICIAN, "123", " ", null, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(applicationCode("forbidden"));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Administrador e Comercial podem cadastrar")
    void administratorAndCommercialCanWrite() {
        registerCustomer.execute(params(CPF, "Maria", null, "maria@x.com", null));
        registerCustomer.execute(new RegisterCustomer.Params(COMMERCIAL, CNPJ, "Empresa", null, null, "+5511999999999"));

        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("documento repetido com nome inválido é erro de formato e existsByDocument nunca é chamado")
    void repeatedDocumentWithAnInvalidNameIsABadRequest() {
        when(repository.existsByDocument(Document.of(CPF))).thenReturn(true);

        assertThatThrownBy(() -> registerCustomer.execute(params(CPF, " ", null, "maria@x.com", null)))
                .isInstanceOf(ValidationException.class);

        verify(repository, never()).existsByDocument(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("documento repetido sem contato é contact_required, não o conflito")
    void repeatedDocumentWithoutContactIsContactRequired() {
        when(repository.existsByDocument(Document.of(CPF))).thenReturn(true);

        assertThatThrownBy(() -> registerCustomer.execute(params(CPF, "Maria", null, null, null)))
                .satisfies(domainCode("contact_required"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("documento repetido é customer_document_taken, sem o documento na mensagem, e nada é salvo")
    void repeatedDocumentIsAConflictAndNothingIsSaved() {
        when(repository.existsByDocument(Document.of(CPF))).thenReturn(true);

        assertThatThrownBy(() -> registerCustomer.execute(params(CPF, "Maria", null, "maria@x.com", null)))
                .isInstanceOf(ConflictException.class)
                .satisfies(applicationCode("customer_document_taken"))
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("529"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("nome fantasia em branco é erro de formato")
    void blankTradeNameIsABadRequest() {
        assertThatThrownBy(() -> registerCustomer.execute(params(CNPJ, "Empresa", "  ", "a@x.com", null)))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("nome fantasia nulo é ausente")
    void nullTradeNameIsAbsent() {
        registerCustomer.execute(params(CNPJ, "Empresa", null, "a@x.com", null));

        var saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(saved.capture());

        assertThat(saved.getValue().getTradeName()).isEmpty();
    }
}
