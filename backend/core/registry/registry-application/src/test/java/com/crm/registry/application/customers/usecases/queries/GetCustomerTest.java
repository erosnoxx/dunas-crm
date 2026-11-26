package com.crm.registry.application.customers.usecases.queries;

import com.crm.registry.application.customers.contracts.readers.CustomerReader;
import com.crm.registry.application.customers.schemas.CustomerDetail;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("GetCustomer")
class GetCustomerTest {

    private CustomerReader reader;
    private GetCustomer getCustomer;

    private final CustomerDetail detail = new CustomerDetail(
            UUID.randomUUID(), CPF, Document.DocumentType.CPF, "Maria Souza", null,
            "maria@x.com", null, CustomerStatus.ACTIVE, List.of());

    @BeforeEach
    void setUp() {
        reader = mock(CustomerReader.class);
        getCustomer = new GetCustomer(reader);
    }

    @Test
    @DisplayName("os três papéis recebem o detalhe do leitor")
    void eachQueryAnswersTheThreeRoles() {
        when(reader.findDetail(detail.id())).thenReturn(Optional.of(detail));

        for (var actor : List.of(ADMINISTRATOR, COMMERCIAL, TECHNICIAN))
            assertThat(getCustomer.execute(new GetCustomer.Params(actor, detail.id()))).isSameAs(detail);
    }

    @Test
    @DisplayName("cliente que não existe é customer_not_found")
    void getCustomerNotFound() {
        var unknown = UUID.randomUUID();
        when(reader.findDetail(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getCustomer.execute(new GetCustomer.Params(TECHNICIAN, unknown)))
                .satisfies(applicationCode("customer_not_found"));
    }

    @Test
    @DisplayName("ator nulo falha antes de tocar o leitor")
    void nullActorFailsBeforeTheReader() {
        assertThatThrownBy(() -> getCustomer.execute(new GetCustomer.Params(null, UUID.randomUUID())))
                .isInstanceOf(Exception.class);

        verifyNoInteractions(reader);
    }
}
