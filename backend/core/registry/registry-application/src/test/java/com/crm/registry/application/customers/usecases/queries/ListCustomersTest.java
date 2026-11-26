package com.crm.registry.application.customers.usecases.queries;

import com.crm.common.application.pages.Page;
import com.crm.common.application.pages.Pageable;
import com.crm.registry.application.customers.contracts.readers.CustomerReader;
import com.crm.registry.application.customers.schemas.CustomerFilter;
import com.crm.registry.application.customers.schemas.CustomerStatusFilter;
import com.crm.registry.application.customers.schemas.CustomerSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.crm.registry.application.customers.CustomersFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ListCustomers")
class ListCustomersTest {

    private CustomerReader reader;
    private ListCustomers listCustomers;

    private final Pageable pageable = Pageable.of(0, 10, "name", "ASC");
    private final Page<CustomerSummary> page = new Page<>(List.of(), 0, 10, 0, 0);

    @BeforeEach
    void setUp() {
        reader = mock(CustomerReader.class);
        listCustomers = new ListCustomers(reader);
    }

    @Test
    @DisplayName("os três papéis recebem a página do leitor")
    void eachQueryAnswersTheThreeRoles() {
        var filter = new CustomerFilter("maria", CustomerStatusFilter.ALL);
        when(reader.search(filter, pageable)).thenReturn(page);

        for (var actor : List.of(ADMINISTRATOR, COMMERCIAL, TECHNICIAN))
            assertThat(listCustomers.execute(new ListCustomers.Params(actor, filter, pageable))).isSameAs(page);
    }

    @Test
    @DisplayName("filtro sem status chega ao leitor como ACTIVE")
    void nullStatusFilterReachesTheReaderAsActive() {
        listCustomers.execute(new ListCustomers.Params(
                ADMINISTRATOR, new CustomerFilter("maria", null), pageable));

        verify(reader).search(new CustomerFilter("maria", CustomerStatusFilter.ACTIVE), pageable);
    }

    @Test
    @DisplayName("ator nulo falha antes de tocar o leitor")
    void nullActorFailsBeforeTheReader() {
        assertThatThrownBy(() -> listCustomers.execute(
                new ListCustomers.Params(null, new CustomerFilter(null, null), pageable)))
                .isInstanceOf(Exception.class);

        verifyNoInteractions(reader);
    }
}
